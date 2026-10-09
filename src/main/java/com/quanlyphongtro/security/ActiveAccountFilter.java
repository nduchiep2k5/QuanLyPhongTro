package com.quanlyphongtro.security;

import com.quanlyphongtro.enums.AccountStatus;
import com.quanlyphongtro.repository.AccountRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ActiveAccountFilter extends OncePerRequestFilter {
  private final AccountRepository accounts;

  public ActiveAccountFilter(AccountRepository a) {
    accounts = a;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
      var a = accounts.findByEmailIgnoreCase(auth.getName());
      if (a.isEmpty() || a.get().getStatus() != AccountStatus.ACTIVE) {
        SecurityContextHolder.clearContext();
        var s = req.getSession(false);
        if (s != null) s.invalidate();
        res.sendRedirect(req.getContextPath() + "/auth/login?blocked");
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
