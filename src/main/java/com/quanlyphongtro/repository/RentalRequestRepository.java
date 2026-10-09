package com.quanlyphongtro.repository;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RentalRequestRepository extends JpaRepository<RentalRequest, Long> {
  List<RentalRequest> findByListingRoomIdAndStatus(Long id, RequestStatus status);

  boolean existsByTenantIdAndListingIdAndStatus(Long tenant, Long listing, RequestStatus status);

  List<RentalRequest> findByTenantIdOrderByCreatedAtDesc(Long id);

  List<RentalRequest> findByListingRoomLandlordIdOrderByCreatedAtDesc(Long id);

  @Query("select q.listing.room.id from RentalRequest q where q.id=:id")
  Optional<Long> roomIdFor(@Param("id") Long id);
}
