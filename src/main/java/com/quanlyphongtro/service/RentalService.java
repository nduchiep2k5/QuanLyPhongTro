package com.quanlyphongtro.service;

import com.quanlyphongtro.dto.*;
import com.quanlyphongtro.dto.Views.*;
import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import com.quanlyphongtro.exception.BusinessException;
import com.quanlyphongtro.repository.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalService {
  private final RoomRepository rooms;
  private final ListingRepository listings;
  private final RentalRequestRepository requests;
  private final TenancyRepository tenancies;
  private final AccessService access;
  private final ViewMapper mapper;

  public static LocalDate today() {
    return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
  }

  public void send(Long roomId, Long actor, Forms.Request f) {
    var room = rooms.lock(roomId).orElseThrow(AccessService::missing);
    var tenant = access.role(actor, Role.TENANT);
    var l = listings.findByRoomId(roomId).orElseThrow(AccessService::missing);
    if (!AccessService.publicRoom(l)) throw new BusinessException("Phòng không còn khả dụng.");
    if (f.getRequestedMoveIn() == null
        || f.getRequestedMoveIn().isBefore(today())
        || f.getRequestedMoveIn().isBefore(l.getAvailableFrom()))
      throw new BusinessException("Ngày dự kiến nhận phòng không hợp lệ.");
    if (f.getMessage() != null && f.getMessage().length() > 1000)
      throw new BusinessException("Lời nhắn quá dài.");
    if (requests.existsByTenantIdAndListingIdAndStatus(actor, l.getId(), RequestStatus.PENDING))
      throw new BusinessException("Bạn đã có yêu cầu đang chờ cho phòng này.");
    var q = new RentalRequest();
    q.setListing(l);
    q.setTenant(tenant);
    q.setStatus(RequestStatus.PENDING);
    q.setRequestedMoveIn(f.getRequestedMoveIn());
    q.setMessage(f.getMessage());
    q.setListingVersionSnapshot(l.getVersion());
    q.setRentSnapshot(l.getMonthlyRent());
    q.setDepositSnapshot(l.getDeposit());
    q.setTitleSnapshot(l.getTitle());
    requests.save(q);
  }

  public void cancel(Long requestId, Long actor) {
    Long roomId = requests.roomIdFor(requestId).orElseThrow(AccessService::missing);
    rooms.lock(roomId).orElseThrow();
    var q = requests.findById(requestId).orElseThrow();
    var a = access.role(actor, Role.TENANT);
    if (!q.getTenant().getId().equals(actor)) throw AccessService.forbidden();
    pending(q);
    decide(q, RequestStatus.CANCELLED, a, "Người tìm thuê hủy yêu cầu");
  }

  public void decide(Long requestId, Long owner, boolean accept, String reason) {
    Long roomId = requests.roomIdFor(requestId).orElseThrow(AccessService::missing);
    var room = rooms.lock(roomId).orElseThrow();
    // Reload managed request after lock; clearing is unnecessary: lock room before loading mutable
    // request state.
    var q = requests.findById(requestId).orElseThrow();
    requests.flush();
    access.owner(room, owner);
    var actor = access.role(owner, Role.LANDLORD);
    pending(q);
    if (!accept) {
      checkReason(reason);
      decide(q, RequestStatus.REJECTED, actor, reason);
      return;
    }
    var l = listings.findByRoomId(roomId).orElseThrow();
    if (!AccessService.publicRoom(l)
        || l.getVersion() != q.getListingVersionSnapshot()
        || q.getTenant().getStatus() != AccountStatus.ACTIVE)
      throw new BusinessException("Phòng hoặc yêu cầu đã thay đổi. Vui lòng tải lại.");
    if (tenancies.existsByRoomIdAndStatusIn(
        roomId, List.of(TenancyStatus.PENDING_MOVE_IN, TenancyStatus.ACTIVE)))
      throw new BusinessException("Phòng đã được giữ.");
    var t = new Tenancy();
    t.setRequest(q);
    t.setRoom(room);
    t.setTenant(q.getTenant());
    t.setStatus(TenancyStatus.PENDING_MOVE_IN);
    t.setPlannedStartDate(q.getRequestedMoveIn());
    t.setMonthlyRentSnapshot(q.getRentSnapshot());
    t.setDepositSnapshot(q.getDepositSnapshot());
    t.setRoomAddressSnapshot(
        room.getAddressLine() + ", " + room.getWard() + ", " + room.getProvince());
    tenancies.save(t);
    room.setStatus(RoomStatus.RESERVED);
    decide(q, RequestStatus.ACCEPTED, actor, "Chủ trọ chấp nhận");
    requests.findByListingRoomIdAndStatus(roomId, RequestStatus.PENDING).stream()
        .filter(other -> !other.getId().equals(q.getId()))
        .forEach(
            other ->
                decide(other, RequestStatus.REJECTED, actor, "Phòng đã được giữ cho yêu cầu khác"));
  }

  public void tenancy(
      Long tenancyId,
      Long owner,
      String action,
      LocalDate date,
      String reason,
      boolean maintenance) {
    Long roomId = tenancies.roomIdFor(tenancyId).orElseThrow(AccessService::missing);
    var room = rooms.lock(roomId).orElseThrow();
    var t = tenancies.findById(tenancyId).orElseThrow();
    access.owner(room, owner);
    switch (action) {
      case "receive" -> {
        if (t.getStatus() != TenancyStatus.PENDING_MOVE_IN)
          throw new BusinessException("Hồ sơ không chờ nhận phòng.");
        if (date == null || date.isAfter(today()) || date.isBefore(t.getPlannedStartDate()))
          throw new BusinessException("Ngày nhận phòng phải từ ngày dự kiến và không ở tương lai.");
        t.setActualStartDate(date);
        t.setStatus(TenancyStatus.ACTIVE);
        room.setStatus(RoomStatus.OCCUPIED);
      }
      case "cancel" -> {
        if (t.getStatus() != TenancyStatus.PENDING_MOVE_IN)
          throw new BusinessException("Chỉ hủy giữ trước nhận phòng.");
        checkReason(reason);
        t.setStatus(TenancyStatus.CANCELLED);
        t.setClosureReason(reason);
        room.setStatus(RoomStatus.AVAILABLE);
        listings.findByRoomId(roomId).orElseThrow().setVisible(false);
      }
      case "end" -> {
        if (t.getStatus() != TenancyStatus.ACTIVE)
          throw new BusinessException("Hồ sơ chưa đang thuê.");
        if (date == null || date.isBefore(t.getActualStartDate()) || date.isAfter(today()))
          throw new BusinessException("Ngày kết thúc không hợp lệ.");
        checkReason(reason);
        t.setEndDate(date);
        t.setClosureReason(reason);
        t.setStatus(TenancyStatus.COMPLETED);
        room.setStatus(maintenance ? RoomStatus.MAINTENANCE : RoomStatus.AVAILABLE);
        listings.findByRoomId(roomId).orElseThrow().setVisible(false);
      }
      default -> throw new BusinessException("Thao tác không hợp lệ.");
    }
  }

  public void cancelPending(Long roomId, Account actor, String reason) {
    requests
        .findByListingRoomIdAndStatus(roomId, RequestStatus.PENDING)
        .forEach(q -> decide(q, RequestStatus.CANCELLED, actor, reason));
  }

  public static void checkReason(String reason) {
    if (reason == null || reason.isBlank() || reason.length() > 1000)
      throw new BusinessException("Cần lý do, tối đa 1000 ký tự.");
  }

  private void pending(RentalRequest q) {
    if (q.getStatus() != RequestStatus.PENDING)
      throw new BusinessException("Yêu cầu đã được xử lý.");
  }

  private void decide(RentalRequest q, RequestStatus s, Account actor, String reason) {
    q.setStatus(s);
    q.setProcessedAt(Instant.now());
    q.setProcessedBy(actor);
    q.setDecisionReason(reason);
  }

  @Transactional(readOnly = true)
  public List<RequestRow> requests(Long id, boolean owner) {
    access.role(id, owner ? Role.LANDLORD : Role.TENANT);
    return (owner
            ? requests.findByListingRoomLandlordIdOrderByCreatedAtDesc(id)
            : requests.findByTenantIdOrderByCreatedAtDesc(id))
        .stream().map(mapper::request).toList();
  }

  @Transactional(readOnly = true)
  public List<TenancyRow> tenancies(Long id, boolean owner) {
    access.role(id, owner ? Role.LANDLORD : Role.TENANT);
    return (owner
            ? tenancies.findByRoomLandlordIdOrderByCreatedAtDesc(id)
            : tenancies.findByTenantIdOrderByCreatedAtDesc(id))
        .stream().map(mapper::tenancy).toList();
  }
}
