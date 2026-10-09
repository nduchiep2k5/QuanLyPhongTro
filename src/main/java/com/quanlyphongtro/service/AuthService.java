package com.quanlyphongtro.service;

import com.quanlyphongtro.dto.Forms;
import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import com.quanlyphongtro.exception.BusinessException;
import com.quanlyphongtro.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AccountRepository accounts;
  private final PasswordResetTokenRepository tokens;
  private final org.springframework.transaction.support.TransactionTemplate transaction;
  private final PasswordEncoder encoder;
  private final ResetMailService mail;
  private final SessionRegistry sessions;

  public static String email(String e) {
    return e.trim().toLowerCase(Locale.ROOT);
  }

  public static void checkPassword(String p) {
    if (p == null
        || p.length() < 10
        || p.length() > 64
        || p.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new BusinessException("Mật khẩu cần 10–64 ký tự và không quá 72 byte UTF-8.");
  }

  @Transactional
  public void register(Forms.Register f) {
    if (f.getRole() != Role.TENANT && f.getRole() != Role.LANDLORD)
      throw new BusinessException("Vai trò đăng ký không hợp lệ.");
    checkPassword(f.getPassword());
    if (accounts.findByEmailIgnoreCase(email(f.getEmail())).isPresent())
      throw new BusinessException("Email đã được sử dụng.");
    var a = new Account();
    a.setEmail(email(f.getEmail()));
    a.setFullName(f.getFullName().trim());
    a.setPasswordHash(encoder.encode(f.getPassword()));
    a.setRole(f.getRole());
    a.setStatus(AccountStatus.ACTIVE);
    accounts.save(a);
  }

  @Transactional(readOnly = true)
  public Account current(Authentication auth) {
    if (auth == null)
      throw new BusinessException(org.springframework.http.HttpStatus.FORBIDDEN, "Cần đăng nhập.");
    var a =
        accounts
            .findByEmailIgnoreCase(auth.getName())
            .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại."));
    if (a.getStatus() != AccountStatus.ACTIVE)
      throw new BusinessException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa.");
    return a;
  }

  @Transactional
  public void profile(Long id, Forms.Profile f) {
    var a = active(id);
    a.setFullName(f.getFullName().trim());
    a.setPhone(f.getPhone() == null ? null : f.getPhone().trim());
  }

  @Transactional
  public void password(Long id, Forms.Password f) {
    var a = accounts.lock(id).orElseThrow();
    if (a.getStatus() != AccountStatus.ACTIVE) throw new BusinessException("Tài khoản đã bị khóa.");
    if (!encoder.matches(f.getCurrentPassword(), a.getPasswordHash()))
      throw new BusinessException("Mật khẩu hiện tại không đúng.");
    checkPassword(f.getNewPassword());
    a.setPasswordHash(encoder.encode(f.getNewPassword()));
    invalidateTokens(id);
    expire(a.getEmail());
  }

  public void requestReset(String input) {
    // Mail failure is deliberately indistinguishable to the caller; no token is exposed through
    // HTTP.
    var result = transaction.execute(status -> createReset(input));
    if (result != null)
      try {
        mail.send(result[0], result[1]);
      } catch (RuntimeException e) {
        org.slf4j.LoggerFactory.getLogger(AuthService.class)
            .warn("Không thể gửi thư đặt lại mật khẩu; kiểm tra cấu hình mail.");
      }
  }

  @Transactional
  public String[] createReset(String input) {
    var found = accounts.findByEmailIgnoreCase(email(input));
    if (found.isEmpty()) return null;
    var a = accounts.lock(found.get().getId()).orElseThrow();
    if (a.getStatus() != AccountStatus.ACTIVE) return null;
    invalidateTokens(a.getId());
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var t = new PasswordResetToken();
    t.setAccount(a);
    t.setTokenHash(hash(raw));
    t.setExpiresAt(Instant.now().plusSeconds(900));
    tokens.save(t);
    return new String[] {a.getEmail(), raw};
  }

  @Transactional
  public void reset(String raw, String password) {
    checkPassword(password);
    var t =
        tokens
            .findByTokenHash(hash(raw))
            .orElseThrow(() -> new BusinessException("Liên kết không hợp lệ hoặc đã hết hạn."));
    if (t.getUsedAt() != null
        || !t.getExpiresAt().isAfter(Instant.now())
        || t.getAccount().getStatus() != AccountStatus.ACTIVE)
      throw new BusinessException("Liên kết không hợp lệ hoặc đã hết hạn.");
    t.getAccount().setPasswordHash(encoder.encode(password));
    invalidateTokens(t.getAccount().getId());
    expire(t.getAccount().getEmail());
  }

  public static String hash(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private void invalidateTokens(Long id) {
    tokens.findByAccountIdAndUsedAtIsNull(id).forEach(t -> t.setUsedAt(Instant.now()));
  }

  public void expire(String email) {
    for (Object p : sessions.getAllPrincipals()) {
      if (p instanceof org.springframework.security.core.userdetails.UserDetails u
          && u.getUsername().equalsIgnoreCase(email))
        sessions.getAllSessions(p, false).forEach(s -> s.expireNow());
    }
  }

  private Account active(Long id) {
    var a = accounts.findById(id).orElseThrow();
    if (a.getStatus() != AccountStatus.ACTIVE) throw new BusinessException("Tài khoản đã bị khóa.");
    return a;
  }
}
