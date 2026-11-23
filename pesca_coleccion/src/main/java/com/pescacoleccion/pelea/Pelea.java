package com.pescacoleccion.pelea;

import java.nio.ByteBuffer;

/** Datos de la pelea que viajan por la red: los parámetros y las teclas (empaquetados en bytes, iguales en todas las versiones). */
public final class Pelea {
    private Pelea() {
    }

    public static byte[] params(PeleaSim.Params p) {
        ByteBuffer b = ByteBuffer.allocate(512);
        b.putLong(p.seed).putInt(p.bpm).putInt(p.keys).putFloat(p.trapChance).putInt(p.jumps).putFloat(p.pull).putInt(p.switchMs)
                .putFloat(p.stamina).putInt(p.approachMs).putInt(p.durationMs).putInt(p.ghostFromMs).putInt(p.burstFromMs).putFloat(p.zone)
                .putFloat(p.control).putFloat(p.lineHp).putFloat(p.damage).putInt(p.good).putInt(p.perfect).putFloat(p.missHp)
                .putFloat(p.mashGain).putInt(p.mashMs);
        b.put((byte) p.patterns.length);
        for (String s : p.patterns) {
            byte[] by = s.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            b.put((byte) by.length).put(by);
        }
        byte[] out = new byte[b.position()];
        b.flip();
        b.get(out);
        return out;
    }

    public static PeleaSim.Params params(byte[] data) {
        ByteBuffer b = ByteBuffer.wrap(data);
        PeleaSim.Params p = new PeleaSim.Params();
        p.seed = b.getLong();
        p.bpm = b.getInt();
        p.keys = b.getInt();
        p.trapChance = b.getFloat();
        p.jumps = b.getInt();
        p.pull = b.getFloat();
        p.switchMs = b.getInt();
        p.stamina = b.getFloat();
        p.approachMs = b.getInt();
        p.durationMs = b.getInt();
        p.ghostFromMs = b.getInt();
        p.burstFromMs = b.getInt();
        p.zone = b.getFloat();
        p.control = b.getFloat();
        p.lineHp = b.getFloat();
        p.damage = b.getFloat();
        p.good = b.getInt();
        p.perfect = b.getInt();
        p.missHp = b.getFloat();
        p.mashGain = b.getFloat();
        p.mashMs = b.getInt();
        int n = b.get();
        p.patterns = new String[n];
        for (int i = 0; i < n; i++) {
            byte[] by = new byte[b.get()];
            b.get(by);
            p.patterns[i] = new String(by, java.nio.charset.StandardCharsets.US_ASCII);
        }
        return p;
    }

    /** Teclas: cada una son 3 números (ms, tecla, 1 apretada / 0 soltada). */
    public static byte[] events(int[] ev) {
        ByteBuffer b = ByteBuffer.allocate(4 + ev.length * 4);
        b.putInt(ev.length);
        for (int v : ev) {
            b.putInt(v);
        }
        return b.array();
    }

    public static int[] events(byte[] data) {
        ByteBuffer b = ByteBuffer.wrap(data);
        int n = Math.min(b.getInt(), (data.length - 4) / 4);
        int[] ev = new int[Math.max(0, n)];
        for (int i = 0; i < ev.length; i++) {
            ev[i] = b.getInt();
        }
        return ev;
    }

    /** Vuelve a jugar la pelea con las teclas (lo que hace el servidor). */
    public static PeleaSim replay(PeleaSim.Params p, int[] ev) {
        PeleaSim s = new PeleaSim(p);
        int last = 0;
        for (int i = 0; i + 2 < ev.length; i += 3) {
            int ms = Math.max(last, ev[i]);   // en orden
            last = ms;
            s.input(ms, ev[i + 1], ev[i + 2] == 1);
            if (s.result != PeleaSim.PELEANDO) {
                break;
            }
        }
        return s;
    }
}
