package com.cathelperawa.owo.app.ovo;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class FunProtoData {
    static final   boolean $assertionsDisabled = false;
    private final HashMap<Integer, List<Object>> values = new HashMap<>();

    public static byte[] getUnpPackage(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        return (bArr.length >= 4 && (bArr[0] & 255) == 0) ? Arrays.copyOfRange(bArr, 4, bArr.length) : bArr;
    }

    public void fromJSON(JSONObject jSONObject) {
        try {
            Iterator<String> keys = jSONObject.keys();
            while (keys.hasNext()) {
                String next = keys.next();
                int parseInt = Integer.parseInt(next);
                Object obj = jSONObject.get(next);
                if (obj instanceof JSONObject) {
                    FunProtoData funProtoData = new FunProtoData();
                    funProtoData.fromJSON((JSONObject) obj);
                    putValue(parseInt, funProtoData);
                } else if (obj instanceof JSONArray) {
                    JSONArray jSONArray = (JSONArray) obj;
                    for (int i = 0; i < jSONArray.length(); i++) {
                        Object obj2 = jSONArray.get(i);
                        if (obj2 instanceof JSONObject) {
                            FunProtoData funProtoData2 = new FunProtoData();
                            funProtoData2.fromJSON((JSONObject) obj2);
                            putValue(parseInt, funProtoData2);
                        } else {
                            putValue(parseInt, obj2);
                        }
                    }
                } else if (obj instanceof String
                        && ((String) obj).startsWith("hex->")) {
                    putValue(parseInt, hexToBytes(((String) obj).substring(5)));
                } else {
                    putValue(parseInt, obj);
                }
            }
        } catch (Exception unused) {
        }
    }

    public static byte[] hexToBytes(String h) {
        try {
            int n = h.length() / 2;
            byte[] out = new byte[n];
            for (int i = 0; i < n; i++) {
                out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
            }
            return out;
        } catch (Throwable t) {
            return new byte[0];
        }
    }

    private static List<Object> lambda$putValue$0(Integer num) {
        return new java.util.ArrayList<>();
    }

    private void putValue(int i, Object obj) {
        List<Object> list = this.values.get(Integer.valueOf(i));
        if (list == null) {
            list = new java.util.ArrayList<>();
            this.values.put(Integer.valueOf(i), list);
        }
        list.add(obj);
    }

    public void fromBytes(byte[] bArr) {
        CodedInputStream newInstance = CodedInputStream.newInstance(bArr);
        try {
            while (newInstance.getBytesUntilLimit() > 0) {
                int readTag = newInstance.readTag();
                int i = readTag >>> 3;
                int i2 = readTag & 7;
                if (i2 == 4 || i2 == 3 || i2 > 5) {
                    throw new java.io.IOException("Unexpected wireType: " + i2);
                }
                if (i2 == 0) {
                    putValue(i, Long.valueOf(newInstance.readInt64()));
                } else if (i2 == 1) {
                    putValue(i, Long.valueOf(newInstance.readRawVarint64()));
                } else if (i2 == 2) {
                    byte[] readByteArray = newInstance.readByteArray();
                    try {
                        try {
                            FunProtoData funProtoData = new FunProtoData();
                            funProtoData.fromBytes(readByteArray);
                            putValue(i, funProtoData);
                        } catch (Exception unused) {
                            Charset charset = StandardCharsets.UTF_8;
                            String str = new String(readByteArray, charset);
                            if (arraysEqual(readByteArray, str.getBytes(charset))) {
                                putValue(i, str);
                            } else {
                                putValue(i, "hex->" + bytesToHex(readByteArray));
                            }
                        }
                    } catch (Exception unused2) {
                        putValue(i, "hex->" + bytesToHex(readByteArray));
                    }
                } else if (i2 == 5) {
                    putValue(i, Integer.valueOf(newInstance.readFixed32()));
                } else {
                    putValue(i, "Unknown wireType: " + i2);
                }
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean arraysEqual(byte[] bArr, byte[] bArr2) {
        if (bArr.length != bArr2.length) {
            return false;
        }
        for (int i = 0; i < bArr.length; i++) {
            if (bArr[i] != bArr2[i]) {
                return false;
            }
        }
        return true;
    }

    private static String bytesToHex(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bArr) {
            sb.append(String.format("%02X", Integer.valueOf(b & 255)));
        }
        return sb.toString();
    }

    public JSONObject toJSON() {
        JSONObject jSONObject = new JSONObject();
        try {
        for (Integer num : this.values.keySet()) {
            List<Object> list = this.values.get(num);
            if (list.size() > 1) {
                JSONArray jSONArray = new JSONArray();
                Iterator<Object> it = list.iterator();
                while (it.hasNext()) {
                    jSONArray.put(valueToText(it.next()));
                }
                jSONObject.put(String.valueOf(num), jSONArray);
            } else {
                Iterator<Object> it2 = list.iterator();
                while (it2.hasNext()) {
                    jSONObject.put(String.valueOf(num), valueToText(it2.next()));
                }
            }
        }
        } catch (Throwable ignore) {
        }
        return jSONObject;
    }

    private Object valueToText(Object obj) {
        return obj instanceof FunProtoData ? ((FunProtoData) obj).toJSON() : obj;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        CodedOutputStream newInstance = CodedOutputStream.newInstance(byteArrayOutputStream);
        try {
            for (Integer num : this.values.keySet()) {
                for (Object obj : this.values.get(num)) {
                    if (obj instanceof Long) {
                        newInstance.writeInt64(num.intValue(), ((Long) obj).longValue());
                    } else if (obj instanceof byte[]) {
                        newInstance.writeByteArray(num.intValue(), (byte[]) obj);
                    } else if (obj instanceof String) {
                        newInstance.writeByteArray(num.intValue(), ((String) obj).getBytes());
                    } else if (obj instanceof FunProtoData) {
                        newInstance.writeByteArray(num.intValue(), ((FunProtoData) obj).toBytes());
                    } else if (obj instanceof Integer) {
                        newInstance.writeInt32(num.intValue(), ((Integer) obj).intValue());
                    } else {
                        com.cathelperawa.owo.app.core.HookLog.w("FunProtoData.toBytes Unknown type: " + obj.getClass().getName());
                    }
                }
            }
            newInstance.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            com.cathelperawa.owo.app.core.HookLog.e("FunProtoData - toBytes", e);
            return new byte[0];
        }
    }
}
