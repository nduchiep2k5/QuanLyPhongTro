package com.quanlyphongtro.entity;

import com.quanlyphongtro.enums.*;
import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "moderation_events")
public class ModerationEvent extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Account actor;

  @ManyToOne(fetch = FetchType.LAZY)
  private Listing listing;

  @ManyToOne(fetch = FetchType.LAZY)
  private Review review;

  @Column(length = 30)
  private String action;

  @Column(length = 30)
  private String fromStatus;

  @Column(length = 30)
  private String toStatus;

  @Column(length = 1000)
  private String reason;
}
