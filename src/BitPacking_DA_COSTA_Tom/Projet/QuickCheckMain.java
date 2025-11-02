package BitPacking_DA_COSTA_Tom.Projet;

import java.util.*;

public class QuickCheckMain {
    static int[] gen(int n, int max, Random rnd) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++)
            a[i] = rnd.nextInt(max + 1);
        return a;
    }

    static void roundtrip(CompressionType type, int[] a) {
        BitPacking c = CompressionFactory.create(type);
        c.compress(a);
        int[] out = new int[a.length];
        c.decompress(out);
        if (!Arrays.equals(a, out))
            throw new AssertionError(type + " decompress mismatch");
        Random r = new Random(123);
        for (int i = 0; i < Math.min(1000, a.length); i++) {
            int idx = r.nextInt(a.length);
            if (a[idx] != c.get(idx))
                throw new AssertionError(type + " get mismatch at " + idx);
        }
    }

    public static void main(String[] args) {
        CompressionType[] types = { CompressionType.OVERLAP, CompressionType.NON_OVERLAP, CompressionType.OVERFLOW };
        long start = System.currentTimeMillis();
        Random rnd = new Random(42);
        int[] Ns = { 1, 2, 3, 31, 32, 33, 100, 999, 1000, 8192, 10000 };
        int[] maxs = { 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 4095 };

        int cases = 0;
        for (int N : Ns) {
            for (int max : maxs) {
                int[] a = gen(N, max, rnd);
                for (CompressionType t : types) {
                    roundtrip(t, a);
                    cases++;
                }
            }
        }
        long ms = System.currentTimeMillis() - start;
        System.out.println("QuickCheck OK ; cases=" + cases + " in " + ms + " ms");

        // Test de l'exemple de la consigne
        int[] example = { 1, 2, 3, 1024, 4, 5, 2048 };
        roundtrip(CompressionType.OVERFLOW, example);
        System.out.println("Example from spec: OK");
    }
}
