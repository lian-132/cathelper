package com.cathelperawa.owo.app.util;

public final class Hex {

    private static final char[] DIGITS = "0123456789ABCDEF".toCharArray();

    private Hex() {}

    public static String encode(byte[] b) {
        if (b == null) return "";
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) {
            int v = x & 0xFF;
            sb.append(DIGITS[v >> 4]).append(DIGITS[v & 0xF]);
        }
        return sb.toString();
    }

    public static byte[] decode(String s) {
        if (s == null) return null;
        String t = s.replace(" ", "").replace("\n", "").trim();
        if (t.length() % 2 != 0) return null;
        byte[] out = new byte[t.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(t.charAt(i * 2), 16);
            int lo = Character.digit(t.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) return null;
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }

    public static boolean isHex(String s) {
        if (s == null || s.length() % 2 != 0 || s.length() == 0) return false;
        for (int i = 0; i < s.length(); i++) {
            if (Character.digit(s.charAt(i), 16) < 0) return false;
        }
        return true;
    }
}
