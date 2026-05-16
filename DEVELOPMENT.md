# Personal Blog — Development Document

## Overview

File-driven server-side rendered blog. Guest area for reading, admin area for CRUD. Articles stored as JSON files, pages rendered with Thymeleaf, auth via login page + Session.

- **Stack**: Spring Boot 4.0.6, Java 21, Thymeleaf, CSS, JSON file storage
- **Package**: `com.frostsalix.personalblog`

---

## 1. Project Structure

```
personal-blog/
├── posts/                                    # Article JSON files
│   ├── hello-world.json
│   └── spring-notes.json
├── src/main/java/com/frostsalix/personalblog/
│   ├── PersonalBlogApplication.java
│   ├── model/
│   │   └── Article.java                     # Article entity
│   ├── repository/
│   │   └── ArticleRepository.java           # File read/write operations
│   ├── service/
│   │   └── ArticleService.java              # Business logic
│   ├── controller/
│   │   ├── PublicController.java            # Guest-facing routes
│   │   └── AdminController.java             # Admin CRUD routes
│   └── config/
│       ├── WebConfig.java                   # Interceptor registration
│       └── LoginInterceptor.java            # Session check interceptor
├── src/main/resources/
│   ├── application.properties
│   ├── templates/
│   │   ├── home.html                        # Article list page
│   │   ├── article.html                     # Article detail page
│   │   ├── admin/
│   │   │   ├── login.html                   # Admin login page
│   │   │   ├── dashboard.html               # Admin article list
│   │   │   ├── editor.html                  # New / Edit article form
│   │   │   └── delete-confirm.html          # Delete confirmation
│   │   └── fragments/
│   │       ├── header.html                  # Public header
│   │       ├── footer.html                  # Public footer
│   │       └── admin-header.html            # Admin nav
│   └── static/
│       └── css/
│           └── style.css                    # Global styles
└── pom.xml
```

---

## 2. Data Model

### Article JSON file format (stored in `posts/` directory)

```json
{
  "slug": "hello-world",
  "title": "Hello World",
  "content": "<p>This is the article body in HTML.</p>",
  "publishedAt": "2026-05-16",
  "updatedAt": "2026-05-16",
  "status": "published"
}
```

Fields:

| Field        | Type   | Description                           |
|-------------|--------|---------------------------------------|
| slug        | String | URL-safe unique identifier            |
| title       | String | Article title                         |
| content     | String | Article body (HTML)                   |
| publishedAt | String | ISO date of first publish             |
| updatedAt   | String | ISO date of last edit                 |
| status      | String | `"published"` or `"draft"`            |

File naming rule: `{slug}.json`. Slug generation: lowercase, spaces → hyphens, remove special chars, append `-2` on collision.

### Java Model — `Article.java`

```java
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
```

---

## 3. Routes

### Guest Area

| Method | Path              | Description              | Template       |
|--------|-------------------|--------------------------|----------------|
| GET    | `/`               | Home page, list published articles (newest first) | `home.html`    |
| GET    | `/posts/{slug}`   | Single article detail    | `article.html` |

### Admin Area

| Method | Path                              | Description                | Template                  |
|--------|-----------------------------------|----------------------------|---------------------------|
| GET    | `/admin/login`                    | Login form                 | `admin/login.html`        |
| POST   | `/admin/login`                    | Authenticate, create session | Redirect to `/admin`      |
| GET    | `/admin/logout`                   | Invalidate session         | Redirect to `/`           |
| GET    | `/admin`                          | Dashboard, list all articles (incl. drafts) | `admin/dashboard.html`    |
| GET    | `/admin/posts/new`                | New article form           | `admin/editor.html`       |
| POST   | `/admin/posts/new`                | Save new article           | Redirect to `/admin`      |
| GET    | `/admin/posts/{slug}/edit`        | Edit article form          | `admin/editor.html`       |
| POST   | `/admin/posts/{slug}/edit`        | Save changes               | Redirect to `/admin`      |
| POST   | `/admin/posts/{slug}/delete`      | Delete article             | Redirect to `/admin`      |

---

## 4. Layer Responsibilities

### 4.1 `ArticleRepository`

File I/O only. No business logic.

```
listAll()         → List<Article>    // Scan posts/, parse each JSON, return all
findBySlug(slug)  → Optional<Article> // Read posts/{slug}.json
save(article)     → void             // Write JSON to posts/{slug}.json
delete(slug)      → boolean          // Delete posts/{slug}.json
```

Implementation notes:
- Use `com.fasterxml.jackson.databind.ObjectMapper` for JSON serialization
- `listAll()` sorts by `publishedAt` descending
- `save()` sets `updatedAt` to current date if article already exists

### 4.2 `ArticleService`

Business logic layer. Delegates I/O to `ArticleRepository`.

```
getPublishedArticles()     → List<Article>    // Filter status=published, newest first
getAllArticles()           → List<Article>    // All articles for admin dashboard
getArticle(slug)           → Article          // Single article, throw if not found
createArticle(form)        → Article          // Generate slug, validate, save with publishedAt=now
updateArticle(slug, form)  → Article          // Merge changes, update updatedAt, save
deleteArticle(slug)        → void             // Delete file
generateSlug(title)        → String           // Slug generation with collision check
```

