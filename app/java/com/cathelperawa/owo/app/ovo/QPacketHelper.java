package com.cathelperawa.owo.app.ovo;

import com.google.protobuf.CodedOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QPacketHelper {

    private QPacketHelper() {}

    public static byte[] buildMessage(String json) {
        try {
            JSONObject o = new JSONObject(json);
            Map<Integer, Object> map = parseJsonToMap(o, new ArrayList<String>());
            return encodeMessage(map);
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("buildMessage 失败", t);
            return null;
        }
    }

    public static Map<Integer, Object> parseJsonToMap(Object jsonElement, List<String> list) {
        Map<Integer, Object> linkedHashMap = new LinkedHashMap<>();
        if (jsonElement instanceof JSONObject) {
            JSONObject obj = (JSONObject) jsonElement;
            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String str = keys.next();
                Object value = obj.opt(str);
                int intValue;
                try {
                    intValue = Integer.parseInt(str);
                } catch (Throwable t) {
                    throw new IllegalStateException("Key is not a valid int: " + str);
                }
                List<String> plus = new ArrayList<>(list);
                plus.add(str);

                if (plus.equals(java.util.Arrays.asList("3", "1", "2"))) {
                    intValue = 2;
                }
                Object result;
                if (value instanceof JSONObject) {
                    result = parseJsonToMap(value, plus);
                } else if (value instanceof JSONArray) {
                    JSONArray arr = (JSONArray) value;
                    List<Object> arrayList = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        Object item = arr.opt(i);
                        if (item instanceof JSONObject || item instanceof JSONArray) {
                            List<String> p2 = new ArrayList<>(plus);
                            p2.add(String.valueOf(i + 1));
                            arrayList.add(parseJsonToMap(item, p2));
                        } else {
                            arrayList.add(primitiveOf(item));
                        }
                    }
                    result = arrayList;
                } else {
                    result = primitiveOrHex(plus, value);
                }
                linkedHashMap.put(intValue, result);
            }
            return linkedHashMap;
        }
        if (jsonElement instanceof JSONArray) {
            JSONArray arr = (JSONArray) jsonElement;
            for (int i = 0; i < arr.length(); i++) {
                Object item = arr.opt(i);
                List<String> p2 = new ArrayList<>(list);
                p2.add(String.valueOf(i + 1));
                linkedHashMap.put(i + 1, (item instanceof JSONObject || item instanceof JSONArray)
                        ? parseJsonToMap(item, p2) : primitiveOf(item));
            }
            return linkedHashMap;
        }

        linkedHashMap.put(1, primitiveOf(jsonElement));
        return linkedHashMap;
    }

    private static Object primitiveOf(Object v) {
        if (v == null) return "";
        if (v instanceof String) return v;
        if (v instanceof Number) {

            if (v instanceof Integer) return v;
            if (v instanceof Long) return v;
            double d = ((Number) v).doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                if (d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) return (int) d;
                return (long) d;
            }
            return String.valueOf(v);
        }
        if (v instanceof Boolean) return v.toString();
        return String.valueOf(v);
    }

    private static Object primitiveOrHex(List<String> list, Object jsonPrimitive) {
        if (!(jsonPrimitive instanceof String)) {
            return primitiveOf(jsonPrimitive);
        }
        String content = (String) jsonPrimitive;
        if (content.startsWith("hex->") && isHexString(content.substring(5))) {
            return hexStringToByteArray(content.substring(5));
        }
        int n = list.size();
        if (n >= 2 && "5".equals(list.get(n - 2)) && "2".equals(list.get(n - 1))
                && isHexString(content)) {
            return hexStringToByteArray(content);
        }
        return content;
    }

    private static boolean isHexString(String str) {
        return str.length() % 2 == 0 && str.matches("^[0-9a-fA-F]+$");
    }

    private static byte[] hexStringToByteArray(String str) {
        byte[] bArr = new byte[str.length() / 2];
        for (int i = 0; i < bArr.length; i++) {
            int d1 = Character.digit(str.charAt(i * 2), 16);
            int d2 = Character.digit(str.charAt(i * 2 + 1), 16);
            if (d1 < 0 || d2 < 0) throw new IllegalArgumentException("Invalid hex at " + (i * 2));
            bArr[i] = (byte) ((d1 << 4) + d2);
        }
        return bArr;
    }

    public static byte[] encodeMessage(Map<Integer, Object> map) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            CodedOutputStream out = CodedOutputStream.newInstance(bos);
            encodeMapToProtobuf(out, map);
            out.flush();
            return bos.toByteArray();
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("encodeMessage 失败", t);
            return new byte[0];
        }
    }

    @SuppressWarnings("unchecked")
    private static void encodeMapToProtobuf(CodedOutputStream out, Map<Integer, Object> map)
            throws Exception {
        for (Map.Entry<Integer, Object> e : map.entrySet()) {
            int field = e.getKey();
            Object value = e.getValue();
            if (value instanceof Integer) {
                out.writeInt32(field, (Integer) value);
            } else if (value instanceof Long) {
                out.writeInt64(field, (Long) value);
            } else if (value instanceof String) {
                out.writeString(field, (String) value);
            } else if (value instanceof byte[]) {
                out.writeTag(field, 2);
                byte[] b = (byte[]) value;
                out.writeUInt32NoTag(b.length);
                out.writeRawBytes(b);
            } else if (value instanceof Map) {
                byte[] sub = encodeMessage((Map<Integer, Object>) value);
                out.writeTag(field, 2);
                out.writeUInt32NoTag(sub.length);
                out.writeRawBytes(sub);
            } else if (value instanceof List) {
                for (Object next : (List<Object>) value) {
                    if (next instanceof Map) {
                        byte[] sub = encodeMessage((Map<Integer, Object>) next);
                        out.writeTag(field, 2);
                        out.writeUInt32NoTag(sub.length);
                        out.writeRawBytes(sub);
                    } else if (next instanceof Integer) {
                        out.writeInt32(field, (Integer) next);
                    } else if (next instanceof Long) {
                        out.writeInt64(field, (Long) next);
                    } else if (next instanceof String) {
                        out.writeString(field, (String) next);
                    } else if (next instanceof byte[]) {
                        out.writeTag(field, 2);
                        byte[] b2 = (byte[]) next;
                        out.writeUInt32NoTag(b2.length);
                        out.writeRawBytes(b2);
                    } else {
                        throw new IllegalStateException("Unsupported list item: " + next.getClass());
                    }
                }
            } else {
                throw new IllegalStateException("Unsupported type: " + value.getClass());
            }
        }
    }

    public static boolean sendMessage(String elementsJson, String peer, boolean isGroup) {
        try {

            Object parsed = new org.json.JSONTokener(elementsJson).nextValue();
            JSONArray build = new JSONArray();
            if (parsed instanceof JSONArray) {
                JSONArray a = (JSONArray) parsed;
                for (int i = 0; i < a.length(); i++) {
                    Object o = a.opt(i);
                    if (o instanceof JSONObject) build.put(o);
                }
            } else if (parsed instanceof JSONObject) {
                build.put(parsed);
            } else {
                com.cathelperawa.owo.app.core.HookLog.w("elements 不是 JSON");
                return false;
            }

            JSONObject base = new JSONObject();
            JSONObject f1 = new JSONObject();
            if (isGroup) {
                f1.put("2", Long.parseLong(peer));
            } else {
                f1.put("1", peer);
            }
            base.put("1", f1);
            JSONObject f2 = new JSONObject();
            f2.put("1", 1);
            f2.put("2", 0);
            f2.put("3", 0);
            base.put("2", f2);
            JSONObject f3inner = new JSONObject();
            f3inner.put("2", build);
            JSONObject f3 = new JSONObject();
            f3.put("1", f3inner);
            base.put("3", f3);

            long r1 = nextUInt();
            long r2 = nextUInt();
            base.put("4", r1);
            base.put("5", r2);

            Map<Integer, Object> map = parseJsonToMap(base, new ArrayList<String>());
            byte[] payload = encodeMessage(map);
            if (payload == null || payload.length == 0) return false;
            QQInterfaces.sendBuffer("MessageSvc.PbSendMsg", true, payload);
            return true;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("sendMessage 失败", t);
            return false;
        }
    }

    private static long nextUInt() {
        long v = (long) (Math.random() * 4294967296L);
        return v & 0xFFFFFFFFL;
    }
}
