package com.likebang.modules.file.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 图片上传的服务端强校验：本地磁盘与 CloudBase 云存储两种实现共用，
 * 保证“不信任前端”的口径一致（扩展名白名单，不合法直接 400）。
 */
final class ImageValidation {

    /** 允许的图片扩展名白名单（服务端强校验，不信任前端） */
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private ImageValidation() {
    }

    /**
     * 校验上传文件非空并返回其（已白名单校验的）小写扩展名，供两种存储实现复用。
     */
    static String requireValidExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        return resolveExtension(file.getOriginalFilename());
    }

    /**
     * 从原始文件名解析并校验扩展名，返回小写扩展名；不在白名单内抛 400。
     */
    static String resolveExtension(String originalFilename) {
        String extension = StrUtil.emptyIfNull(
                FileUtil.extName(originalFilename)).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "仅支持 jpg/jpeg/png/gif/webp 格式的图片");
        }
        return extension;
    }

    /**
     * 生成基于年月子目录、UUID 防覆盖的对象相对路径（如 {@code 202609/{uuid}.png}），
     * 本地落盘与云存储 objectId 共用同一命名约定，便于排查与定期清理。
     */
    static String newRelativeObjectPath(String extension) {
        String monthDir = LocalDate.now().format(MONTH_FORMATTER);
        String fileName = IdUtil.fastSimpleUUID() + "." + extension;
        return monthDir + "/" + fileName;
    }
}
