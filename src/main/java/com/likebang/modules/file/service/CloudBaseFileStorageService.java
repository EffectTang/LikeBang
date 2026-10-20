package com.likebang.modules.file.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * CloudBase 传统模式云存储图片存储（后端代理上传）。
 * <p>
 * 流程遵循官方 HTTP API：先调 {@code get-objects-upload-info} 用 API Key（管理员·长期有效）
 * 换取 COS 临时上传签名，再把文件字节直传 COS；成功后不落临时签名地址（会过期），
 * 而是用固定的公有读 CDN 域名 {@code likebang.file.cloudbase.cdn-base-url} 拼出永久 https 链接返回，
 * 由业务侧落库。安全规则须为“所有用户可读”，公网 {@code <img>}/小程序才能长期直读，前端渲染零改动。
 * <p>
 * 与 {@link FileStorageService} 互斥存在，通过 {@code likebang.file.storage-mode=cloudbase} 启用。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "likebang.file.storage-mode", havingValue = "cloudbase")
public class CloudBaseFileStorageService implements ImageStorageService {

    /** 上传凭证接口一次性直传的超时（毫秒），网络抖动时快速失败并给友好文案 */
    private static final int TIMEOUT_MILLIS = 10_000;

    /** CloudBase 环境 ID，用于拼开放接口网关域名 */
    @Value("${likebang.file.cloudbase.env-id:}")
    private String envId;

    /** API Key（管理员权限、长期有效），仅服务端持有，严禁下发客户端 */
    @Value("${likebang.file.cloudbase.api-key:}")
    private String apiKey;

    /** 公有读 CDN 域名前缀，如 https://xxx.tcb.qcloud.la/（缺失将无法拼永久链接） */
    @Value("${likebang.file.cloudbase.cdn-base-url:}")
    private String cdnBaseUrl;

    /** 云存储对象目录前缀，默认 images；object 路径不能以 / 开头（官方规范） */
    @Value("${likebang.file.cloudbase.object-prefix:images}")
    private String objectPrefix;

    @PostConstruct
    void init() {
        log.info("图片存储模式: cloudbase（env-id={}, object-prefix={}）", envId, objectPrefix);
    }

    @Override
    public String storeImage(MultipartFile file) {
        String extension = ImageValidation.requireValidExtension(file);
        // objectId：{prefix}/{yyyyMM}/{uuid}.ext，不以 / 开头、纯英文数字，规避编码与规范问题
        String objectId = objectPrefix + "/" + ImageValidation.newRelativeObjectPath(extension);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            log.error("读取上传文件失败: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "图片保存失败，请重试");
        }

        JSONObject uploadInfo = requestUploadInfo(objectId);
        putToCos(uploadInfo, bytes);

        return StrUtil.appendIfMissing(normalizedCdnBase(), "/") + objectId;
    }

    /**
     * 调开放接口获取该 objectId 的 COS 直传签名信息。
     */
    private JSONObject requestUploadInfo(String objectId) {
        requireConfig();
        String url = gatewayUrl("get-objects-upload-info");
        String body = JSONUtil.toJsonStr(java.util.List.of(java.util.Map.of("objectId", objectId)));

        JSONArray result;
        try (HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(TIMEOUT_MILLIS)
                .body(body)
                .execute()) {
            if (!response.isOk()) {
                log.error("获取云存储上传信息失败: status={}, body={}", response.getStatus(), response.body());
                throw new BusinessException(ResultCode.ERROR.getCode(), "图片保存失败，请重试");
            }
            result = JSONUtil.parseArray(response.body());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用云存储上传信息接口异常: objectId={}", objectId, e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "图片保存失败，请重试");
        }

        if (result == null || result.isEmpty()) {
            log.error("云存储上传信息为空: objectId={}", objectId);
            throw new BusinessException(ResultCode.ERROR.getCode(), "图片保存失败，请重试");
        }
        return result.getJSONObject(0);
    }

