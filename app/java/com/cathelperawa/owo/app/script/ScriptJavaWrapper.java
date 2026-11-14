package com.cathelperawa.owo.app.script;

import java.util.ArrayList;
import java.util.List;

public final class ScriptJavaWrapper {

    private static final String FIELDS =
            "    public static com.cathelperawa.owo.app.script.ScriptApi ctx = (com.cathelperawa.owo.app.script.ScriptApi) ApiBridge.current();\n"
            + "    public static Object context;\n"
            + "    public static Object classLoader;\n"
            + "    public static String pluginPath = \"\";\n"
            + "    public static String scriptDir = \"\";\n"
            + "    public static String pluginID = \"\";\n"
            + "    public static String myUin = \"\";\n";

    private static final String API =
            "    public static int getInt(String f, String k, int d) { return ApiBridge.getInt(ctx, f, k, d); }\n"
            + "    public static boolean getBoolean(String f, String k, boolean d) { return ApiBridge.getBoolean(ctx, f, k, d); }\n"
            + "    public static String getString(String f, String k, String d) { return ApiBridge.getString(ctx, f, k, d); }\n"
            + "    public static long getLong(String f, String k, long d) { return ApiBridge.getLong(ctx, f, k, d); }\n"
            + "    public static void putInt(String f, String k, int v) { ApiBridge.putInt(ctx, f, k, v); }\n"
            + "    public static void putBoolean(String f, String k, boolean v) { ApiBridge.putBoolean(ctx, f, k, v); }\n"
            + "    public static void putString(String f, String k, String v) { ApiBridge.putString(ctx, f, k, v); }\n"
            + "    public static void putLong(String f, String k, long v) { ApiBridge.putLong(ctx, f, k, v); }\n"
            + "    public static boolean sendMsg(String peer, String text, int type) { return ApiBridge.sendMsg(ctx, peer, text, type); }\n"
            + "    public static boolean sendText(String peer, String text, int type) { return ApiBridge.sendMsg(ctx, peer, text, type); }\n"
            + "    public static boolean sendPic(String peer, String path, int type) { return ApiBridge.sendPic(ctx, peer, path, type); }\n"
            + "    public static boolean sendPtt(String peer, String path, int type) { return ApiBridge.sendPtt(ctx, peer, path, type); }\n"
            + "    public static boolean sendFile(String peer, String path, int type) { return ApiBridge.sendFile(ctx, peer, path, type); }\n"
            + "    public static boolean sendReplyMsg(String peer, long id, String text, int type) { return ApiBridge.sendReply(ctx, peer, id, text, type); }\n"
            + "    public static void toast(String s) { ApiBridge.toast(ctx, s); }\n"
            + "    public static void log(String s) { ApiBridge.log(ctx, s); }\n"
            + "    public static void logD(String s) { ApiBridge.log(ctx, s); }\n"
            + "    public static void logInfo(String s) { ApiBridge.log(ctx, s); }\n"
            + "    public static android.app.Activity getNowActivity() { return ApiBridge.activity(ctx); }\n"
            + "    public static android.content.Context getNowContext() { return ApiBridge.context(ctx); }\n"
            + "    public static void shutUp(String g, String u, long s) { ApiBridge.shutUp(ctx, g, u, s); }\n"
            + "    public static void shutUpAll(String g, long s) { ApiBridge.shutUp(ctx, g, null, s); }\n"
            + "    public static void kickGroup(String g, String u) { ApiBridge.kick(ctx, g, u); }\n"
            + "    public static void recallMsg(String p, long id, int t) { ApiBridge.recall(ctx, p, id, t); }\n"
            + "    public static java.util.List getGroupMemberList(String g) { return ApiBridge.groupMembers(ctx, g); }\n"
            + "    public static String getMemberInfo(String g, String u) { return ApiBridge.memberInfo(ctx, g, u); }\n"
            + "    public static String getSkey() { return ApiBridge.skey(ctx); }\n"
            + "    public static String getPskey(String u) { return ApiBridge.pskey(ctx, u); }\n"
            + "    public static int getInt(String k, int d) { return ApiBridge.getInt(ctx, null, k, d); }\n"
            + "    public static boolean getBoolean(String k, boolean d) { return ApiBridge.getBoolean(ctx, null, k, d); }\n"
            + "    public static String getString(String k, String d) { return ApiBridge.getString(ctx, null, k, d); }\n"
            + "    public static long getLong(String k, long d) { return ApiBridge.getLong(ctx, null, k, d); }\n"
            + "    public static void putInt(String k, int v) { ApiBridge.putInt(ctx, null, k, v); }\n"
            + "    public static void putBoolean(String k, boolean v) { ApiBridge.putBoolean(ctx, null, k, v); }\n"
            + "    public static void putString(String k, String v) { ApiBridge.putString(ctx, null, k, v); }\n"
            + "    public static void putLong(String k, long v) { ApiBridge.putLong(ctx, null, k, v); }\n"
            ;

