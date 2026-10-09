package com.quanlyphongtro.entity;

import com.quanlyphongtro.enums.*;
import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "room_images")
public class RoomImage extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Room room;

  @Column(nullable = false, unique = true)
  private String storageKey;

  private String altText;

  private int sortOrder;

  @Column(name = "is_cover")
  private boolean cover;
}