    /**
     * 用签名信息把文件字节直传 COS。四个头部分别取自上传信息响应字段。
     */
    private void putToCos(JSONObject uploadInfo, byte[] bytes) {
        String uploadUrl = uploadInfo.getStr("uploadUrl");
        String authorization = uploadInfo.getStr("authorization");
        String token = uploadInfo.getStr("token");
        String cloudObjectMeta = uploadInfo.getStr("cloudObjectMeta");
        if (StrUtil.hasBlank(uploadUrl, authorization, token)) {
            log.error("云存储上传信息字段缺失: {}", uploadInfo);
            throw new BusinessException(ResultCode.ERROR.getCode(), "图片保存失败，请重试");
        }

        try (HttpResponse response = HttpRequest.put(uploadUrl)
                .header("Authorization", authorization)
                .header("X-Cos-Security-Token", token)
                .header("X-Cos-Meta-Fileid", StrUtil.nullToEmpty(cloudObjectMeta))
                .timeout(TIMEOUT_MILLIS)
                .body(bytes)
                .execute()) {
            if (!response.isOk()) {
                log.error("直传 COS 失败: status={}, body={}", response.getStatus(), response.body());
                throw new BusinessException(ResultCode.ERROR.getCode(), "图片保存失败，请重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("直传 COS 异常", e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "图片保存失败，请重试");
        }
    }

    private String normalizedCdnBase() {
        return StrUtil.removeSuffix(cdnBaseUrl.trim(), "/");
    }

    /**
     * 删除已孤立的云对象：仅处理属于本站 CDN 域名的 https 链接（http 外链/微信头像一律忽略）。
     * 从链接裁出 objectId，拼成 cloudObjectId（格式 {@code cloud://{envId}.{bucket}/{objectId}}）后调
     * {@code delete-objects}。删除是 best-effort，失败仅告警不抛出。
     */
    @Override
    public void deleteImage(String url) {
        String cdnBase = normalizedCdnBase();
        if (StrUtil.isBlank(url) || !url.startsWith(cdnBase + "/")) {
            return;
        }
        String objectId = url.substring(cdnBase.length() + 1);
        if (StrUtil.isBlank(objectId)) {
            return;
        }
        try {
            requireConfig();
            String cloudObjectId = StrUtil.format("cloud://{}.{}", envId, extractBucket()) + "/" + objectId;
            String body = JSONUtil.toJsonStr(java.util.List.of(java.util.Map.of("cloudObjectId", cloudObjectId)));
            try (HttpResponse response = HttpRequest.post(gatewayUrl("delete-objects"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_MILLIS)
                    .body(body)
                    .execute()) {
                if (!response.isOk()) {
                    log.warn("云存储删除对象返回非 2xx（忽略）: status={}, body={}", response.getStatus(), response.body());
                }
            }
        } catch (Exception e) {
            log.warn("云存储图片删除失败（忽略）: {}", url, e);
        }
    }

    /** 开放接口网关地址：https://{envId}.api.tcloudbasegateway.com/v1/storages/{action} */
    private String gatewayUrl(String action) {
        return StrUtil.format("https://{}.api.tcloudbasegateway.com/v1/storages/{}", envId, action);
    }

    /**
     * 从 CDN 域名派生存储桶名：默认公有读域名为 {@code {bucket}.tcb.qcloud.la}，取首段即 bucket。
     * 用于拼 delete 所需的 cloudObjectId。
     */
    private String extractBucket() {
        String host = java.net.URI.create(normalizedCdnBase()).getHost();
        return StrUtil.subBefore(host, ".", false);
    }

    /**
     * 关键配置缺失时给出明确提示，而非拼出残缺 URL 落库（不静默失败）。
     */
    private void requireConfig() {
        if (StrUtil.hasBlank(envId, apiKey, cdnBaseUrl)) {
            throw new BusinessException(ResultCode.FILE_STORAGE_CONFIG_MISSING);
        }
    }
}
