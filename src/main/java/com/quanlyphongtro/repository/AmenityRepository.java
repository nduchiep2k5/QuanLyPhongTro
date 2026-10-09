package com.quanlyphongtro.repository;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {
  List<Amenity> findAllByOrderByNameAsc();

  List<Amenity> findByActiveTrueOrderByNameAsc();
}
