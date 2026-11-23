package com.axeliscore.test;

import com.axeliscore.anim.client.EstudioArchivos;
import com.axeliscore.anim.client.EstudioEditor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Prueba con fotos del estudio: el editor de animaciones (/axeliscore estudio) con una Persona (elegir hueso, poner un cuadro clave,
 * guardar) y la carpeta del estudio recargando un proyecto de Blockbench (.bbmodel) sin reiniciar.
 */
final class EstudioScenario {
    private static final double G = CoreScenario.G;

    /** Un proyecto de Blockbench 5 pequeño (como lo guarda Blockbench): tres bloques apilados que rebotan con curvas Bézier. */
    static final String BBMODEL = """
            {"meta":{"format_version":"5.0","model_format":"bedrock","box_uv":true},"name":"gelatina","model_identifier":"gelatina",
             "resolution":{"width":64,"height":64},
             "elements":[
              {"name":"base","box_uv":true,"from":[-6,0,-6],"to":[6,6,6],"origin":[0,0,0],"uv_offset":[0,0],"type":"cube","uuid":"e1"},
              {"name":"medio","box_uv":true,"from":[-4,6,-4],"to":[4,11,4],"origin":[0,0,0],"uv_offset":[0,20],"type":"cube","uuid":"e2"},
              {"name":"punta","box_uv":true,"from":[-2,11,-2],"to":[2,15,2],"origin":[0,13,0],"rotation":[0,45,0],"uv_offset":[0,36],"type":"cube","uuid":"e3"}],
             "groups":[{"name":"body","uuid":"g1","origin":[0,0,0],"rotation":[0,0,0]},{"name":"top","uuid":"g2","origin":[0,11,0],"rotation":[0,0,0]}],
             "outliner":[{"uuid":"g1","children":["e1","e2",{"uuid":"g2","children":["e3"]}]}],
             "textures":[],
             "animations":[{"uuid":"a1","name":"animation.gelatina.idle","loop":"loop","length":1.2,"animators":{
               "g1":{"name":"body","type":"bone","keyframes":[
                 {"channel":"scale","data_points":[{"x":"1","y":"1","z":"1"}],"time":0,"interpolation":"linear"},
                 {"channel":"scale","data_points":[{"x":"1.2","y":"0.8","z":"1.2"}],"time":0.6,"interpolation":"bezier",
                  "bezier_left_time":[-0.2,-0.2,-0.2],"bezier_left_value":[0,0,0],"bezier_right_time":[0.2,0.2,0.2],"bezier_right_value":[0,0,0]},
                 {"channel":"scale","data_points":[{"x":"1","y":"1","z":"1"}],"time":1.2,"interpolation":"linear"}]},
               "g2":{"name":"top","type":"bone","keyframes":[
                 {"channel":"rotation","data_points":[{"x":"0","y":"0","z":"0"}],"time":0,"interpolation":"catmullrom"},
                 {"channel":"rotation","data_points":[{"x":"0","y":"90","z":"15"}],"time":0.6,"interpolation":"catmullrom"},
                 {"channel":"rotation","data_points":[{"x":"0","y":"180","z":"0"}],"time":1.2,"interpolation":"catmullrom"}]}}}]}
            """;

    private EstudioScenario() {
    }

