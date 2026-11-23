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
        idaYVuelta();
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

    /**
     * Ida y vuelta con Blockbench: el modelo de la Persona (un pedazo) se abrió en Blockbench 5.2, Blockbench lo guardó como .bbmodel y
     * ese archivo, pasado por BbModel, tiene que dar exactamente el mismo modelo que el original (huesos, padres, cubos, UV, espejo, caras).
     */
    static void idaYVuelta() {
        String original = """
                {"format_version":"1.12.0","minecraft:geometry":[{"description":{"identifier":"geometry.axelis_core.persona","texture_width":64,"texture_height":64},"bones":[{"name":"root","pivot":[0,0,0]},{"name":"body","pivot":[0,12,0],"parent":"root","cubes":[{"origin":[-4,12,-2],"size":[8,12,4],"uv":[16,16]},{"origin":[-4,12,-2],"size":[8,12,4],"uv":[16,32],"inflate":0.25}]},{"name":"chest","pivot":[0,18,0],"parent":"body"},{"name":"head","pivot":[0,24,0],"parent":"chest","cubes":[{"origin":[-4,24,-4],"size":[8,8,8],"uv":[0,0]},{"origin":[-4,24,-4],"size":[8,8,8],"uv":[32,0],"inflate":0.5}]},{"name":"face_eyes","pivot":[0,24,0],"parent":"head","cubes":[{"origin":[-4,24,-4.03],"size":[8,8,0],"uv":{"north":{"uv":[0,0],"uv_size":[8,8]}}}]},{"name":"right_arm","pivot":[-5,22,0],"parent":"chest","rotation":[5,10,-15],"cubes":[{"origin":[-8,12,-2],"size":[4,12,4],"uv":[40,16]},{"origin":[-8,12,-2],"size":[4,12,4],"uv":[40,32],"inflate":0.25,"mirror":true}]},{"name":"right_forearm","pivot":[-6,18,0],"parent":"right_arm"},{"name":"left_leg","pivot":[1.9,12,0],"parent":"root","cubes":[{"origin":[-0.1,0,-2],"size":[4,12,4],"uv":[16,48],"pivot":[1.9,6,0],"rotation":[10,0,5]}]}]}]}
                """;
        String guardadoPorBlockbench = """
                {"meta":{"format_version":"5.0","model_format":"bedrock","box_uv":true},"name":"persona","resolution":{"width":64,"height":64},"elements":[{"name":"body","type":"cube","uuid":"8999805c-abd7-2ce8-14a6-9846e4717bb8","from":[-4,12,-2],"to":[4,24,2],"origin":[0,0,0],"box_uv":true,"uv_offset":[16,16]},{"name":"body","type":"cube","uuid":"fcf11756-4e93-329a-310b-2341da24e04d","from":[-4,12,-2],"to":[4,24,2],"origin":[0,0,0],"box_uv":true,"inflate":0.25,"uv_offset":[16,32]},{"name":"head","type":"cube","uuid":"61c2f984-267f-6acc-3053-3d019509578c","from":[-4,24,-4],"to":[4,32,4],"origin":[0,0,0],"box_uv":true},{"name":"head","type":"cube","uuid":"f05a8ebd-58bd-7d2f-3dde-7a9b4cbf2fe2","from":[-4,24,-4],"to":[4,32,4],"origin":[0,0,0],"box_uv":true,"inflate":0.5,"uv_offset":[32,0]},{"name":"face_eyes","type":"cube","uuid":"60b80ed0-9426-bb6c-84ea-6d0b835792bc","from":[-4,24,-4.03],"to":[4,32,-4.03],"origin":[0,0,0],"box_uv":false,"faces":{"north":{"uv":[0,0,8,8]},"east":{"uv":[0,0,0,0],"texture":null},"south":{"uv":[0,0,0,0],"texture":null},"west":{"uv":[0,0,0,0],"texture":null},"up":{"uv":[0,0,0,0],"texture":null},"down":{"uv":[0,0,0,0],"texture":null}}},{"name":"right_arm","type":"cube","uuid":"9a22684e-97a9-3e3b-5dea-e638eb72dbe2","from":[4,12,-2],"to":[8,24,2],"origin":[0,0,0],"box_uv":true,"uv_offset":[40,16]},{"name":"right_arm","type":"cube","uuid":"c979898d-e8e0-c016-4e63-1cd819ac274e","from":[4,12,-2],"to":[8,24,2],"origin":[0,0,0],"box_uv":true,"inflate":0.25,"mirror_uv":true,"uv_offset":[40,32]},{"name":"left_leg","type":"cube","uuid":"5a4a0016-2d2b-6671-2e71-79bb70de3cd6","from":[-3.9,0,-2],"to":[0.10000000000000009,12,2],"origin":[-1.9,6,0],"box_uv":true,"rotation":[-10,0,5],"uv_offset":[16,48]}],"groups":[{"name":"root","uuid":"4e6532fe-ce84-c419-21a0-ea8f18f496ea","origin":[0,0,0],"rotation":[0,0,0]},{"name":"body","uuid":"ad52a49d-e8e6-60c7-7516-956fabbfc049","origin":[0,12,0],"rotation":[0,0,0]},{"name":"chest","uuid":"2f1b3796-7da4-5424-a4fe-63dac803d9aa","origin":[0,18,0],"rotation":[0,0,0]},{"name":"head","uuid":"22a288dc-9794-d3ae-0dc0-573d1cec010d","origin":[0,24,0],"rotation":[0,0,0]},{"name":"face_eyes","uuid":"f3adc67f-108a-9c2c-39cb-497c29eeb5f8","origin":[0,24,0],"rotation":[0,0,0]},{"name":"right_arm","uuid":"15ab30e5-0505-1caf-5f50-f648af559d65","origin":[5,22,0],"rotation":[-5,-10,-15]},{"name":"right_forearm","uuid":"abe4540d-7908-c007-6cf6-f21d4bd44c4b","origin":[6,18,0],"rotation":[0,0,0]},{"name":"left_leg","uuid":"30b6ffa4-ad31-4f5e-7837-00e5cecb3bc1","origin":[-1.9,12,0],"rotation":[0,0,0]}],"outliner":[{"uuid":"4e6532fe-ce84-c419-21a0-ea8f18f496ea","isOpen":false,"children":[{"uuid":"ad52a49d-e8e6-60c7-7516-956fabbfc049","isOpen":false,"children":["8999805c-abd7-2ce8-14a6-9846e4717bb8","fcf11756-4e93-329a-310b-2341da24e04d",{"uuid":"2f1b3796-7da4-5424-a4fe-63dac803d9aa","isOpen":false,"children":[{"uuid":"22a288dc-9794-d3ae-0dc0-573d1cec010d","isOpen":false,"children":["61c2f984-267f-6acc-3053-3d019509578c","f05a8ebd-58bd-7d2f-3dde-7a9b4cbf2fe2",{"uuid":"f3adc67f-108a-9c2c-39cb-497c29eeb5f8","isOpen":false,"children":["60b80ed0-9426-bb6c-84ea-6d0b835792bc"]}]},{"uuid":"15ab30e5-0505-1caf-5f50-f648af559d65","isOpen":false,"children":["9a22684e-97a9-3e3b-5dea-e638eb72dbe2","c979898d-e8e0-c016-4e63-1cd819ac274e",{"uuid":"abe4540d-7908-c007-6cf6-f21d4bd44c4b","isOpen":false,"children":[]}]}]}]},{"uuid":"30b6ffa4-ad31-4f5e-7837-00e5cecb3bc1","isOpen":false,"children":["5a4a0016-2d2b-6671-2e71-79bb70de3cd6"]}]}]}
                """;
        Geo o = GeoLoader.parse(original);
        Geo c = GeoLoader.parse(BbModel.parse(guardadoPorBlockbench).geo);
        eq("ida y vuelta: huesos", c.bones.length, o.bones.length);
        for (Geo.Bone b : o.bones) {
            int i = c.bone(b.name);
            if (i < 0) {
                fails++;
                System.out.println("FALLO: ida y vuelta perdió el hueso " + b.name);
                continue;
            }
            Geo.Bone x = c.bones[i];
            String n = "ida y vuelta " + b.name;
            eq(n + " pivote", b.px + b.py * 7 + b.pz * 13, x.px + x.py * 7 + x.pz * 13);
            eq(n + " giro", b.rx + b.ry * 7 + b.rz * 13, x.rx + x.ry * 7 + x.rz * 13);
            eq(n + " padre", b.parent < 0 ? -1 : c.bone(o.bones[b.parent].name), x.parent);
            eq(n + " cubos", b.cubes.size(), x.cubes.size());
            for (int j = 0; j < Math.min(b.cubes.size(), x.cubes.size()); j++) {
                Geo.Cube u = b.cubes.get(j), v = x.cubes.get(j);
                float[] m = {u.x0, u.y0, u.z0, u.sx, u.sy, u.sz, u.inflate, u.u, u.v, u.crx, u.cry, u.crz, u.cpx, u.cpy, u.cpz, u.mirror ? 1 : 0};
                float[] w = {v.x0, v.y0, v.z0, v.sx, v.sy, v.sz, v.inflate, v.u, v.v, v.crx, v.cry, v.crz, v.cpx, v.cpy, v.cpz, v.mirror ? 1 : 0};
                for (int k = 0; k < m.length; k++) {
                    eq(n + " cubo " + j + " dato " + k, w[k], m[k]);
                }
                eq(n + " cubo " + j + " caras", u.faces == null ? 0 : 1, v.faces == null ? 0 : 1);
                if (u.faces != null && v.faces != null) {
                    for (int f = 0; f < 6; f++) {
                        eq(n + " cara " + f, u.faces[f] == null ? 0 : 1, v.faces[f] == null ? 0 : 1);
                        if (u.faces[f] != null && v.faces[f] != null) {
                            for (int k = 0; k < 4; k++) {
                                eq(n + " cara " + f + " uv " + k, v.faces[f][k], u.faces[f][k]);
                            }
                        }
                    }
                }
            }
        }
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
