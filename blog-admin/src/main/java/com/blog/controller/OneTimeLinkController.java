package com.blog.controller;

import com.blog.service.OneTimeLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/link")
public class OneTimeLinkController {

    @Autowired
    private OneTimeLinkService oneTimeLinkService;

    @GetMapping("/{token}")
    public String viewLink(@PathVariable String token, Model model) {
        String content = oneTimeLinkService.useLink(token);
        
        if (content == null) {
            model.addAttribute("error", "链接已失效或已被使用");
            return "link/error";
        }
        
        model.addAttribute("content", content);
        return "link/view";
    }
} 