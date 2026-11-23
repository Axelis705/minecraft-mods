package com.pescacoleccion.pesca;

import java.util.List;
import java.util.Map;

/** Un pez de la colección (los datos salen de resources/pesca_coleccion/peces.json, que genera herramientas/generar_peces.py). */
public record Especie(String id, int rareza, List<String> lugar, List<String> hora, String clima, String luna, float cmMin, float cmMax,
                      float kg, Map<String, Object> pelea) {

    public String itemId() {
        return "pez_" + id;
    }

    /** Tamaño al azar: casi todos medianos; los enormes salen poco. */
    public float tamano(double r) {
        return Math.round((cmMin + (cmMax - cmMin) * (float) Math.pow(r, 1.8)) * 10F) / 10F;
    }

    /** Peso en kg para un tamaño (crece con el cubo del largo). */
    public float peso(float cm) {
        float m = cm / 100F;
        return Math.round(kg * m * m * m * 100F) / 100F;
    }

    /** ¿Es de los grandes? (para la estrella de maestría) */
    public boolean grande(float cm) {
        return cm >= cmMin + (cmMax - cmMin) * 0.75F;
    }

    public float num(String k, float def) {
        Object o = pelea.get(k);
        return o instanceof Number n ? n.floatValue() : def;
    }

    public boolean rasgo(String k) {
        return Boolean.TRUE.equals(pelea.get(k));
    }
}
