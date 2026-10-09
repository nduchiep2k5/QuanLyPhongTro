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
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {
  private final AuthService auth;
  private final RentalService rentals;
  private final TenantService tenants;

  @GetMapping("/favorites")
  String favorites(Authentication a, Model m) {
    m.addAttribute("items", tenants.favorites(auth.current(a).getId()));
    return "tenant/favorites";
  }

  @PostMapping("/favorites/{id}")
  String favorite(
      @PathVariable Long id,
      @RequestParam(defaultValue = "true") boolean add,
      Authentication a,
      RedirectAttributes ra) {
    tenants.favorite(id, auth.current(a).getId(), add);
    ra.addFlashAttribute("success", add ? "Đã lưu yêu thích." : "Đã bỏ yêu thích.");
    return "redirect:/tenant/favorites";
  }

  @PostMapping("/rooms/{id}/requests")
  String send(
      @PathVariable Long id,
      @Valid @ModelAttribute Forms.Request f,
      BindingResult b,
      Authentication a,
      RedirectAttributes ra) {
    if (b.hasErrors())
      throw new BusinessException("Vui lòng kiểm tra ngày và lời nhắn (tối đa 1000 ký tự).");
    rentals.send(id, auth.current(a).getId(), f);
    ra.addFlashAttribute("success", "Đã gửi yêu cầu. Theo dõi phản hồi của chủ trọ tại đây.");
    return "redirect:/tenant/requests";
  }

  @GetMapping("/requests")
  String requests(Authentication a, Model m) {
    m.addAttribute("rows", rentals.requests(auth.current(a).getId(), false));
    m.addAttribute("owner", false);
    return "rental/requests";
  }

  @PostMapping("/requests/{id}/cancel")
  String cancel(@PathVariable Long id, Authentication a, RedirectAttributes ra) {
    rentals.cancel(id, auth.current(a).getId());
    ra.addFlashAttribute("success", "Đã hủy yêu cầu.");
    return "redirect:/tenant/requests";
  }

  @GetMapping("/tenancies")
  String tenancies(Authentication a, Model m) {
    m.addAttribute("rows", rentals.tenancies(auth.current(a).getId(), false));
    m.addAttribute("owner", false);
    return "rental/tenancies";
  }

  @GetMapping("/tenancies/{id}/review")
  String review(@PathVariable Long id, Authentication a, Model m) {
    m.addAttribute("form", tenants.reviewForm(id, auth.current(a).getId()));
    m.addAttribute("tenancyId", id);
    return "tenant/review";
  }

  @PostMapping("/tenancies/{id}/review")
  String review(
      @PathVariable Long id,
      @Valid @ModelAttribute("form") Forms.ReviewForm f,
      BindingResult b,
      Authentication a,
      Model m,
      RedirectAttributes ra) {
    if (b.hasErrors()) {
      m.addAttribute("tenancyId", id);
      return "tenant/review";
    }
    tenants.review(id, auth.current(a).getId(), f);
    ra.addFlashAttribute("success", "Đã lưu đánh giá.");
    return "redirect:/tenant/tenancies";
  }

  @PostMapping("/tenancies/{id}/review/delete")
  String deleteReview(@PathVariable Long id, Authentication a, RedirectAttributes ra) {
    tenants.deleteReview(id, auth.current(a).getId());
    ra.addFlashAttribute("success", "Đã xóa đánh giá.");
    return "redirect:/tenant/tenancies";
  }
}
