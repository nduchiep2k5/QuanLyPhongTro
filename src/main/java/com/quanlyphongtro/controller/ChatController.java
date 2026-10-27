package com.quanlyphongtro.controller;

import com.quanlyphongtro.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatController {
  private final ChatService chat;

  @PostMapping("/api/chat")
  ResponseEntity<?> answer(@RequestBody ChatService.Request request) {
    try {
      return ResponseEntity.ok(chat.answer(request));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new Error(e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new Error(e.getMessage()));
    }
  }

  record Error(String error) {}
}
