package com.quanlyphongtro.entity;

import com.quanlyphongtro.enums.*;
import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, unique = true)
  private Tenancy tenancy;

  private int rating;

  @Column(nullable = false, length = 2000)
  private String comment;

  @Enumerated(EnumType.STRING)
  private ReviewStatus status;
}
