package com.quanlyphongtro.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {
  private record Window(long start, int count) {}

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse r, FilterChain c)
      throws IOException, ServletException {
    String path = q.getServletPath();
    if (q.getMethod().equals("POST")
        && (path.equals("/auth/login") || path.equals("/auth/forgot") || path.equals("/api/chat"))) {
      long now = System.currentTimeMillis();
      windows.entrySet().removeIf(e -> now - e.getValue().start() > 60000);
      String key = q.getRemoteAddr() + path;
      Window w =
          windows.compute(
              key,
              (k, v) ->
                  v == null || now - v.start() > 60000
                      ? new Window(now, 1)
                      : new Window(v.start(), v.count() + 1));
      if (w.count() > (path.equals("/api/chat") ? 5 : 10)) {
        r.setStatus(429);
        r.setHeader("Retry-After", "60");
        r.setContentType("text/plain;charset=UTF-8");
        r.getWriter().write("Thao tác quá nhanh. Vui lòng thử lại sau 60 giây.");
        return;
      }
    }
    c.doFilter(q, r);
  }
}
