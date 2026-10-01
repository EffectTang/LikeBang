package com.likebang.modules.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 自助修改个人资料请求（仅本人，userId 取自登录态，不由前端传入）
 * <p>
 * 局部更新语义：null = 不修改。
 * - 昵称：NOT NULL 不可清空，空白一律按"不修改"处理；
 * - 头像/自我介绍：传空字符串 = 清空。
 * <p>
 * 有意采用字段白名单只开放 nickname/avatarUrl/intro 三个字段：
 * role/status/username/email/phone/openid/passwordHash 在此 DTO 中不存在，
 * 使"自助改资料不可能提权或篡改身份"成为结构性约束而非运行时判断
 * （与管理域 UserUpdateRequest 故意不含 role 同一设计思路）。
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 32, message = "昵称长度不能超过32个字符")
    private String nickname;

    /**
     * 头像地址：站内相对路径（/uploads/xxx）或 http(s) 外链；空字符串=清空
     */
    @Size(max = 512, message = "头像地址长度不能超过512个字符")
    private String avatarUrl;

    @Size(max = 200, message = "自我介绍长度不能超过200个字符")
    private String intro;
}
