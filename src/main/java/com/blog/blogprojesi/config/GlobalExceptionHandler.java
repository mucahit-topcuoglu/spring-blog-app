package com.blog.blogprojesi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global Exception Handler
 * Uygulama genelindeki hataları yakalar ve yönetir
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handleRuntimeException(RuntimeException ex, RedirectAttributes redirectAttributes) {
        log.error("Runtime hatası: {}", ex.getMessage(), ex);
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("error", ex.getMessage());
        mav.setViewName("redirect:/home");
        
        return mav;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Geçersiz argüman: {}", ex.getMessage());
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("error", ex.getMessage());
        mav.setViewName("redirect:/home");
        
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex) {
        log.error("Beklenmeyen hata: {}", ex.getMessage(), ex);
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("error", "Bir hata oluştu. Lütfen daha sonra tekrar deneyin.");
        mav.setViewName("redirect:/home");
        
        return mav;
    }
}
