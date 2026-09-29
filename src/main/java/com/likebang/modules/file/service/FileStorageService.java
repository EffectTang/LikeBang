package com.likebang.modules.file.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 本地磁盘图片存储：按 年月 目录落盘，文件名用 UUID 防覆盖，
 * 返回的相对访问路径与 WebConfig 静态资源映射前缀保持一致。
 * <p>
 * 后续若迁移 OSS，仅需替换本实现的落盘与 URL 拼接逻辑，接口契约不变。
 */
@Slf4j
@Service
public class FileStorageService {

    /** 允许的图片扩展名白名单（服务端强校验，不信任前端） */
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

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
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        String extension = resolveExtension(file.getOriginalFilename());

        String monthDir = LocalDate.now().format(MONTH_FORMATTER);
        String fileName = IdUtil.fastSimpleUUID() + "." + extension;
        // 直接基于归一化后的绝对 storageRoot 拼接，与静态映射解析口径完全一致；
        // 不依赖 Hutool 对相对路径的基准解析，避免写入目录与读取目录分叉
        Path dest = storageRoot.resolve(monthDir).resolve(fileName);
        try {
            Files.createDirectories(dest.getParent());
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("图片落盘失败: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "图片保存失败，请重试");
        }

        return StrUtil.appendIfMissing(accessPrefix, "/") + monthDir + "/" + fileName;
    }

    private String resolveExtension(String originalFilename) {
        String extension = StrUtil.emptyIfNull(
                FileUtil.extName(originalFilename)).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "仅支持 jpg/jpeg/png/gif/webp 格式的图片");
        }
        return extension;
    }
}
