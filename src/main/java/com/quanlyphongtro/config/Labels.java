package com.quanlyphongtro.config;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component("labels")
public class Labels {
  private final Map<String, String> labels =
      Map.ofEntries(
          Map.entry("TENANT", "Người tìm thuê"),
          Map.entry("LANDLORD", "Chủ trọ"),
          Map.entry("ADMIN", "Quản trị viên"),
          Map.entry("ACTIVE", "Đang hoạt động"),
          Map.entry("BANNED", "Đã khóa"),
          Map.entry("AVAILABLE", "Phòng trống"),
          Map.entry("RESERVED", "Đã giữ phòng"),
          Map.entry("OCCUPIED", "Đang có người thuê"),
          Map.entry("MAINTENANCE", "Bảo trì"),
          Map.entry("ARCHIVED", "Đã lưu trữ"),
          Map.entry("DRAFT", "Bản nháp"),
          Map.entry("PENDING_REVIEW", "Chờ kiểm duyệt"),
          Map.entry("APPROVED", "Đã duyệt"),
          Map.entry("REJECTED", "Đã từ chối"),
          Map.entry("BLOCKED", "Bị chặn"),
          Map.entry("PENDING", "Đang chờ"),
          Map.entry("ACCEPTED", "Đã chấp nhận"),
          Map.entry("CANCELLED", "Đã hủy"),
          Map.entry("PENDING_MOVE_IN", "Chờ nhận phòng"),
          Map.entry("COMPLETED", "Đã kết thúc"),
          Map.entry("VISIBLE", "Đang hiển thị"),
          Map.entry("HIDDEN", "Đã ẩn"),
          Map.entry("DELETED", "Đã xóa"));

  public String get(String key) {
    return labels.getOrDefault(key, key);
  }
}
