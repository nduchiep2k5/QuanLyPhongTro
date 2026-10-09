package com.quanlyphongtro.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ResetMailService {
  private final JavaMailSender sender;
  private final String mode, base, from, upload;
  private final Environment env;

  public ResetMailService(
      JavaMailSender sender,
      Environment env,
      @Value("${app.mail-mode}") String mode,
      @Value("${app.base-url}") String base,
      @Value("${app.mail-from}") String from,
      @Value("${app.upload-dir}") String upload) {
    this.sender = sender;
    this.env = env;
    this.mode = mode;
    this.base = base;
    this.from = from;
    this.upload = upload;
  }

  public void send(String email, String token) {
    String text = "Đặt lại mật khẩu (hiệu lực 15 phút): " + base + "/auth/reset?token=" + token;
    if (mode.equals("local")) {
      if (!java.util.Arrays.asList(env.getActiveProfiles()).contains("demo")
          && !java.util.Arrays.asList(env.getActiveProfiles()).contains("test")
          && !java.util.Arrays.asList(env.getActiveProfiles()).contains("postgres-test"))
        throw new IllegalStateException("Local mail chỉ được bật trong demo/test.");
      try {
        Path dir = Path.of(upload, "mailbox");
        Files.createDirectories(dir);
        Files.writeString(
            dir.resolve(java.util.UUID.randomUUID() + ".txt"),
            "To: " + email + "\n" + text,
            StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      return;
    }
    var msg = new SimpleMailMessage();
    msg.setFrom(from);
    msg.setTo(email);
    msg.setSubject("Đặt lại mật khẩu — Quản lý phòng trọ");
    msg.setText(text);
    sender.send(msg);
  }
}
