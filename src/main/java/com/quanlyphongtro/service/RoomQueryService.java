package com.quanlyphongtro.service;

import com.quanlyphongtro.dto.Forms;
import com.quanlyphongtro.dto.Views.*;
import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import com.quanlyphongtro.repository.*;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomQueryService {
  private final ListingRepository listings;
  private final RoomRepository rooms;
  private final RoomAmenityRepository amenities;
  private final ViewMapper mapper;
  private final AccessService access;

  public Page<RoomCard> search(
      String q,
      String province,
      String ward,
      BigDecimal min,
      BigDecimal max,
      BigDecimal minArea,
      BigDecimal maxArea,
      Long category,
      List<Long> amenityIds,
      String order,
      int page) {
    if (page < 0
        || page > 10000
        || min != null && min.signum() < 0
        || max != null && max.signum() < 0
        || min != null && max != null && min.compareTo(max) > 0
        || minArea != null && minArea.signum() < 0
        || maxArea != null && maxArea.signum() < 0
        || minArea != null && maxArea != null && minArea.compareTo(maxArea) > 0)
      throw new com.quanlyphongtro.exception.BusinessException(
          "Khoảng lọc hoặc trang không hợp lệ.");
    String field =
        switch (order == null ? "newest" : order) {
          case "priceAsc", "priceDesc" -> "monthlyRent";
          case "area" -> "room.area";
          case "newest" -> "createdAt";
          default ->
              throw new com.quanlyphongtro.exception.BusinessException("Sắp xếp không hợp lệ.");
        };
    Sort.Direction dir = "priceAsc".equals(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Specification<Listing> spec =
        (l, query, cb) -> {
          var r = l.join("room");
          var owner = r.join("landlord");
          List<Predicate> p = new ArrayList<>();
          p.add(cb.equal(l.get("moderationStatus"), ListingStatus.APPROVED));
          p.add(cb.isTrue(l.get("visible")));
          p.add(cb.equal(r.get("status"), RoomStatus.AVAILABLE));
          p.add(cb.equal(owner.get("status"), AccountStatus.ACTIVE));
          if (q != null && !q.isBlank()) {
            String like =
                "%"
                    + q.trim()
                        .toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_")
                    + "%";
            p.add(
                cb.or(
                    cb.like(cb.lower(l.get("title")), like, '\\'),
                    cb.like(cb.lower(r.get("addressLine")), like, '\\')));
          }
          if (province != null && !province.isBlank())
            p.add(cb.equal(cb.lower(r.get("province")), province.trim().toLowerCase(Locale.ROOT)));
          if (ward != null && !ward.isBlank())
            p.add(cb.equal(cb.lower(r.get("ward")), ward.trim().toLowerCase(Locale.ROOT)));
          if (min != null) p.add(cb.ge(l.get("monthlyRent"), min));
          if (max != null) p.add(cb.le(l.get("monthlyRent"), max));
          if (minArea != null) p.add(cb.ge(r.get("area"), minArea));
          if (maxArea != null) p.add(cb.le(r.get("area"), maxArea));
          if (category != null) p.add(cb.equal(r.get("category").get("id"), category));
          if (amenityIds != null)
            for (Long aid : new HashSet<>(amenityIds)) {
              var sub = query.subquery(Long.class);
              var ra = sub.from(RoomAmenity.class);
              sub.select(ra.get("id"))
                  .where(cb.equal(ra.get("room"), r), cb.equal(ra.get("amenity").get("id"), aid));
              p.add(cb.exists(sub));
            }
          return cb.and(p.toArray(Predicate[]::new));
        };
    return listings
        .findAll(spec, PageRequest.of(page, 12, Sort.by(dir, field).and(Sort.by("id"))))
        .map(mapper::card);
  }

  public RoomCard detail(Long roomId, Long viewerId) {
    var l = listings.findByRoomId(roomId).orElseThrow(AccessService::missing);
    if (!AccessService.publicRoom(l)) {
      if (viewerId == null) throw AccessService.missing();
      var r = l.getRoom();
      boolean allowed = r.getLandlord().getId().equals(viewerId);
      if (!allowed)
        try {
          access.role(viewerId, Role.ADMIN);
          allowed = true;
        } catch (RuntimeException e) {
        }
      if (!allowed) throw AccessService.missing();
    }
    return mapper.card(l);
  }

  public List<RoomCard> owned(Long id) {
    access.role(id, Role.LANDLORD);
    return listings.findByRoomLandlordIdOrderByCreatedAtDesc(id).stream()
        .map(mapper::card)
        .toList();
  }

  public Forms.RoomForm edit(Long roomId, Long owner) {
    var l = listings.findByRoomId(roomId).orElseThrow(AccessService::missing);
    var r = l.getRoom();
    access.owner(r, owner);
    var f = new Forms.RoomForm();
    f.setRoomId(r.getId());
    f.setVersion(l.getVersion());
    f.setRoomVersion(r.getVersion());
    f.setCategoryId(r.getCategory().getId());
    f.setCode(r.getCode());
    f.setProvince(r.getProvince());
    f.setWard(r.getWard());
    f.setAddressLine(r.getAddressLine());
    f.setLatitude(r.getLatitude());
    f.setLongitude(r.getLongitude());
    f.setArea(r.getArea());
    f.setMaxOccupants(r.getMaxOccupants());
    f.setTitle(l.getTitle());
    f.setDescription(l.getDescription());
    f.setMonthlyRent(l.getMonthlyRent());
    f.setDeposit(l.getDeposit());
    f.setElectricityUnitPrice(l.getElectricityUnitPrice());
    f.setWaterUnitPrice(l.getWaterUnitPrice());
    f.setWaterBillingMethod(l.getWaterBillingMethod());
    f.setServiceFee(l.getServiceFee());
    f.setAvailableFrom(l.getAvailableFrom());
    f.setAmenityIds(
        amenities.findByRoomId(roomId).stream().map(a -> a.getAmenity().getId()).toList());
    return f;
  }
}
