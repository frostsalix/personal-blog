package com.frostsalix.personalblog.service;

import com.frostsalix.personalblog.model.Article;
import com.frostsalix.personalblog.repository.ArticleRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository repository;

    public ArticleService(ArticleRepository repository) {
        this.repository = repository;
    }

    public List<Article> getPublishedArticles() {
        return repository.listAll().stream()
                .filter(a -> "published".equals(a.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Article> getAllArticles() {
        return repository.listAll();
    }

    public Article getArticle(String slug) {
        return repository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Article not found: " + slug));
    }

    public Article createArticle(String title, String content, String status) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }

        Article article = new Article();
        article.setSlug(generateSlug(title));
        article.setTitle(title.trim());
        article.setContent(content.trim());
        article.setPublishedAt(LocalDate.now());
        article.setUpdatedAt(LocalDate.now());
        article.setStatus(status != null ? status : "draft");

        repository.save(article);
        return article;
    }

    public Article updateArticle(String slug, String title, String content, String status) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }
        Article article = getArticle(slug);
        article.setTitle(title.trim());
        article.setContent(content.trim());
        article.setStatus(status != null ? status : article.getStatus());
        article.setUpdatedAt(LocalDate.now());
        repository.save(article);
        return article;
    }

    public void deleteArticle(String slug) {
        if (!repository.delete(slug)) {
            throw new RuntimeException("Article not found: " + slug);
        }
    }

    public String generateSlug(String title) {
        String slug = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isBlank()) {
            slug = "untitled";
        }

        String base = slug;
        int suffix = 2;
        while (repository.slugExists(slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }

        return slug;
    }
}
