package com.pzhgp.backend.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Czyści HTML z edytora tekstu (ogłoszenia, wpisy na forum) przed zapisem w bazie.
 * <p>
 * Przeglądarka dodatkowo czyści treść przez DOMPurify przy wyświetlaniu. Ta warstwa chroni
 * dane w bazie i każdego innego klienta API. Przepuszczane są tylko znaczniki, które tworzy
 * edytor TipTap: formatowanie tekstu, nagłówki, listy, cytaty, kod, linki, wyróżnienie kolorem
 * i wyrównanie akapitów. Z atrybutu style zostają wyłącznie te ustawienia.
 */
public final class RichTextSanitizer {

    private static final String[] TEXT_BLOCKS = {"p", "h1", "h2", "h3", "h4", "h5", "h6"};

    private static final Safelist SAFELIST = new Safelist()
            .addTags("p", "br", "hr", "strong", "b", "em", "i", "u", "s", "code", "pre", "blockquote",
                    "ul", "ol", "li", "h1", "h2", "h3", "h4", "h5", "h6", "mark", "a", "span")
            .addAttributes("a", "href", "target")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer nofollow")
            .addAttributes("ol", "start")
            .addAttributes("code", "class")
            .addAttributes("mark", "data-color", "style")
            .addAttributes(":all", "style");

    /** Dozwolone ustawienia CSS: wyrównanie akapitów i kolor wyróżnienia. */
    private static final Map<String, Pattern> ALLOWED_STYLES = Map.of(
            "text-align", Pattern.compile("left|center|right|justify"),
            "background-color", Pattern.compile("#[0-9a-fA-F]{3,8}|rgba?\\([0-9.,\\s%]+\\)"),
            "color", Pattern.compile("inherit")
    );

    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{3,8}");
    private static final Pattern CODE_LANGUAGE_CLASS = Pattern.compile("language-[\\w-]+");

    private RichTextSanitizer() {
    }

    /**
     * Zwraca bezpieczną wersję HTML-a z edytora.
     *
     * @return oczyszczony HTML albo {@code null}, gdy przekazano {@code null}
     */
    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }

        Document clean = new Cleaner(SAFELIST).clean(Jsoup.parseBodyFragment(html));
        clean.outputSettings().prettyPrint(false);

        for (Element element : clean.body().getAllElements()) {
            filterStyle(element);
            filterAttribute(element, "data-color", HEX_COLOR);
            filterAttribute(element, "class", CODE_LANGUAGE_CLASS);
        }

        return clean.body().html();
    }

    private static void filterStyle(Element element) {
        if (!element.hasAttr("style")) {
            return;
        }

        boolean textBlock = Arrays.asList(TEXT_BLOCKS).contains(element.tagName());
        boolean highlight = element.tagName().equals("mark");

        String allowed = Arrays.stream(element.attr("style").split(";"))
                .map(String::trim)
                .filter(declaration -> declaration.contains(":"))
                .filter(declaration -> {
                    String property = declaration.substring(0, declaration.indexOf(':')).trim().toLowerCase(Locale.ROOT);
                    String value = declaration.substring(declaration.indexOf(':') + 1).trim();
                    boolean propertyAllowedHere = property.equals("text-align") ? textBlock : highlight;
                    Pattern valuePattern = ALLOWED_STYLES.get(property);
                    return propertyAllowedHere && valuePattern != null && valuePattern.matcher(value).matches();
                })
                .map(declaration -> declaration.replaceAll("\\s*:\\s*", ": "))
                .collect(Collectors.joining("; "));

        if (allowed.isEmpty()) {
            element.removeAttr("style");
        } else {
            element.attr("style", allowed);
        }
    }

    private static void filterAttribute(Element element, String attribute, Pattern allowedValue) {
        if (element.hasAttr(attribute) && !allowedValue.matcher(element.attr(attribute)).matches()) {
            element.removeAttr(attribute);
        }
    }
}
