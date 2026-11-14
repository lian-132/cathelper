#!/bin/sh
# ═══════════════════════════════════════════════════════════
#  猫娘助手 · 构建脚本 v2
#  用法：
#    sh 构建APK.sh            完整构建（资源 + 编译 + 打包 + 签名）
#    sh 构建APK.sh --java     只编译 Java（跳过资源，快）
#    sh 构建APK.sh --install  构建后自动装机
#    sh 构建APK.sh --clean    清理后构建
#  配置：toolchain.env（路径/版本/依赖都在那）
# ═══════════════════════════════════════════════════════════
set -e

PROJ=$(cd "$(dirname "$0")" && pwd)
cd "$PROJ"

# ── 载入配置 ──
if [ ! -f toolchain.env ]; then
  echo "✗ 缺少 toolchain.env"; exit 1
fi
. ./toolchain.env

# ── 参数解析 ──
SKIP_RES=0
DO_INSTALL=0
DO_CLEAN=0
for arg in "$@"; do
  case "$arg" in
    --java)    SKIP_RES=1 ;;
    --install) DO_INSTALL=1 ;;
    --clean)   DO_CLEAN=1 ;;
    *) echo "未知参数: $arg"; exit 1 ;;
  esac
done

# ── 环境 ──
export JAVA_HOME="$JDK_HOME"
export PATH="$JDK_HOME/bin:$PATH"

AJ="$TOOLS/$JAR_ANDROID"
D8="$TOOLS/$JAR_D8"
AS="$TOOLS/$JAR_APKSIGNER"
XP="$TOOLS/$JAR_XPOSED"
HT="$TOOLS/$HOST_TOOLS"

# ── 依赖解析：把运行时要打进 APK 的 jar 收集起来 ──
resolve_dep() {
  # $1 = jar 名字；在 TOOLS/libs、QFUN_LIBS 里找
  for base in "$TOOLS/libs" "$QFUN_LIBS"; do
    if [ -f "$base/$1" ]; then echo "$base/$1"; return 0; fi
  done
  echo ""; return 1
}

# ── 阶段输出 ──
step() { printf '\n\033[36m▸ %s\033[0m\n' "$1"; }
ok()   { printf '\033[32m  ✓ %s\033[0m\n' "$1"; }
warn() { printf '\033[33m  ! %s\033[0m\n' "$1"; }
die()  { printf '\033[31m  ✗ %s\033[0m\n' "$1"; exit 1; }

# ── 前置检查 ──
step "环境检查"
[ -f "$AJ" ] || die "缺 android.jar: $AJ"
[ -f "$D8" ] || die "缺 d8.jar: $D8"
[ -f "$AS" ] || die "缺 apksigner.jar: $AS"
[ -f "$XP" ] || die "缺 xposed-api: $XP"
command -v java >/dev/null 2>&1 || die "找不到 java"
ok "JDK $(java -version 2>&1 | head -1 | cut -d'"' -f2)"

# 收集依赖 jar
DEP_JARS=""
for d in $DEP_PROTOBUF $DEP_BSH $DEP_RHINO $DEP_LUAJ $DEP_DX $DEP_JANINO $DEP_COMMONS; do
  p=$(resolve_dep "$d") || die "缺依赖 jar: $d（找过 $TOOLS/libs 和 $QFUN_LIBS）"
  DEP_JARS="$DEP_JARS $p"
  ok "依赖: $d"
done

# ── 清理 ──
if [ "$DO_CLEAN" = "1" ]; then
  step "清理"; rm -rf build; ok "build/ 已删除"
fi

mkdir -p build/gen build/classes build/dex "$TOOLS/libs"

# ── 宿主侧路径映射（Shizuku 执行 aapt2 需要宿主路径）──
step "路径映射"
HT_HOST=$(传输 映射 "$TOOLS/host_tools" 2>/dev/null | rg -o '/data/[^ ]*' | head -1)
P_HOST=$(传输 映射 "$PROJ" 2>/dev/null | rg -o '/data/[^ ]*' | head -1)
[ -n "$HT_HOST" ] || die "工具链路径映射失败"
[ -n "$P_HOST" ] || die "工程路径映射失败"
ok "工具链: $HT_HOST"
ok "工程:   $P_HOST"

# ── 清单版本号同步（★ 不论是否跳过资源都要做，否则 --java 构建的 APK 版本号不更新）──
if grep -q 'android:versionCode=' app/AndroidManifest.xml 2>/dev/null; then
  sed -i "s/android:versionCode=\"[0-9]*\"/android:versionCode=\"$VERSION_CODE\"/" app/AndroidManifest.xml
  sed -i "s/android:versionName=\"[^\"]*\"/android:versionName=\"$VERSION_NAME\"/" app/AndroidManifest.xml
  ok "清单版本 → $VERSION_NAME ($VERSION_CODE)"
fi

