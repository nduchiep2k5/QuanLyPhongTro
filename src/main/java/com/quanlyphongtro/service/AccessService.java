package com.quanlyphongtro.service;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import com.quanlyphongtro.exception.BusinessException;
import com.quanlyphongtro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessService {
  private final AccountRepository accounts;

  public Account role(Long id, Role role) {
    var a = accounts.findById(id).orElseThrow(() -> missing());
    if (a.getRole() != role || a.getStatus() != AccountStatus.ACTIVE) throw forbidden();
    return a;
  }

  public void owner(Room r, Long id) {
    role(id, Role.LANDLORD);
    if (!r.getLandlord().getId().equals(id)) throw forbidden();
  }

  public static BusinessException missing() {
    return new BusinessException(
        HttpStatus.NOT_FOUND, "Không tìm thấy nội dung hoặc nội dung không còn công khai.");
  }

  public static BusinessException forbidden() {
    return new BusinessException(
        HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
  }

  public static boolean publicRoom(Listing l) {
    return l.getRoom().getLandlord().getStatus() == AccountStatus.ACTIVE
        && l.getRoom().getStatus() == RoomStatus.AVAILABLE
        && l.getModerationStatus() == ListingStatus.APPROVED
        && l.isVisible();
  }
}
