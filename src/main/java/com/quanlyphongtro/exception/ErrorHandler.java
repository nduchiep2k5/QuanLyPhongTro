package com.quanlyphongtro.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class ErrorHandler {
  @ExceptionHandler(BusinessException.class)
  ModelAndView business(BusinessException e) {
    return error(e.getStatus(), e.getMessage());
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    ObjectOptimisticLockingFailureException.class
  })
  ModelAndView conflict(Exception e) {
    return error(
        HttpStatus.CONFLICT,
        "Dữ liệu vừa thay đổi hoặc thao tác bị trùng. Vui lòng tải lại trang.");
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ModelAndView upload(Exception e) {
    return error(HttpStatus.BAD_REQUEST, "Ảnh vượt giới hạn 5 MB hoặc tổng dung lượng 50 MB.");
  }

  private ModelAndView error(HttpStatus s, String m) {
    var v = new ModelAndView("error/business");
    v.setStatus(s);
    v.addObject("message", m);
    v.addObject("code", s.value());
    return v;
  }
}
