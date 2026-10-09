package com.quanlyphongtro.service;

import com.quanlyphongtro.dto.*;
import com.quanlyphongtro.dto.Views.*;
import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import com.quanlyphongtro.exception.BusinessException;
import com.quanlyphongtro.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantService {
  private final FavoriteRepository favorites;
  private final RoomRepository rooms;
  private final ListingRepository listings;
  private final TenancyRepository tenancies;
  private final ReviewRepository reviews;
  private final ModerationEventRepository events;
  private final AccessService access;
  private final ViewMapper mapper;

  public void favorite(Long roomId, Long actor, boolean add) {
    var room = rooms.lock(roomId).orElseThrow(AccessService::missing);
    var a = access.role(actor, Role.TENANT);
    var found = favorites.findByAccountIdAndRoomId(actor, roomId);
    if (!add) {
      found.ifPresent(favorites::delete);
      return;
    }
    var l = listings.findByRoomId(roomId).orElseThrow(AccessService::missing);
    if (!AccessService.publicRoom(l)) throw AccessService.missing();
    if (found.isEmpty()) {
      var f = new Favorite();
      f.setRoom(room);
      f.setAccount(a);
      favorites.save(f);
    }
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> favorites(Long actor) {
    access.role(actor, Role.TENANT);
    return favorites.findByAccountIdOrderByCreatedAtDesc(actor).stream()
        .map(
            f -> {
              Map<String, Object> item = new HashMap<>();
              item.put("roomId", f.getRoom().getId());
              var l = listings.findByRoomId(f.getRoom().getId());
              item.put("room", l.filter(AccessService::publicRoom).map(mapper::card).orElse(null));
              return item;
            })
        .toList();
  }

  public void review(Long tenancyId, Long actor, Forms.ReviewForm f) {
    Long roomId = tenancies.roomIdFor(tenancyId).orElseThrow(AccessService::missing);
    rooms.lock(roomId).orElseThrow();
    access.role(actor, Role.TENANT);
    var t = tenancies.findById(tenancyId).orElseThrow();
    if (!t.getTenant().getId().equals(actor)) throw AccessService.forbidden();
    if (!Set.of(TenancyStatus.ACTIVE, TenancyStatus.COMPLETED).contains(t.getStatus()))
      throw new BusinessException("Bạn cần nhận phòng trước khi đánh giá.");
    if (f.getRating() < 1
        || f.getRating() > 5
        || f.getComment() == null
        || f.getComment().isBlank()
        || f.getComment().length() > 2000)
      throw new BusinessException("Nhận xét cần 1–5 sao và tối đa 2000 ký tự.");
    var r =
        reviews
            .findByTenancyId(tenancyId)
            .orElseGet(
                () -> {
                  var x = new Review();
                  x.setTenancy(t);
                  x.setStatus(ReviewStatus.VISIBLE);
                  return x;
                });
    if (r.getStatus() == ReviewStatus.DELETED)
      throw new BusinessException("Đánh giá đã xóa; không tạo lại để tránh đăng trùng.");
    r.setRating(f.getRating());
    r.setComment(f.getComment().trim());
    reviews.save(r);
  }

  public void deleteReview(Long tenancyId, Long actor) {
    var roomId = tenancies.roomIdFor(tenancyId).orElseThrow(AccessService::missing);
    rooms.lock(roomId).orElseThrow();
    access.role(actor, Role.TENANT);
    var r = reviews.findByTenancyId(tenancyId).orElseThrow(AccessService::missing);
    if (!r.getTenancy().getTenant().getId().equals(actor)) throw AccessService.forbidden();
    r.setStatus(ReviewStatus.DELETED);
  }

  public void moderateReview(Long id, Long admin, boolean hide, String reason) {
    var found = reviews.findById(id).orElseThrow(AccessService::missing);
    rooms.lock(found.getTenancy().getRoom().getId()).orElseThrow();
    var a = access.role(admin, Role.ADMIN);
    var r = reviews.findById(id).orElseThrow();
    if (r.getStatus() == ReviewStatus.DELETED)
      throw new BusinessException("Không khôi phục đánh giá người dùng đã xóa.");
    RentalService.checkReason(reason);
    var e = new ModerationEvent();
    e.setActor(a);
    e.setReview(r);
    e.setAction(hide ? "hide" : "restore");
    e.setFromStatus(r.getStatus().name());
    r.setStatus(hide ? ReviewStatus.HIDDEN : ReviewStatus.VISIBLE);
    e.setToStatus(r.getStatus().name());
    e.setReason(reason);
    events.save(e);
  }

  @Transactional(readOnly = true)
  public List<ReviewRow> publicReviews(Long room) {
    var l = listings.findByRoomId(room).orElseThrow(AccessService::missing);
    if (!AccessService.publicRoom(l)) return List.of();
    return reviews
        .findByTenancyRoomIdAndStatusOrderByCreatedAtDesc(room, ReviewStatus.VISIBLE)
        .stream()
        .map(mapper::review)
        .toList();
  }

  @Transactional(readOnly = true)
  public Forms.ReviewForm reviewForm(Long tenancyId, Long actor) {
    var t = tenancies.findById(tenancyId).orElseThrow(AccessService::missing);
    access.role(actor, Role.TENANT);
    if (!t.getTenant().getId().equals(actor)) throw AccessService.forbidden();
    var f = new Forms.ReviewForm();
    reviews
        .findByTenancyId(tenancyId)
        .ifPresent(
            r -> {
              f.setRating(r.getRating());
              f.setComment(r.getComment());
            });
    return f;
  }
}
