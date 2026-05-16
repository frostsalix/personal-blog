package com.frostsalix.personalblog.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.frostsalix.personalblog.model.Article;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class ArticleRepository {

    private final Path postsDir;
    private final ObjectMapper objectMapper;

    public ArticleRepository(@Value("${blog.posts.directory:posts}") String postsDirPath) {
        this.postsDir = Paths.get(postsDirPath);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        ensurePostsDirExists();
    }

    private void ensurePostsDirExists() {
        File dir = postsDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public List<Article> listAll() {
        List<Article> articles = new ArrayList<>();
        File[] files = postsDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return articles;
        }
        for (File file : files) {
            try {
                Article article = objectMapper.readValue(file, Article.class);
                articles.add(article);
            } catch (IOException e) {
                // Skip malformed files
            }
        }
        articles.sort(Comparator.comparing(Article::getPublishedAt).reversed());
        return articles;
    }

    public Optional<Article> findBySlug(String slug) {
        Path filePath = postsDir.resolve(slug + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            Article article = objectMapper.readValue(filePath.toFile(), Article.class);
            return Optional.of(article);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void save(Article article) {
        Path filePath = postsDir.resolve(article.getSlug() + ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), article);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save article: " + article.getSlug(), e);
        }
    }

    public boolean delete(String slug) {
        Path filePath = postsDir.resolve(slug + ".json");
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    public boolean slugExists(String slug) {
        return Files.exists(postsDir.resolve(slug + ".json"));
    }
}
