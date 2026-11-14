package com.cathelperawa.owo.app;

import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

public final class CatProcessor {

    private static final String SPECIAL_SYMBOLS = "嗷呜？！?!。（），,()～~、；;：:…· .\n\r\t";
    private static final String PARTICLE_SYMBOLS = "啊呀哦啦嘛吧呢哇呐捏哟喔啵喽咯";
    private static final Random R = new Random();

    private CatProcessor() {}

    public static String process(String msg, AppConfig cfg) {
        if (msg == null) return "";
        if (!cfg.total) return msg;

        if (com.cathelperawa.owo.app.script.StrategyRegistry.has("catify.transform")) {
            String custom = com.cathelperawa.owo.app.script.StrategyRegistry
                    .pick("catify.transform", msg, null);
            if (custom != null) return custom;
        }

        if (cfg.keywordReply) {
            String hit = hitCustomReply(msg, cfg.parseCustomReplies());
            if (hit != null) return hit;
        }

        if (cfg.trainSwitch && !passTrain(msg, cfg)) return "喵喵喵";

        if (cfg.catNameSwitch) {
            msg = msg.replace("我", cfg.catName);
            msg = msg.replace(cfg.catName + "的世界", "我的世界");
        }
        if (cfg.masterNameSwitch) {
            msg = msg.replace("你", cfg.masterName);
            msg = msg.replace("迷" + cfg.masterName, "迷你");
        }

        boolean wholeHit = false;
        if (cfg.spoiled) {
            String whole = spoiledWhole(msg);
            if (whole != null) {
                msg = whole;
                wholeHit = true;
            } else {
                msg = spoiledProcess(msg, cfg.autoSuffix);
            }
        }

        if (cfg.baseCat && !wholeHit) {
            msg = msg.replace("什么", "什喵")
                    .replace("怎么", "怎么了")
                    .replace("在吗", "在嘛")
                    .replace("不要啊", "不要呀")
                    .replace("他", "她")
                    .replace("你", "您")
                    .replace("是", "素")
                    .replace("了", "惹");
            msg = msg.replaceAll("啦(?!喵)", "啦喵")
                    .replaceAll("哦(?!喵)", "哦喵")
                    .replaceAll("嗯(?!喵)", "嗯喵");
        }

        if (!wholeHit) msg = modifyEnd(msg, cfg.endMeow);

        if (cfg.randomCute) msg = randomCute(msg);
        if (cfg.emoticon) msg = msg + pick(CatLexicon.EMOTICONS);
        if (cfg.wave) {
            msg = msg + "~";
            msg = msg.replace("~~", "~");
        }
        if (cfg.paw && !msg.contains("[atUin=")) {
            String paw = "\u0014\u01FF\u000F\u0043\u0000";
            msg = msg.replace("ǿC", "");
            msg = paw + msg + paw;
        }

        return msg;
    }

    private static String hitCustomReply(String msg, Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        String text = normalize(msg).toLowerCase();
        if (text.isEmpty()) return null;
        for (Map.Entry<String, String> e : map.entrySet()) {
            String k = e.getKey();
            if (text.equals(k) || text.contains(k)) return e.getValue();
        }
        return null;
    }

    private static boolean passTrain(String msg, AppConfig cfg) {
        String text = stripTailSymbols(msg.trim());
        boolean hasPrefix = cfg.trainPrefix != null && !cfg.trainPrefix.trim().isEmpty();
        boolean hasSuffix = cfg.trainSuffix != null && !cfg.trainSuffix.trim().isEmpty();
        boolean hasKeyword = cfg.trainKeywords != null && !cfg.trainKeywords.trim().isEmpty();
        if (!hasPrefix && !hasSuffix && !hasKeyword) return true;

        boolean match = true;
        if (hasPrefix && !text.startsWith(cfg.trainPrefix.trim())) match = false;
        if (hasSuffix && !text.endsWith(cfg.trainSuffix.trim())) match = false;
        if (hasKeyword) {
            String[] words = text.split("[\\s,，。！？!?;；:：、|｜]+");
            String[] keys = cfg.trainKeywords.trim().split("[\\s|｜、，,]+");
            boolean found = false;
            for (String w : words) {
                for (String k : keys) {
                    if (k.isEmpty()) continue;
                    if (w.equals(k) || w.equalsIgnoreCase(k) || text.contains(k)) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            if (!found) match = false;
        }
        return match;
    }

    private static String spoiledWhole(String original) {
        if (original == null || original.trim().isEmpty()) return null;
        String trim = original.trim();
        if (trim.length() <= 25) {
            String key = normalize(trim).toLowerCase();
            String[] arr = CatLexicon.SHORT.get(key);
            if (arr != null && arr.length > 0) return pick(arr);
        }
        for (CatLexicon.RegexRule r : CatLexicon.REGEX) {
            try {
                if (Pattern.matches(r.pattern, trim)) return pick(r.replies);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String spoiledProcess(String original, boolean forceSuffix) {
        if (original == null || original.trim().isEmpty()) return original;
        String msg = longReplace(original);
        if (msg.equals(original)) {
            msg = msg.replace("什么", "什喵")
                     .replace("怎么", "怎么了")
                     .replace("在吗", "在嘛")
                     .replace("不要啊", "不要呀")
                     .replace("他", "她")
                     .replace("你", "您");
        }
        if (msg.equals(original.trim()) || forceSuffix) {
            if (msg.length() <= 12) msg = autoSuffix(msg);
        }
        return msg;
    }

    private static String longReplace(String msg) {
        String out = msg;
        for (String[] kv : CatLexicon.LONG_REPLACE) {
            out = out.replaceAll(kv[0], kv[1]);
        }
        return out;
    }

    private static String autoSuffix(String msg) {
        if (msg.matches(".*[喵呀嘛哦啦呜～~！!？?]$")) return msg;
        double r = Math.random();
        if (r < 0.15) return msg + "喵～";
        if (r < 0.30) return msg + "呀喵～";
        if (r < 0.45) return msg + "嘛喵～";
        if (r < 0.55) return msg + "的说喵～";
        if (r < 0.65) return msg + "呜喵～";
        if (r < 0.75) return msg + "喵喵～";
        if (r < 0.85) return msg + "哒喵～";
        return msg + "喵！";
    }

    private static String randomCute(String msg) {
        if (msg.contains("[atUin=")) return msg;
        double r = Math.random();
        if (r < 0.3) return msg + "（蹭蹭）";
        if (r < 0.55) return msg + "，尾巴摇摇～";
        if (r < 0.75) return msg + "，耳朵抖抖～";
        if (r < 0.9) return msg + "（歪头）";
        return msg;
    }

    private static String modifyEnd(String msg, boolean endMeow) {
        if (!endMeow || msg.isEmpty()) return msg;
        boolean endWithSpecial = SPECIAL_SYMBOLS.contains(String.valueOf(msg.charAt(msg.length() - 1)));
        if (!endWithSpecial && !msg.endsWith("喵")) msg = msg + "喵";
        return msg;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        s = s.trim();
        s = stripTailSymbols(s);
        while (s.length() > 1 && PARTICLE_SYMBOLS.indexOf(s.charAt(s.length() - 1)) != -1) {
            s = s.substring(0, s.length() - 1);
        }
        return s.trim();
    }

    private static String stripTailSymbols(String s) {
        while (s.length() > 0 && SPECIAL_SYMBOLS.indexOf(s.charAt(s.length() - 1)) != -1) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String pick(String[] arr) {
        if (arr == null || arr.length == 0) return "";
        return arr[R.nextInt(arr.length)];
    }
}
