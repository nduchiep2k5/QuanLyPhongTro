package com.quanlyphongtro.service;

import com.quanlyphongtro.exception.BusinessException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
  private final Path root;

  public StorageService(@Value("${app.upload-dir}") String path) {
    root = Path.of(path).toAbsolutePath().normalize();
  }

  public String store(MultipartFile file) {
    if (file.getSize() > 5 * 1024 * 1024 || file.isEmpty())
      throw new BusinessException("Ảnh phải có dữ liệu và không quá 5 MB.");
    try (var stream = ImageIO.createImageInputStream(file.getInputStream())) {
      var it = ImageIO.getImageReaders(stream);
      if (!it.hasNext()) throw new BusinessException("Chỉ hỗ trợ ảnh JPEG hoặc PNG hợp lệ.");
      ImageReader reader = it.next();
      try {
        reader.setInput(stream);
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!Set.of("jpeg", "jpg", "png").contains(format))
          throw new BusinessException("Chỉ hỗ trợ JPEG/PNG.");
        int w = reader.getWidth(0), h = reader.getHeight(0);
        if (w <= 0 || h <= 0 || (long) w * h > 20_000_000)
          throw new BusinessException("Ảnh không được vượt 20 triệu pixel.");
        var image = reader.read(0);
        Files.createDirectories(root);
        String key = UUID.randomUUID() + ".png";
        ImageIO.write(image, "png", root.resolve(key).toFile());
        return key;
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      throw new BusinessException(
          "Không thể đọc hoặc lưu ảnh. Kiểm tra dung lượng và quyền thư mục upload.");
    }
  }

  public Resource read(String key) {
    Path path = resolve(key);
    if (!Files.isRegularFile(path)) throw AccessService.missing();
    return new FileSystemResource(path);
  }

  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException e) {
      org.slf4j.LoggerFactory.getLogger(StorageService.class)
          .warn("Không thể dọn file upload; cần kiểm tra thư mục.");
    }
  }

  private Path resolve(String key) {
    if (key == null || !key.matches("[A-Za-z0-9-]+\\.png")) throw AccessService.missing();
    var p = root.resolve(key).normalize();
    if (!p.getParent().equals(root)) throw AccessService.forbidden();
    return p;
  }
}