    static void estudio(ClientTest.Script t) {
        t.add("estudio: preparar", 2000, () -> {
            com.axeliscore.plat.client.Screens.close();
            t.hud(true);
            t.cmd("kill @e[type=!player]");
            t.cmd("summon axelis_core:persona 0.5 -60 3.5 {NoAI:1b,Rotation:[200f,0f]}");
            t.lookFrom(0.5, G, -0.5, 0.5, G + 1.2, 3.5);
        });
        t.photo("29_estudio", 1600, () -> t.cmd("execute as @p at @s run axeliscore estudio"));
        t.add("estudio abierto", 100, () -> {
            t.check(EstudioEditor.activo(), "el editor tomó a la Persona");
            t.check(EstudioEditor.elegirClip("wave"), "la Persona tiene la animación de saludar");
        });
        t.photo("29_estudio_hueso", 900, () -> {
            EstudioEditor.elegirHueso("right_arm");
            EstudioEditor.irA(0.5F);
            if (com.axeliscore.plat.client.Screens.current() instanceof com.axeliscore.ui.UiScreen s) {
                s.reconstruir();
            }
        });
        t.photo("29_estudio_clave", 900, () -> {
            EstudioEditor.poner("rotation", new float[] {-160, 10, 25});
            if (com.axeliscore.plat.client.Screens.current() instanceof com.axeliscore.ui.UiScreen s) {
                s.reconstruir();
            }
        });
        t.add("clave puesta", 100, () -> {
            float[] v = EstudioEditor.valor("right_arm", "rotation");
            t.check(Math.abs(v[0] + 160) < 0.5F && Math.abs(v[2] - 25) < 0.5F, "el brazo quedó donde se puso (" + v[0] + ", " + v[2] + ")");
        });
        t.add("guardar", 600, () -> t.check(EstudioEditor.guardar() != null, "se guardó el archivo de animaciones"));
        t.add("cerrar estudio", 1500, () -> com.axeliscore.plat.client.Screens.close());
        t.add("guardado", 100, () -> {
            Path f = EstudioArchivos.carpeta().resolve("persona.animation.json");
            t.check(Files.exists(f), "existe axelis_estudio/persona.animation.json");
            t.check(!EstudioEditor.activo(), "el editor soltó a la Persona");
        });
        // carpeta del estudio: un .bbmodel que reemplaza a la gelatina sin reiniciar
        t.add("bbmodel", 2500, () -> {
            try {
                Files.writeString(EstudioArchivos.carpeta().resolve("gelatina.bbmodel"), BBMODEL, StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                throw new AssertionError("no se pudo escribir el .bbmodel: " + e);
            }
            t.cmd("kill @e[type=!player]");
            t.cmd("summon axelis_core:gelatina 0.5 -60 3.5 {NoAI:1b,Rotation:[160f,0f]}");
            t.lookFrom(0.5, G, 0.0, 0.5, G + 0.6, 3.5);
        });
        t.add("bbmodel cargado", 100, () -> t.check(EstudioArchivos.geo(com.axeliscore.plat.Ids.core("geo/gelatina.geo.json")) != null,
                "el estudio cargó gelatina.bbmodel"));
        t.photo("30_estudio_bbmodel", 400, () -> {
        });
        t.photo("30_estudio_bbmodel_2", 600, () -> {
        });
        t.add("estudio: limpiar", 2500, () -> {
            try {
                Files.deleteIfExists(EstudioArchivos.carpeta().resolve("gelatina.bbmodel"));
                Files.deleteIfExists(EstudioArchivos.carpeta().resolve("persona.animation.json"));
            } catch (java.io.IOException ignored) {
                // nada
            }
            EstudioArchivos.editadas("persona", null);
        });
        t.add("estudio vacío", 100, () -> t.check(EstudioArchivos.geo(com.axeliscore.plat.Ids.core("geo/gelatina.geo.json")) == null,
                "al borrar el archivo vuelve el modelo del mod"));
        // efectos de partículas por capas (los manda el servidor)
        t.add("efectos", 800, () -> {
            t.cmd("kill @e[type=!player]");
            t.lookFrom(0.5, G, -2.5, 0.5, G + 1.0, 4.0);
        });
        String[][] efectos = {{"explosion_magica", "350"}, {"curacion", "700"}, {"aparecer", "450"}, {"fuego_circulo", "900"},
                {"subir_nivel", "500"}, {"portal", "1100"}, {"chispas", "150"}, {"nevada", "1600"}};
        for (String[] e : efectos) {
            t.photo("31_efecto_" + e[0], Long.parseLong(e[1]), () -> t.cmd("execute positioned 0.5 -59.8 4.0 run axeliscore particulas " + e[0]));
            t.add("esperar " + e[0], 1200, () -> {
            });
        }
        t.add("efectos llegaron", 100, () -> t.check(com.axeliscore.fx.client.EfectosCliente.lanzados >= efectos.length,
                "llegaron los efectos al cliente (" + com.axeliscore.fx.client.EfectosCliente.lanzados + ")"));
        t.add("estudio: fin", 300, () -> {
            t.cmd("kill @e[type=!player]");
            t.hud(false);
        });
    }
}
