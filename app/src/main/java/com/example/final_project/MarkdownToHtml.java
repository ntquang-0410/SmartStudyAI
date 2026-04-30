package com.example.final_project;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts Gemini's Markdown+LaTeX response to styled HTML.
 * LaTeX delimiters ($$...$$, $...$, \[...\], \(...\)) are extracted before
 * markdown processing and restored afterward, then rendered by KaTeX CDN.
 */
public class MarkdownToHtml {

    // Placeholder patterns unlikely to appear in Gemini math output
    private static final String LATEX_OPEN  = "LXTKQ";
    private static final String LATEX_CLOSE = "QLXTK";
    private static final String CODE_OPEN   = "CDTKQ";
    private static final String CODE_CLOSE  = "QCDTK";

    private static final String HTML_TEMPLATE =
        "<!DOCTYPE html><html lang='vi'><head>"
        + "<meta charset='UTF-8'>"
        + "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=2'>"
        + "<link rel='stylesheet'"
        + " href='https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css'"
        + " crossorigin='anonymous'>"
        + "<script defer src='https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js'"
        + " crossorigin='anonymous'></script>"
        + "<script defer"
        + " src='https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js'"
        + " crossorigin='anonymous'"
        + " onload='renderMathInElement(document.body,{delimiters:["
        + "{left:\"$$\",right:\"$$\",display:true},"
        + "{left:\"$\",right:\"$\",display:false},"
        + "{left:\"\\\\[\",right:\"\\\\]\",display:true},"
        + "{left:\"\\\\(\",right:\"\\\\)\",display:false}"
        + "],throwOnError:false});'></script>"
        + "<style>"
        + "*{box-sizing:border-box}"
        + "body{font-family:system-ui,-apple-system,'Segoe UI',sans-serif;"
        +      "font-size:15px;line-height:1.8;color:#1e293b;"
        +      "padding:16px 16px 24px;margin:0;background:#fff;word-break:break-word}"
        + "h2{font-size:18px;color:#4f46e5;margin:18px 0 8px;font-weight:700}"
        + "h3{font-size:16px;color:#1d4ed8;margin:14px 0 6px;font-weight:700}"
        + "h4{font-size:15px;color:#334155;margin:10px 0 4px;font-weight:600}"
        + ".sec{display:block;margin:18px 0 4px;padding:10px 14px;"
        +       "background:#eff6ff;border-left:4px solid #4f46e5;"
        +       "border-radius:0 8px 8px 0;font-weight:700;font-size:15px;color:#1e293b}"
        + ".sec.answer{background:#f0fdf4;border-color:#10b981}"
        + ".sec.tip{background:#fffbeb;border-color:#f59e0b}"
        + "p{margin:8px 0}"
        + "strong{color:#0f172a;font-weight:700}"
        + "em{color:#475569;font-style:italic}"
        + "code{background:#f1f5f9;border-radius:4px;padding:1px 5px;"
        +      "font-size:13px;font-family:'Courier New',monospace;color:#be185d}"
        + "pre{background:#1e293b;border-radius:8px;padding:14px;overflow-x:auto;margin:12px 0}"
        + "pre code{background:none;color:#e2e8f0;padding:0;font-size:13px}"
        + "ul,ol{padding-left:22px;margin:6px 0}"
        + "li{margin:4px 0}"
        + "hr{border:none;border-top:1px solid #e2e8f0;margin:16px 0}"
        + ".katex-display{background:#f8fafc;border-radius:8px;"
        +                "padding:12px 14px;margin:10px 0;overflow-x:auto}"
        + ".katex{font-size:1.05em}"
        + "</style></head><body>%CONTENT%</body></html>";

    // ── Public API ────────────────────────────────────────────────────────────

