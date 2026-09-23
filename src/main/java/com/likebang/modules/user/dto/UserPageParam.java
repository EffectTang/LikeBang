package com.likebang.modules.user.dto;

import com.likebang.common.dto.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户管理分页查询参数（管理后台）
 * <p>
 * 继承 PageParam 复用 current/size/keyword 与 toPage() 钳制，
 * 额外支持按状态、角色精确筛选。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageParam extends PageParam {

    /**
     * 状态筛选：0禁用，1正常（null=不筛选）
     */
    private Integer status;

    /**
     * 角色筛选：0普通用户，1管理员，2运营管理员（null=不筛选）
     */
    private Integer role;
}
