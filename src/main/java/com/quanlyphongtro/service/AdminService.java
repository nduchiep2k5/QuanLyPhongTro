package com.quanlyphongtro.service;

import com.quanlyphongtro.dto.Views.*;
import com.quanlyphongtro.entity.*;
import com.quanlyphongtro.enums.*;
import com.quanlyphongtro.exception.BusinessException;
import com.quanlyphongtro.repository.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
  private final AccountRepository accounts;
  private final RoomRepository rooms;
  private final ListingRepository listings;
  private final ReviewRepository reviews;
  private final RentalRequestRepository requests;
  private final TenancyRepository tenancies;
  private final ModerationEventRepository events;
  private final CategoryRepository categories;
  private final AmenityRepository amenities;
  private final AccessService access;
  private final ViewMapper mapper;
  private final RentalService rentals;
  private final AuthService auth;

  public void accountStatus(Long target, Long admin, boolean ban) {
    var admins = accounts.lockAdmins();
    access.role(admin, Role.ADMIN);
    var a = accounts.lock(target).orElseThrow(AccessService::missing);
    if (ban && target.equals(admin))
      throw new BusinessException("Không tự khóa tài khoản đang sử dụng.");
    if (ban
        && a.getRole() == Role.ADMIN
        && admins.stream().filter(x -> x.getStatus() == AccountStatus.ACTIVE).count() <= 1)
      throw new BusinessException("Không khóa ADMIN hoạt động cuối cùng.");
    // Serialize against room workflow using ascending room ids.
    var affected = new TreeSet<Long>();
    if (a.getRole() == Role.LANDLORD)
      rooms.findByLandlordIdOrderByCreatedAtDesc(target).forEach(r -> affected.add(r.getId()));
    if (a.getRole() == Role.TENANT)
      requests.findByTenantIdOrderByCreatedAtDesc(target).stream()
          .filter(q -> q.getStatus() == RequestStatus.PENDING)
          .forEach(q -> affected.add(q.getListing().getRoom().getId()));
    for (Long id : affected) {
      var r = rooms.lock(id).orElseThrow();
      if (ban && a.getRole() == Role.LANDLORD) {
        listings.findByRoomId(id).ifPresent(l -> l.setVisible(false));
        rentals.cancelPending(
            id, accounts.findById(admin).orElseThrow(), "Tài khoản chủ trọ đã bị khóa");
      }
    }
    if (ban && a.getRole() == Role.TENANT)
      requests.findByTenantIdOrderByCreatedAtDesc(target).stream()
          .filter(q -> q.getStatus() == RequestStatus.PENDING)
          .forEach(
              q -> {
                q.setStatus(RequestStatus.CANCELLED);
                q.setProcessedAt(Instant.now());
                q.setProcessedBy(accounts.findById(admin).orElseThrow());
                q.setDecisionReason("Tài khoản người tìm thuê đã bị khóa");
              });
    a.setStatus(ban ? AccountStatus.BANNED : AccountStatus.ACTIVE);
    if (ban) auth.expire(a.getEmail());
  }

  public void category(Long id, String name, boolean active, Long admin) {
    access.role(admin, Role.ADMIN);
    checkName(name);
    var c =
        id == null ? new Category() : categories.findById(id).orElseThrow(AccessService::missing);
    c.setName(name.trim());
    c.setActive(active);
    categories.save(c);
  }

  public void amenity(Long id, String name, boolean active, Long admin) {
    access.role(admin, Role.ADMIN);
    checkName(name);
    var c = id == null ? new Amenity() : amenities.findById(id).orElseThrow(AccessService::missing);
    c.setName(name.trim());
    c.setActive(active);
    amenities.save(c);
  }

  private void checkName(String name) {
    if (name == null || name.isBlank() || name.length() > 80)
      throw new BusinessException("Tên danh mục cần 1–80 ký tự.");
  }

  @Transactional(readOnly = true)
  public List<Account> users(Long admin) {
    access.role(admin, Role.ADMIN);
    return accounts.findAllByOrderByCreatedAtDesc();
  }

  @Transactional(readOnly = true)
  public List<RoomCard> listings(Long admin) {
    access.role(admin, Role.ADMIN);
    return listings.findAllByOrderByCreatedAtDesc().stream().map(mapper::card).toList();
  }

  @Transactional(readOnly = true)
  public List<ReviewRow> reviews(Long admin) {
    access.role(admin, Role.ADMIN);
    return reviews.findAllByOrderByCreatedAtDesc().stream().map(mapper::review).toList();
  }

  @Transactional(readOnly = true)
  public List<AuditRow> audit(Long admin) {
    access.role(admin, Role.ADMIN);
    return events.findAllByOrderByCreatedAtDesc().stream()
        .map(
            e ->
                new AuditRow(
                    e.getActor().getFullName(),
                    e.getListing() != null
                        ? "Bài #" + e.getListing().getId()
                        : "Nhận xét #" + e.getReview().getId(),
                    e.getAction(),
                    e.getFromStatus(),
                    e.getToStatus(),
                    e.getReason(),
                    e.getCreatedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Long> stats(Long admin, LocalDate from, LocalDate to) {
    access.role(admin, Role.ADMIN);
    if (from != null && to != null && from.isAfter(to))
      throw new BusinessException("Ngày bắt đầu phải trước ngày kết thúc.");
    var zone = ZoneId.of("Asia/Ho_Chi_Minh");
    Instant start = from == null ? Instant.MIN : from.atStartOfDay(zone).toInstant(),
        end = to == null ? Instant.MAX : to.plusDays(1).atStartOfDay(zone).toInstant();
    var out = new LinkedHashMap<String, Long>();
    count(out, "Tài khoản", accounts.findAll(), start, end, a -> a.getStatus().name());
    count(out, "Phòng", rooms.findAll(), start, end, r -> r.getStatus().name());
    count(out, "Bài đăng", listings.findAll(), start, end, l -> l.getModerationStatus().name());
    count(out, "Yêu cầu", requests.findAll(), start, end, q -> q.getStatus().name());
    count(out, "Hồ sơ thuê", tenancies.findAll(), start, end, t -> t.getStatus().name());
    return out;
  }

  private <T extends BaseEntity> void count(
      Map<String, Long> out,
      String label,
      List<T> items,
      Instant start,
      Instant end,
      java.util.function.Function<T, String> state) {
    var selected =
        items.stream()
            .filter(i -> !i.getCreatedAt().isBefore(start) && i.getCreatedAt().isBefore(end))
            .toList();
    out.put(label + " · Tổng", (long) selected.size());
    selected.forEach(i -> out.merge(label + " · " + state.apply(i), 1L, Long::sum));
  }

  public static String csvCell(String value) {
    String x = value == null ? "" : value;
    String trim = x.stripLeading();
    if (!trim.isEmpty() && "=+-@".indexOf(trim.charAt(0)) >= 0) x = "'" + x;
    return "\"" + x.replace("\"", "\"\"") + "\"";
  }
}
