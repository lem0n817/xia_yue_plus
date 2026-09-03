package plus.rule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * 规则引擎：对响应做规则提取并判定行染色。
 *
 * 关键点：插件经 makeHttpRequest 拿到的响应是原始字节，Proxy 会自动解压而这里不会，
 * 因此 extractResponse 需自行识别 gzip/deflate 并解压后再跑正则。
 */
public class RuleEngine {

    private static final String SEP = "\u0001";
    private static final int MAX_MATCH_PER_RULE = 50;
    private static final int MAX_VALUE_LEN = 200;
    private static final int MAX_BODY_CHARS = 1024 * 1024;

    private final RuleStore store;

    public RuleEngine(RuleStore store) {
        this.store = store;
    }

    public boolean hasRules() {
        return !this.store.getRules().isEmpty();
    }

    /**
     * 对一条完整响应（header+body 原始字节）做规则提取：
     * 自行定位 header/body 边界，gzip/deflate 自动解压，再匹配规则。
     */
    public Set<String> extractResponse(byte[] response) {
        if (response == null || response.length == 0) {
            return new HashSet<String>();
        }
        int off = findHeaderEnd(response);
        if (off < 0) {
            return new HashSet<String>();
        }
        String header = new String(response, 0, off, StandardCharsets.ISO_8859_1);
        byte[] bodyBytes = Arrays.copyOfRange(response, off, response.length);
        String lowerHeader = header.toLowerCase();
        boolean isGzip = lowerHeader.contains("content-encoding: gzip")
                || (bodyBytes.length > 2 && bodyBytes[0] == 0x1f && bodyBytes[1] == (byte) 0x8b);
        String body;
        if (isGzip) {
            body = decompress(bodyBytes, true);
        } else if (lowerHeader.contains("content-encoding: deflate")) {
            body = decompress(bodyBytes, false);
        } else {
            body = new String(bodyBytes, StandardCharsets.UTF_8);
        }
        if (body.length() > MAX_BODY_CHARS) {
            body = body.substring(0, MAX_BODY_CHARS);
        }
        return extract(header, body);
    }

    private static int findHeaderEnd(byte[] data) {
        for (int i = 0; i + 3 < data.length; ++i) {
            if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') {
                return i + 4;
            }
        }
        return -1;
    }

    private static String decompress(byte[] data, boolean gzip) {
        try (InputStream is = gzip
                ? new GZIPInputStream(new ByteArrayInputStream(data))
                : new InflaterInputStream(new ByteArrayInputStream(data))) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    public Set<String> extract(String[] headerAndBody) {
        return extract(headerAndBody[0], headerAndBody[1]);
    }

    public Set<String> extract(String header, String body) {
        Set<String> out = new HashSet<String>();
        for (RuleDefinition rule : this.store.getRules()) {
            if (rule.matchHeader()) {
                match(rule, header, out);
            }
            if (rule.matchBody()) {
                match(rule, body, out);
            }
        }
        return out;
    }

    private void match(RuleDefinition rule, String text, Set<String> out) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int found = 0;
        Matcher m = rule.getPattern().matcher(text);
        while (m.find() && found < MAX_MATCH_PER_RULE) {
            String value = null;
            if (m.groupCount() > 0) {
                value = m.group(1);
            }
            if (value == null || value.isEmpty()) {
                value = m.group(0);
            }
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (value.length() > MAX_VALUE_LEN) {
                value = value.substring(0, MAX_VALUE_LEN);
            }
            out.add(rule.getName() + SEP + value);
            found++;
        }
    }

    /**
     * 命中判定：低权限/未授权响应中命中敏感规则即视为泄露（不做与原始响应的差集——
     * 原始响应同样包含该数据时，未授权也能拿到本身就是越权信号）。
     * 无任何命中返回 null（行不染色）。
     */
    public Match hit(Set<String> low, Set<String> unauth) {
        Set<String> found = new HashSet<String>();
        if (low != null) {
            found.addAll(low);
        }
        if (unauth != null) {
            found.addAll(unauth);
        }
        if (found.isEmpty()) {
            return null;
        }

        Map<String, List<String>> byRule = new LinkedHashMap<String, List<String>>();
        int bestPriority = Integer.MAX_VALUE;
        String bestColor = null;
        for (String s : found) {
            int idx = s.indexOf(SEP);
            String ruleName = idx < 0 ? s : s.substring(0, idx);
            String value = idx < 0 ? "" : s.substring(idx + 1);
            List<String> values = byRule.get(ruleName);
            if (values == null) {
                values = new ArrayList<String>();
                byRule.put(ruleName, values);
            }
            values.add(value);
            RuleDefinition rd = this.store.find(ruleName);
            if (rd != null && rd.getPriority() < bestPriority) {
                bestPriority = rd.getPriority();
                bestColor = rd.getColor();
            }
        }
        return new Match(bestColor, byRule);
    }

    /** 命中结果：整行染色颜色（可能为 null，表示无颜色规则命中）+ 按规则分组的提取明细 */
    public static final class Match {
        public final String color;
        public final Map<String, List<String>> byRule;

        public Match(String color, Map<String, List<String>> byRule) {
            this.color = color;
            this.byRule = byRule;
        }
    }
}
