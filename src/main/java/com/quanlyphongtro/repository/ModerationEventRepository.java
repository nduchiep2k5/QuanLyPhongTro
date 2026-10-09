package com.quanlyphongtro.repository;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ModerationEventRepository extends JpaRepository<ModerationEvent, Long> {
  List<ModerationEvent> findAllByOrderByCreatedAtDesc();
}
