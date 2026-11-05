package com.pzhgp.backend.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Rich Text Sanitizer Tests")
class RichTextSanitizerTest {

    @Test
    @DisplayName("Should keep the formatting produced by the text editor")
    void shouldKeepEditorFormatting() {
        String html = "<h2 style=\"text-align: center\">Lot z Dahme</h2>"
                + "<p style=\"text-align: justify\">Zbiórka <strong>w sobotę</strong>, <em>godz.</em> <u>8:00</u> <s>9:00</s>. "
                + "<mark data-color=\"#fde68a\" style=\"background-color: #fde68a; color: inherit\">Ważne</mark></p>"
                + "<ul><li><p>Punkt pierwszy</p></li></ul><ol start=\"3\"><li><p>Punkt trzeci</p></li></ol>"
                + "<blockquote><p>Cytat</p></blockquote><p>Linia<br>druga</p><hr>";

        assertEquals(html, RichTextSanitizer.sanitize(html));
    }

    @Test
    @DisplayName("Should remove scripts, event handlers and embedded content")
    void shouldRemoveScriptsAndEventHandlers() {
        String html = "<p onclick=\"alert(1)\">Tekst<script>alert(1)</script></p>"
                + "<img src=\"x\" onerror=\"alert(1)\"><iframe src=\"https://example.com\"></iframe>";

        assertEquals("<p>Tekst</p>", RichTextSanitizer.sanitize(html));
    }

    @Test
    @DisplayName("Should drop javascript links and force safe rel on remaining links")
    void shouldSanitizeLinks() {
        String html = "<p><a href=\"javascript:alert(1)\">zły</a> <a href=\"https://pzhgp.pl\" target=\"_blank\">dobry</a></p>";

        assertEquals(
                "<p><a rel=\"noopener noreferrer nofollow\">zły</a> "
                        + "<a href=\"https://pzhgp.pl\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">dobry</a></p>",
                RichTextSanitizer.sanitize(html)
        );
    }

    @Test
    @DisplayName("Should keep only text alignment and highlight colours in style attributes")
    void shouldFilterStyleDeclarations() {
        String html = "<p style=\"text-align: right; position: fixed; background-image: url(x)\">a</p>"
                + "<mark data-color=\"red;x\" style=\"background-color: expression(alert(1))\">b</mark>"
                + "<strong style=\"text-align: center\">c</strong>";

        assertEquals("<p style=\"text-align: right\">a</p><mark>b</mark><strong>c</strong>", RichTextSanitizer.sanitize(html));
    }

    @Test
    @DisplayName("Should return null for missing content")
    void shouldReturnNullForNull() {
        assertNull(RichTextSanitizer.sanitize(null));
    }
}
