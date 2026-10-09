package com.quanlyphongtro.repository;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  Optional<Review> findByTenancyId(Long id);

  List<Review> findByTenancyRoomIdAndStatusOrderByCreatedAtDesc(Long id, ReviewStatus status);

  List<Review> findAllByOrderByCreatedAtDesc();
}