    /** Convert a Gemini markdown+LaTeX string to a self-contained HTML page. */
    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return HTML_TEMPLATE.replace("%CONTENT%", "<p>Không có kết quả.</p>");
        }
        List<String> latexTokens = new ArrayList<>();
        String text    = protectLatex(markdown, latexTokens);
        String content = processMarkdown(text);
        content        = restoreLatex(content, latexTokens);
        return HTML_TEMPLATE.replace("%CONTENT%", content);
    }

    // ── LaTeX protection ──────────────────────────────────────────────────────

    private static String protectLatex(String text, List<String> tokens) {
        StringBuilder sb = new StringBuilder();
        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            // $$...$$ display math (check before single $)
            if (c == '$' && i + 1 < n && text.charAt(i + 1) == '$') {
                int end = text.indexOf("$$", i + 2);
                if (end != -1) {
                    addLatexToken(sb, tokens, text.substring(i, end + 2));
                    i = end + 2;
                    continue;
                }
            }
            // $...$ inline math
            else if (c == '$') {
                int end = closingDollar(text, i + 1);
                if (end != -1) {
                    addLatexToken(sb, tokens, text.substring(i, end + 1));
                    i = end + 1;
                    continue;
                }
            }
            // \[...\] display math
            else if (c == '\\' && i + 1 < n && text.charAt(i + 1) == '[') {
                int end = text.indexOf("\\]", i + 2);
                if (end != -1) {
                    addLatexToken(sb, tokens, text.substring(i, end + 2));
                    i = end + 2;
                    continue;
                }
            }
            // \(...\) inline math
            else if (c == '\\' && i + 1 < n && text.charAt(i + 1) == '(') {
                int end = text.indexOf("\\)", i + 2);
                if (end != -1) {
                    addLatexToken(sb, tokens, text.substring(i, end + 2));
                    i = end + 2;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static int closingDollar(String text, int start) {
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') return -1;
            if (c == '$') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '$') return -1;
                if (i > start) return i;
            }
        }
        return -1;
    }

    private static void addLatexToken(StringBuilder sb, List<String> tokens, String latex) {
        sb.append(LATEX_OPEN).append(tokens.size()).append(LATEX_CLOSE);
        tokens.add(latex);
    }

    /**
     * Restore LaTeX into HTML, escaping HTML-special chars first so the HTML
     * parser does not mangle them. The browser decodes &lt; back to < before
     * KaTeX reads the text node.
     */
    private static String restoreLatex(String html, List<String> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            String safe = tokens.get(i)
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            html = html.replace(LATEX_OPEN + i + LATEX_CLOSE, safe);
        }
        return html;
    }

    // ── Markdown → HTML ───────────────────────────────────────────────────────

    private static String processMarkdown(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder html = new StringBuilder();
        boolean inCode = false, inUl = false, inOl = false;

        for (String raw : lines) {
            String line = raw.trim();

            // Code fence
            if (line.startsWith("```")) {
                closeLists(html, inUl, inOl); inUl = false; inOl = false;
                if (!inCode) { html.append("<pre><code>"); inCode = true; }
                else         { html.append("</code></pre>\n"); inCode = false; }
                continue;
            }
            if (inCode) { html.append(esc(raw)).append('\n'); continue; }

            // Blank line
            if (line.isEmpty()) {
                closeLists(html, inUl, inOl); inUl = false; inOl = false;
                continue;
            }

            // Numbered section header: **N. Title** (Gemini format)
            if (line.matches("^\\*\\*\\d+\\..*")) {
                closeLists(html, inUl, inOl); inUl = false; inOl = false;
                String cls = ("sec " + sectionClass(line)).trim();
                html.append("<span class=\"").append(cls).append("\">")
                    .append(inline(line)).append("</span>\n");
                continue;
            }

            // ATX headers
            if (line.startsWith("### ")) {
                closeLists(html, inUl, inOl); inUl = false; inOl = false;
                html.append("<h4>").append(inline(line.substring(4))).append("</h4>\n");
                continue;
            }
            if (line.startsWith("## ")) {
                closeLists(html, inUl, inOl); inUl = false; inOl = false;
                html.append("<h3>").append(inline(line.substring(3))).append("</h3>\n");
                continue;
            }
            if (line.startsWith("# ")) {
                closeLists(html, inUl, inOl); inUl = false; inOl = false;
                html.append("<h2>").append(inline(line.substring(2))).append("</h2>\n");
                continue;
            }

            // Horizontal rule
            if (line.matches("^[-*_]{3,}$")) {
                closeLists(html, inUl, inOl); inUl = false; inOl = false;
                html.append("<hr>\n");
                continue;
            }

            // Unordered list item
            if (line.length() > 2
                    && (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• "))) {
                if (inOl) { html.append("</ol>\n"); inOl = false; }
                if (!inUl) { html.append("<ul>\n"); inUl = true; }
                html.append("<li>").append(inline(line.substring(2))).append("</li>\n");
                continue;
            }

            // Ordered list item  "1. " or "1) "
            if (line.matches("^\\d+[.)].+")) {
                if (inUl) { html.append("</ul>\n"); inUl = false; }
                if (!inOl) { html.append("<ol>\n"); inOl = true; }
                String item = line.replaceFirst("^\\d+[.)]\\s*", "");
                html.append("<li>").append(inline(item)).append("</li>\n");
                continue;
            }

            // Regular paragraph
            closeLists(html, inUl, inOl); inUl = false; inOl = false;
            html.append("<p>").append(inline(line)).append("</p>\n");
        }

        closeLists(html, inUl, inOl);
        if (inCode) html.append("</code></pre>\n");
        return html.toString();
    }

    private static String sectionClass(String line) {
        if (line.contains("Kết quả") || line.contains("kết quả")) return "answer";
        if (line.contains("Mẹo") || line.contains("mẹo")) return "tip";
        return "";
    }

    /** Apply bold, italic, and inline-code to a single text line. */
    private static String inline(String text) {
        text = esc(text);

        // Protect backtick code spans before bold/italic regex
        List<String> codes = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '`') {
                int end = text.indexOf('`', i + 1);
                if (end != -1) {
                    codes.add(text.substring(i + 1, end));
                    sb.append(CODE_OPEN).append(codes.size() - 1).append(CODE_CLOSE);
                    i = end;
                    continue;
                }
            }
            sb.append(text.charAt(i));
        }
        text = sb.toString();

        // Bold: **text** or __text__
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("__(.+?)__",          "<strong>$1</strong>");

        // Italic: *text* or _text_
        text = text.replaceAll("(?<!\\*)\\*([^*\n]+?)\\*(?!\\*)", "<em>$1</em>");
        text = text.replaceAll("(?<!_)_([^_\n]+?)_(?!_)",          "<em>$1</em>");

        // Restore code spans
        for (int j = 0; j < codes.size(); j++) {
            text = text.replace(CODE_OPEN + j + CODE_CLOSE,
                    "<code>" + esc(codes.get(j)) + "</code>");
        }
        return text;
    }

    private static void closeLists(StringBuilder sb, boolean ul, boolean ol) {
        if (ul) sb.append("</ul>\n");
        if (ol) sb.append("</ol>\n");
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
