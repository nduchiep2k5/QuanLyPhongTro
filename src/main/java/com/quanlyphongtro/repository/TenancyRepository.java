package com.quanlyphongtro.repository;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TenancyRepository extends JpaRepository<Tenancy, Long> {
  List<Tenancy> findByRoomLandlordIdOrderByCreatedAtDesc(Long id);

  List<Tenancy> findByTenantIdOrderByCreatedAtDesc(Long id);

  Optional<Tenancy> findByRequestId(Long id);

  boolean existsByRoomIdAndStatusIn(Long id, Collection<TenancyStatus> statuses);

  @Query("select t.room.id from Tenancy t where t.id=:id")
  Optional<Long> roomIdFor(@Param("id") Long id);
}
