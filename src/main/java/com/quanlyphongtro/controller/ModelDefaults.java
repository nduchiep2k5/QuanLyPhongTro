package com.quanlyphongtro.controller;

import com.quanlyphongtro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
@RequiredArgsConstructor
public class ModelDefaults {
  private final CategoryRepository categories;
  private final AmenityRepository amenities;
  private final AccountRepository accounts;

  @ModelAttribute
  void defaults(Model model, Authentication auth) {
    model.addAttribute("categories", categories.findAllByOrderByNameAsc());
    model.addAttribute("amenities", amenities.findAllByOrderByNameAsc());
    if (auth != null)
      accounts
          .findByEmailIgnoreCase(auth.getName())
          .ifPresent(
              a -> {
                model.addAttribute("me", a);
              });
  }
}
