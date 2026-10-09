package com.quanlyphongtro.repository;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
  Optional<Favorite> findByAccountIdAndRoomId(Long user, Long room);

  List<Favorite> findByAccountIdOrderByCreatedAtDesc(Long id);
}
