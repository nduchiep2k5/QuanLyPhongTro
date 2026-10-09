package com.quanlyphongtro.entity;

import com.quanlyphongtro.enums.*;
import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {
  @Column(nullable = false, length = 254)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false, length = 120)
  private String fullName;

  @Column(length = 20)
  private String phone;

  private String avatarStorageKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountStatus status;
}