Validation rules:
- Title: required, max 200 chars
- Content: required, not blank
- Slug: auto-generated from title, unique

### 4.3 `PublicController`

Handles guest-facing pages. Injects `ArticleService`.

```java
@Controller
public class PublicController {
    // GET / → home page with published articles
    // GET /posts/{slug} → article detail page
}
```

### 4.4 `AdminController`

Handles `/admin/**` routes. Injects `ArticleService`.

```java
@Controller
@RequestMapping("/admin")
public class AdminController {
    // GET  /admin/login → login form
    // POST /admin/login → authenticate, set session
    // GET  /admin/logout → invalidate session
    // GET  /admin → dashboard
    // GET  /admin/posts/new → new article form
    // POST /admin/posts/new → save new article
    // GET  /admin/posts/{slug}/edit → edit form
    // POST /admin/posts/{slug}/edit → save edits
    // POST /admin/posts/{slug}/delete → delete article
}
```

---

## 5. Authentication

Login page + Session approach.

### Login Flow

1. Admin navigates to `GET /admin/login`, enters username + password.
2. `POST /admin/login` validates credentials (hardcoded or configured in `application.properties`).
3. On success: `session.setAttribute("isAdmin", true)`, redirect to `/admin`.
4. On failure: redirect back to `/admin/login` with error message.

### Interceptor — `LoginInterceptor`

```java
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("isAdmin") == null) {
            response.sendRedirect("/admin/login");
            return false;
        }
        return true;
    }
}
```

### `WebConfig` — Register Interceptor

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
    }
}
```

### Credentials

Store in `application.properties`:

```properties
blog.admin.username=admin
blog.admin.password=changeme
```

Inject via `@Value` in `AdminController`.

---

## 6. Templates

### Thymeleaf Layout Strategy

Each page includes shared fragments:

```html
<!-- home.html -->
<html xmlns:th="http://www.thymeleaf.org">
<head>...</head>
<body>
  <div th:replace="~{fragments/header :: header}"></div>
  <main>...</main>
  <div th:replace="~{fragments/footer :: footer}"></div>
</body>
</html>
```

### Template Data Flow

| Template             | Model Attributes                              |
|---------------------|-----------------------------------------------|
| `home.html`         | `articles` (List\<Article\>)                   |
| `article.html`      | `article` (Article)                            |
| `admin/login.html`  | `error` (String, optional)                     |
| `admin/dashboard.html` | `articles` (List\<Article\>)                |
| `admin/editor.html` | `article` (Article, null for new), `isNew` (boolean) |

---

## 7. Build Order

Follow this sequence to close the core loop first:

| Step | Task                     | Deliverable                                           |
|------|--------------------------|-------------------------------------------------------|
| 1    | Article model + repository | Can read/write/delete JSON files in `posts/`         |
| 2    | Home page                | `GET /` lists published articles                      |
| 3    | Article detail page      | `GET /posts/{slug}` renders single article            |
| 4    | Admin login              | `GET/POST /admin/login` with Session auth             |
| 5    | Login interceptor        | All `/admin/**` protected                             |
| 6    | Dashboard                | `GET /admin` lists all articles with action buttons   |
| 7    | New article              | `GET/POST /admin/posts/new` — form + save             |
| 8    | Edit article             | `GET/POST /admin/posts/{slug}/edit` — form + save     |
| 9    | Delete article           | `POST /admin/posts/{slug}/delete`                     |
| 10   | CSS styling              | Make pages look polished                              |

---

## 8. CSS Design Guidelines

Minimal, readable, blog-appropriate.

- **Font**: System font stack (`-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`)
- **Max width**: 720px for article content, 960px for layout
- **Colors**: Dark text on light background, one accent color for links/buttons
- **Spacing**: Generous padding/margins, clear heading hierarchy
- **Admin sidebar**: Fixed left sidebar (200px) with article list, right content area

### Page-specific notes

**Home page**: Article cards with title, date, excerpt (first 200 chars of content without tags), "Read more" link.

**Article page**: Title (h1), date in muted text, article body, "Back to home" link.

**Admin dashboard**: Table or card list of all articles. Each row shows title, status badge, date, Edit/Delete buttons. "New Article" button prominent at top.

**Editor page**: Title input, content textarea, status dropdown (published/draft), Save button.

---

## 9. Application Properties

```properties
spring.application.name=personal-blog
blog.posts.directory=posts
blog.admin.username=admin
blog.admin.password=changeme
```

---

## 10. Extension Ideas (Post-MVP)

Priority order for portfolio value:

1. **Draft/published toggle** — already built into the model, just expose in editor
2. **Article search** — simple title/content keyword match on the home page
3. **Markdown support** — store content as Markdown, render to HTML server-side (use `flexmark` or `commonmark` library)
4. **Auto excerpt** — strip HTML tags, take first 200 chars for card preview
5. **Responsive layout** — media queries for mobile
6. **Tags / categories** — add `tags: []` field to JSON
7. **Pagination** — limit home page to N articles per page
8. **Code syntax highlighting** — highlight.js integration
