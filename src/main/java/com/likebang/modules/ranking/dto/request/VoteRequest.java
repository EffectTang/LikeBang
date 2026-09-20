package com.likebang.modules.ranking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 投票请求：认同/反对（换票与投票共用同一请求体，取消走 DELETE 接口）
 */
@Data
public class VoteRequest {

    /**
     * 投票类型：1认同，-1反对（0 由 Service 层拒绝）
     */
    @NotNull(message = "投票类型不能为空")
    @Min(value = -1, message = "投票类型只能是 1（认同）或 -1（反对）")
    @Max(value = 1, message = "投票类型只能是 1（认同）或 -1（反对）")
    private Integer voteType;
}
