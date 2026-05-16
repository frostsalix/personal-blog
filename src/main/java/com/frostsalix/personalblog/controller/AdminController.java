package com.frostsalix.personalblog.controller;

import com.frostsalix.personalblog.service.ArticleService;
import com.frostsalix.personalblog.model.Article;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ArticleService articleService;
    private final String adminUsername;
    private final String adminPassword;

    public AdminController(
            ArticleService articleService,
            @Value("${blog.admin.username:admin}") String adminUsername,
            @Value("${blog.admin.password:changeme}") String adminPassword) {
        this.articleService = articleService;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            session.setAttribute("isAdmin", true);
            return "redirect:/admin";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid username or password");
        return "redirect:/admin/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("articles", articleService.getAllArticles());
        return "admin/dashboard";
    }

    @GetMapping("/posts/new")
    public String newArticleForm(Model model) {
        model.addAttribute("isNew", true);
        return "admin/editor";
    }

    @PostMapping("/posts/new")
    public String createArticle(@RequestParam String title,
                                @RequestParam String content,
                                @RequestParam(defaultValue = "draft") String status,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            var article = articleService.createArticle(title, content, status);
            redirectAttributes.addFlashAttribute("success", "Article created: " + article.getTitle());
            return "redirect:/admin";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("isNew", true);
            Article formData = new Article();
            formData.setTitle(title);
            formData.setContent(content);
            formData.setStatus(status);
            model.addAttribute("article", formData);
            return "admin/editor";
        }
    }

    @GetMapping("/posts/{slug}/edit")
    public String editArticleForm(@PathVariable String slug, Model model) {
        model.addAttribute("article", articleService.getArticle(slug));
        model.addAttribute("isNew", false);
        return "admin/editor";
    }

    @PostMapping("/posts/{slug}/edit")
    public String updateArticle(@PathVariable String slug,
                                @RequestParam String title,
                                @RequestParam String content,
                                @RequestParam(defaultValue = "draft") String status,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            articleService.updateArticle(slug, title, content, status);
            redirectAttributes.addFlashAttribute("success", "Article updated: " + title);
            return "redirect:/admin";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("isNew", false);
            Article formData = new Article();
            formData.setSlug(slug);
            formData.setTitle(title);
            formData.setContent(content);
            formData.setStatus(status);
            model.addAttribute("article", formData);
            return "admin/editor";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin";
        }
    }

    @PostMapping("/posts/{slug}/delete")
    public String deleteArticle(@PathVariable String slug,
                                RedirectAttributes redirectAttributes) {
        try {
            articleService.deleteArticle(slug);
            redirectAttributes.addFlashAttribute("success", "Article deleted");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }
}
