package com.quanlyphongtro;

import static org.assertj.core.api.Assertions.*;

import com.quanlyphongtro.exception.BusinessException;
import com.quanlyphongtro.service.*;
import org.junit.jupiter.api.Test;

class BusinessRulesTest {
  @Test
  void passwordLimitCountsUtf8Bytes() {
    AuthService.checkPassword("A good long password");
    assertThatThrownBy(() -> AuthService.checkPassword("á".repeat(40)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> AuthService.checkPassword("short"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void csvEscapesFormulaAndQuotes() {
    assertThat(AdminService.csvCell("=1+1")).isEqualTo("\"'=1+1\"");
    assertThat(AdminService.csvCell("hello, \"world\"")).isEqualTo("\"hello, \"\"world\"\"\"");
  }

  @Test
  void reasonsRequiredForRejection() {
    assertThatThrownBy(() -> RentalService.checkReason(" ")).isInstanceOf(BusinessException.class);
    RentalService.checkReason("Không đáp ứng nhu cầu");
  }
}
