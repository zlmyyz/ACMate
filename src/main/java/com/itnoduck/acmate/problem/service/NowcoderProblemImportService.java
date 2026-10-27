package com.itnoduck.acmate.problem.service;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.ImportedProblemResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class NowcoderProblemImportService {
    private final ObjectMapper json;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final Pattern PRACTICE = Pattern.compile("/practice/([a-fA-F0-9]{32})/?");
    private static final Pattern CONTEST = Pattern.compile("/acm/contest/([1-9][0-9]{0,11})/([A-Za-z][A-Za-z0-9]{0,7})/?");
    private static final Pattern PROBLEM = Pattern.compile("/acm/problem/([1-9][0-9]{0,11})/?");
    public NowcoderProblemImportService(ObjectMapper json) { this.json = json; }

    public ImportedProblemResponse preview(String address) {
        URI source = canonicalUrl(address);
        String path = source.getPath();
        if (path.startsWith("/practice/")) {
            String uuid = path.substring("/practice/".length());
            return parsePractice(fetch(URI.create("https://www.nowcoder.com/practice/terminal/" + uuid)), source);
        }
        return parseContest(fetch(source), source);
    }

    public record Sample(String input, String output) {}
    public List<Sample> samples(String address) {
        URI source = canonicalUrl(address);
        var result = new ArrayList<Sample>();
        if (source.getPath().startsWith("/practice/")) {
            String uuid = source.getPath().substring("/practice/".length());
            var root = json.readTree(fetch(URI.create("https://www.nowcoder.com/practice/terminal/" + uuid)));
            if (root.path("code").asInt(-1) != 0 || root.path("data").path("question").path("paidQuestion").asBoolean(false)) throw unavailable();
            for (var sample : root.path("data").path("question").path("samples")) {
                if (result.size() >= 10) break;
                String input = sample.path("input").asText(""), output = sample.path("output").asText("");
                if (input.length() <= 65536 && output.length() <= 65536) result.add(new Sample(input, output));
            }
        } else {
            var document = Jsoup.parse(fetch(source), source.toString());
            if (document.selectFirst(".subject-describe") == null) throw unavailable();
            for (var sample : document.select(".subject-describe .question-oi")) {
                var sections = sample.select(".question-oi-mod");
                if (sections.size() < 2 || result.size() >= 10) continue;
                var input = sections.get(0).selectFirst("pre"); var output = sections.get(1).selectFirst("pre");
                if (input != null && output != null && input.wholeText().length() <= 65536 && output.wholeText().length() <= 65536)
                    result.add(new Sample(input.wholeText(), output.wholeText()));
            }
        }
        return result;
    }

    public URI canonicalUrl(String address) {
        try {
            if (address == null || address.length() > 1024) throw invalidUrl();
            URI uri = URI.create(address.strip());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) throw invalidUrl();
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getRawPath();
            if (path == null) throw invalidUrl();
            var practice = PRACTICE.matcher(path);
            if (Set.of("www.nowcoder.com", "nowcoder.com").contains(host) && practice.matches())
                return URI.create("https://www.nowcoder.com/practice/" + practice.group(1).toLowerCase(Locale.ROOT));
            var contest = CONTEST.matcher(path);
            if ("ac.nowcoder.com".equals(host) && contest.matches())
                return URI.create("https://ac.nowcoder.com/acm/contest/" + contest.group(1) + "/" + contest.group(2).toUpperCase(Locale.ROOT));
            var problem = PROBLEM.matcher(path);
            if ("ac.nowcoder.com".equals(host) && problem.matches())
                return URI.create("https://ac.nowcoder.com/acm/problem/" + problem.group(1));
            throw invalidUrl();
        } catch (IllegalArgumentException ex) { throw invalidUrl(); }
    }

    // Requests are built only from validated IDs and fixed HTTPS hosts. Never follow redirects or forward cookies.
    protected String fetch(URI uri) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000); connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "ACMate/1.0 (public problem preview)");
            connection.setRequestProperty("Accept", "text/html, application/json");
            int status = connection.getResponseCode();
            if (status == 404) throw new BusinessException(404, "牛客题目不存在，请检查链接");
            if (status != 200) throw new BusinessException(502, "牛客暂不允许读取此题，请确认题目公开且无需登录");
            try (var stream = connection.getInputStream()) {
                byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) throw new BusinessException(502, "题目页面过大，暂不支持导入");
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException ex) { throw new BusinessException(502, "读取牛客题目超时或连接失败，请稍后重试"); }
        finally { if (connection != null) connection.disconnect(); }
    }

    public ImportedProblemResponse parsePractice(String body, URI source) {
        try {
            var root = json.readTree(body);
            if (root.path("code").asInt(-1) != 0) throw unavailable();
            var data = root.path("data"); var question = data.path("question");
            String title = question.path("title").asText("").strip();
            if (question.path("paidQuestion").asBoolean(false) || title.isBlank()
                    || question.path("content").asText("").isBlank()) throw unavailable();
            StringBuilder content = new StringBuilder();
            section(content, "题目描述", markdown(question.path("content").asText(""), source));
            section(content, "输入描述", markdown(question.path("inputDesc").asText(""), source));
            section(content, "输出描述", markdown(question.path("outputDesc").asText(""), source));
            int index = 1;
            for (var sample : question.path("samples")) {
                content.append("## 示例 ").append(index++).append("\n\n");
                section(content, "输入", fenced(sample.path("input").asText("")));
                section(content, "输出", fenced(sample.path("output").asText("")));
                section(content, "说明", markdown(sample.path("note").asText(""), source));
            }
            section(content, "提示", markdown(question.path("hint").asText(""), source));
            var tags = new LinkedHashSet<String>();
            for (var tag : data.path("questionTags")) {
                String name = tag.path("name").asText(tag.path("tagName").asText(""));
                if (!name.isBlank()) tags.add(name);
            }
            String id = question.path("problemId").asText("");
            String key = id.matches("[1-9][0-9]*") ? "NC" + id : source.getPath().substring(1).replace('/', '-');
            return response(key, title, source, question.path("difficulty").asText(""), String.join(",", tags), content.toString());
        } catch (tools.jackson.core.JacksonException ex) { throw unavailable(); }
    }

    public ImportedProblemResponse parseContest(String html, URI source) {
        var document = Jsoup.parse(html, source.toString());
        var titleElement = document.selectFirst(".question-title, .terminal-topic-title h1, .terminal-topic-title h2");
        var description = document.selectFirst(".subject-describe");
        if (description == null || description.text().isBlank()) throw unavailable();
        String title = titleElement == null ? "" : titleElement.text().strip();
        if (title.isBlank()) title = document.title().split("_", 2)[0].replaceFirst("^[A-Za-z]+-", "").strip();
        String key = source.getPath().substring(1).replace('/', '-');
        var id = Pattern.compile("\\bproblemId\\s*:\\s*['\"]?([1-9][0-9]*)").matcher(html);
        if (id.find()) key = "NC" + id.group(1);
        else if (source.getPath().startsWith("/acm/problem/")) key = "NC" + source.getPath().substring("/acm/problem/".length());
        StringBuilder content = new StringBuilder();
        var limits = document.selectFirst(".subject-item-wrap");
        if (limits != null) section(content, "题目限制", markdown(limits.html(), source));
        section(content, "题目描述", markdown(description.html(), source));
        return response(key, title, source, "", "", content.toString());
    }

    private ImportedProblemResponse response(String key, String title, URI source, String difficulty, String tags, String content) {
        if (title.isBlank() || title.length() > 255 || content.isBlank()) throw unavailable();
        var warnings = new ArrayList<String>();
        if (difficulty.isBlank()) warnings.add("牛客未公开难度，可手动填写");
        if (tags.isBlank()) warnings.add("牛客未公开标签，可手动填写");
        return new ImportedProblemResponse("NOWCODER", key, title, source.toString(), difficulty, tags, content.strip(), warnings);
    }
    private static void section(StringBuilder out, String heading, String content) {
        if (!content.isBlank()) out.append("## ").append(heading).append("\n\n").append(content.strip()).append("\n\n");
    }
    private static String fenced(String text) {
        String fence = "```";
        while (text.contains(fence)) fence += "`";
        return fence + "text\n" + text + "\n" + fence;
    }
    private String markdown(String html, URI source) {
        var fragment = Jsoup.parseBodyFragment(html, source.toString());
        fragment.select("script,style,button,.copy-btn,.js-copy,.icon-fullscreen,.icon-resize-small").remove();
        return convert(fragment.body()).replaceAll("\\n[ \\t]+\\n", "\n\n").replaceAll("\\n{3,}", "\n\n").strip();
    }
    private String convert(Node node) {
        if (node instanceof TextNode text) return text.getWholeText().replaceAll("[\\r\\n\\t]+", " ").replaceAll(" {2,}", " ").replaceAll("([\\\\`*_<>{}\\[\\]])", "\\\\$1");
        if (!(node instanceof Element element)) return "";
        String tag = element.normalName();
        if (tag.equals("br")) return "\n";
        if (tag.equals("img")) {
            String src = element.absUrl("src");
            if (!src.startsWith("https://") && !src.startsWith("http://")) return element.attr("alt");
            return "![" + element.attr("alt").replace("[", "").replace("]", "").replace("\\", "\\\\") + "](<" + src.replace(">", "%3E").replace("<", "%3C") + ">)";
        }
        if (tag.equals("pre") && element.select("img").isEmpty()) return "\n\n" + fenced(element.wholeText().strip()) + "\n\n";
        if (tag.equals("code")) return "`" + element.text().replace("`", "\\`") + "`";
        StringBuilder result = new StringBuilder();
        for (Node child : element.childNodes()) result.append(convert(child));
        String text = result.toString();
        if (tag.matches("h[1-6]") || element.hasClass("question-oi-hd")) return "\n\n### " + element.text() + "\n\n";
        if (tag.equals("li")) return "\n- " + text.strip();
        if (Set.of("p", "div", "pre", "ul", "ol").contains(tag)) return "\n" + text + "\n";
        if (Set.of("strong", "b").contains(tag)) return "**" + text + "**";
        return text;
    }
    private BusinessException invalidUrl() { return new BusinessException(400, "请输入牛客 HTTPS 题目链接：竞赛题、公开题库或练习题链接"); }
    private BusinessException unavailable() { return new BusinessException(422, "无法读取完整公开题面，请确认题目无需登录或付费，或手动填写"); }
}


