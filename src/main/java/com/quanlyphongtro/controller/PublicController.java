package com.quanlyphongtro.controller;

import com.quanlyphongtro.dto.Forms;
import com.quanlyphongtro.service.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PublicController {
  private final RoomQueryService rooms;
  private final TenantService tenants;
  private final AuthService auth;
  private final ImageService images;

  @GetMapping("/rooms")
  String search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String province,
      @RequestParam(required = false) String ward,
      @RequestParam(required = false) BigDecimal min,
      @RequestParam(required = false) BigDecimal max,
      @RequestParam(required = false) BigDecimal minArea,
      @RequestParam(required = false) BigDecimal maxArea,
      @RequestParam(required = false) Long category,
      @RequestParam(required = false) List<Long> amenityIds,
      @RequestParam(defaultValue = "newest") String order,
      @RequestParam(defaultValue = "0") int page,
      Model m) {
    m.addAttribute(
        "page",
        rooms.search(
            q, province, ward, min, max, minArea, maxArea, category, amenityIds, order, page));
    return "rooms/search";
  }

  @GetMapping("/rooms/{id}")
  String room(@PathVariable Long id, Authentication a, Model m) {
    var r = rooms.detail(id, a == null ? null : auth.current(a).getId());
    m.addAttribute("room", r);
    var reviews = tenants.publicReviews(id);
    m.addAttribute("reviews", reviews);
    m.addAttribute("average", reviews.stream().mapToInt(v -> v.rating()).average().orElse(0));
    m.addAttribute("requestForm", new Forms.Request());
    return "rooms/detail";
  }

  @GetMapping("/images/{id}")
  ResponseEntity<Resource> image(@PathVariable Long id, Authentication a) {
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body(images.get(id, a == null ? null : a.getName()));
  }
}
