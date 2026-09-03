#!/bin/bash
# xia_yue_plus 构建脚本
# 依赖：
#   JDK 17+（需支持 --release 17；JDK 21 已验证）——通过环境变量 JAVA_HOME 指定
#   libs/snakeyaml-2.2.jar（https://mvnrepository.com/artifact/org.yaml/snakeyaml/2.2）
#   burp-extender-api 接口（官方 Maven 构件 net.portswigger:burp-extender-api，
#   或从任意 Burp Suite 安装目录自带插件中提取；本仓库不含该 API，避免分发第三方接口）
# 用法：JAVA_HOME=/path/to/jdk21 ./build.sh
set -e
cd "$(dirname "$0")"

JAVA_HOME="${JAVA_HOME:?请设置 JAVA_HOME 指向 JDK 17+}"
JAR_BIN="$JAVA_HOME/bin/jar"
JAVAC="$JAVA_HOME/bin/javac"

# burp-extender-api jar（自行下载后放入 libs/，文件名如下）
API_JAR="${BURP_API_JAR:-libs/burp-extender-api-2.3.jar}"
[ -f "$API_JAR" ] || { echo "缺少 Burp Extender API: $API_JAR"; echo "从 https://mvnrepository.com/artifact/net.portswigger/burp-extender-api 下载"; exit 1; }

rm -rf build/classes build/patched build/yaml
mkdir -p build/classes build/patched build/yaml

# 1. 编译（字节码锁定 Java 17，兼容近几年 Burp 自带 JRE）
"$JAVAC" --release 17 -encoding UTF-8 \
    -cp "$API_JAR;libs/snakeyaml-2.2.jar" -d build/classes \
    src/burp/BurpExtender.java src/plus/rule/RuleDefinition.java \
    src/plus/rule/RuleStore.java src/plus/rule/RuleEngine.java \
    src/plus/ui/ColorScheme.java

# 2. snakeyaml 解包（shade 进最终 jar）
cd build/yaml && "$JAR_BIN" xf ../../libs/snakeyaml-2.2.jar && cd ../..

# 3. 组装：我们的类 + snakeyaml + 内置默认规则 + LICENSE/NOTICE
cp -r build/classes/* build/patched/
cp -r build/yaml/org build/patched/
mkdir -p build/patched/rules build/patched/META-INF
cp resources/rules/Rules.yml build/patched/rules/
cp resources/META-INF/LICENSE resources/META-INF/NOTICE build/patched/META-INF/

"$JAR_BIN" cf xia_yue_plus-1.3.jar -C build/patched .
echo "BUILD OK -> xia_yue_plus-1.3.jar"
