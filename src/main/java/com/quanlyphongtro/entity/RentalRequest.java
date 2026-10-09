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
@Table(name = "rental_requests")
public class RentalRequest extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Listing listing;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Account tenant;

  @Enumerated(EnumType.STRING)
  private RequestStatus status;

  @Column(nullable = false)
  private LocalDate requestedMoveIn;

  @Column(length = 1000)
  private String message;

  private long listingVersionSnapshot;

  @Column(precision = 14, scale = 2)
  private BigDecimal rentSnapshot;

  @Column(precision = 14, scale = 2)
  private BigDecimal depositSnapshot;

  @Column(length = 180)
  private String titleSnapshot;

  private Instant processedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  private Account processedBy;

  @Column(length = 1000)
  private String decisionReason;
}