# ── 资源编译 ──
if [ "$SKIP_RES" = "0" ]; then
  # ── 清单版本号同步（★ aapt2 的 --version-code 在本工具链被忽略，只能改清单）──

step "资源编译 (aapt2)"
  android-shizuku-cli exec "rm -rf $P_HOST/build/res; mkdir -p $P_HOST/build/res; chmod 777 $P_HOST/build/res" >/dev/null 2>&1
  android-shizuku-cli exec "$HT_HOST/bin/aapt2 compile --dir $P_HOST/app/res -o $P_HOST/build/res/res.zip; chmod 666 $P_HOST/build/res/res.zip" >/dev/null 2>&1 \
    || die "aapt2 compile 失败"
  ok "res.zip"

step "资源链接 (aapt2 link)"
  android-shizuku-cli exec "$HT_HOST/bin/aapt2 link -o $P_HOST/build/res/base.apk -I $HT_HOST/../$JAR_ANDROID --manifest $P_HOST/app/AndroidManifest.xml $P_HOST/build/res/res.zip --java $P_HOST/build/res/gen --min-sdk-version $MIN_SDK --target-sdk-version $TARGET_SDK --package-id $PKG_ID --auto-add-overlay; chmod -R 777 $P_HOST/build/res" 2>&1 \
    | rg -v "No package ID" >/dev/null || true
  [ -f build/res/base.apk ] || die "aapt2 link 失败（base.apk 未生成）"
  # 把 R.java 和 base.apk 移到约定位置
  cp -f build/res/base.apk build/base.apk
  if [ -d build/res/gen ]; then cp -rf build/res/gen/. build/gen/ 2>/dev/null || true; fi
  ok "base.apk + R.java"
else
  step "资源编译（跳过）"
  [ -f build/base.apk ] || die "base.apk 不存在，不能跳过资源编译（先跑一次完整构建）"
  ok "复用已有 base.apk"
fi

# ── 生成 BuildInfo 常量（★ 100% 可靠，不依赖运行时包查询）──
step "生成 BuildInfo"
mkdir -p build/gen/com/cathelperawa/owo/app
BUILD_TIME=$(date '+%Y-%m-%d %H:%M:%S')
# 构建UUID：时间戳 + 版本号 + 随机数的短哈希，用来区分「同版本号的两次构建」
BUILD_UUID=$(printf '%s|%s|%s' "$BUILD_TIME" "$VERSION_CODE" "$$$(date '+%N')" | md5sum | cut -c1-12)
cat > build/gen/com/cathelperawa/owo/app/BuildInfo.java <<BIEOF
package com.cathelperawa.owo.app;

/** 构建期生成的常量 —— 由 toolchain.env 注入（勿手改，改配置后重新构建） */
public final class BuildInfo {
    public static final String VERSION_NAME = "$VERSION_NAME";
    public static final int VERSION_CODE = $VERSION_CODE;
    public static final String MODULE_PKG = "$MODULE_PKG";
    public static final String SCOPE_LABEL = "$SCOPE_LABEL";
    /** 本 exe 的编译时刻（排查「用户装的到底是哪一版」） */
    public static final String BUILD_TIME = "$BUILD_TIME";
    /** 构建唯一标识（同版本号重打包也能分辨） */
    public static final String BUILD_UUID = "$BUILD_UUID";
    private BuildInfo() {}
}
BIEOF
ok "v$VERSION_NAME · 作用域 $SCOPE_LABEL · 构建 $BUILD_UUID"

# ── 硬编码类名清单（生成 Java 常量类）──
#   ★ 不用 assets：模块跑在宿主进程里，ctx.getAssets() 拿到的是宿主的 assets，
#     读不到我们自己的文件。生成成类就跟着 dex 走，永远能读到。
#   格式：文件:行<TAB>类名 —— 按调用点分组，才能判断这处有没有候选兜底
step "导出硬编码类名"
GEN_D=build/gen/com/cathelperawa/owo/app/core/dex
mkdir -p "$GEN_D"
if command -v rg >/dev/null 2>&1; then
    TAB=$(printf '\t')
    # ① 常规：以大写类名结尾（未混淆 / 半混淆）
    rg -o -n --no-heading \
       '"[a-z][a-zA-Z0-9_]*(\.[a-z][a-zA-Z0-9_]*){1,}\.[A-Z][A-Za-z0-9_$]*"' \
       app/java -g '*.java' > /tmp/hc1.txt || true
    # ② 混淆：以 1~3 个小写字母结尾（如 com.tencent.aio.menu.d）—— ① 抓不到
    rg -o -n --no-heading \
       '"[a-z][a-zA-Z0-9_]*(\.[a-z][a-zA-Z0-9_]*){2,}\.[a-z]{1,3}"' \
       app/java -g '*.java' > /tmp/hc2.txt || true
    cat /tmp/hc1.txt /tmp/hc2.txt \
      | sed 's/:"/'"$TAB"'/' | tr -d '"' | sort -u \
      | grep -v "${TAB}com\.cathelperawa\." \
      | grep -v "${TAB}android\." \
      | grep -v "${TAB}androidx\." \
      | grep -v "${TAB}java\." \
      | grep -v -E "${TAB}(com\.tencent\.(tuwen|structmsg|miniapp|mannounce|qcircle|qqsports|gamecenter)|guild\.share|qzone\.share|tianxuan\.share|mqq)" > /tmp/hardcoded.txt || true

    {
      echo 'package com.cathelperawa.owo.app.core.dex;'
      echo '/** 构建期生成：模块里所有硬编码类名（文件:行<TAB>类名）。勿手改。 */'
      echo 'public final class HardcodedClasses {'
      echo '    public static final String[] LIST = {'
      sed 's/\\/\\\\/g; s/"/\\"/g; s/^/        "/; s/$/",/' /tmp/hardcoded.txt
      echo '    };'
      echo '    private HardcodedClasses() {}'
      echo '}'
    } > "$GEN_D/HardcodedClasses.java"
    ok "$(wc -l < /tmp/hardcoded.txt) 条类名引用"
