package com.quanlyphongtro.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class Views {
  public record Image(Long id, String altText, boolean cover) {}

  public record RoomCard(
      Long id,
      Long listingId,
      String title,
      String description,
      String category,
      String province,
      String ward,
      String addressLine,
      BigDecimal latitude,
      BigDecimal longitude,
      BigDecimal area,
      int maxOccupants,
      BigDecimal monthlyRent,
      BigDecimal deposit,
      BigDecimal electricityUnitPrice,
      BigDecimal waterUnitPrice,
      String waterBillingMethod,
      BigDecimal serviceFee,
      LocalDate availableFrom,
      String roomStatus,
      String listingStatus,
      boolean visible,
      boolean publicVisible,
      long version,
      String ownerName,
      String ownerPhone,
      List<Image> images,
      List<String> amenities,
      String moderationReason) {}

  public record RequestRow(
      Long id,
      Long roomId,
      String title,
      String tenantName,
      String tenantPhone,
      String status,
      LocalDate requestedMoveIn,
      BigDecimal rent,
      String message,
      String reason,
      Instant createdAt) {}

  public record TenancyRow(
      Long id,
      Long roomId,
      String title,
      String tenantName,
      String tenantPhone,
      String status,
      LocalDate plannedStartDate,
      LocalDate actualStartDate,
      LocalDate endDate,
      BigDecimal rent,
      String address,
      Long reviewId) {}

  public record ReviewRow(
      Long id,
      Long tenancyId,
      String author,
      String title,
      int rating,
      String comment,
      String status,
      Instant createdAt) {}

  public record AuditRow(
      String actor,
      String target,
      String action,
      String fromStatus,
      String toStatus,
      String reason,
      Instant createdAt) {}
}
