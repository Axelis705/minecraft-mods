import com.axeliscore.anim.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pruebas del motor de animación (sin Minecraft): expresiones, carga de modelos y animaciones, mezcla y eventos. */
public class AnimTest {
    static int fails = 0;

    static void eq(String what, double got, double want) {
        if (Math.abs(got - want) > 1e-3) {
            System.out.println("FALLO " + what + ": " + got + " (esperaba " + want + ")");
            fails++;
        }
    }

    public static void main(String[] a) {
        Expr.Ctx c = n -> switch (n) {
            case "query.anim_time" -> 0.5;
            case "variable.x" -> 2;
            default -> 0;
        };
        eq("precedencia", Expr.parse("1+2*3").eval(c), 7);
        eq("negativos", Expr.parse("-2*-3").eval(c), 6);
        eq("seno en grados", Expr.parse("math.sin(90)*2").eval(c), 2);
        eq("query", Expr.parse("q.anim_time * 10").eval(c), 5);
        eq("ternario", Expr.parse("v.x > 1 ? 5 : 6").eval(c), 5);
        eq("clamp", Expr.parse("math.clamp(v.x * 10, 0, 7)").eval(c), 7);
        eq("constante", Expr.parse("math.pi * 2").isConst() ? 1 : 0, 1);
        eq("resta encadenada", Expr.parse("10 - 3 - 2").eval(c), 5);
        eq("and/or", Expr.parse("(1 && 0) || 1").eval(c), 1);
        eq("ease inout", Ease.get("easeInOutSine").apply(0.5F), 0.5);
        eq("ease outback pasa de 1", Ease.get("easeOutBack").apply(0.6F) > 1 ? 1 : 0, 1);

        // el brazo derecho del humano de Bedrock debe quedar como en Java: offset (-5, 2, 0), caja (-3, -2, -2) 4x12x4
        Geo g = GeoLoader.parse("""
                {"format_version":"1.12.0","minecraft:geometry":[{"description":{"identifier":"geometry.t","texture_width":64,"texture_height":64},
                 "bones":[{"name":"body","pivot":[0,24,0],"cubes":[{"origin":[-4,12,-2],"size":[8,12,4],"uv":[16,16]}]},
                  {"name":"rightArm","parent":"body","pivot":[-5,22,0],"rotation":[-90,0,0],"cubes":[{"origin":[-8,12,-2],"size":[4,12,4],"uv":[40,16]}]},
                  {"name":"rightForearm","parent":"rightArm","pivot":[-6,18,0]}],
                 "axelis_bends":[{"bone":"rightArm","child":"rightForearm","at":18}], "axelis_glow":["body"]}]}""");
        Geo.Bone arm = g.bones[g.bone("rightArm")];
        eq("pivote x", arm.px, -5);
        eq("pivote y", arm.py, 2);
        eq("cubo x0", arm.cubes.get(0).x0, -8);
        eq("cubo y0", arm.cubes.get(0).y0, 0);
        eq("giro x", arm.rx, -Math.PI / 2);
        eq("padre", arm.parent, g.bone("body"));
        eq("doblez hueso", arm.cubes.get(0).bendBone, g.bone("rightForearm"));
        eq("doblez y", arm.cubes.get(0).bendY, 6);
        eq("brillo", g.bones[g.bone("body")].glow ? 1 : 0, 1);
        eq("hijos", g.bones[g.bone("body")].children.length, 1);

        Map<String, Clip> clips = ClipLoader.parse("""
                {"format_version":"1.8.0","animations":{
                 "animation.t.lin":{"loop":true,"animation_length":1.0,"bones":{"rightArm":{"rotation":{"0.0":[0,0,0],"1.0":[90,0,0]},
                     "position":[0,"q.anim_time * 2",0]}},
                   "sound_effects":{"0.5":{"effect":"paso"}},"timeline":{"0.75":"golpe"}},
                 "animation.t.step":{"loop":false,"bones":{"body":{"rotation":{"0.0":{"post":[10,0,0],"lerp_mode":"step"},"1.0":[50,0,0]}}}},
                 "animation.t.ease":{"loop":"hold_on_last_frame","bones":{"body":{"rotation":{"0.0":[0,0,0],"1.0":{"vector":[100,0,0],"easing":"easeInQuad"}}}}},
                 "animation.t.cat":{"loop":true,"animation_length":3,"bones":{"body":{"rotation":{"0.0":[0,0,0],"1.0":{"post":[10,0,0],"lerp_mode":"catmullrom"},"2.0":{"post":[20,0,0],"lerp_mode":"catmullrom"},"3.0":[30,0,0]}}}}
                }}""");
        Clip.Library lib = ClipLoader.library(clips);
        eq("nombre corto", lib.get("lin") == clips.get("animation.t.lin") ? 1 : 0, 1);
        eq("hold", lib.get("ease").loop == Clip.Loop.HOLD ? 1 : 0, 1);
        eq("largo calculado", lib.get("step").length, 1);

        Animator an = new Animator(g);
        List<String> ev = new ArrayList<>();
        an.listen((layer, clip, e) -> ev.add(e.kind + ":" + e.value));
        Pose p = new Pose(g.bones.length);
        an.play("base", lib.get("lin"), 0);
        an.tick(0.5F);
        an.sample(p);
        int ra = g.bone("rightArm");
        eq("lineal a la mitad (rad)", p.rot[ra * 3], Math.toRadians(45));
        eq("posición con expresión (y al revés)", p.pos[ra * 3 + 1], -1);
        an.tick(0.3F);
        an.tick(0.5F); // cruza el final y vuelve a empezar
        eq("eventos", ev.size(), 2);
        eq("evento 1", ev.get(0).equals("sound:paso") ? 1 : 0, 1);
        eq("evento 2", ev.get(1).equals("timeline:golpe") ? 1 : 0, 1);
        an.tick(0.4F); // 0.3 -> 0.7: pasa por 0.5 otra vez
        eq("eventos tras el bucle", ev.size(), 3);

        Animator s = new Animator(g);
        s.play("base", lib.get("step"), 0);
        s.tick(0.6F);
        s.sample(p);
        eq("step", p.rot[g.bone("body") * 3], Math.toRadians(10));

        Animator e = new Animator(g);
        e.play("base", lib.get("ease"), 0);
        e.tick(0.5F);
        e.sample(p);
        eq("easeInQuad", p.rot[g.bone("body") * 3], Math.toRadians(25));
        e.tick(2F);
        e.sample(p);
        eq("hold al final", p.rot[g.bone("body") * 3], Math.toRadians(100));

        Animator k = new Animator(g);
        k.play("base", lib.get("cat"), 0);
        k.tick(1.5F);
        k.sample(p);
        eq("catmullrom en recta", p.rot[g.bone("body") * 3], Math.toRadians(15));

        // mezcla: de "lin" (brazo a 0..90) a "step" (cuerpo) en 0.5 s: a la mitad, el brazo va a medio camino de vuelta a 0
        Animator m = new Animator(g);
        m.play("base", lib.get("lin"), 0);
        m.tick(0.5F); // brazo a 45
        m.play("base", lib.get("step"), 0.5F);
        m.tick(0.25F); // mezcla 50 %, el brazo de la vieja ya va en 0.75 -> 67.5
        m.sample(p);
        eq("mezcla a la mitad", p.rot[ra * 3], Math.toRadians(67.5 * 0.5));
        m.tick(0.3F);
        m.sample(p);
        eq("mezcla terminada", p.rot[ra * 3], 0);

        // una de una vez termina y se desvanece
        Animator o = new Animator(g);
        o.play("gesto", lib.get("step"), 0);
        o.tick(1.05F);
        eq("terminó", o.finished("gesto") ? 1 : 0, 1);
        o.tick(0.5F);
        o.sample(p);
        eq("desvanecida", p.rot[g.bone("body") * 3], 0);

        // capa con máscara: solo el brazo
        Animator mk = new Animator(g);
        mk.layer("brazo").mask(g, "rightArm");
        mk.play("brazo", lib.get("cat"), 0);
        mk.tick(1F);
        mk.sample(p);
        eq("máscara deja el cuerpo quieto", p.rot[g.bone("body") * 3], 0);

        try {
            Expr.parse("math.nada(1)");
            fails++;
            System.out.println("FALLO: debía rechazar una función desconocida");
        } catch (IllegalArgumentException ex) {
            // bien
        }
        bbmodel();
        mezcla();
        if (fails > 0) {
            System.out.println("AnimTest: " + fails + " fallos");
            System.exit(1);
        }
        System.out.println("AnimTest: todo bien");
    }

