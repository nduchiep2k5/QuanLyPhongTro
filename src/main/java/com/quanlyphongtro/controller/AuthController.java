package com.quanlyphongtro.controller;

import com.quanlyphongtro.dto.Forms;
import com.quanlyphongtro.exception.BusinessException;
import com.quanlyphongtro.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
  private final AuthService auth;

  @GetMapping("/auth/login")
  String login() {
    return "auth/login";
  }

  @GetMapping("/auth/register")
  String register(Model m) {
    m.addAttribute("form", new Forms.Register());
    return "auth/register";
  }

  @PostMapping("/auth/register")
  String register(
      @Valid @ModelAttribute("form") Forms.Register f, BindingResult b, RedirectAttributes ra) {
    if (b.hasErrors()) return "auth/register";
    try {
      auth.register(f);
    } catch (BusinessException e) {
      b.reject("business", e.getMessage());
      return "auth/register";
    }
    ra.addFlashAttribute("success", "Đăng ký thành công. Bạn có thể đăng nhập.");
    return "redirect:/auth/login";
  }

  @GetMapping("/auth/forgot")
  String forgot() {
    return "auth/forgot";
  }

  @PostMapping("/auth/forgot")
  String forgot(@RequestParam String email, RedirectAttributes ra) {
    if (email.length() <= 254 && email.contains("@")) auth.requestReset(email);
    ra.addFlashAttribute(
        "success", "Nếu email thuộc tài khoản hoạt động, hướng dẫn đặt lại mật khẩu sẽ được gửi.");
    return "redirect:/auth/forgot";
  }

  @GetMapping("/auth/reset")
  String reset(@RequestParam(defaultValue = "") String token, Model m) {
    m.addAttribute("token", token);
    return "auth/reset";
  }

  @PostMapping("/auth/reset")
  String reset(@RequestParam String token, @RequestParam String password, RedirectAttributes ra) {
    auth.reset(token, password);
    ra.addFlashAttribute("success", "Đã đặt lại mật khẩu. Vui lòng đăng nhập.");
    return "redirect:/auth/login";
  }

  @GetMapping("/dashboard")
  String dashboard(Authentication a) {
    return switch (auth.current(a).getRole()) {
      case TENANT -> "redirect:/tenant/requests";
      case LANDLORD -> "redirect:/landlord/rooms";
      case ADMIN -> "redirect:/admin";
    };
  }

  @GetMapping("/profile")
  String profile(Authentication a, Model m) {
    var u = auth.current(a);
    var f = new Forms.Profile();
    f.setFullName(u.getFullName());
    f.setPhone(u.getPhone());
    m.addAttribute("form", f);
    m.addAttribute("passwordForm", new Forms.Password());
    return "profile";
  }

  @PostMapping("/profile")
  String profile(
      Authentication a,
      @Valid @ModelAttribute("form") Forms.Profile f,
      BindingResult b,
      Model m,
      RedirectAttributes ra) {
    if (b.hasErrors()) {
      m.addAttribute("passwordForm", new Forms.Password());
      return "profile";
    }
    auth.profile(auth.current(a).getId(), f);
    ra.addFlashAttribute("success", "Đã cập nhật hồ sơ.");
    return "redirect:/profile";
  }

  @PostMapping("/profile/password")
  String password(
      Authentication a,
      @Valid @ModelAttribute("passwordForm") Forms.Password f,
      BindingResult b,
      RedirectAttributes ra) {
    if (b.hasErrors()) throw new BusinessException("Mật khẩu mới cần 10–64 ký tự.");
    auth.password(auth.current(a).getId(), f);
    ra.addFlashAttribute("success", "Đã đổi mật khẩu. Vui lòng đăng nhập lại.");
    return "redirect:/auth/login";
  }
}
