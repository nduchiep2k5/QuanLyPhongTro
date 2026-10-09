package com.quanlyphongtro.entity;

import com.quanlyphongtro.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "listings")
public class Listing extends BaseEntity {
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, unique = true)
  private Room room;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal monthlyRent;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal deposit;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal electricityUnitPrice;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal waterUnitPrice;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal serviceFee;

  @Enumerated(EnumType.STRING)
  private WaterBillingMethod waterBillingMethod;

  @Column(nullable = false)
  private LocalDate availableFrom;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ListingStatus moderationStatus;

  private boolean visible;

  private Instant submittedAt;

  private Instant reviewedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  private Account reviewedBy;

  @Column(length = 1000)
  private String moderationReason;
}
