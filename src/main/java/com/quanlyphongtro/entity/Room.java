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
@Table(name = "rooms")
public class Room extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Account landlord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Category category;

  @Column(nullable = false, length = 40)
  private String code;

  @Column(nullable = false, length = 100)
  private String province;

  @Column(nullable = false, length = 100)
  private String ward;

  @Column(nullable = false)
  private String addressLine;

  @Column(precision = 9, scale = 6)
  private BigDecimal latitude;

  @Column(precision = 9, scale = 6)
  private BigDecimal longitude;

  @Column(nullable = false, precision = 8, scale = 2)
  private BigDecimal area;

  private int maxOccupants;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RoomStatus status;
}
