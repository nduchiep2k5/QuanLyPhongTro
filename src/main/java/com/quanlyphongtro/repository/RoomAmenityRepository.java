package com.quanlyphongtro.repository;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface RoomAmenityRepository extends JpaRepository<RoomAmenity, Long> {
  List<RoomAmenity> findByRoomId(Long id);

  void deleteByRoomId(Long id);
}
