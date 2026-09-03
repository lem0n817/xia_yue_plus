/*
 * Path resolution & first-run initialization derived from HaE - Highlighter and Extractor
 * Copyright 2016-2025 gh0stkey and HaE contributors.
 * Licensed under the Apache License, Version 2.0 — https://github.com/gh0stkey/HaE (modified).
 */
package plus.rule;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

/**
 * 规则仓库：定位规则文件、解析 YAML、过滤、缓存编译后的 Pattern。
 *
 * 规则文件解析顺序（resolveRulesFile）：
 *   1. ~/.config/HaE/Rules.yml   —— 联动 HaE，存在即直接使用
 *   2. <jar目录>/.config/xia-yue-plus/Rules.yml —— 本地独立配置
 *   3. 两者皆无 —— 创建 2 号目录，从插件内置资源 /rules/Rules.yml 复制一份默认规则
 *
 * 参与染色的规则（硬过滤，无开关）：
 *   loaded=true 且 scope 含响应侧 且 sensitive=true 且 color != none
 * 指纹类规则（sensitive=false，如 Swagger UI / Shiro）在解析阶段直接丢弃。
 */
public class RuleStore {

    private static final Set<String> RESPONSE_SCOPES = new HashSet<String>(Arrays.asList(
            "any", "any body", "response", "response body", "response header", "response line"));

    /**
     * 纯组件指纹规则名单（仅识别"目标用了某组件"，不含敏感数据），不参与染色。
     * 用规则名精确匹配；sensitive 字段无法区分指纹与敏感数据（手机号规则也是 sensitive=false）。
     */
    private static final Set<String> FINGERPRINT_RULES = new HashSet<String>(Arrays.asList(
            "Shiro", "Swagger UI", "Ueditor", "Druid", "PDF.js Viewer"));

    private volatile List<RuleDefinition> rules = Collections.emptyList();
    private volatile Map<String, RuleDefinition> byName = Collections.emptyMap();
    private volatile File sourceFile;
    private volatile String sourceType = "";
    private volatile Stats stats = new Stats();

    public static class Stats {
        public int loaded;
        public int fingerprint;
        public int scope;
        public int noneColor;
        public int notLoaded;
        public int error;
    }

    public synchronized void reload() throws Exception {
        File file = resolveRulesFile();
        Stats st = new Stats();
        List<RuleDefinition> list = new ArrayList<RuleDefinition>();
        Map<String, RuleDefinition> map = new HashMap<String, RuleDefinition>();

        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        try (InputStream in = Files.newInputStream(file.toPath())) {
            Object rootObj = yaml.load(in);
            if (!(rootObj instanceof Map)) {
                throw new IllegalStateException("Rules.yml 格式错误：根节点不是 Map");
            }
            Object groups = ((Map<?, ?>) rootObj).get("rules");
            if (groups instanceof List) {
                for (Object groupObj : (List<?>) groups) {
                    if (!(groupObj instanceof Map)) {
                        continue;
                    }
                    Object ruleList = ((Map<?, ?>) groupObj).get("rule");
                    if (!(ruleList instanceof List)) {
                        continue;
                    }
                    for (Object ruleObj : (List<?>) ruleList) {
                        if (!(ruleObj instanceof Map)) {
                            continue;
                        }
                        parseRule((Map<?, ?>) ruleObj, list, map, st);
                    }
                }
            }
        }

        this.rules = Collections.unmodifiableList(list);
        this.byName = map;
        this.sourceFile = file;
        this.stats = st;
    }

    private void parseRule(Map<?, ?> fields, List<RuleDefinition> out,
                           Map<String, RuleDefinition> byName, Stats st) {
        if (!Boolean.TRUE.equals(fields.get("loaded"))) {
            st.notLoaded++;
            return;
        }
        String scope = str(fields.get("scope"), "any");
        if (!RESPONSE_SCOPES.contains(scope)) {
            st.scope++;
            return;
        }
        String name = str(fields.get("name"), "");
        if (FINGERPRINT_RULES.contains(name)) {
            st.fingerprint++;
            return;
        }
        String color = str(fields.get("color"), "gray");
        if ("none".equalsIgnoreCase(color)) {
            st.noneColor++;
            return;
        }
        String regex = str(fields.get("f_regex"), "");
        if (regex.isEmpty()) {
            st.error++;
            return;
        }
        if (name.isEmpty()) {
            name = "rule-" + (st.loaded + 1);
        }
        try {
            RuleDefinition rule = new RuleDefinition(name, regex, color, scope);
            out.add(rule);
            byName.put(name, rule);
            st.loaded++;
        } catch (PatternSyntaxException e) {
            st.error++;
        }
    }

    private File resolveRulesFile() throws Exception {
        File haERules = new File(System.getProperty("user.home"),
                ".config" + File.separator + "HaE" + File.separator + "Rules.yml");
        if (haERules.isFile()) {
            this.sourceType = "HaE";
            return haERules;
        }
        File plusDir = new File(jarDirectory(), ".config" + File.separator + "xia-yue-plus");
        File local = new File(plusDir, "Rules.yml");
        if (local.isFile()) {
            this.sourceType = "本地";
            return local;
        }
        // 首次运行：从插件内置资源复制默认规则（内容为本机 HaE 规则快照），之后永不回写覆盖
        if (!plusDir.isDirectory() && !plusDir.mkdirs()) {
            throw new IllegalStateException("无法创建配置目录: " + plusDir);
        }
        try (InputStream in = RuleStore.class.getResourceAsStream("/rules/Rules.yml")) {
            if (in == null) {
                throw new IllegalStateException("未找到 HaE 规则，且插件内置默认规则资源缺失");
            }
            Files.copy(in, local.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        this.sourceType = "本地";
        return local;
    }

    private static File jarDirectory() {
        try {
            File f = new File(RuleStore.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isFile() ? f.getParentFile() : f;
        } catch (Exception e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    private static String str(Object value, String def) {
        return value == null ? def : String.valueOf(value);
    }

    public List<RuleDefinition> getRules() {
        return this.rules;
    }

    public boolean isLoaded() {
        return !this.rules.isEmpty();
    }

    public RuleDefinition find(String name) {
        return this.byName.get(name);
    }

    public Stats getStats() {
        return this.stats;
    }

    public String getSourceType() {
        return this.sourceType;
    }

    public String getSourceFilePath() {
        File f = this.sourceFile;
        return f == null ? "" : f.getAbsolutePath();
    }
}
