package com.quanlyphongtro.service;

import com.quanlyphongtro.dto.Views.*;
import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ViewMapper {
  private final RoomImageRepository images;
  private final RoomAmenityRepository amenities;
  private final ReviewRepository reviews;

  public RoomCard card(Listing l) {
    var r = l.getRoom();
    return new RoomCard(
        r.getId(),
        l.getId(),
        l.getTitle(),
        l.getDescription(),
        r.getCategory().getName(),
        r.getProvince(),
        r.getWard(),
        r.getAddressLine(),
        r.getLatitude(),
        r.getLongitude(),
        r.getArea(),
        r.getMaxOccupants(),
        l.getMonthlyRent(),
        l.getDeposit(),
        l.getElectricityUnitPrice(),
        l.getWaterUnitPrice(),
        l.getWaterBillingMethod().name(),
        l.getServiceFee(),
        l.getAvailableFrom(),
        r.getStatus().name(),
        l.getModerationStatus().name(),
        l.isVisible(),
        AccessService.publicRoom(l),
        l.getVersion(),
        r.getLandlord().getFullName(),
        r.getLandlord().getPhone(),
        images.findByRoomIdOrderBySortOrderAsc(r.getId()).stream()
            .map(i -> new Image(i.getId(), i.getAltText(), i.isCover()))
            .toList(),
        amenities.findByRoomId(r.getId()).stream().map(a -> a.getAmenity().getName()).toList(),
        l.getModerationReason());
  }

  public RequestRow request(RentalRequest q) {
    return new RequestRow(
        q.getId(),
        q.getListing().getRoom().getId(),
        q.getTitleSnapshot(),
        q.getTenant().getFullName(),
        q.getTenant().getPhone(),
        q.getStatus().name(),
        q.getRequestedMoveIn(),
        q.getRentSnapshot(),
        q.getMessage(),
        q.getDecisionReason(),
        q.getCreatedAt());
  }

  public TenancyRow tenancy(Tenancy t) {
    return new TenancyRow(
        t.getId(),
        t.getRoom().getId(),
        t.getRequest().getTitleSnapshot(),
        t.getTenant().getFullName(),
        t.getTenant().getPhone(),
        t.getStatus().name(),
        t.getPlannedStartDate(),
        t.getActualStartDate(),
        t.getEndDate(),
        t.getMonthlyRentSnapshot(),
        t.getRoomAddressSnapshot(),
        reviews.findByTenancyId(t.getId()).map(Review::getId).orElse(null));
  }

  public ReviewRow review(Review r) {
    return new ReviewRow(
        r.getId(),
        r.getTenancy().getId(),
        r.getTenancy().getTenant().getFullName(),
        r.getTenancy().getRequest().getTitleSnapshot(),
        r.getRating(),
        r.getComment(),
        r.getStatus().name(),
        r.getCreatedAt());
  }
}
