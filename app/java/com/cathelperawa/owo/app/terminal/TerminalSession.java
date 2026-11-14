package com.cathelperawa.owo.app.terminal;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class TerminalSession {

    public interface OutputListener {
        void onOutput(String text);
        void onExit(int code);
    }

    private static final List<TerminalSession> sessions = new ArrayList<>();
    private static int nextId = 1;

    public final int id;
    public final String name;

    private Process process;
    private OutputStream stdin;
    private BufferedReader stdout;
    private Thread readerThread;
    private volatile boolean alive;
    private OutputListener listener;

    private TerminalSession(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static TerminalSession open(File workDir, OutputListener l) {
        TerminalSession s = new TerminalSession(nextId++, "session-" + nextId);
        s.listener = l;
        try {
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh");
            pb.directory(workDir);
            pb.redirectErrorStream(true);
            s.process = pb.start();
            s.stdin  = s.process.getOutputStream();
            s.stdout = new BufferedReader(new InputStreamReader(s.process.getInputStream()));
            s.alive  = true;
            s.startReader();
            sessions.add(s);
            l.onOutput("# 会话已开启  cwd=" + workDir.getAbsolutePath() + "\n");
        } catch (Throwable t) {
            l.onOutput("# 启动失败: " + t + "\n");
        }
        return s;
    }

    private void startReader() {
        readerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String line;
                    while (alive && (line = stdout.readLine()) != null) {
                        if (listener != null) listener.onOutput(line + "\n");
                    }
                } catch (Throwable ignore) {}
                int code = 0;
                try { code = process.waitFor(); } catch (Throwable ignore) {}
                alive = false;
                if (listener != null) listener.onExit(code);
            }
        }, "term-reader-" + id);
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void write(String cmd) {
        try {
            stdin.write((cmd + "\n").getBytes("UTF-8"));
            stdin.flush();
        } catch (Throwable t) {
            if (listener != null) listener.onOutput("# 写入失败: " + t + "\n");
        }
    }

    public boolean isAlive() { return alive; }

    public void destroy() {
        alive = false;
        try { process.destroy(); } catch (Throwable ignore) {}
        sessions.remove(this);
    }

    public static List<TerminalSession> all() { return sessions; }
}
