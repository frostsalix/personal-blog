package com.frostsalix.personalblog.config;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Collections;
import java.util.Set;

public class BlogDialect extends AbstractDialect implements IExpressionObjectDialect {

    private final IExpressionObjectFactory factory = new BlogExpressionObjectFactory();

    public BlogDialect() {
        super("Blog");
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return factory;
    }

    private static class BlogExpressionObjectFactory implements IExpressionObjectFactory {
        private static final String NAME = "blog";
        private static final Set<String> NAMES = Collections.singleton(NAME);

        @Override
        public Set<String> getAllExpressionObjectNames() {
            return NAMES;
        }

        @Override
        public Object buildObject(IExpressionContext context, String name) {
            if (NAME.equals(name)) {
                return new BlogUtils();
            }
            return null;
        }

        @Override
        public boolean isCacheable(String name) {
            return true;
        }
    }

    public static class BlogUtils {
        public String stripHtml(String html) {
            if (html == null) return "";
            return html.replaceAll("<[^>]+>", "");
        }

        public String excerpt(String html, int maxLength) {
            String plain = stripHtml(html);
            if (plain.length() <= maxLength) return plain;
            int cutoff = plain.lastIndexOf(' ', maxLength);
            if (cutoff < maxLength / 2) cutoff = maxLength;
            return plain.substring(0, cutoff) + "...";
        }
    }
}
