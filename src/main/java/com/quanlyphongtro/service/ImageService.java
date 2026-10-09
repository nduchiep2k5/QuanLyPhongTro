package com.quanlyphongtro.service;

import com.quanlyphongtro.enums.Role;
import com.quanlyphongtro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageService {
  private final RoomImageRepository images;
  private final ListingRepository listings;
  private final AccountRepository accounts;
  private final StorageService storage;

  @Transactional(readOnly = true)
  public Resource get(Long imageId, String viewer) {
    var im = images.findById(imageId).orElseThrow(AccessService::missing);
    var l = listings.findByRoomId(im.getRoom().getId()).orElseThrow(AccessService::missing);
    if (!AccessService.publicRoom(l)) {
      var a = viewer == null ? null : accounts.findByEmailIgnoreCase(viewer).orElse(null);
      if (a == null
          || a.getStatus() != com.quanlyphongtro.enums.AccountStatus.ACTIVE
          || a.getRole() != Role.ADMIN && !im.getRoom().getLandlord().getId().equals(a.getId()))
        throw AccessService.missing();
    }
    return storage.read(im.getStorageKey());
  }
}