else
    warn "没有 rg，跳过类名导出（体检会读不到名单）"
fi

# ── Java 编译 ──
step "Java 编译"
SRC=$(find app/java build/gen -name '*.java')
PKG_CP="$XP"
for p in $DEP_JARS; do PKG_CP="$PKG_CP:$p"; done
# ★ 清空 classes，避免旧 class 掩盖编译错误
rm -rf build/classes; mkdir -p build/classes
set +e
javac -source "$JAVA_SOURCE" -target "$JAVA_TARGET" -bootclasspath "$AJ" \
      -classpath "$PKG_CP" -d build/classes $SRC > /tmp/cathelper_javac.log 2>&1
JRC=$?
# ★ set -e 下 javac 非零会直接退出，跳过后面的日志输出与 die —— 所以整段先关掉
#   （重构计划 A4：修 set -e 静默陷阱）
rg -v "^Note:" /tmp/cathelper_javac.log || true
[ "$JRC" -eq 0 ] || die "javac 编译失败（见上）"
[ -n "$(find build/classes -name '*.class' 2>/dev/null)" ] || die "javac 没产出 class"
set -e
N=$(find build/classes -name '*.class' | wc -l)
ok "$N 个 class"

# ── d8：class + 依赖 → dex ──
step "D8 转换"
rm -rf build/dex; mkdir -p build/dex
CLS=$(find build/classes -name '*.class')
set +e
java -cp "$D8" com.android.tools.r8.D8 --min-api "$MIN_SDK" --lib "$AJ" \
     --output build/dex $CLS $DEP_JARS > /tmp/cathelper_d8.log 2>&1
DRC=$?
rg -v "^Warning|Info:" /tmp/cathelper_d8.log || true
[ "$DRC" -eq 0 ] && [ -f build/dex/classes.dex ] || die "d8 失败（见上）"
set -e
ok "classes.dex $(du -h build/dex/classes.dex | cut -f1)"

# ── 打包 ──
step "打包"
cp build/base.apk build/unsigned.apk
( cd build/dex && zip -q "$PROJ/build/unsigned.apk" *.dex )
( cd app && zip -q "$PROJ/build/unsigned.apk" $ASSETS_TO_PACK )
ok "unsigned.apk"

# ── 签名 ──
step "签名"
if [ ! -f "$KEYSTORE" ]; then
  warn "密钥不存在，生成新密钥（★ 覆盖安装会变签名）"
  keytool -genkeypair -keystore "$KEYSTORE" -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass "$KS_PASS" -keypass "$KEY_PASS" -dname "$KS_DNAME" >/dev/null 2>&1
fi
java -jar "$AS" sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KEY_PASS" \
     --out "build/$APP_NAME.apk" build/unsigned.apk
ok "$(du -h "build/$APP_NAME.apk" | cut -f1)  build/$APP_NAME.apk"

# ── 装机 ──
if [ "$DO_INSTALL" = "1" ]; then
  step "装机"
  APK_HOST="$PHONE_STORE/$APP_NAME.apk"
  传输 到手机 "$PROJ/build/$APP_NAME.apk" "$APK_HOST" >/dev/null 2>&1 || die "推送失败"
  ok "已推送 $APK_HOST"
  android-shizuku-cli exec "cp -a '$APK_HOST' $PHONE_TMP/cathelper_preview.apk && chmod 666 $PHONE_TMP/cathelper_preview.apk && pm install -r $PHONE_TMP/cathelper_preview.apk" 2>&1 | rg -q "Success" \
    && ok "安装成功" || die "安装失败"
fi

printf '\n\033[32m══ 构建完成 ══\033[0m\n'
