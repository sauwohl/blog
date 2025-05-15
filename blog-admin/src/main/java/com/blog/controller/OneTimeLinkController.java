package com.blog.controller;

import com.blog.service.OneTimeLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/link")
public class OneTimeLinkController {

    @Autowired
    private OneTimeLinkService oneTimeLinkService;

    @GetMapping("/{token}")
    public String viewLink(@PathVariable String token) {
        String content = oneTimeLinkService.useLink(token);
        
        if (content == null) {
            return "链接已失效或已被使用";
        }
        
        return content;
    }
} 