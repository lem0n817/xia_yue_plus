package plus.rule;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 染色规则模型（HaE 规则的精简映射）。
 * 只保留染色所需的字段：name / f_regex / color / scope，
 * s_regex、format、engine、validator 对行染色无意义，一律忽略。
 */
public class RuleDefinition {

    private static final Map<String, Integer> COLOR_PRIORITY = new HashMap<String, Integer>();

    static {
        // 数值越小优先级越高；多规则命中时取最高优先级颜色
        COLOR_PRIORITY.put("red", 0);
        COLOR_PRIORITY.put("orange", 1);
        COLOR_PRIORITY.put("magenta", 2);
        COLOR_PRIORITY.put("pink", 3);
        COLOR_PRIORITY.put("yellow", 4);
        COLOR_PRIORITY.put("cyan", 5);
        COLOR_PRIORITY.put("blue", 6);
        COLOR_PRIORITY.put("green", 7);
        COLOR_PRIORITY.put("gray", 8);
    }

    private final String name;
    private final String regex;
    private final String color;
    private final String scope;
    private final Pattern pattern;
    private final int priority;

    public RuleDefinition(String name, String regex, String color, String scope) {
        this.name = name;
        this.regex = regex;
        this.color = color == null ? "gray" : color;
        this.scope = scope == null ? "any" : scope;
        this.pattern = Pattern.compile(regex);
        Integer p = COLOR_PRIORITY.get(this.color.toLowerCase());
        this.priority = p == null ? 9 : p;
    }

    public String getName() {
        return this.name;
    }

    public String getRegex() {
        return this.regex;
    }

    public String getColor() {
        return this.color;
    }

    public String getScope() {
        return this.scope;
    }

    public Pattern getPattern() {
        return this.pattern;
    }

    public int getPriority() {
        return this.priority;
    }

    /** 该规则是否作用于响应头（含状态行） */
    public boolean matchHeader() {
        return this.scope.equals("any")
                || this.scope.equals("response")
                || this.scope.equals("any header")
                || this.scope.equals("response header")
                || this.scope.equals("response line");
    }

    /** 该规则是否作用于响应体 */
    public boolean matchBody() {
        return this.scope.equals("any")
                || this.scope.equals("response")
                || this.scope.equals("any body")
                || this.scope.equals("response body");
    }
}
