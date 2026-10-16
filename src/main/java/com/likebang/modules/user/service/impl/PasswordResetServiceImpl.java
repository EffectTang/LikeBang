package com.likebang.modules.user.service.impl;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HtmlUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.config.auth.AuthProperties;
import com.likebang.modules.user.dto.request.PasswordResetCompleteRequest;
import com.likebang.modules.user.dto.request.PasswordResetRequest;
import com.likebang.modules.user.entity.PasswordResetToken;
import com.likebang.modules.user.entity.SysUser;
import com.likebang.modules.user.mapper.PasswordResetTokenMapper;
import com.likebang.modules.user.mapper.SysUserMapper;
import com.likebang.modules.user.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;

/**
 * 找回密码服务实现：邮件一次性重置链接（高熵令牌，仅存 SHA-256 哈希）
 * <p>
 * 安全口径：
 * 1. 防账号探测——邮箱不存在时与存在走同样的响应与时延（补齐 BCrypt 时延），绝不回传差异信息；
 * 2. 频控——同邮箱 1 小时 10 次、同 IP 1 小时 30 次、全局 60 秒 1 次（进程内 TimedCache，
 *    单实例部署够用；多实例需升级 Redis 计数，届时键名与阈值集中管理）；
 * 3. 一次性——完成重置即核销本凭证并作废同邮箱全部未用凭证；已核销凭证被再次提交视为
 *    链接泄露，连坐作废旧凭证（同类"重复使用即全部失效"标准处理）；
 * 4. 邮件发送为异步外部 IO：不裹进 @Transactional，申请主流程只有凭证落库是同步事务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    /** 同邮箱申请频控：1 小时最多 10 次 */
    private static final int EMAIL_LIMIT_PER_HOUR = 10;
    /** 同 IP 申请频控：1 小时最多 30 次 */
    private static final int IP_LIMIT_PER_HOUR = 30;
    /** 同邮箱+IP 组合最小申请间隔（秒），防连点刷邮件 */
    private static final int RESEND_INTERVAL_SECONDS = 60;
    /** 令牌明文长度：32 字符 Base62（约 190 bit 熵），仅瞬时出现在链接中，库内只存哈希 */
    private static final int TOKEN_LENGTH = 32;
    /** 邮址未命中时用于对齐耗时的 BCrypt 哑哈希（cost 与真实哈希一致） */
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOa6iH8nobXYIcHXxjtTZcVwNslVjVw6G";

    private final SysUserMapper sysUserMapper;
    private final PasswordResetTokenMapper passwordResetTokenMapper;
    private final AuthProperties authProperties;
    private final Environment environment;
    /** 邮件通道可能未配置（spring-boot-starter-mail 自动配置依赖 host 非空），故用 Provider 延迟获取 */
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    /** 申请频控计数器：Key 维度到期自动清理（超额保留 2 倍时长），无需后台清理任务 */
    private final TimedCache<String, Integer> rateCache =
            CacheUtil.newTimedCache(2 * 60 * 60 * 1000L);

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Override
    public void requestReset(PasswordResetRequest request, String clientIp) {
        String email = request.getEmail().trim().toLowerCase();
        if (!isMailConfigured()) {
            throw new BusinessException(ResultCode.MAIL_CONFIG_MISSING);
        }
        checkRateLimit(clientIp, email);

        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getEmail, email));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            // 邮箱未注册/被禁用与已注册走相同响应与时延（补齐一次 BCrypt 比对），
            // 攻击者无法通过响应差异或耗时差探测账号是否存在
            BCrypt.checkpw(StrUtil.uuid(), DUMMY_HASH);
            return;
        }

        // 滚动失效：作废该邮箱全部未用旧凭证，保证任意时刻仅最新一条链接有效
        revokeUnusedTokensByEmail(email, LocalDateTime.now());

        String token = RandomUtil.randomString(TOKEN_LENGTH);
        PasswordResetToken entity = new PasswordResetToken();
        entity.setEmail(email);
        entity.setTokenHash(DigestUtil.sha256Hex(token));
        entity.setExpiryAt(LocalDateTime.now().plusMinutes(resolveExpiryMinutes()));
        entity.setIp(clientIp);
        passwordResetTokenMapper.insert(entity);

        // 异步发信：SMTP 外部调用不阻塞请求线程，也不与落库事务捆绑；失败仅记日志（调用方无感知）
        String nickname = user.getNickname();
        ThreadUtil.execAsync(() -> sendResetMail(email, nickname, token, clientIp));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeReset(PasswordResetCompleteRequest request) {
        String tokenHash = DigestUtil.sha256Hex(request.getToken().trim());
        PasswordResetToken record = passwordResetTokenMapper.selectOne(
                Wrappers.<PasswordResetToken>lambdaQuery().eq(PasswordResetToken::getTokenHash, tokenHash));
        if (record == null) {
            throw new BusinessException(ResultCode.RESET_TOKEN_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        // 已核销凭证被再次提交：视为链接泄露（如邮件转发/日志残留），连坐作废全部未用凭证
        if (record.getUsedAt() != null) {
            revokeUnusedTokensByEmail(record.getEmail(), now);
            throw new BusinessException(ResultCode.RESET_TOKEN_INVALID);
        }
        if (record.getExpiryAt().isBefore(now)) {
            throw new BusinessException(ResultCode.RESET_TOKEN_INVALID);
        }

        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getEmail, record.getEmail()));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.RESET_TOKEN_INVALID);
        }
        if (user.getPasswordHash() != null && BCrypt.checkpw(request.getNewPassword(), user.getPasswordHash())) {
            // 新密码与旧密码命中同一哈希才拒绝：拿旧密码当新密码重设属无效操作，错误语义与他场景区分开
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "新密码不能与原密码相同");
        }

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPasswordHash(BCrypt.hashpw(request.getNewPassword()));
        sysUserMapper.updateById(update);

        // 核销本凭证 + 作废该账号全部未用凭证：改密后所有历史链接立即失效
        PasswordResetToken used = new PasswordResetToken();
        used.setId(record.getId());
        used.setUsedAt(now);
        passwordResetTokenMapper.updateById(used);
        revokeUnusedTokensByEmail(record.getEmail(), now);

        log.info("[AUDIT] 密码重置完成: userId={}, email={}, tokenId={}", user.getId(), record.getEmail(), record.getId());
    }

    /**
     * 发送重置邮件：正文仅含链接不含令牌明文之外的敏感信息，有效期如实告知
     */
    private void sendResetMail(String email, String nickname, String token, String clientIp) {
        try {
            AuthProperties.PasswordReset config = authProperties.getPasswordReset();
            String link = StrUtil.removeSuffix(config.getWebBaseUrl(), "/") + "/reset-password?token=" + token;

            JavaMailSender sender = mailSenderProvider.getObject();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(resolveFrom());
            helper.setTo(email);
            helper.setSubject("【榜了个榜】密码重置");
            helper.setText(buildMailHtml(nickname, link, resolveExpiryMinutes()), true);

            sender.send(message);
            log.info("密码重置邮件已发送: email={}, ip={}", email, clientIp);
        } catch (Exception e) {
            // 邮件通道异常不外抛（调用方已按"已发送"返回以维持防探测口径），仅告警便于运维排查
            log.warn("密码重置邮件发送失败: email={}, ip={}, cause={}", email, clientIp, e.getMessage());
        }
    }

    private String buildMailHtml(String nickname, String link, int expiryMinutes) {
        // 昵称为用户可控内容，拼入邮件 HTML 前必须转义，防 HTML/脚本注入钓鱼正文
        String greeting = StrUtil.isNotBlank(nickname) ? HtmlUtil.escape(nickname) : "同学";
        return "<div style=\"max-width:560px;margin:0 auto;padding:24px;"
                + "font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;"
                + "color:#1f2d3d;border:1px solid #e4e7ed;border-radius:12px\">"
                + "<h2 style=\"margin:0 0 16px\">重置你的 LikeBang 密码</h2>"
                + "<p style=\"margin:0 0 8px;line-height:1.7\">" + greeting + "，你好：</p>"
                + "<p style=\"margin:0 0 20px;line-height:1.7\">"
                + "我们收到了一次重置你在「榜了个榜」（LikeBang）账号密码的请求。"
                + "点击下方按钮设置新密码，链接 <b>" + expiryMinutes + " 分钟</b>内有效，仅可使用一次。"
                + "</p>"
                + "<p style=\"margin:0 0 20px;text-align:center\">"
                + "<a href=\"" + link + "\" target=\"_blank\" "
                + "style=\"display:inline-block;padding:12px 32px;background:#40bad5;color:#fff;"
                + "border-radius:8px;text-decoration:none;font-weight:600\">重置密码</a></p>"
                + "<p style=\"margin:0 0 8px;font-size:13px;color:#909399;line-height:1.7\">"
                + "按钮无法使用？请复制以下链接到浏览器打开：<br/>"
                + "<span style=\"word-break:break-all\">" + link + "</span></p>"
                + "<p style=\"margin:16px 0 0;font-size:13px;color:#909399;line-height:1.7\">"
                + "如果这不是你本人的操作，可以忽略本邮件，你的密码不会被更改。为账号安全，切勿将重置链接转发他人。"
                + "</p></div>";
    }

    /**
     * 邮件通道就绪判断：host 配置非空且 mail starter 已自动装配 JavaMailSender
     */
    private boolean isMailConfigured() {
        return StrUtil.isNotBlank(mailHost) && mailSenderProvider.getIfAvailable() != null;
    }

    private String resolveFrom() {
        String from = authProperties.getPasswordReset().getFrom();
        return StrUtil.isNotBlank(from) ? from : environment.getProperty("spring.mail.username");
    }

    private int resolveExpiryMinutes() {
        int minutes = authProperties.getPasswordReset().getExpiryMinutes();
        // 配置误填非法值时收敛到可用区间，不因运营失误导致链接永不过期或秒过期
        return Math.min(Math.max(minutes, 5), 1440);
    }

    /**
     * 申请频控：先检查后计数，任一维度超限即拒绝。
     * 60 秒最小间隔通过"同邮箱+IP 不写入计数键"实现：窗口内重复申请查不到新键即报错
     */
    private void checkRateLimit(String clientIp, String email) {
        String ipKey = "ip:" + clientIp;
        String emailKey = "email:" + email;
        String intervalKey = "interval:" + clientIp + "|" + email;

        if (rateCache.containsKey(intervalKey)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "操作过于频繁，请 " + RESEND_INTERVAL_SECONDS + " 秒后再试");
        }
        if (countOf(ipKey) >= IP_LIMIT_PER_HOUR || countOf(emailKey) >= EMAIL_LIMIT_PER_HOUR) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "重置申请次数已达上限，请稍后再试");
        }

        rateCache.put(ipKey, countOf(ipKey) + 1);
        rateCache.put(emailKey, countOf(emailKey) + 1);
        // 间隔键挂超时无计数含义（存在即拒绝），用独立短 TTL 键位实现
        rateCache.put(intervalKey, 1, RESEND_INTERVAL_SECONDS * 1000L);
    }

    private int countOf(String key) {
        Integer value = rateCache.get(key, false);
        return value == null ? 0 : value;
    }

    /**
     * 作废该邮箱全部未使用凭证（used_at 标记为当前时间，保留行供审计追溯）
     */
    private void revokeUnusedTokensByEmail(String email, LocalDateTime now) {
        passwordResetTokenMapper.update(null, Wrappers.<PasswordResetToken>lambdaUpdate()
                .eq(PasswordResetToken::getEmail, email)
                .isNull(PasswordResetToken::getUsedAt)
                .set(PasswordResetToken::getUsedAt, now));
    }
}
