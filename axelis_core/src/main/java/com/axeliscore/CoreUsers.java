package com.axeliscore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Qué mods usan el Core (cada uno se anota al arrancar con {@link AxelisCore#use(String)}). */
public final class CoreUsers {
    private static final List<String> USERS = Collections.synchronizedList(new ArrayList<>());

    private CoreUsers() {
    }

    static void add(String modid) {
        if (!USERS.contains(modid)) {
            USERS.add(modid);
        }
    }

    public static List<String> list() {
        synchronized (USERS) {
            return new ArrayList<>(USERS);
        }
    }
}
