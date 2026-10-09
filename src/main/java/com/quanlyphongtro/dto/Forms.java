package com.quanlyphongtro.dto;

import com.quanlyphongtro.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

public final class Forms {
  @Getter
  @Setter
  public static class Register {
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 120)
    private String fullName;

    @NotBlank
    @Size(min = 10, max = 64)
    private String password;

    @NotNull private Role role = Role.TENANT;
  }

  @Getter
  @Setter
  public static class Profile {
    @NotBlank
    @Size(max = 120)
    private String fullName;

    @Pattern(regexp = "[+0-9 ()-]{0,20}")
    private String phone;
  }

  @Getter
  @Setter
  public static class Password {
    @NotBlank private String currentPassword;

    @NotBlank
    @Size(min = 10, max = 64)
    private String newPassword;
  }

  @Getter
  @Setter
  public static class RoomForm {
    private Long roomId;
    private long version;
    private long roomVersion;
    @NotNull private Long categoryId;

    @NotBlank
    @Size(max = 40)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String province;

    @NotBlank
    @Size(max = 100)
    private String ward;

    @NotBlank
    @Size(max = 255)
    private String addressLine;

    @DecimalMin("-90")
    @DecimalMax("90")
    private BigDecimal latitude;

    @DecimalMin("-180")
    @DecimalMax("180")
    private BigDecimal longitude;

    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("999999.99")
    private BigDecimal area;

    @Min(1)
    @Max(100)
    private int maxOccupants = 2;

    @NotBlank
    @Size(max = 180)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String description;

    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("999999999999.99")
    private BigDecimal monthlyRent;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("999999999999.99")
    private BigDecimal deposit = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("999999999999.99")
    private BigDecimal electricityUnitPrice = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("999999999999.99")
    private BigDecimal waterUnitPrice = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("999999999999.99")
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @NotNull private WaterBillingMethod waterBillingMethod = WaterBillingMethod.PER_M3;

    @NotNull
    private LocalDate availableFrom = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

    private List<Long> amenityIds = new ArrayList<>();
  }

  @Getter
  @Setter
  public static class Request {
    @NotNull private LocalDate requestedMoveIn;

    @Size(max = 1000)
    private String message;
  }

  @Getter
  @Setter
  public static class ReviewForm {
    @Min(1)
    @Max(5)
    private int rating = 5;

    @NotBlank
    @Size(max = 2000)
    private String comment;
  }
}
