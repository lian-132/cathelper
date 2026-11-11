package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.api.IoApi;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class AiProvider {

    public String id = "";
    
    public String name = "";
    
    public String baseUrl = "";
    
    public String apiKey = "";
    
    public String model = "";
    
    public float temperature = 0.9f;
    
    public int maxTokens = 4096;
    
    public int historyLimit = 20;
    
    public boolean enabled = true;

    public AiProvider() {}

    public static AiProvider create(String name) {
        AiProvider p = new AiProvider();
        p.id = String.valueOf(System.currentTimeMillis());
        p.name = name == null ? "新提供商" : name;
        return p;
    }

    public String displayName() {
        if (name != null && name.trim().length() > 0) return name;
        if (baseUrl != null && baseUrl.length() > 0) return baseUrl;
        return "未命名";
    }

    public String summary() {
        String m = model == null || model.isEmpty() ? "未选模型" : model;
        return m + " · " + (enabled ? "启用" : "停用");
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("baseUrl", baseUrl);
            
            String enc = AiCrypto.encrypt(apiKey);
            if (enc.length() > 0) {
                o.put("apiKeyEnc", enc);
            } else if (apiKey != null && apiKey.length() > 0) {
                
                o.put("apiKey", apiKey);
                HookLog.w("AI Key 加密失败，回落明文存储");
            }
            o.put("model", model);
            o.put("temperature", temperature);
            o.put("maxTokens", maxTokens);
            o.put("historyLimit", historyLimit);
            o.put("enabled", enabled);
        } catch (Throwable ignore) {
        }
        return o;
    }

    public static AiProvider fromJson(JSONObject o) {
        AiProvider p = new AiProvider();
        if (o == null) return p;
        p.id = o.optString("id", String.valueOf(System.currentTimeMillis()));
        p.name = o.optString("name", "");
        p.baseUrl = o.optString("baseUrl", "");
        String enc = o.optString("apiKeyEnc", "");
        if (enc.length() > 0) p.apiKey = AiCrypto.decrypt(enc);
        if (p.apiKey == null || p.apiKey.length() == 0) {
            
            p.apiKey = o.optString("apiKey", "");
        }
        p.model = o.optString("model", "");
        p.temperature = (float) o.optDouble("temperature", 0.9);
        p.maxTokens = o.optInt("maxTokens", 4096);
        p.historyLimit = o.optInt("historyLimit", 20);
        p.enabled = o.optBoolean("enabled", true);
        return p;
    }

    

    private static final String FILE = "ai_providers.json";

    public static List<AiProvider> loadAll() {
        List<AiProvider> out = new ArrayList<AiProvider>();
        try {
            String txt = IoApi.readFile(FILE);
            if (txt == null || txt.trim().length() == 0) return out;
            JSONArray arr = new JSONArray(txt);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) out.add(fromJson(o));
            }
        } catch (Throwable t) {
            HookLog.e("读取提供商失败", t);
        }
        return out;
    }

    public static boolean saveAll(List<AiProvider> list) {
        try {
            JSONArray arr = new JSONArray();
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    arr.put(list.get(i).toJson());
                }
            }
            return IoApi.writeFile(FILE, arr.toString());
        } catch (Throwable t) {
            HookLog.e("保存提供商失败", t);
            return false;
        }
    }

    public static boolean upsert(AiProvider p) {
        if (p == null) return false;
        List<AiProvider> list = loadAll();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(p.id)) {
                list.set(i, p);
                found = true;
                break;
            }
        }
        if (!found) list.add(p);
        return saveAll(list);
    }

    public static boolean remove(String id) {
        List<AiProvider> list = loadAll();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                list.remove(i);
                return saveAll(list);
            }
        }
        return false;
    }

    public static AiProvider find(String id) {
        List<AiProvider> list = loadAll();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) return list.get(i);
        }
        return null;
    }

    public static AiProvider current() {
        List<AiProvider> list = loadAll();
        if (list.isEmpty()) return null;
        String id = null;
        try {
            id = com.cathelperawa.owo.app.core.ConfigStore.getString("ai_current_id");
        } catch (Throwable ignore) {
        }
        if (id != null && id.length() > 0) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).id.equals(id)) return list.get(i);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).enabled) return list.get(i);
        }
        return null;
    }

    public static String currentId() {
        try {
            String s = com.cathelperawa.owo.app.core.ConfigStore.getString("ai_current_id");
            return s == null ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    public static void setCurrent(String id) {
        try {
            com.cathelperawa.owo.app.core.ConfigStore.setString("ai_current_id", id);
        } catch (Throwable ignore) {
        }
    }
}
