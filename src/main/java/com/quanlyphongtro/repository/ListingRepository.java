package com.quanlyphongtro.repository;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ListingRepository
    extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
  Optional<Listing> findByRoomId(Long roomId);

  List<Listing> findByRoomLandlordIdOrderByCreatedAtDesc(Long id);

  List<Listing> findAllByOrderByCreatedAtDesc();
}
