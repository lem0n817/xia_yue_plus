import plus.rule.RuleDefinition;
import plus.rule.RuleEngine;
import plus.rule.RuleStore;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则引擎自测（不随插件打包）：
 * 1. 真实加载本机 HaE Rules.yml，打印过滤统计
 * 2. 低权限/未授权响应命中敏感规则即染色（不做与原始响应的差集）
 */
public class SelfTest {

    public static void main(String[] args) throws Exception {
        RuleStore store = new RuleStore();
        store.reload();
        RuleStore.Stats st = store.getStats();
        System.out.println("[1] sourceType=" + store.getSourceType());
        System.out.println("[1] file=" + store.getSourceFilePath());
        System.out.println("[1] loaded=" + st.loaded + " scope=" + st.scope
                + " noneColor=" + st.noneColor + " notLoaded=" + st.notLoaded + " error=" + st.error);
        List<RuleDefinition> rules = store.getRules();
        boolean hasMobile = false;
        for (RuleDefinition r : rules) {
            if (r.getName().toLowerCase().contains("mobile")) {
                hasMobile = true;
                System.out.println("[1] mobile rule: " + r.getName() + " color=" + r.getColor());
            }
        }
        if (!hasMobile) {
            System.out.println("[1] FAIL: 手机号规则未加载");
            System.exit(1);
        }

        RuleEngine engine = new RuleEngine(store);

        // 场景A：未授权响应有手机号，原始响应也有同一个手机号 → 仍应染色（hit 不做差集）
        String same = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n"
                + "{\"mobile\":\"13812345678\"}";
        RuleEngine.Match a = engine.hit(engine.extract(parts(same)), engine.extract(parts(same)));
        System.out.println("[2] same-mobile color=" + (a == null ? "null (FAIL)" : a.color)
                + " => " + (a != null && "orange".equals(a.color) ? "OK" : "FAIL"));

        // 场景B：未授权响应泄露 JWT + 手机号 → 染最高优先级色
        String leaked = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n"
                + "{\"user\":\"admin\",\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abcDEF123_-x\","
                + "\"mobile\":\"13812345678\"}";
        String clean = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n<html>hello</html>";
        RuleEngine.Match b = engine.hit(engine.extract(parts(clean)), engine.extract(parts(leaked)));
        System.out.println("[3] leak color=" + (b == null ? "null (FAIL)" : b.color));
        if (b != null) {
            for (Map.Entry<String, List<String>> en : b.byRule.entrySet()) {
                System.out.println("    rule=" + en.getKey() + " values=" + en.getValue());
            }
        }

        // 场景C：三个响应都干净 → 不染色
        RuleEngine.Match c = engine.hit(engine.extract(parts(clean)), engine.extract(parts(clean)));
        System.out.println("[4] clean no-hit => " + (c == null ? "null (OK)" : "FAIL: " + c.color));

        // 场景D：指纹类规则（Shiro rememberMe / Swagger UI）不参与
        String fingerprint = "HTTP/1.1 200 OK\r\nSet-Cookie: rememberMe=deleteMe; Path=/; Max-Age=1\r\n\r\n"
                + "{\"swagger\":\"2.0\",\"swaggerVersion\":\"1.2\"}";
        Set<String> f = engine.extract(parts(fingerprint));
        boolean bad = false;
        for (String s : f) {
            if (s.startsWith("Shiro") || s.startsWith("Swagger")) bad = true;
        }
        System.out.println("[5] fingerprint => " + (bad ? "FAIL: 指纹规则参与了染色" : "OK（Shiro/Swagger 未参与）"));

        // 场景E：gzip 压缩的未授权响应（模拟服务器强制压缩）→ 解压后应命中
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gos = new java.util.zip.GZIPOutputStream(bos)) {
            gos.write(leaked.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        byte[] gzHeader = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Encoding: gzip\r\n\r\n"
                .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        byte[] gzBody = bos.toByteArray();
        byte[] gzipResp = new byte[gzHeader.length + gzBody.length];
        System.arraycopy(gzHeader, 0, gzipResp, 0, gzHeader.length);
        System.arraycopy(gzBody, 0, gzipResp, gzHeader.length, gzBody.length);
        RuleEngine.Match e = engine.hit(new java.util.HashSet<String>(), engine.extractResponse(gzipResp));
        System.out.println("[6] gzip leak color=" + (e == null ? "null (FAIL)" : e.color)
                + " => " + (e != null && "orange".equals(e.color) ? "OK" : "FAIL"));

        System.out.println("SelfTest done.");
    }

    private static String[] parts(String response) {
        int idx = response.indexOf("\r\n\r\n");
        return new String[]{response.substring(0, idx + 4), response.substring(idx + 4)};
    }
}
