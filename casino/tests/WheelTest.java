import com.casino.game.WheelLayout;
import java.util.*;
public class WheelTest {
    public static void main(String[] a) {
        Random r = new Random(7);
        int[] c = new int[20];
        int N = 2_000_000;
        for (int i = 0; i < N; i++) c[WheelLayout.roll(r)]++;
        for (int i = 0; i < 20; i++) { double p = 100.0 * c[i] / N; if (Math.abs(p - 5) > 0.1) throw new RuntimeException("sesgo " + i + " " + p); }
        // la casilla que sale es la que queda bajo el puntero
        for (int s = 0; s < 20; s++) for (double off = -0.35; off <= 0.35; off += 0.05) {
            double ang = WheelLayout.restAngleFor(s, off) + 360 * 6;
            if (WheelLayout.sliceAt(ang) != s) throw new RuntimeException("casilla " + s + " off " + off + " -> " + WheelLayout.sliceAt(ang));
        }
        Map<WheelLayout.Kind,Integer> k = new EnumMap<>(WheelLayout.Kind.class);
        for (var s : WheelLayout.SLICES) k.merge(s.kind(), 1, Integer::sum);
        // no hay dos casillas iguales juntas
        for (int i = 0; i < 20; i++) if (WheelLayout.SLICES[i].kind() == WheelLayout.SLICES[(i+1)%20].kind()) throw new RuntimeException("vecinas iguales " + i);
        System.out.println("RULETA OK " + k);
    }
}
