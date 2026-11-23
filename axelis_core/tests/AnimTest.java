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
        if (fails > 0) {
            System.out.println("AnimTest: " + fails + " fallos");
            System.exit(1);
        }
        System.out.println("AnimTest: todo bien");
    }
}
