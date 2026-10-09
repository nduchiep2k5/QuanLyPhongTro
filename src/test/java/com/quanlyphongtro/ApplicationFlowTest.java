package com.quanlyphongtro;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.quanlyphongtro.dto.Forms;
import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import com.quanlyphongtro.exception.BusinessException;
import com.quanlyphongtro.repository.*;
import com.quanlyphongtro.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(resolver = ApplicationFlowTest.Profile.class)
class ApplicationFlowTest {
  public static class Profile implements ActiveProfilesResolver {
    public String[] resolve(Class<?> c) {
      String profile = System.getProperty("integration.profile", "test");
      if (profile.equals("postgres-test")) {
        String url = System.getenv("TEST_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql://[^/]+/[^?;]+_test(\\?.*)?"))
          throw new IllegalStateException("TEST_DB_URL phải trỏ database riêng kết thúc _test.");
      }
      return new String[] {profile};
    }
  }

  @Autowired MockMvc mvc;
  @Autowired AccountRepository accounts;
  @Autowired RoomRepository rooms;
  @Autowired ListingRepository listings;
  @Autowired CategoryRepository categories;
  @Autowired AmenityRepository amenities;
  @Autowired RoomImageRepository images;
  @Autowired RentalRequestRepository requests;
  @Autowired TenancyRepository tenancies;
  @Autowired PasswordResetTokenRepository tokens;
  @Autowired ReviewRepository reviews;
  @Autowired AuthService auth;
  @Autowired ListingService listingService;
  @Autowired RoomQueryService query;
  @Autowired RentalService rentals;
  @Autowired TenantService tenants;
  @Autowired AdminService adminService;
  @Autowired PasswordEncoder encoder;
  @Autowired TransactionTemplate tx;
  Account owner, otherOwner, tenant, otherTenant, admin;
  Long roomId;

  @BeforeEach
  void fixture() {
    owner = account(Role.LANDLORD);
    otherOwner = account(Role.LANDLORD);
    tenant = account(Role.TENANT);
    otherTenant = account(Role.TENANT);
    admin = account(Role.ADMIN);
    var c = new Category();
    c.setName("Loại " + UUID.randomUUID());
    c.setActive(true);
    categories.save(c);
    var f = new Forms.RoomForm();
    f.setCategoryId(c.getId());
    f.setCode("P" + System.nanoTime());
    f.setProvince("Hà Nội");
    f.setWard("Minh Khai");
    f.setAddressLine("Địa chỉ kiểm thử");
    f.setArea(new BigDecimal("25"));
    f.setTitle("Phòng kiểm thử");
    f.setDescription("Thông tin phòng kiểm thử");
    f.setMonthlyRent(new BigDecimal("2500000"));
    roomId = listingService.save(owner.getId(), f, List.of());
    var im = new RoomImage();
    im.setRoom(rooms.findById(roomId).orElseThrow());
    im.setStorageKey(UUID.randomUUID() + ".png");
    im.setAltText("Test");
    im.setCover(true);
    images.save(im);
    listingService.action(roomId, owner.getId(), "submit");
    var version = listings.findByRoomId(roomId).orElseThrow().getVersion();
    listingService.moderate(roomId, admin.getId(), "approve", "", version);
    listingService.action(roomId, owner.getId(), "show");
  }

  Account account(Role role) {
    var a = new Account();
    a.setEmail(UUID.randomUUID() + "@test.invalid");
    a.setFullName("Tài khoản test");
    a.setRole(role);
    a.setStatus(AccountStatus.ACTIVE);
    a.setPasswordHash(encoder.encode("DemoPassword123"));
    return accounts.save(a);
  }

  Long send(Account a) {
    var f = new Forms.Request();
    f.setRequestedMoveIn(RentalService.today());
    f.setMessage("Tôi muốn thuê phòng");
    rentals.send(roomId, a.getId(), f);
    return rentals.requests(a.getId(), false).get(0).id();
  }

  @Test
  void registrationNormalizesEmailAndRejectsAdmin() {
    var f = new Forms.Register();
    String email = UUID.randomUUID() + "@TEST.INVALID";
    f.setEmail(" " + email + " ");
    f.setFullName("Người dùng");
    f.setPassword("Password1234");
    f.setRole(Role.TENANT);
    auth.register(f);
    assertThat(accounts.findByEmailIgnoreCase(email).orElseThrow().getEmail())
        .isEqualTo(email.toLowerCase(Locale.ROOT));
    assertThatThrownBy(() -> auth.register(f)).isInstanceOf(BusinessException.class);
    f.setEmail(UUID.randomUUID() + "@test.invalid");
    f.setRole(Role.ADMIN);
    assertThatThrownBy(() -> auth.register(f)).isInstanceOf(BusinessException.class);
  }

  @Test
  void csrfAndRoleBoundaries() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk());
    mvc.perform(get("/rooms")).andExpect(status().isOk());
    mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    mvc.perform(get("/admin").with(user(tenant.getEmail()).roles("TENANT")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/tenant/favorites/" + roomId).with(user(tenant.getEmail()).roles("TENANT")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/tenant/favorites/" + roomId)
                .with(user(tenant.getEmail()).roles("TENANT"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void wrongOwnerAndTenantCannotMutate() {
    Long q = send(tenant);
    assertThatThrownBy(() -> rentals.decide(q, otherOwner.getId(), true, ""))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> rentals.cancel(q, otherTenant.getId()))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> listingService.action(roomId, otherOwner.getId(), "hide"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void rentalLifecycleAndSnapshots() {
    Long q = send(tenant);
    assertThatThrownBy(() -> send(tenant)).isInstanceOf(BusinessException.class);
    Long q2 = send(otherTenant);
    rentals.decide(q, owner.getId(), true, "");
    assertThat(rooms.findById(roomId).orElseThrow().getStatus()).isEqualTo(RoomStatus.RESERVED);
    assertThat(requests.findById(q2).orElseThrow().getStatus()).isEqualTo(RequestStatus.REJECTED);
    var t = tenancies.findByRequestId(q).orElseThrow();
    var rf = new Forms.ReviewForm();
    rf.setComment("Rất phù hợp");
    assertThatThrownBy(() -> tenants.review(t.getId(), tenant.getId(), rf))
        .isInstanceOf(BusinessException.class);
    rentals.tenancy(t.getId(), owner.getId(), "receive", RentalService.today(), "", false);
    tenants.review(t.getId(), tenant.getId(), rf);
    tenants.review(t.getId(), tenant.getId(), rf);
    assertThat(reviews.findByTenancyId(t.getId())).isPresent();
    rentals.tenancy(
        t.getId(), owner.getId(), "end", RentalService.today(), "Kết thúc theo thỏa thuận", false);
    assertThat(rooms.findById(roomId).orElseThrow().getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    assertThat(listings.findByRoomId(roomId).orElseThrow().isVisible()).isFalse();
    assertThat(tenancies.findById(t.getId()).orElseThrow().getMonthlyRentSnapshot())
        .isEqualByComparingTo("2500000");
  }

  @Test
  void editCancelsPendingAndReentersModeration() {
    Long q = send(tenant);
    var f = query.edit(roomId, owner.getId());
    f.setTitle("Tiêu đề cập nhật");
    listingService.save(owner.getId(), f, List.of());
    assertThat(requests.findById(q).orElseThrow().getStatus()).isEqualTo(RequestStatus.CANCELLED);
    assertThat(listings.findByRoomId(roomId).orElseThrow().getModerationStatus())
        .isEqualTo(ListingStatus.PENDING_REVIEW);
    assertThatThrownBy(() -> query.detail(roomId, null)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> listingService.save(owner.getId(), f, List.of()))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void simultaneousAcceptHasExactlyOneWinner() throws Exception {
    Long first = send(tenant), second = send(otherTenant);
    var gate = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<Boolean>> result = new ArrayList<>();
      for (Long id : List.of(first, second))
        result.add(
            pool.submit(
                () -> {
                  gate.await();
                  try {
                    rentals.decide(id, owner.getId(), true, "");
                    return true;
                  } catch (BusinessException e) {
                    return false;
                  }
                }));
      gate.countDown();
      int wins = 0;
      for (var future : result) if (future.get(15, TimeUnit.SECONDS)) wins++;
      assertThat(wins).isEqualTo(1);
      assertThat(rentals.tenancies(owner.getId(), true)).hasSize(1);
      assertThat(rooms.findById(roomId).orElseThrow().getStatus()).isEqualTo(RoomStatus.RESERVED);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void favoriteIsIdempotentAndHiddenContentDoesNotLeak() throws Exception {
    tenants.favorite(roomId, tenant.getId(), true);
    tenants.favorite(roomId, tenant.getId(), true);
    assertThat(tenants.favorites(tenant.getId())).hasSize(1);
    listingService.action(roomId, owner.getId(), "hide");
    assertThat(tenants.favorites(tenant.getId()).get(0).get("room")).isNull();
    mvc.perform(get("/rooms/" + roomId)).andExpect(status().isNotFound());
    Long image = images.findByRoomIdOrderBySortOrderAsc(roomId).get(0).getId();
    mvc.perform(get("/images/" + image)).andExpect(status().isNotFound());
    mvc.perform(get("/tenant/favorites").with(user(tenant.getEmail()).roles("TENANT")))
        .andExpect(status().isOk());
  }

  @Test
  void resetTokenIsSingleUseAndExpiredTokenFails() {
    String[] reset = tx.execute(s -> auth.createReset(tenant.getEmail()));
    assertThat(reset).isNotNull();
    assertThat(
            tokens.findAll().stream()
                .anyMatch(t -> t.getTokenHash().equals(AuthService.hash(reset[1]))))
        .isTrue();
    auth.reset(reset[1], "NewPassword123");
    assertThat(
            encoder.matches(
                "NewPassword123",
                accounts.findById(tenant.getId()).orElseThrow().getPasswordHash()))
        .isTrue();
    assertThatThrownBy(() -> auth.reset(reset[1], "AnotherPassword123"))
        .isInstanceOf(BusinessException.class);
    String[] expired = tx.execute(s -> auth.createReset(tenant.getEmail()));
    tx.executeWithoutResult(
        s ->
            tokens
                .findByTokenHash(AuthService.hash(expired[1]))
                .orElseThrow()
                .setExpiresAt(Instant.now().minusSeconds(1)));
    assertThatThrownBy(() -> auth.reset(expired[1], "AnotherPassword123"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void bannedPrincipalIsRemovedAndPendingRequestCancelled() throws Exception {
    Long q = send(tenant);
    adminService.accountStatus(tenant.getId(), admin.getId(), true);
    assertThat(requests.findById(q).orElseThrow().getStatus()).isEqualTo(RequestStatus.CANCELLED);
    mvc.perform(get("/tenant/requests").with(user(tenant.getEmail()).roles("TENANT")))
        .andExpect(status().is3xxRedirection());
    assertThatThrownBy(() -> adminService.accountStatus(admin.getId(), admin.getId(), true))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void reviewHiddenByAdminDoesNotReappearOnEdit() {
    Long q = send(tenant);
    rentals.decide(q, owner.getId(), true, "");
    var t = tenancies.findByRequestId(q).orElseThrow();
    rentals.tenancy(t.getId(), owner.getId(), "receive", RentalService.today(), "", false);
    var f = new Forms.ReviewForm();
    f.setComment("Nhận xét");
    tenants.review(t.getId(), tenant.getId(), f);
    var r = reviews.findByTenancyId(t.getId()).orElseThrow();
    tenants.moderateReview(r.getId(), admin.getId(), true, "Cần kiểm tra nội dung");
    f.setComment("Đã cập nhật");
    tenants.review(t.getId(), tenant.getId(), f);
    assertThat(reviews.findById(r.getId()).orElseThrow().getStatus())
        .isEqualTo(ReviewStatus.HIDDEN);
    tenants.deleteReview(t.getId(), tenant.getId());
    assertThatThrownBy(() -> tenants.review(t.getId(), tenant.getId(), f))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void invalidUploadAndFilterRejected() {
    var f = query.edit(roomId, owner.getId());
    assertThatThrownBy(
            () ->
                listingService.save(
                    owner.getId(),
                    f,
                    List.of(
                        new MockMultipartFile(
                            "files", "fake.png", "image/png", "not an image".getBytes()))))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                query.search(
                    null,
                    null,
                    null,
                    new BigDecimal(100),
                    new BigDecimal(1),
                    null,
                    null,
                    null,
                    null,
                    "newest",
                    0))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                query.search(
                    null, null, null, null, null, null, null, null, null, "passwordHash", 0))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void allRolePagesRenderAndNoTemplateErrors() throws Exception {
    for (String p :
        List.of(
            "/auth/login",
            "/auth/register",
            "/auth/forgot",
            "/auth/reset?token=test",
            "/rooms/" + roomId)) mvc.perform(get(p)).andExpect(status().isOk());
    for (String p :
        List.of("/profile", "/tenant/requests", "/tenant/tenancies", "/tenant/favorites"))
      mvc.perform(get(p).with(user(tenant.getEmail()).roles("TENANT"))).andExpect(status().isOk());
    for (String p :
        List.of(
            "/landlord/rooms",
            "/landlord/rooms/new",
            "/landlord/rooms/" + roomId + "/edit",
            "/landlord/requests",
            "/landlord/tenancies"))
      mvc.perform(get(p).with(user(owner.getEmail()).roles("LANDLORD"))).andExpect(status().isOk());
    for (String p :
        List.of(
            "/admin",
            "/admin/users",
            "/admin/listings",
            "/admin/reviews",
            "/admin/catalog",
            "/admin/report.csv"))
      mvc.perform(get(p).with(user(admin.getEmail()).roles("ADMIN"))).andExpect(status().isOk());
  }
}
