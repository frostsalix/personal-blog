package com.frostsalix.personalblog.model;

import lombok.Data;
import java.time.LocalDate;

@Data
public class Article {
    private String slug;
    private String title;
    private String content;
    private LocalDate publishedAt;
    private LocalDate updatedAt;
    private String status; // "published" or "draft"
}