    private ScriptJavaWrapper() {}

    public static final class Wrapped {
        public final String source;
        public final String className;
        public final List<String> skippedImports;

        Wrapped(String source, String className, List<String> skipped) {
            this.source = source;
            this.className = className;
            this.skippedImports = skipped;
        }
    }

    public static Wrapped wrap(String code, String[] pkgs) {
        String[] raw = code.replace("\r\n", "\n").split("\n");
        List<String> imports = new ArrayList<String>();
        List<String> members = new ArrayList<String>();
        List<String> statements = new ArrayList<String>();
        List<String> skipped = new ArrayList<String>();

        int depth = 0;
        boolean inClass = false;
        boolean inMethod = false;
        StringBuilder cur = new StringBuilder();
        boolean curIsStatement = false;

        for (int i = 0; i < raw.length; i++) {
            String line = raw[i];
            String t = line.trim();

            if (depth == 0 && t.startsWith("import ")) {
                if (isAllowed(t, pkgs)) imports.add(line);
                else skipped.add(t);
                continue;
            }
            if (depth == 0 && (t.startsWith("package ") || t.length() == 0)) {
                if (t.length() == 0) continue;
                continue;
            }

            if (depth == 0 && !inMethod && !inClass) {
                cur.setLength(0);
                cur.append(line).append('\n');
                if (isTypeDecl(t)) {
                    inClass = true; curIsStatement = false;
                } else if (isMethodDecl(t)) {
                    inMethod = true; curIsStatement = false;
                } else if (isFieldDecl(t)) {

                    members.add("    " + withStatic(t));
                    cur.setLength(0);
                    depth = 0;
                    inClass = inMethod = curIsStatement = false;
                    continue;
                } else {
                    curIsStatement = true;
                }
            } else {
                cur.append(line).append('\n');
            }

            depth += countBraces(line);

            if (depth == 0 && (inClass || inMethod || curIsStatement)) {
                String block = cur.toString();
                if (inClass) {
                    members.add("    static " + block.trim().replaceAll("(?m)^(\\s*)", "    "));
                } else if (inMethod) {
                    members.add(indent(withStaticMethod(block), "    "));
                } else {
                    statements.add(indent(block, "        "));
                }
                inClass = inMethod = curIsStatement = false;
                cur.setLength(0);
            }
        }

        if (cur.length() > 0) {
            if (curIsStatement) statements.add(indent(cur.toString(), "        "));
            else members.add(indent(cur.toString(), "    "));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package scripts;\n");
        sb.append("import com.cathelperawa.owo.app.script.ApiBridge;\n");
        for (String s : imports) sb.append(s).append('\n');
        sb.append("\npublic class Master {\n");
        sb.append(FIELDS);
        sb.append(API);
        for (String m : members) sb.append(m).append('\n');
        if (!statements.isEmpty()) {
            sb.append("    static {\n");
            for (String s : statements) sb.append(s);
            sb.append("    }\n");
        }
        sb.append("}\n");
        return new Wrapped(sb.toString(), "scripts.Master", skipped);
    }

    private static String withStatic(String decl) {
        String t = decl.trim();
        if (t.startsWith("static ") || t.contains(" static ")) return t;
        return "static " + t;
    }

    private static String withStaticMethod(String block) {
        String t = block.trim();
        String[] mods = {"public ", "private ", "protected ", "final ", "synchronized ", "abstract ", "native "};
        if (t.startsWith("static ")) return block;
        for (String m : mods) {
            if (t.startsWith(m)) {
                return "static " + t;
            }
        }

        for (String m : mods) {
            if (t.contains(" " + m)) {
                int idx = t.indexOf(" " + m);
                return t.substring(0, idx + 1) + "static " + t.substring(idx + 1);
            }
        }
        return "static " + t;
    }

    private static boolean isAllowed(String importLine, String[] pkgs) {
        if (pkgs == null || pkgs.length == 0) return true;
        String n = importLine.substring(6).trim();
        if (n.endsWith(";")) n = n.substring(0, n.length() - 1);
        if (n.startsWith("static ")) n = n.substring(7).trim();
        for (String p : pkgs) {
            String base = p.endsWith(".*") ? p.substring(0, p.length() - 2) : p;
            if (n.equals(base) || n.startsWith(base + ".")) return true;
        }
        return false;
    }

    private static boolean isTypeDecl(String t) {
        return t.startsWith("class ") || t.startsWith("public class ") || t.startsWith("final class ")
                || t.startsWith("abstract class ") || t.startsWith("interface ")
                || t.startsWith("public interface ") || t.startsWith("enum ") || t.startsWith("public enum ");
    }

    private static boolean isMethodDecl(String t) {
        if (!t.endsWith("{") && !t.endsWith(";")) return false;
        if (!t.contains("(")) return false;
        String[] control = {"if ", "for ", "while ", "switch ", "catch ", "do ", "else", "try", "return ", "new ",
                "throw ", "synchronized ", "assert ", "log(", "toast(", "ctx.", "System.", "sendMsg(", "putInt(",
                "putString(", "putBoolean(", "putLong(", "sendText(", "sendPic(", "sendPtt("};
        for (String c : control) if (t.startsWith(c)) return false;

        return t.matches("^[A-Za-z_$][\\w$.<>\\[\\],\\s]*\\s+[A-Za-z_$][\\w$]*\\s*\\(.*");
    }

    private static boolean isFieldDecl(String t) {
        if (!t.endsWith(";")) return false;
        if (t.startsWith("return ") || t.startsWith("throw ") || t.startsWith("if ")
                || t.startsWith("for ") || t.startsWith("while ")) return false;

        int eq = t.indexOf('=');
        String lhs = eq < 0 ? t : t.substring(0, eq);
        if (lhs.contains("(")) return false;
        return t.matches("^(?:(?:public|private|protected|static|final|volatile|transient)\\s+)*"
                + "[A-Za-z_$][\\w$.<>\\[\\],\\s]*?\\s+[A-Za-z_$][\\w$]*\\s*(?:=[^;]*)?;$");
    }

    private static int countBraces(String line) {
        int d = 0;
        boolean str = false, chr = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (str) {
                if (c == '\\') { i++; continue; }
                if (c == '"') str = false;
                continue;
            }
            if (chr) {
                if (c == '\\') { i++; continue; }
                if (c == '\'') chr = false;
                continue;
            }
            if (c == '"') { str = true; continue; }
            if (c == '\'') { chr = true; continue; }
            if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') break;
            if (c == '{') d++;
            else if (c == '}') d--;
        }
        return d;
    }

    private static String indent(String block, String pad) {
        StringBuilder sb = new StringBuilder();
        for (String l : block.split("\n")) {
            if (l.trim().length() == 0) continue;
            sb.append(pad).append(l.trim()).append('\n');
        }
        return sb.toString();
    }
}
