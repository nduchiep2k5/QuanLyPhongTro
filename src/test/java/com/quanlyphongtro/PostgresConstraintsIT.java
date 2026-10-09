package com.quanlyphongtro;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Opt-in native PostgreSQL checks; run only with -Dintegration.profile=postgres-test. */
class PostgresConstraintsIT extends ApplicationFlowTest {
  @Autowired JdbcTemplate jdbc;

  @BeforeAll
  static void requirePostgres() {
    if (!System.getProperty("integration.profile", "").equals("postgres-test"))
      throw new IllegalStateException(
          "Chạy với -Dintegration.profile=postgres-test và database _test riêng.");
  }

  @Test
  void postgresPartialIndexesExist() {
    var names =
        jdbc.queryForList(
            "select indexname from pg_indexes where schemaname=current_schema() and indexdef like"
                + " '%WHERE%'",
            String.class);
    assertThat(names).contains("ux_pending_request", "ux_active_tenancy", "ux_room_cover");
  }

  @Test
  void postgresRejectsDuplicatePendingRequest() {
    Long id = send(tenant);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into"
                        + " rental_requests(created_at,updated_at,listing_id,tenant_id,status,requested_move_in,listing_version_snapshot,rent_snapshot,deposit_snapshot,title_snapshot)"
                        + " select"
                        + " created_at,updated_at,listing_id,tenant_id,status,requested_move_in,listing_version_snapshot,rent_snapshot,deposit_snapshot,title_snapshot"
                        + " from rental_requests where id=?",
                    id))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void postgresRejectsInvalidAreaAndDuplicateCover() {
    assertThatThrownBy(() -> jdbc.update("update rooms set area=-1 where id=?", roomId))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into"
                        + " room_images(created_at,updated_at,room_id,storage_key,sort_order,is_cover)"
                        + " values(now(),now(),?,'constraint-test-'||gen_random_uuid()||'.png',1,true)",
                    roomId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
