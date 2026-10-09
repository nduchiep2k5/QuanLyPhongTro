package com.quanlyphongtro;

import com.quanlyphongtro.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
  private final RoomQueryService rooms;

  @GetMapping("/")
  public String home(Model m) {
    m.addAttribute(
        "page", rooms.search(null, null, null, null, null, null, null, null, null, "newest", 0));
    return "index";
  }
}
