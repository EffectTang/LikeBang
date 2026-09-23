package com.likebang.modules.ranking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.auth.LoginUser;
import com.likebang.common.dto.PageParam;
import com.likebang.modules.ranking.dto.request.CommentCreateRequest;
import com.likebang.modules.ranking.dto.response.CommentResponse;

/**
 * 理由评论服务：评论挂在理由之下（楼中楼），仅登录可见可发
 */
public interface ReasonCommentService {

    /**
     * 发布评论，返回完整评论体供前端直接追加到流尾部（时间正序，新评论即在末尾）
     */
    CommentResponse add(Long reasonId, CommentCreateRequest request, LoginUser operator);

    /**
     * 某理由下的评论分页（created_at 升序，同秒按 id 稳定）
     */
    IPage<CommentResponse> pageByReason(Long reasonId, PageParam pageParam);

    /**
     * 删除评论（软删）：仅评论者本人或管理员
     */
    void delete(Long commentId, LoginUser operator);
}
