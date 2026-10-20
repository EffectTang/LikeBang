package com.likebang.modules.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 图片存储策略抽象。
 * <p>
 * 业务侧只依赖“保存一张图片、返回可落库的访问 URL”这一契约，具体落到本地磁盘
 * 还是 CloudBase 云存储由实现决定，通过配置 {@code likebang.file.storage-mode} 切换，
 * 二者互斥存在（{@code @ConditionalOnProperty}），故注入点始终只有一个实现。
 */
public interface ImageStorageService {

    /**
     * 保存图片，返回可直接落库/渲染的访问 URL。
     * 本地模式返回相对路径（如 /uploads/202609/xxx.png），云存储模式返回 https 永久链接。
     */
    String storeImage(MultipartFile file);

    /**
     * 删除一张已孤立的图片（被替换/清空后的旧图）。
     * <p>
     * 实现须做到：空 URL、非本存储所属（如 http 外链/微信头像、本地模式下非本站目录）一律忽略；
     * 软删除可恢复的数据不应调用此方法。删除是 best-effort 的清理动作，失败不应影响业务流程。
     */
    void deleteImage(String url);
}
