package com.cathelperawa.owo.app.core;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.QQEnv;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class TemplateStore {

    private TemplateStore() {
    }

    public static final class Item {
        public String name = "";
        public int mode = 0;
        public String content = "";
        
        public String cmd = "";
        public long time = 0L;

        public Item() {
        }

        public Item(String name, int mode, String content, long time) {
            this.name = name;
            this.mode = mode;
            this.content = content;
            this.time = time;
        }

        public Item(String name, int mode, String content, String cmd, long time) {
            this.name = name;
            this.mode = mode;
            this.content = content;
            this.cmd = cmd;
            this.time = time;
        }
    }

    private static final String FILE = "send_templates.json";

    private static File file(Context c) {
        return new File(c.getFilesDir(), FILE);
    }

    public static synchronized List<Item> all(Context c) {
        List<Item> out = new ArrayList<Item>();
        try {
            if (c == null) c = QQEnv.topActivity();
            if (c == null) return out;
            File f = file(c);
            if (!f.exists()) return out;
            String s = read(f);
            if (s == null || s.trim().length() == 0) return out;
            JSONObject root = new JSONObject(s);
            JSONArray arr = root.optJSONArray("items");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Item it = new Item();
                it.name = o.optString("name", "未命名");
                it.mode = o.optInt("mode", 0);
                it.content = o.optString("content", "");
                it.cmd = o.optString("cmd", "");
                it.time = o.optLong("time", 0L);
                if (it.content.length() > 0) out.add(it);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static synchronized boolean save(Context c, String name, int mode, String content) {
        return save(c, name, mode, content, "");
    }

    public static synchronized boolean save(Context c, String name, int mode, String content,
                                            String cmd) {
        try {
            if (c == null) c = QQEnv.topActivity();
            if (c == null) return false;
            if (name == null || name.trim().length() == 0) name = "未命名";
            if (content == null) content = "";
            if (cmd == null) cmd = "";

            List<Item> list = all(c);
            boolean replaced = false;
            for (Item it : list) {
                if (it.name.equals(name)) {
                    it.mode = mode;
                    it.content = content;
                    it.cmd = cmd;
                    it.time = System.currentTimeMillis();
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                list.add(0, new Item(name, mode, content, cmd, System.currentTimeMillis()));
            }
            return write(c, list);
        } catch (Throwable t) {
            return false;
        }
    }

    public static synchronized boolean remove(Context c, String name) {
        try {
            if (c == null) c = QQEnv.topActivity();
            if (c == null) return false;
            List<Item> list = all(c);
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).name.equals(name)) {
                    list.remove(i);
                    return write(c, list);
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    public static synchronized boolean rename(Context c, String oldName, String newName) {
        try {
            if (c == null) c = QQEnv.topActivity();
            if (c == null) return false;
            if (newName == null || newName.trim().length() == 0) return false;
            List<Item> list = all(c);
            boolean hit = false;
            for (Item it : list) {
                if (it.name.equals(oldName)) {
                    it.name = newName;
                    it.time = System.currentTimeMillis();
                    hit = true;
                    break;
                }
            }
            return hit && write(c, list);
        } catch (Throwable t) {
            return false;
        }
    }

    public static synchronized String export(Context c) {
        try {
            JSONArray arr = new JSONArray();
            for (Item it : all(c)) {
                JSONObject o = new JSONObject();
                o.put("name", it.name);
                o.put("mode", it.mode);
                o.put("content", it.content);
                if (it.cmd != null && it.cmd.length() > 0) o.put("cmd", it.cmd);
                o.put("time", it.time);
                arr.put(o);
            }
            JSONObject root = new JSONObject();
            root.put("items", arr);
            return root.toString(2);
        } catch (Throwable t) {
            return "{\"items\":[]}";
        }
    }

    

    private static boolean write(Context c, List<Item> list) {
        try {
            JSONArray arr = new JSONArray();
            for (Item it : list) {
                JSONObject o = new JSONObject();
                o.put("name", it.name);
                o.put("mode", it.mode);
                o.put("content", it.content);
                if (it.cmd != null && it.cmd.length() > 0) o.put("cmd", it.cmd);
                o.put("time", it.time);
                arr.put(o);
            }
            JSONObject root = new JSONObject();
            root.put("items", arr);

            File f = file(c);
            FileOutputStream o = new FileOutputStream(f, false);
            try {
                o.write(root.toString().getBytes("UTF-8"));
            } finally {
                o.close();
            }
            return true;
        } catch (Throwable t) {
            HookLog.e("TemplateStore.write", t);
            return false;
        }
    }

    private static String read(File f) {
        try {
            FileInputStream in = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            } finally {
                in.close();
            }
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return null;
        }
    }
}
