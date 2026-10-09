package com.quanlyphongtro.controller;

import com.quanlyphongtro.repository.*;
import com.quanlyphongtro.service.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
  private final AuthService auth;
  private final AdminService admin;
  private final ListingService listings;
  private final TenantService tenants;
  private final CategoryRepository categories;
  private final AmenityRepository amenities;

  @GetMapping
  String dashboard(
      Authentication a,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      Model m) {
    m.addAttribute("stats", admin.stats(auth.current(a).getId(), from, to));
    return "admin/dashboard";
  }

  @GetMapping("/report.csv")
  ResponseEntity<byte[]> csv(
      Authentication a,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    var map = admin.stats(auth.current(a).getId(), from, to);
    var b = new StringBuilder("\uFEFFChỉ tiêu,Số lượng\r\n");
    map.forEach((k, v) -> b.append(AdminService.csvCell(k)).append(',').append(v).append("\r\n"));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bao-cao.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(b.toString().getBytes(StandardCharsets.UTF_8));
  }

  @GetMapping("/users")
  String users(Authentication a, Model m) {
    m.addAttribute("users", admin.users(auth.current(a).getId()));
    return "admin/users";
  }

  @PostMapping("/users/{id}/status")
  String status(
      @PathVariable Long id, @RequestParam boolean ban, Authentication a, RedirectAttributes ra) {
    admin.accountStatus(id, auth.current(a).getId(), ban);
    ra.addFlashAttribute("success", "Đã cập nhật tài khoản.");
    return "redirect:/admin/users";
  }

  @GetMapping("/listings")
  String listings(Authentication a, Model m) {
    m.addAttribute("rooms", admin.listings(auth.current(a).getId()));
    m.addAttribute("audit", admin.audit(auth.current(a).getId()));
    return "admin/listings";
  }

  @PostMapping("/listings/{id}/moderate")
  String moderate(
      @PathVariable Long id,
      @RequestParam String action,
      @RequestParam(defaultValue = "") String reason,
      @RequestParam long version,
      Authentication a,
      RedirectAttributes ra) {
    listings.moderate(id, auth.current(a).getId(), action, reason, version);
    ra.addFlashAttribute("success", "Đã ghi nhận quyết định kiểm duyệt.");
    return "redirect:/admin/listings";
  }

  @GetMapping("/reviews")
  String reviews(Authentication a, Model m) {
    m.addAttribute("reviews", admin.reviews(auth.current(a).getId()));
    return "admin/reviews";
  }

  @PostMapping("/reviews/{id}/moderate")
  String review(
      @PathVariable Long id,
      @RequestParam boolean hide,
      @RequestParam String reason,
      Authentication a,
      RedirectAttributes ra) {
    tenants.moderateReview(id, auth.current(a).getId(), hide, reason);
    ra.addFlashAttribute("success", "Đã xử lý nhận xét.");
    return "redirect:/admin/reviews";
  }

  @GetMapping("/catalog")
  String catalog(Model m) {
    return "admin/catalog";
  }

  @PostMapping("/catalog/{type}")
  String catalog(
      @PathVariable String type,
      @RequestParam(required = false) Long id,
      @RequestParam String name,
      @RequestParam(defaultValue = "false") boolean active,
      Authentication a,
      RedirectAttributes ra) {
    if (type.equals("category")) admin.category(id, name, active, auth.current(a).getId());
    else if (type.equals("amenity")) admin.amenity(id, name, active, auth.current(a).getId());
    else throw new com.quanlyphongtro.exception.BusinessException("Danh mục không hợp lệ.");
    ra.addFlashAttribute("success", "Đã lưu danh mục.");
    return "redirect:/admin/catalog";
  }
}
