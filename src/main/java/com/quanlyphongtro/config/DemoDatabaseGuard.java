package com.quanlyphongtro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.*;

/** Prevent the demo profile from running migrations against the user's main DB. */
@Configuration
@Profile("demo")
public class DemoDatabaseGuard {
  @Bean
  FlywayMigrationStrategy guardedMigration(@Value("${spring.datasource.url}") String url) {
    return flyway -> {
      if (!url.split("\\?")[0].endsWith("_demo"))
        throw new IllegalStateException(
            "Profile demo chỉ được dùng với database có tên kết thúc _demo.");
      flyway.migrate();
    };
  }
}
