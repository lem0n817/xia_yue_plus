# Yue Plus（xia_yue_plus）

基于经典越权检测插件 **xia Yue V1.2**（作者：算命縖子）增强修改的 Burp Suite 扩展：在原有的"低权限 / 未授权"三包对比检测之上，引入 **HaE 规则联动**与**敏感数据整行染色**，让存在敏感数据泄露的越权点在表格里一眼可见。

> 插件名：瞎越 author：算命縖子、lemoni

## 功能特性

### 继承自 xia Yue V1.2
- 拦截 Proxy 流量，对每条唯一请求（URL + 参数名 + 方法 MD5 去重）自动发送两个探测包：
  - **低权限包**：将请求头中的认证信息（Cookie / Authorization 等）替换为配置的低权限凭据
  - **未授权包**：移除请求头中的认证信息
- 对比原始 / 低权限 / 未授权三个响应的包体长度，辅助判断越权与未授权访问
- 域名白名单、静态资源过滤、原始/低权限/未授权三包查看器

### Yue Plus 新增
- **敏感数据行染色**：低权限 / 未授权响应命中敏感信息规则（手机号、身份证、JWT、私钥、云密钥等）时，表格整行按规则颜色染色；**选中行同样保留染色**（叠加在选择底色上）
- **HaE 规则联动**：直接读取 `~/.config/HaE/Rules.yml`，与 HaE - Highlighter and Extractor 共用一套规则库，HaE 里调整规则后点「重载规则」即刻生效
- **指纹规则排除**：Shiro、Swagger UI、Ueditor、Druid、PDF.js Viewer 等纯组件指纹规则不参与染色（内置名单精确排除，避免噪音）
- **Burp 原生高亮透传**：命中时同步调用 `setHighlight`，在 Proxy HTTP history / 站点地图中同样标色
- **gzip/deflate 自动处理**：发送探测包前剥离 `Accept-Encoding`，并对强制压缩的响应自动解压后再匹配（Proxy 会解压而扩展原始字节不会，这是同类实现常见的漏报坑）

## 规则文件解析顺序

启动（或点击「重载规则」）时按以下顺序定位规则文件：

1. `~/.config/HaE/Rules.yml` —— 已安装 HaE 时直接联动使用
2. `<jar 所在目录>/.config/xia-yue-plus/Rules.yml` —— 独立配置
3. 均不存在时，自动创建目录 2 并释放插件内置的默认规则（源自 HaE 规则集快照），此后插件不会回写覆盖

参与染色的规则（过滤条件，无开关）：

- `loaded: true`
- 作用域包含响应侧：`any` / `any body` / `response` / `response body` / `response header` / `response line`
- 规则名不在指纹名单内（Shiro、Swagger UI、Ueditor、Druid、PDF.js Viewer）
- `color` 不为 `none`

多规则命中时按优先级取最高色：`red > orange > magenta > pink > yellow > cyan > blue > green > gray`。

## 使用方法

1. Burp Suite → Extensions → Add → 选择 `xia_yue_plus-1.3.jar`
2. 切到 `Yue Plus` 标签页，勾选 **启动插件**（重载插件后需重新勾选）
3. 在「越权」文本框填入低权限凭据（替换型），「未授权」文本框填入要移除的请求头名称（每行一个）
4. 正常浏览目标站点即可；表格中出现记录后：
   - **整行变色** = 低权限或未授权响应中命中了敏感数据规则（颜色 = 最高优先级命中规则的颜色）
   - 点击行可切换查看原始 / 低权限 / 未授权三组请求响应
5. 修改 HaE 规则后点击 **重载规则** 同步

> 注意：MD5 去重只包含参数名不含参数值，同一 URL 换参数值不会重复探测（与原版一致）。

## 构建

依赖：

- JDK 17+（JDK 21 验证通过；字节码 `--release 17`，兼容近几年 Burp 自带 JRE）
- [snakeyaml 2.2](https://mvnrepository.com/artifact/org.yaml/snakeyaml/2.2) → 放入 `libs/`
- [burp-extender-api](https://mvnrepository.com/artifact/net.portswigger/burp-extender-api)（如 2.3.x）→ 放入 `libs/`

```bash
JAVA_HOME=/path/to/jdk ./build.sh
# 产物：xia_yue_plus-1.3.jar
```

Windows 下 classpath 分隔符已按 `;` 处理（脚本面向 Git Bash / MSYS2 环境）。

## 目录结构

```
src/burp/BurpExtender.java   主扩展（继承自原版逻辑 + 染色接入）
src/plus/rule/RuleStore.java    规则文件定位、YAML 解析、指纹过滤
src/plus/rule/RuleEngine.java   响应解压 + 正则提取 + 命中判定
src/plus/rule/RuleDefinition.java  规则模型（名称/正则/颜色/作用域）
src/plus/ui/ColorScheme.java    色名 → Swing 颜色映射与 alpha 混合
resources/rules/Rules.yml       内置默认规则（HaE 规则集快照）
test/SelfTest.java              规则引擎自测（真实加载 Rules.yml 验证过滤与命中）
test/vuln_server.py             本地越权模拟服务（强制 gzip，用于联调）
build.sh                        一键构建脚本
```

## 免责声明

本工具仅面向**已授权**的安全测试与教学研究场景，请勿用于未授权测试。使用者对其一切行为自行负责。

## 致谢

- [算命縖子](https://www.t00ls.net) —— 原版 xia Yue V1.2 作者
- [Moonlit](https://github.com/Moonlit-7) —— 原版致谢名单
- [HaE - Highlighter and Extractor](https://github.com/gh0stkey/HaE)（Apache-2.0）—— 规则文件定位与规则集来源
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml)（Apache-2.0）

## License

[Apache-2.0](META-INF/LICENSE)（见 `resources/META-INF/LICENSE` 与 `NOTICE`）
