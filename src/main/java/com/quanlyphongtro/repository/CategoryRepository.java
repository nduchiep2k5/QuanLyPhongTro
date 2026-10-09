package com.quanlyphongtro.repository;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  List<Category> findAllByOrderByNameAsc();

  List<Category> findByActiveTrueOrderByNameAsc();
}
