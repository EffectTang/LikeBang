package com.likebang.modules.ranking.service;

import com.likebang.modules.ranking.dto.request.VoteRequest;
import com.likebang.modules.ranking.dto.response.VoteResponse;
import com.likebang.common.auth.LoginUser;

/**
 * 投票服务接口：排名项 / 理由的认同、反对与取消
 * <p>
 * 独立于 RankingService，便于后续动态得分重算（第二步）在此之上挂载重算入口。
 */
public interface VoteService {

    /**
     * 对排名项投/换 认同或反对票（upsert 语义，重复投同类型幂等成功）
     */
    VoteResponse voteItem(Long rankingId, Long itemId, VoteRequest request, LoginUser operator);

    /**
     * 取消对排名项的投票（无票可取消时幂等成功）
     */
    VoteResponse cancelItemVote(Long rankingId, Long itemId, LoginUser operator);

    /**
     * 对理由投/换 认同或反对票（榜单/项归属由理由记录反查）
     */
    VoteResponse voteReason(Long reasonId, VoteRequest request, LoginUser operator);

    /**
     * 取消对理由的投票（无票可取消时幂等成功）
     */
    VoteResponse cancelReasonVote(Long reasonId, LoginUser operator);
}
