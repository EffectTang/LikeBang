package com.likebang.modules.file.service;

import cn.hutool.core.util.StrUtil;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘图片存储：按 年月 目录落盘，文件名用 UUID 防覆盖，
 * 返回的相对访问路径与 WebConfig 静态资源映射前缀保持一致。
 * <p>
 * 仅当 {@code likebang.file.storage-mode=local}（或未配置，默认）时启用；
 * 切到 {@code cloudbase} 时由 {@link CloudBaseFileStorageService} 接管，本 Bean 不再创建。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "likebang.file.storage-mode", havingValue = "local", matchIfMissing = true)
public class FileStorageService implements ImageStorageService {

    /** 存储根目录，来自配置 likebang.file.storage-path */
    @Value("${likebang.file.storage-path:./uploads}")
    private String storagePath;

    /** 对外访问前缀，须与 WebConfig 静态资源映射、AuthInterceptor 放行路径一致 */
    @Value("${likebang.file.access-prefix:/uploads}")
    private String accessPrefix;

    /**
     * 解析后的绝对存储根目录。启动时按 user.dir 归一化一次并对外共享：
     * 落盘与 WebConfig 静态映射必须用同一个值，否则会出现“写进去的图读不出来”
     * （历史 bug：Hutool 与 JDK Paths 对相对路径 ./uploads 解析基准不一致）。
     */
    private Path storageRoot;

    @PostConstruct
    void initStorageRoot() {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        log.info("图片存储根目录（绝对路径）: {}", storageRoot);
    }

    /** 供 WebConfig 复用同一解析结果，保证读写目录同源 */
    public Path getStorageRoot() {
        return storageRoot;
    }

    /** 供 WebConfig 复用访问前缀 */
    public String getAccessPrefix() {
        return accessPrefix;
    }

    /**
     * 保存图片，返回可落库的相对访问路径（如 /uploads/202609/xxx.png）
     */
    @Override
    public String storeImage(MultipartFile file) {
        String extension = ImageValidation.requireValidExtension(file);
        String relativePath = ImageValidation.newRelativeObjectPath(extension);

        // 直接基于归一化后的绝对 storageRoot 拼接，与静态映射解析口径完全一致；
        // 不依赖 Hutool 对相对路径的基准解析，避免写入目录与读取目录分叉
        Path dest = storageRoot.resolve(relativePath);
        try {
            Files.createDirectories(dest.getParent());
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("图片落盘失败: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "图片保存失败，请重试");
        }

        return StrUtil.appendIfMissing(accessPrefix, "/") + relativePath;
    }

    /**
     * 删除本站目录下的图片：仅处理以 {@code accessPrefix} 开头的相对路径（http 外链一律忽略），
     * 并做目录穿越防护，确保删除目标仍在存储根内。
     */
    @Override
    public void deleteImage(String url) {
        if (StrUtil.isBlank(url) || !url.startsWith(accessPrefix)) {
            return;
        }
        String relative = StrUtil.removePrefix(StrUtil.removePrefix(url, accessPrefix), "/");
        if (StrUtil.isBlank(relative)) {
            return;
        }
        Path target = storageRoot.resolve(relative).normalize();
        if (!target.startsWith(storageRoot)) {
            log.warn("忽略越界的图片删除路径: {}", url);
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("本地图片删除失败（忽略）: {}", url, e);
        }
    }
}
