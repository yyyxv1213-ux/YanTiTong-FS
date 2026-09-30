package com.ytt.modules.auth;

import com.ytt.common.BizException;
import com.ytt.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

/**
 * 邮件发送服务：通过 SMTP 发送验证码邮件（HTML 中文模板）。
 * 发送失败统一包装为 {@link ErrorCode#MAIL_SEND_FAILED}，并记录错误日志（不含验证码明文）。
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    /** 发件人地址，来自 spring.mail.username */
    @Value("${spring.mail.username:}")
    private String from;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送登录验证码邮件。
     *
     * @param toEmail 收件邮箱
     * @param code    6 位验证码
     */
    public void sendLoginCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("【研题通】登录验证码");
            helper.setText(buildContent(code), true);
            mailSender.send(message);
            log.info("登录验证码邮件已发送至 {}", toEmail);
        } catch (Exception e) {
            log.error("登录验证码邮件发送失败：to={}, reason={}", toEmail, e.getMessage());
            throw new BizException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    /** 邮件正文（HTML）：验证码醒目展示并提示 5 分钟有效、勿外泄 */
    private String buildContent(String code) {
        return "<div style='padding:24px;font-family:Microsoft YaHei,Arial,sans-serif;color:#333;'>"
                + "<h2 style='color:#4f5dff;margin:0 0 16px;'>研题通·考研408刷题训练系统</h2>"
                + "<p style='font-size:15px;'>你好，你正在使用邮箱验证码登录，验证码为：</p>"
                + "<p style='font-size:30px;font-weight:bold;letter-spacing:8px;color:#4f5dff;margin:12px 0;'>"
                + code + "</p>"
                + "<p style='font-size:14px;color:#909399;'>验证码 5 分钟内有效，请勿泄露给他人。"
                + "如非本人操作，请忽略此邮件。</p>"
                + "</div>";
    }
}
