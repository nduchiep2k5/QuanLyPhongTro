package com.quanlyphongtro.service;

import com.quanlyphongtro.dto.Forms;
import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import com.quanlyphongtro.exception.BusinessException;
import com.quanlyphongtro.repository.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ListingService {
  private final RoomRepository rooms;
  private final ListingRepository listings;
  private final CategoryRepository categories;
  private final AmenityRepository amenities;
  private final RoomAmenityRepository roomAmenities;
  private final RoomImageRepository images;
  private final AccessService access;
  private final RentalService rentals;
  private final StorageService storage;
  private final ModerationEventRepository events;

  public Long save(Long owner, Forms.RoomForm f, List<MultipartFile> files) {
    var actor = access.role(owner, Role.LANDLORD);
    Room r;
    Listing l;
    if (f.getRoomId() == null) {
      r = new Room();
      r.setLandlord(actor);
      r.setStatus(RoomStatus.AVAILABLE);
      l = new Listing();
      l.setRoom(r);
      l.setModerationStatus(ListingStatus.DRAFT);
    } else {
      r = rooms.lock(f.getRoomId()).orElseThrow(AccessService::missing);
      access.owner(r, owner);
      l = listings.findByRoomId(r.getId()).orElseThrow();
      if (l.getVersion() != f.getVersion() || r.getVersion() != f.getRoomVersion())
        throw new BusinessException("Nội dung vừa thay đổi. Hãy tải lại form.");
      if (r.getStatus() == RoomStatus.RESERVED
          || r.getStatus() == RoomStatus.OCCUPIED
          || r.getStatus() == RoomStatus.ARCHIVED
          || l.getModerationStatus() == ListingStatus.BLOCKED)
        throw new BusinessException(
            "Phòng đang giữ/thuê/lưu trữ hoặc bị chặn, chưa được sửa nội dung.");
    }
    if ((f.getLatitude() == null) != (f.getLongitude() == null))
      throw new BusinessException("Nhập đủ cả vĩ độ và kinh độ, hoặc bỏ trống cả hai.");
    var category = categories.findById(f.getCategoryId()).orElseThrow(AccessService::missing);
    if (!category.isActive()
        && (r.getId() == null || !r.getCategory().getId().equals(category.getId())))
      throw new BusinessException("Loại phòng đã ngừng dùng.");
    var selected = new ArrayList<Amenity>();
    var oldAmenities =
        r.getId() == null
            ? Set.<Long>of()
            : new HashSet<>(
                roomAmenities.findByRoomId(r.getId()).stream()
                    .map(a -> a.getAmenity().getId())
                    .toList());
    for (Long id : new HashSet<>(f.getAmenityIds())) {
      var a = amenities.findById(id).orElseThrow(AccessService::missing);
      if (!a.isActive() && !oldAmenities.contains(id))
        throw new BusinessException("Tiện ích đã ngừng dùng.");
      selected.add(a);
    }
    var actual =
        files == null
            ? List.<MultipartFile>of()
            : files.stream().filter(file -> !file.isEmpty()).toList();
    int oldCount = r.getId() == null ? 0 : images.findByRoomIdOrderBySortOrderAsc(r.getId()).size();
    if (actual.size() + oldCount > 10) throw new BusinessException("Tối đa 10 ảnh/phòng.");
    var keys = new ArrayList<String>();
    try {
      for (var file : actual) keys.add(storage.store(file));
    } catch (RuntimeException e) {
      keys.forEach(storage::delete);
      throw e;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) keys.forEach(storage::delete);
          }
        });
    r.setCategory(category);
    r.setCode(f.getCode().trim());
    r.setProvince(f.getProvince().trim());
    r.setWard(f.getWard().trim());
    r.setAddressLine(f.getAddressLine().trim());
    r.setLatitude(f.getLatitude());
    r.setLongitude(f.getLongitude());
    r.setArea(f.getArea());
    r.setMaxOccupants(f.getMaxOccupants());
    rooms.save(r);
    l.setTitle(f.getTitle().trim());
    l.setDescription(f.getDescription().trim());
    l.setMonthlyRent(f.getMonthlyRent());
    l.setDeposit(f.getDeposit());
    l.setElectricityUnitPrice(f.getElectricityUnitPrice());
    l.setWaterUnitPrice(f.getWaterUnitPrice());
    l.setWaterBillingMethod(f.getWaterBillingMethod());
    l.setServiceFee(f.getServiceFee());
    l.setAvailableFrom(f.getAvailableFrom());
    if (l.getId() != null) {
      rentals.cancelPending(r.getId(), actor, "Nội dung bài đăng thay đổi");
      if (l.getModerationStatus() == ListingStatus.APPROVED
          || l.getModerationStatus() == ListingStatus.PENDING_REVIEW) {
        l.setModerationStatus(ListingStatus.PENDING_REVIEW);
        l.setSubmittedAt(Instant.now());
      } else l.setModerationStatus(ListingStatus.DRAFT);
      l.setVisible(false);
      l.setModerationReason(null);
    }
    listings.save(l);
    roomAmenities.deleteByRoomId(r.getId());
    roomAmenities.flush();
    for (var a : selected) {
      var ra = new RoomAmenity();
      ra.setRoom(r);
      ra.setAmenity(a);
      roomAmenities.save(ra);
    }
    for (int i = 0; i < keys.size(); i++) {
      var im = new RoomImage();
      im.setRoom(r);
      im.setStorageKey(keys.get(i));
      im.setAltText(l.getTitle());
      im.setSortOrder(oldCount + i);
      im.setCover(oldCount + i == 0);
      images.save(im);
    }
    return r.getId();
  }

  public void action(Long roomId, Long owner, String action) {
    var r = rooms.lock(roomId).orElseThrow(AccessService::missing);
    access.owner(r, owner);
    var l = listings.findByRoomId(roomId).orElseThrow();
    var actor = access.role(owner, Role.LANDLORD);
    switch (action) {
      case "submit" -> {
        if (!Set.of(ListingStatus.DRAFT, ListingStatus.REJECTED).contains(l.getModerationStatus()))
          throw new BusinessException("Bài không thể gửi duyệt ở trạng thái hiện tại.");
        if (images.findByRoomIdOrderBySortOrderAsc(roomId).isEmpty())
          throw new BusinessException("Cần ít nhất một ảnh trước khi gửi duyệt.");
        l.setModerationStatus(ListingStatus.PENDING_REVIEW);
        l.setVisible(false);
        l.setSubmittedAt(Instant.now());
      }
      case "show" -> {
        if (l.getModerationStatus() != ListingStatus.APPROVED
            || r.getStatus() != RoomStatus.AVAILABLE)
          throw new BusinessException("Chỉ hiện bài đã duyệt và phòng trống.");
        l.setVisible(true);
      }
      case "hide" -> {
        l.setVisible(false);
        rentals.cancelPending(roomId, actor, "Chủ trọ ẩn bài đăng");
      }
      case "withdraw" -> {
        if (l.getModerationStatus() != ListingStatus.PENDING_REVIEW)
          throw new BusinessException("Bài không chờ duyệt.");
        l.setModerationStatus(ListingStatus.DRAFT);
        l.setVisible(false);
      }
      case "archive" -> {
        free(r);
        if (l.getModerationStatus() == ListingStatus.BLOCKED)
          throw new BusinessException("Bài bị chặn cần quản trị viên xử lý trước khi lưu trữ.");
        r.setStatus(RoomStatus.ARCHIVED);
        l.setModerationStatus(ListingStatus.ARCHIVED);
        l.setVisible(false);
        rentals.cancelPending(roomId, actor, "Phòng ngừng cho thuê");
      }
      case "restore" -> {
        if (r.getStatus() != RoomStatus.ARCHIVED)
          throw new BusinessException("Phòng chưa lưu trữ.");
        r.setStatus(RoomStatus.MAINTENANCE);
        l.setModerationStatus(ListingStatus.DRAFT);
        l.setVisible(false);
      }
      case "maintenance" -> {
        free(r);
        r.setStatus(RoomStatus.MAINTENANCE);
        l.setVisible(false);
        rentals.cancelPending(roomId, actor, "Phòng đang bảo trì");
      }
      case "available" -> {
        if (r.getStatus() != RoomStatus.MAINTENANCE)
          throw new BusinessException("Phòng không đang bảo trì.");
        r.setStatus(RoomStatus.AVAILABLE);
      }
      default -> throw new BusinessException("Thao tác không hợp lệ.");
    }
  }

  public void moderate(Long roomId, Long admin, String action, String reason, long version) {
    var r = rooms.lock(roomId).orElseThrow(AccessService::missing);
    var a = access.role(admin, Role.ADMIN);
    var l = listings.findByRoomId(roomId).orElseThrow();
    if (version != l.getVersion())
      throw new BusinessException("Bài vừa thay đổi. Vui lòng tải lại.");
    String from = l.getModerationStatus().name();
    switch (action) {
      case "approve" -> {
        if (l.getModerationStatus() != ListingStatus.PENDING_REVIEW)
          throw new BusinessException("Bài không chờ duyệt.");
        if (images.findByRoomIdOrderBySortOrderAsc(roomId).isEmpty())
          throw new BusinessException("Bài cần ít nhất một ảnh.");
        l.setModerationStatus(ListingStatus.APPROVED);
        l.setVisible(false);
      }
      case "reject" -> {
        if (l.getModerationStatus() != ListingStatus.PENDING_REVIEW)
          throw new BusinessException("Bài không chờ duyệt.");
        RentalService.checkReason(reason);
        l.setModerationStatus(ListingStatus.REJECTED);
        l.setVisible(false);
      }
      case "block" -> {
        if (l.getModerationStatus() == ListingStatus.ARCHIVED
            || l.getModerationStatus() == ListingStatus.BLOCKED)
          throw new BusinessException("Không thể chặn ở trạng thái này.");
        RentalService.checkReason(reason);
        l.setModerationStatus(ListingStatus.BLOCKED);
        l.setVisible(false);
        rentals.cancelPending(roomId, a, "Bài bị quản trị viên chặn");
      }
      case "unblock" -> {
        if (l.getModerationStatus() != ListingStatus.BLOCKED)
          throw new BusinessException("Bài chưa bị chặn.");
        l.setModerationStatus(ListingStatus.PENDING_REVIEW);
        l.setVisible(false);
      }
      default -> throw new BusinessException("Quyết định không hợp lệ.");
    }
    l.setReviewedAt(Instant.now());
    l.setReviewedBy(a);
    l.setModerationReason(reason);
    var e = new ModerationEvent();
    e.setActor(a);
    e.setListing(l);
    e.setAction(action);
    e.setFromStatus(from);
    e.setToStatus(l.getModerationStatus().name());
    e.setReason(reason);
    events.save(e);
  }

  public void deleteImage(Long imageId, Long owner) {
    Long roomId = images.findById(imageId).orElseThrow(AccessService::missing).getRoom().getId();
    var r = rooms.lock(roomId).orElseThrow();
    access.owner(r, owner);
    free(r);
    var l = listings.findByRoomId(roomId).orElseThrow();
    if (l.getModerationStatus() == ListingStatus.BLOCKED)
      throw new BusinessException("Bài đang bị chặn.");
    var im = images.findById(imageId).orElseThrow();
    String key = im.getStorageKey();
    boolean cover = im.isCover();
    images.delete(im);
    images.flush();
    var remaining = images.findByRoomIdOrderBySortOrderAsc(roomId);
    if (cover && !remaining.isEmpty()) remaining.get(0).setCover(true);
    l.setVisible(false);
    l.setModerationStatus(ListingStatus.DRAFT);
    rentals.cancelPending(roomId, r.getLandlord(), "Ảnh bài đăng thay đổi");
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCommit() {
            storage.delete(key);
          }
        });
  }

  private void free(Room r) {
    if (r.getStatus() != RoomStatus.AVAILABLE && r.getStatus() != RoomStatus.MAINTENANCE)
      throw new BusinessException("Phòng đang giữ/thuê hoặc đã lưu trữ.");
  }
}
