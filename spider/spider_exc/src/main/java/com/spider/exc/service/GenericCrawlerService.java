package com.spider.exc.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import com.spider.exc.dto.GenericCrawlRequest;
import com.spider.exc.dto.GenericCrawlResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GenericCrawlerService {

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int MAX_ITEMS_PER_PAGE = 200;

    public GenericCrawlResponse crawl(GenericCrawlRequest request) {
        if (canUseHttpCrawler(request)) {
            try {
                GenericCrawlResponse response = crawlWithHttp(request);
                callbackToProjectGl(request.getCallbackUrl(), response);
                return response;
            } catch (Exception ex) {
                log.warn("HTTP crawl failed for taskId={}, falling back to browser: {}",
                        request.getTaskId(), ex.getMessage());
            }
        }

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;

        try {
            playwright = Playwright.create();
            String chromiumPath = System.getenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH");
            String headlessShellPath = System.getenv("PLAYWRIGHT_HEADLESS_SHELL_PATH");

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.Arrays.asList(
                        "--no-sandbox",
                        "--disable-setuid-sandbox",
                        "--disable-gpu",
                        "--disable-dev-shm-usage",
                        "--disable-software-rasterizer"
                    ));

            // Try headless shell first (no GPU, lighter weight)
            if (headlessShellPath != null && !headlessShellPath.isBlank()) {
                launchOptions.setExecutablePath(java.nio.file.Paths.get(headlessShellPath));
            } else if (chromiumPath != null && !chromiumPath.isBlank()) {
                launchOptions.setExecutablePath(java.nio.file.Paths.get(chromiumPath));
            }
            browser = playwright.chromium().launch(launchOptions);
            context = createContext(browser, request);

            if (request.getCookies() != null && !request.getCookies().isEmpty()) {
                applyCookies(context, request);
            }
            Page page = context.newPage();
            int timeout = request.getTimeout() != null ? request.getTimeout() : 30000;
            page.setDefaultTimeout(timeout);

            log.info("Navigating generic crawl taskId={}, url={}, timeout={}ms",
                    request.getTaskId(), request.getUrl(), timeout);
            page.navigate(request.getUrl(), new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(timeout));

            waitForExtractionTarget(page, request, timeout);

            boolean hasExtractionRules = request.getExtractionRules() != null && !request.getExtractionRules().isEmpty();
            ExtractionRun extractionRun = hasExtractionRules
                    ? extractAcrossPages(page, request)
                    : new ExtractionRun(Collections.emptyList(), 1);
            String summaryText = extractSummary(page);
            List<Map<String, Object>> structuredData = extractionRun.data();
            if (structuredData.isEmpty()) {
                structuredData = buildFallbackData(page, summaryText);
            }

            GenericCrawlResponse response = GenericCrawlResponse.builder()
                    .taskId(request.getTaskId())
                    .status("completed")
                    .finalUrl(page.url())
                    .title(page.title())
                    .summaryText(summaryText)
                    .rawHtml(page.content())
                    .structuredData(structuredData)
                    .totalPages(getRequestedPages(request, hasExtractionRules))
                    .crawledPages(extractionRun.pages())
                    .totalCount(structuredData.size())
                    .build();

            callbackToProjectGl(request.getCallbackUrl(), response);
            return response;
        } catch (Exception ex) {
            log.error("Generic crawl failed for taskId={}", request.getTaskId(), ex);
            GenericCrawlResponse response = GenericCrawlResponse.builder()
                    .taskId(request.getTaskId())
                    .status("failed")
                    .errorMessage(ex.getMessage())
                    .structuredData(Collections.emptyList())
                    .totalCount(0)
                    .crawledPages(0)
                    .build();
            callbackToProjectGl(request.getCallbackUrl(), response);
            return response;
        } finally {
            if (context != null) {
                context.close();
            }
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
        }
    }

    private BrowserContext createContext(Browser browser, GenericCrawlRequest request) {
        Browser.NewContextOptions options = new Browser.NewContextOptions();
        Map<String, String> extraHeaders = new HashMap<>();
        if (request.getHeaders() != null) {
            extraHeaders.putAll(request.getHeaders());
        }
        String userAgent = extraHeaders.getOrDefault("User-Agent", DEFAULT_USER_AGENT);
        extraHeaders.remove("User-Agent");
        options.setUserAgent(userAgent);
        BrowserContext context = browser.newContext(options);
        if (!extraHeaders.isEmpty()) {
            context.setExtraHTTPHeaders(extraHeaders);
        }
        return context;
    }

    private void applyCookies(BrowserContext context, GenericCrawlRequest request) {
        if (request.getCookies() == null || request.getCookies().isEmpty()) {
            return;
        }
        try {
            List<Cookie> cookies = new ArrayList<>();
            for (GenericCrawlRequest.CookieRule rule : request.getCookies()) {
                if (!hasText(rule.getName()) || rule.getValue() == null) {
                    continue;
                }

                Cookie cookie = new Cookie(rule.getName(), rule.getValue());
                if (hasText(rule.getUrl())) {
                    cookie.setUrl(rule.getUrl());
                } else if (hasText(rule.getDomain())) {
                    cookie.setDomain(rule.getDomain());
                    cookie.setPath(hasText(rule.getPath()) ? rule.getPath() : "/");
                } else if (hasText(request.getUrl())) {
                    cookie.setUrl(request.getUrl());
                } else {
                    continue;
                }
                if (rule.getExpires() != null) {
                    cookie.setExpires(rule.getExpires());
                }
                if (rule.getHttpOnly() != null) {
                    cookie.setHttpOnly(rule.getHttpOnly());
                }
                if (rule.getSecure() != null) {
                    cookie.setSecure(rule.getSecure());
                }
                cookies.add(cookie);
            }

            if (!cookies.isEmpty()) {
                context.addCookies(cookies);
                log.info("Applied {} cookies for taskId={}", cookies.size(), request.getTaskId());
            }
        } catch (Exception ex) {
            log.warn("Failed to apply cookies for taskId={}: {}", request.getTaskId(), ex.getMessage());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean canUseHttpCrawler(GenericCrawlRequest request) {
        GenericCrawlRequest.PaginationRule pagination = request.getPagination();
        return pagination == null || pagination.getMaxPages() == null || pagination.getMaxPages() <= 1;
    }

    private GenericCrawlResponse crawlWithHttp(GenericCrawlRequest request) throws java.io.IOException, InterruptedException {
        int timeout = request.getTimeout() != null ? request.getTimeout() : 30000;
        log.info("HTTP crawling taskId={}, url={}, timeout={}ms", request.getTaskId(), request.getUrl(), timeout);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.getUrl()))
                .timeout(Duration.ofMillis(timeout))
                .GET()
                .header("User-Agent", resolveUserAgent(request));
        if (request.getHeaders() != null) {
            request.getHeaders().forEach((name, value) -> {
                if (hasText(name) && hasText(value) && !"User-Agent".equalsIgnoreCase(name)
                        && !"Cookie".equalsIgnoreCase(name)) {
                    builder.header(name, value);
                }
            });
        }
        String cookieHeader = buildCookieHeader(request);
        if (hasText(cookieHeader)) {
            builder.header("Cookie", cookieHeader);
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
        HttpResponse<String> httpResponse = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (httpResponse.statusCode() >= 400) {
            throw new java.io.IOException("HTTP status " + httpResponse.statusCode());
        }

        String rawHtml = httpResponse.body() != null ? httpResponse.body() : "";
        String finalUrl = httpResponse.uri() != null ? httpResponse.uri().toString() : request.getUrl();
        String title = extractTitle(rawHtml);
        String summaryText = extractSummary(rawHtml);
        boolean hasExtractionRules = request.getExtractionRules() != null && !request.getExtractionRules().isEmpty();
        List<Map<String, Object>> structuredData = hasExtractionRules
                ? extractData(rawHtml, finalUrl, request.getExtractionRules())
                : Collections.emptyList();
        if (structuredData.isEmpty()) {
            structuredData = buildFallbackData(title, finalUrl, summaryText);
        }

        return GenericCrawlResponse.builder()
                .taskId(request.getTaskId())
                .status("completed")
                .finalUrl(finalUrl)
                .title(title)
                .summaryText(summaryText)
                .rawHtml(rawHtml)
                .structuredData(structuredData)
                .totalPages(getRequestedPages(request, hasExtractionRules))
                .crawledPages(1)
                .totalCount(structuredData.size())
                .build();
    }

    private String buildCookieHeader(GenericCrawlRequest request) {
        StringBuilder header = new StringBuilder();
        if (request.getCookies() != null) {
            for (GenericCrawlRequest.CookieRule cookie : request.getCookies()) {
                if (hasText(cookie.getName()) && cookie.getValue() != null) {
                    if (header.length() > 0) {
                        header.append("; ");
                    }
                    header.append(cookie.getName()).append('=').append(cookie.getValue());
                }
            }
        }
        return header.toString();
    }

    private String resolveUserAgent(GenericCrawlRequest request) {
        if (request.getHeaders() != null && hasText(request.getHeaders().get("User-Agent"))) {
            return request.getHeaders().get("User-Agent");
        }
        return DEFAULT_USER_AGENT;
    }

    private void waitForExtractionTarget(Page page, GenericCrawlRequest request, int timeout) {
        int selectorTimeout = Math.min(Math.max(timeout / 5, 3000), 10000);
        if (request.getExtractionRules() != null) {
            for (GenericCrawlRequest.ExtractionRule rule : request.getExtractionRules()) {
                if (!hasText(rule.getSelector())) {
                    continue;
                }
                try {
                    page.waitForSelector(rule.getSelector(),
                            new Page.WaitForSelectorOptions().setTimeout(selectorTimeout));
                    break;
                } catch (Exception ex) {
                    log.debug("Extraction selector not ready yet: {}", rule.getSelector());
                }
            }
        }

        // Modern marketing sites often keep analytics/network connections open forever.
        // A small hydration wait is safer than waiting for Playwright NETWORKIDLE.
        page.waitForTimeout(Math.min(Math.max(timeout / 30, 1500), 5000));
    }

    private ExtractionRun extractAcrossPages(Page page, GenericCrawlRequest request) {
        List<Map<String, Object>> allData = new ArrayList<>();
        GenericCrawlRequest.PaginationRule pagination = request.getPagination();
        int maxPages = pagination != null && pagination.getMaxPages() != null ? pagination.getMaxPages() : 1;
        int waitTime = pagination != null && pagination.getWaitTime() != null ? pagination.getWaitTime() : 2000;
        int crawledPages = 0;

        for (int pageIndex = 0; pageIndex < maxPages; pageIndex++) {
            allData.addAll(extractData(page, request.getExtractionRules()));
            crawledPages++;
            if (pageIndex >= maxPages - 1 || pagination == null || maxPages <= 1) {
                break;
            }
            boolean hasNext = goToNextPage(page, pagination);
            if (!hasNext) {
                break;
            }
            page.waitForTimeout(waitTime);
        }
        return new ExtractionRun(allData, crawledPages);
    }

    private List<Map<String, Object>> extractData(Page page, List<GenericCrawlRequest.ExtractionRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        int maxItems = 0;
        for (GenericCrawlRequest.ExtractionRule rule : rules) {
            int count = page.locator(rule.getSelector()).count();
            maxItems = Math.max(maxItems, count);
        }
        maxItems = Math.min(maxItems, MAX_ITEMS_PER_PAGE);

        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            for (GenericCrawlRequest.ExtractionRule rule : rules) {
                item.put(rule.getField(), extractField(page, rule, i));
            }
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> extractData(
            String rawHtml,
            String baseUrl,
            List<GenericCrawlRequest.ExtractionRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        int maxItems = 0;
        Map<GenericCrawlRequest.ExtractionRule, List<HtmlMatch>> matched = new LinkedHashMap<>();
        for (GenericCrawlRequest.ExtractionRule rule : rules) {
            List<HtmlMatch> elements = findMatches(rawHtml, rule.getSelector(), baseUrl);
            matched.put(rule, elements);
            maxItems = Math.max(maxItems, elements.size());
        }
        maxItems = Math.min(maxItems, MAX_ITEMS_PER_PAGE);

        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            for (GenericCrawlRequest.ExtractionRule rule : rules) {
                List<HtmlMatch> elements = matched.getOrDefault(rule, Collections.emptyList());
                item.put(rule.getField(), i < elements.size() ? extractField(elements.get(i), rule) : "");
            }
            results.add(item);
        }
        return results;
    }

    private List<HtmlMatch> findMatches(String html, String selector, String baseUrl) {
        if (!hasText(html) || !hasText(selector)) {
            return Collections.emptyList();
        }

        List<HtmlMatch> matches = new ArrayList<>();
        for (String selectorPart : selector.split(",")) {
            String simpleSelector = normalizeSimpleSelector(selectorPart);
            if (!hasText(simpleSelector)) {
                continue;
            }

            String tagName = extractTagName(simpleSelector);
            if (!hasText(tagName)) {
                continue;
            }

            String requiredAttr = extractRequiredAttribute(simpleSelector);
            if (isVoidTag(tagName)) {
                Pattern pattern = Pattern.compile("<" + Pattern.quote(tagName) + "\\b([^>]*)>",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher matcher = pattern.matcher(html);
                while (matcher.find() && matches.size() < MAX_ITEMS_PER_PAGE) {
                    Map<String, String> attrs = parseAttributes(matcher.group(1), baseUrl);
                    if (hasText(requiredAttr) && !attrs.containsKey(requiredAttr.toLowerCase())) {
                        continue;
                    }
                    matches.add(new HtmlMatch("", matcher.group(0), attrs));
                }
                continue;
            }

            Pattern pattern = Pattern.compile(
                    "<" + Pattern.quote(tagName) + "\\b([^>]*)>(.*?)</" + Pattern.quote(tagName) + ">",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            while (matcher.find() && matches.size() < MAX_ITEMS_PER_PAGE) {
                Map<String, String> attrs = parseAttributes(matcher.group(1), baseUrl);
                if (hasText(requiredAttr) && !attrs.containsKey(requiredAttr.toLowerCase())) {
                    continue;
                }
                String innerHtml = matcher.group(2);
                matches.add(new HtmlMatch(stripTags(innerHtml), innerHtml, attrs));
            }
        }
        return matches;
    }

    private Object extractField(Page page, GenericCrawlRequest.ExtractionRule rule, int index) {
        try {
            Locator locator = page.locator(rule.getSelector()).nth(index);
            String type = rule.getType() != null ? rule.getType() : "text";
            return switch (type) {
                case "html" -> locator.innerHTML();
                case "attr" -> locator.getAttribute(rule.getAttr() != null && !rule.getAttr().isBlank() ? rule.getAttr() : "href");
                default -> locator.textContent();
            };
        } catch (Exception ex) {
            log.debug("Failed to extract field {}", rule.getField(), ex);
            return "";
        }
    }

    private Object extractField(HtmlMatch match, GenericCrawlRequest.ExtractionRule rule) {
        String type = rule.getType() != null ? rule.getType() : "text";
        return switch (type) {
            case "html" -> match.html();
            case "attr" -> match.attrs().getOrDefault((hasText(rule.getAttr()) ? rule.getAttr() : "href").toLowerCase(), "");
            default -> match.text();
        };
    }

    private List<Map<String, Object>> buildFallbackData(Page page, String summaryText) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", page.title());
        item.put("finalUrl", page.url());
        item.put("summaryText", summaryText);
        return List.of(item);
    }

    private List<Map<String, Object>> buildFallbackData(String title, String finalUrl, String summaryText) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("finalUrl", finalUrl);
        item.put("summaryText", summaryText);
        return List.of(item);
    }

    private boolean goToNextPage(Page page, GenericCrawlRequest.PaginationRule pagination) {
        try {
            String type = pagination.getType() != null ? pagination.getType() : "next";
            String selector = pagination.getSelector();
            if ("scroll".equals(type)) {
                page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                return true;
            }
            if (selector == null || selector.isBlank()) {
                return false;
            }
            page.click(selector, new Page.ClickOptions().setTimeout(5000));
            return true;
        } catch (Exception ex) {
            log.debug("No next page available", ex);
            return false;
        }
    }

    private String extractSummary(Page page) {
        try {
            Object text = page.evaluate("() => document.body ? document.body.innerText : ''");
            String summary = text != null ? String.valueOf(text).replaceAll("\\s+", " ").trim() : "";
            if (summary.length() > 2000) {
                return summary.substring(0, 2000);
            }
            return summary;
        } catch (Exception ex) {
            log.debug("Failed to extract summary text", ex);
            return "";
        }
    }

    private String extractTitle(String rawHtml) {
        if (!hasText(rawHtml)) {
            return "";
        }
        Matcher matcher = Pattern.compile("<title\\b[^>]*>(.*?)</title>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(rawHtml);
        if (!matcher.find()) {
            return "";
        }
        return trimToLimit(stripTags(matcher.group(1)), 500);
    }

    private String extractSummary(String rawHtml) {
        if (!hasText(rawHtml)) {
            return "";
        }
        String text = rawHtml
                .replaceAll("(?is)<script\\b[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style\\b[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<noscript\\b[^>]*>.*?</noscript>", " ")
                .replaceAll("(?is)<svg\\b[^>]*>.*?</svg>", " ");
        return trimToLimit(stripTags(text), 4000);
    }

    private String normalizeSimpleSelector(String selector) {
        if (!hasText(selector)) {
            return "";
        }
        String normalized = selector.trim();
        String[] parts = normalized.split("\\s*>\\s*|\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (hasText(parts[i])) {
                return parts[i].trim();
            }
        }
        return normalized;
    }

    private String extractTagName(String selector) {
        Matcher matcher = Pattern.compile("^([a-zA-Z][\\w-]*)").matcher(selector);
        return matcher.find() ? matcher.group(1).toLowerCase() : "";
    }

    private String extractRequiredAttribute(String selector) {
        Matcher matcher = Pattern.compile("\\[\\s*([:\\w-]+)(?:\\s*[~|^$*]?=\\s*[^\\]]+)?\\s*]").matcher(selector);
        return matcher.find() ? matcher.group(1).toLowerCase() : "";
    }

    private boolean isVoidTag(String tagName) {
        return "img".equalsIgnoreCase(tagName)
                || "meta".equalsIgnoreCase(tagName)
                || "link".equalsIgnoreCase(tagName)
                || "input".equalsIgnoreCase(tagName)
                || "source".equalsIgnoreCase(tagName);
    }

    private Map<String, String> parseAttributes(String attributeText, String baseUrl) {
        if (attributeText == null) {
            return Collections.emptyMap();
        }

        Map<String, String> attrs = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("([:\\w-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))")
                .matcher(attributeText);
        while (matcher.find()) {
            String name = matcher.group(1).toLowerCase();
            String value = matcher.group(3) != null
                    ? matcher.group(3)
                    : matcher.group(4) != null ? matcher.group(4) : matcher.group(5);
            attrs.put(name, decodeHtml(value));
        }

        resolveUrlAttribute(attrs, "href", baseUrl);
        resolveUrlAttribute(attrs, "src", baseUrl);
        return attrs;
    }

    private void resolveUrlAttribute(Map<String, String> attrs, String attrName, String baseUrl) {
        String value = attrs.get(attrName);
        if (!hasText(value) || !hasText(baseUrl)) {
            return;
        }
        try {
            attrs.put(attrName, URI.create(baseUrl).resolve(value).toString());
        } catch (Exception ignored) {
            // Keep the original attribute value if URI resolution fails.
        }
    }

    private String stripTags(String html) {
        if (html == null) {
            return "";
        }
        return decodeHtml(html.replaceAll("(?is)<[^>]+>", " "))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String decodeHtml(String text) {
        if (text == null) {
            return "";
        }

        String decoded = text
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");

        Matcher numeric = Pattern.compile("&#(x?[0-9a-fA-F]+);").matcher(decoded);
        StringBuffer buffer = new StringBuffer();
        while (numeric.find()) {
            try {
                String value = numeric.group(1);
                int codePoint = value.startsWith("x") || value.startsWith("X")
                        ? Integer.parseInt(value.substring(1), 16)
                        : Integer.parseInt(value);
                numeric.appendReplacement(buffer, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
            } catch (Exception ex) {
                numeric.appendReplacement(buffer, Matcher.quoteReplacement(numeric.group(0)));
            }
        }
        numeric.appendTail(buffer);
        return buffer.toString();
    }

    private String trimToLimit(String value, int limit) {
        if (value == null) {
            return "";
        }
        return value.length() > limit ? value.substring(0, limit) : value;
    }

    private int getRequestedPages(GenericCrawlRequest request, boolean hasExtractionRules) {
        if (!hasExtractionRules || request.getPagination() == null || request.getPagination().getMaxPages() == null) {
            return 1;
        }
        return request.getPagination().getMaxPages();
    }

    private void callbackToProjectGl(String callbackUrl, GenericCrawlResponse response) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            return;
        }
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("taskId", response.getTaskId());
            body.put("status", response.getStatus());
            body.put("errorMessage", response.getErrorMessage());
            body.put("finalUrl", response.getFinalUrl());
            body.put("title", response.getTitle());
            body.put("summaryText", response.getSummaryText());
            body.put("rawHtml", response.getRawHtml());
            body.put("structuredData", response.getStructuredData());
            body.put("totalCount", response.getTotalCount());
            body.put("crawledPages", response.getCrawledPages());

            restTemplate.postForEntity(
                    callbackUrl,
                    new org.springframework.http.HttpEntity<>(body, headers),
                    String.class
            );
        } catch (Exception ex) {
            log.warn("Callback failed for taskId={}", response.getTaskId(), ex);
        }
    }

    private record ExtractionRun(List<Map<String, Object>> data, int pages) {
    }

    private record HtmlMatch(String text, String html, Map<String, String> attrs) {
    }
}
