package com.quanlyphongtro.controller;

import com.quanlyphongtro.dto.Forms;
import com.quanlyphongtro.service.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/landlord")
@RequiredArgsConstructor
public class LandlordController {
  private final AuthService auth;
  private final RoomQueryService query;
  private final ListingService listings;
  private final RentalService rentals;

  @GetMapping("/rooms")
  String rooms(Authentication a, Model m) {
    m.addAttribute("rooms", query.owned(auth.current(a).getId()));
    return "landlord/rooms";
  }

  @GetMapping("/rooms/new")
  String add(Model m) {
    m.addAttribute("form", new Forms.RoomForm());
    m.addAttribute("existingImages", List.of());
    return "landlord/form";
  }

  @GetMapping("/rooms/{id}/edit")
  String edit(@PathVariable Long id, Authentication a, Model m) {
    m.addAttribute("form", query.edit(id, auth.current(a).getId()));
    m.addAttribute("existingImages", query.detail(id, auth.current(a).getId()).images());
    return "landlord/form";
  }

  @PostMapping("/rooms/save")
  String save(
      Authentication a,
      @Valid @ModelAttribute("form") Forms.RoomForm f,
      BindingResult b,
      @RequestParam(required = false) List<MultipartFile> files,
      Model m,
      RedirectAttributes ra) {
    if (b.hasErrors()) {
      m.addAttribute(
          "existingImages",
          f.getRoomId() == null
              ? List.of()
              : query.detail(f.getRoomId(), auth.current(a).getId()).images());
      return "landlord/form";
    }
    listings.save(auth.current(a).getId(), f, files);
    ra.addFlashAttribute("success", "Đã lưu phòng. Bài mới cần gửi duyệt trước khi hiển thị.");
    return "redirect:/landlord/rooms";
  }

  @PostMapping("/rooms/{id}/action")
  String action(
      @PathVariable Long id, @RequestParam String action, Authentication a, RedirectAttributes ra) {
    listings.action(id, auth.current(a).getId(), action);
    ra.addFlashAttribute("success", "Đã cập nhật trạng thái.");
    return "redirect:/landlord/rooms";
  }

  @PostMapping("/images/{id}/delete")
  String image(@PathVariable Long id, Authentication a, RedirectAttributes ra) {
    listings.deleteImage(id, auth.current(a).getId());
    ra.addFlashAttribute("success", "Đã xóa ảnh; gửi duyệt lại bài sau khi hoàn thiện.");
    return "redirect:/landlord/rooms";
  }

  @GetMapping("/requests")
  String requests(Authentication a, Model m) {
    m.addAttribute("rows", rentals.requests(auth.current(a).getId(), true));
    m.addAttribute("owner", true);
    return "rental/requests";
  }

  @PostMapping("/requests/{id}/decide")
  String decide(
      @PathVariable Long id,
      @RequestParam boolean accept,
      @RequestParam(defaultValue = "") String reason,
      Authentication a,
      RedirectAttributes ra) {
    rentals.decide(id, auth.current(a).getId(), accept, reason);
    ra.addFlashAttribute("success", "Đã xử lý yêu cầu.");
    return "redirect:/landlord/requests";
  }

  @GetMapping("/tenancies")
  String tenancies(Authentication a, Model m) {
    m.addAttribute("rows", rentals.tenancies(auth.current(a).getId(), true));
    m.addAttribute("owner", true);
    m.addAttribute("today", RentalService.today());
    return "rental/tenancies";
  }

  @PostMapping("/tenancies/{id}/action")
  String tenancy(
      @PathVariable Long id,
      @RequestParam String action,
      @RequestParam(required = false) LocalDate date,
      @RequestParam(defaultValue = "") String reason,
      @RequestParam(defaultValue = "false") boolean maintenance,
      Authentication a,
      RedirectAttributes ra) {
    rentals.tenancy(id, auth.current(a).getId(), action, date, reason, maintenance);
    ra.addFlashAttribute("success", "Đã cập nhật hồ sơ thuê.");
    return "redirect:/landlord/tenancies";
  }
}