    /**
     * Un .bbmodel de Blockbench 5 (grupos aparte, cubo girado, UV por cara, Bézier, catmullrom, paso): debe quedar igual que lo que
     * exporta el propio Blockbench (valores sacados de su exportador Bedrock).
     */
    static void bbmodel() {
        String bb = """
                {"meta":{"format_version":"5.0","model_format":"bedrock","box_uv":true},"name":"prueba_conversion","model_identifier":"","resolution":{"width":16,"height":16},"elements":[{"name":"c1","box_uv":false,"from":[-2,0,-1],"to":[3,4,2],"inflate":0.25,"rotation":[0,22.5,0],"origin":[0.5,1,0.5],"faces":{"north":{"uv":[1,2,6,6]},"east":{"uv":[0,0,3,4]},"south":{"uv":[6,0,11,4]},"west":{"uv":[3,0,6,4]},"up":{"uv":[10,12,15,9]},"down":{"uv":[20,21,25,24]}},"type":"cube","uuid":"c47751ab-891e-4d06-6836-6b8a93476555"},{"name":"c2","box_uv":true,"from":[3,8,-1],"to":[5,12,1],"mirror_uv":true,"origin":[0,0,0],"uv_offset":[16,32],"type":"cube","uuid":"69be3dc8-aec3-d715-f13a-8f968f79280a"}],"groups":[{"name":"body","uuid":"85539e00-1170-38f4-c8be-224f3ec02488","origin":[1,2,3],"rotation":[10,20,30]},{"name":"arm","uuid":"f0d47077-e2a7-4ed3-39d1-14f72d5ca9e5","origin":[4,10,0],"rotation":[0,0,-15]}],"outliner":[{"uuid":"85539e00-1170-38f4-c8be-224f3ec02488","isOpen":false,"children":[{"uuid":"f0d47077-e2a7-4ed3-39d1-14f72d5ca9e5","isOpen":false,"children":["69be3dc8-aec3-d715-f13a-8f968f79280a"]},"c47751ab-891e-4d06-6836-6b8a93476555"]}],"textures":[],"animations":[{"uuid":"70b374fe","name":"animation.test.a","loop":"loop","length":1,"animators":{"85539e00-1170-38f4-c8be-224f3ec02488":{"name":"body","type":"bone","keyframes":[{"channel":"rotation","data_points":[{"x":"0","y":"0","z":"0"}],"time":0,"interpolation":"linear"},{"channel":"rotation","data_points":[{"x":"10","y":"-20","z":"30"}],"time":0.5,"interpolation":"bezier","bezier_left_time":[-0.2,-0.1,-0.15],"bezier_left_value":[5,0,-3],"bezier_right_time":[0.1,0.2,0.1],"bezier_right_value":[-2,1,0]},{"channel":"rotation","data_points":[{"x":"0","y":"math.sin(q.anim_time*90)*4","z":"0"}],"time":1,"interpolation":"catmullrom"},{"channel":"position","data_points":[{"x":"1","y":"2","z":"3"},{"x":"4","y":"5","z":"6"}],"time":0.25,"interpolation":"step"}]},"f0d47077-e2a7-4ed3-39d1-14f72d5ca9e5":{"name":"arm","type":"bone","keyframes":[{"channel":"scale","data_points":[{"x":"1.5","y":"1","z":"0.5"}],"time":0.75,"interpolation":"linear"}]}}}]}
                """;
        BbModel.Resultado r = BbModel.parse(bb);
        Geo g = GeoLoader.parse(r.geo);
        eq("bb huesos", g.bones.length, 2);
        Geo.Bone body = g.bones[g.bone("body")];
        // pivote de Bedrock [-1, 2, 3] -> Java (x, 24 - y, z)
        eq("bb pivote x", body.px, -1);
        eq("bb pivote y", body.py, 22);
        eq("bb giro x", Math.toDegrees(body.rx), -10);
        eq("bb giro y", Math.toDegrees(body.ry), -20);
        eq("bb giro z", Math.toDegrees(body.rz), 30);
        Geo.Cube c1 = body.cubes.get(0);
        eq("bb cubo x0", c1.x0, -3);
        eq("bb cubo ancho", c1.sx, 5);
        eq("bb cubo giro y", Math.toDegrees(c1.cry), -22.5);
        Clip clip = ClipLoader.parse(r.animaciones).get("animation.test.a");
        float[] out = new float[3];
        Clip.Track tr = null;
        for (Clip.Track t : clip.tracks) {
            if (t.bone.equals("body")) {
                tr = t;
            }
        }
        // lo que da Blockbench al exportar (hornea la curva a 24 cuadros por segundo)
        float[][] esperado = {{0.25F, -8.1403F, 10.0F, 15.2975F}, {0.125F, -3.5221F, 3.9486F, 5.9998F}, {0.75F, -4.25F, 11.6698F, 15.0F}};
        for (float[] e : esperado) {
            tr.rot.sample(e[0], n -> 0, out);
            eq("bezier x en " + e[0], out[0], e[1]);
            eq("bezier y en " + e[0], out[1], e[2]);
            eq("bezier z en " + e[0], out[2], e[3]);
        }
        tr.pos.sample(0.3F, n -> 0, out);
        eq("paso: después del cuadro usa el valor de salida", out[0], -4);
    }

    /** Mezcla por valor: a mitad de camino entre dos animaciones queda en medio. */
    static void mezcla() {
        Geo g = GeoLoader.parse("""
                {"format_version":"1.12.0","minecraft:geometry":[{"description":{"identifier":"geometry.m","texture_width":16,"texture_height":16},
                 "bones":[{"name":"a","pivot":[0,0,0]}]}]}
                """);
        var lib = ClipLoader.parse("""
                {"animations":{"animation.m.lento":{"loop":true,"animation_length":1,"bones":{"a":{"rotation":[10,0,0]}}},
                 "animation.m.rapido":{"loop":true,"animation_length":0.5,"bones":{"a":{"rotation":[30,0,0]}}}}}
                """);
        Animator an = new Animator(g);
        an.layer("base").mezcla(new Clip[] {lib.get("animation.m.lento"), lib.get("animation.m.rapido")}, new float[] {1, 3}).valor(2);
        an.tick(0.1F);
        Pose p = new Pose(g.bones.length);
        an.sample(p);
        eq("mezcla a la mitad", Math.toDegrees(p.rot[0]), 20);
    }
}
