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
@Table(name = "tenancies")
public class Tenancy extends BaseEntity {
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, unique = true)
  private RentalRequest request;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Room room;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Account tenant;

  @Enumerated(EnumType.STRING)
  private TenancyStatus status;

  private LocalDate plannedStartDate;

  private LocalDate actualStartDate;

  private LocalDate endDate;

  @Column(precision = 14, scale = 2)
  private BigDecimal monthlyRentSnapshot;

  @Column(precision = 14, scale = 2)
  private BigDecimal depositSnapshot;

  @Column(length = 500)
  private String roomAddressSnapshot;

  @Column(length = 1000)
  private String closureReason;
}
