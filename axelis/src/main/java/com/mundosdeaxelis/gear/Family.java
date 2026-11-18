package com.mundosdeaxelis.gear;

/** Tipos de herramienta de un set. */
public enum Family {
    SWORD("sword"), PICK("pick"), AXE("axe"), SHOVEL("shovel");

    public final String id;

    Family(String id) {
        this.id = id;
    }
}
