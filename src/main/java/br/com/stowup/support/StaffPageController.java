package br.com.stowup.support;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaffPageController {
  @GetMapping({"/staff", "/staff/"})
  public String portal() {
    return "forward:/staff/index.html";
  }
}
