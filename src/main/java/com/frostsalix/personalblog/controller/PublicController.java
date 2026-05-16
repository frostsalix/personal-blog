package com.frostsalix.personalblog.controller;

import com.frostsalix.personalblog.service.ArticleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicController {

    private final ArticleService articleService;

    public PublicController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("articles", articleService.getPublishedArticles());
        return "home";
    }

    @GetMapping("/posts/{slug}")
    public String article(@PathVariable String slug, Model model) {
        model.addAttribute("article", articleService.getArticle(slug));
        return "article";
    }
}
