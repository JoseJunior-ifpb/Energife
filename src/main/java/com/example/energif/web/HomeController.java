package com.example.energif.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", "EnergIF");
        return "index";
    }

    @GetMapping({"/energif"})
    public String homeRedirect() {
        // keep backwards compatibility: /energif redirects to new candidate form
        return "redirect:/candidatos/novo";
    }
}
