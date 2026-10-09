package com.quanlyphongtro.config;

import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import com.quanlyphongtro.repository.*;
import com.quanlyphongtro.service.*;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
@ConditionalOnProperty(name = "app.demo-seed", havingValue = "true")
public class DemoSeeder implements CommandLineRunner {
  private final AccountRepository accounts;
  private final RoomRepository rooms;
  private final ListingRepository listings;
  private final CategoryRepository categories;
  private final AmenityRepository amenities;
  private final RoomAmenityRepository roomAmenities;
  private final RoomImageRepository images;
  private final RentalRequestRepository requests;
  private final TenancyRepository tenancies;
  private final ReviewRepository reviews;
  private final PasswordEncoder encoder;
  private final String adminPassword, userPassword, url, upload;

  public DemoSeeder(
      AccountRepository accounts,
      RoomRepository rooms,
      ListingRepository listings,
      CategoryRepository categories,
      AmenityRepository amenities,
      RoomAmenityRepository roomAmenities,
      RoomImageRepository images,
      RentalRequestRepository requests,
      TenancyRepository tenancies,
      ReviewRepository reviews,
      PasswordEncoder encoder,
      @Value("${app.demo-admin-password}") String adminPassword,
      @Value("${app.demo-user-password}") String userPassword,
      @Value("${spring.datasource.url}") String url,
      @Value("${app.upload-dir}") String upload) {
    this.accounts = accounts;
    this.rooms = rooms;
    this.listings = listings;
    this.categories = categories;
    this.amenities = amenities;
    this.roomAmenities = roomAmenities;
    this.images = images;
    this.requests = requests;
    this.tenancies = tenancies;
    this.reviews = reviews;
    this.encoder = encoder;
    this.adminPassword = adminPassword;
    this.userPassword = userPassword;
    this.url = url;
    this.upload = upload;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    String db = url.split("\\?")[0];
    if (!db.endsWith("_demo"))
      throw new IllegalStateException("Seed chỉ cho database có tên kết thúc _demo.");
    if (accounts.count() > 0) return;
    AuthService.checkPassword(adminPassword);
    AuthService.checkPassword(userPassword);
    var admin = account("admin@tro.demo", "Quản trị viên", Role.ADMIN, adminPassword);
    var owner1 = account("chutro1@tro.demo", "Chủ trọ Minh Anh", Role.LANDLORD, userPassword);
    var owner2 = account("chutro2@tro.demo", "Chủ trọ Hoàng Nam", Role.LANDLORD, userPassword);
    var tenants = new ArrayList<Account>();
    for (int i = 1; i <= 5; i++)
      tenants.add(
          account(
              "nguoithue" + i + "@tro.demo", "Người thuê demo " + i, Role.TENANT, userPassword));
    var cats = new ArrayList<Category>();
    for (String n : List.of("Phòng khép kín", "Studio", "Phòng có gác")) {
      var c = new Category();
      c.setName(n);
      c.setActive(true);
      cats.add(categories.save(c));
    }
    var ams = new ArrayList<Amenity>();
    for (String n : List.of("Điều hòa", "Wifi", "Chỗ để xe", "Máy giặt", "Ban công", "Bếp riêng")) {
      var a = new Amenity();
      a.setName(n);
      a.setActive(true);
      ams.add(amenities.save(a));
    }
    String[] titles = {
      "Studio sáng thoáng, ban công riêng",
      "Phòng khép kín gần trường đại học",
      "Phòng có gác, bếp riêng tiện nghi",
      "Không gian yên tĩnh cho người đi làm"
    };
    for (int i = 0; i < 14; i++) {
      var r = new Room();
      r.setLandlord(i % 2 == 0 ? owner1 : owner2);
      r.setCategory(cats.get(i % 3));
      r.setCode("P" + (101 + i));
      r.setProvince(i < 10 ? "Hà Nội" : "Phú Thọ");
      r.setWard(i < 10 ? "Minh Khai" : "Hải Lựu");
      r.setAddressLine("Địa chỉ minh họa — phòng " + (101 + i));
      r.setArea(new BigDecimal(20 + i * 2));
      r.setMaxOccupants(2 + i % 2);
      r.setStatus(RoomStatus.AVAILABLE);
      rooms.save(r);
      var l = new Listing();
      l.setRoom(r);
      l.setTitle(titles[i % 4] + " · P" + (101 + i));
      l.setDescription(
          "Phòng minh họa phục vụ trình diễn đồ án. Không gian thoáng, phù hợp học tập và sinh"
              + " hoạt. Liên hệ chủ trọ để kiểm tra thông tin trước khi thuê.\n"
              + "Địa chỉ và hình ảnh là dữ liệu demo, không phải quảng cáo phòng thật.");
      l.setMonthlyRent(new BigDecimal(2200000 + i * 150000));
      l.setDeposit(l.getMonthlyRent());
      l.setElectricityUnitPrice(new BigDecimal(3500));
      l.setWaterUnitPrice(new BigDecimal(20000));
      l.setWaterBillingMethod(WaterBillingMethod.PER_M3);
      l.setServiceFee(new BigDecimal(100000));
      l.setAvailableFrom(RentalService.today());
      l.setModerationStatus(
          i == 11
              ? ListingStatus.PENDING_REVIEW
              : i == 12
                  ? ListingStatus.REJECTED
                  : i == 13 ? ListingStatus.DRAFT : ListingStatus.APPROVED);
      l.setVisible(i < 11);
      if (i < 11) {
        l.setReviewedBy(admin);
        l.setReviewedAt(Instant.now());
      }
      listings.save(l);
      for (int j = 0; j < 3; j++) {
        var ra = new RoomAmenity();
        ra.setRoom(r);
        ra.setAmenity(ams.get((i + j) % ams.size()));
        roomAmenities.save(ra);
      }
      String key = UUID.randomUUID() + ".png";
      Path path = Path.of(upload);
      Files.createDirectories(path);
      var img = new BufferedImage(960, 640, BufferedImage.TYPE_INT_RGB);
      var g = img.createGraphics();
      g.setPaint(
          new GradientPaint(
              0,
              0,
              new Color(223 - i % 3 * 12, 231, 215),
              960,
              640,
              new Color(242, 224 - i % 3 * 10, 200)));
      g.fillRect(0, 0, 960, 640);
      g.setColor(new Color(72, 112, 95));
      g.fillRoundRect(150, 130, 440, 400, 25, 25);
      g.setColor(new Color(241, 220, 188));
      for (int j = 0; j < 4; j++)
        g.fillRoundRect(190 + (j % 2) * 190, 180 + (j / 2) * 150, 130, 95, 15, 15);
      g.setColor(new Color(195, 146, 97));
      g.fillRect(340, 410, 70, 120);
      g.setColor(new Color(247, 247, 239));
      g.fillRoundRect(650, 360, 210, 100, 20, 20);
      g.setColor(new Color(37, 74, 56));
      g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 24));
      g.drawString("DEMO P" + (101 + i), 666, 420);
      g.dispose();
      ImageIO.write(img, "png", path.resolve(key).toFile());
      var im = new RoomImage();
      im.setRoom(r);
      im.setStorageKey(key);
      im.setAltText("Hình minh họa phòng demo");
      im.setCover(true);
      im.setSortOrder(0);
      images.save(im);
      if (i == 8 || i == 9 || i == 10) {
        var q = new RentalRequest();
        q.setListing(l);
        q.setTenant(tenants.get(i - 8));
        q.setStatus(RequestStatus.ACCEPTED);
        q.setRequestedMoveIn(i == 8 ? RentalService.today() : RentalService.today().minusDays(30));
        q.setCreatedAt(Instant.now().minusSeconds(i == 8 ? 0 : 35 * 86400L));
        q.setListingVersionSnapshot(l.getVersion());
        q.setRentSnapshot(l.getMonthlyRent());
        q.setDepositSnapshot(l.getDeposit());
        q.setTitleSnapshot(l.getTitle());
        q.setProcessedAt(Instant.now());
        q.setProcessedBy(r.getLandlord());
        requests.save(q);
        var t = new Tenancy();
        t.setRequest(q);
        t.setRoom(r);
        t.setTenant(q.getTenant());
        t.setPlannedStartDate(q.getRequestedMoveIn());
        t.setCreatedAt(q.getCreatedAt());
        t.setMonthlyRentSnapshot(l.getMonthlyRent());
        t.setDepositSnapshot(l.getDeposit());
        t.setRoomAddressSnapshot(r.getAddressLine() + ", " + r.getWard());
        t.setStatus(
            i == 8
                ? TenancyStatus.PENDING_MOVE_IN
                : i == 9 ? TenancyStatus.ACTIVE : TenancyStatus.COMPLETED);
        if (i != 8) t.setActualStartDate(RentalService.today().minusDays(30));
        if (i == 10) t.setEndDate(RentalService.today().minusDays(1));
        tenancies.save(t);
        r.setStatus(
            i == 8 ? RoomStatus.RESERVED : i == 9 ? RoomStatus.OCCUPIED : RoomStatus.AVAILABLE);
        if (i == 10) {
          var v = new Review();
          v.setTenancy(t);
          v.setRating(5);
          v.setComment("Nhận xét minh họa: phòng thoáng và thông tin chi phí rõ ràng.");
          v.setStatus(ReviewStatus.VISIBLE);
          reviews.save(v);
        } else l.setVisible(false);
      }
      if (i == 0) {
        var q = new RentalRequest();
        q.setListing(l);
        q.setTenant(tenants.get(3));
        q.setStatus(RequestStatus.PENDING);
        q.setRequestedMoveIn(RentalService.today().plusDays(2));
        q.setRentSnapshot(l.getMonthlyRent());
        q.setDepositSnapshot(l.getDeposit());
        q.setTitleSnapshot(l.getTitle());
        q.setListingVersionSnapshot(l.getVersion());
        requests.save(q);
      }
    }
  }

  private Account account(String email, String name, Role role, String password) {
    var a = new Account();
    a.setEmail(email);
    a.setFullName(name);
    a.setRole(role);
    a.setStatus(AccountStatus.ACTIVE);
    a.setPasswordHash(encoder.encode(password));
    return accounts.save(a);
  }
}
