package com.quanlyphongtro.repository;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> findByEmailIgnoreCase(String email);

  List<Account> findAllByOrderByCreatedAtDesc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Account a where a.id=:id")
  Optional<Account> lock(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Account a where a.role=com.quanlyphongtro.enums.Role.ADMIN order by a.id")
  List<Account> lockAdmins();
}
