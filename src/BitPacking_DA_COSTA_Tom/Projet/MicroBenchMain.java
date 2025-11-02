package BitPacking_DA_COSTA_Tom.Projet;

import java.util.*;

public class MicroBenchMain {
    static int[] genUniform(int n, int max, Random rnd) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++)
            a[i] = rnd.nextInt(max + 1);
        return a;
    }

    static long time(Runnable r, int reps) {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < reps; i++) {
            long t0 = System.nanoTime();
            r.run();
            long dt = System.nanoTime() - t0;
            if (dt < best)
                best = dt;
        }
        return best;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: MicroBenchMain TYPE N MAX");
            return;
        }
        // accepter les noms d'enum (OVERLAP, NON_OVERLAP, OVERFLOW)
        CompressionType type = CompressionType.valueOf(args[0].toUpperCase());
        int N = Integer.parseInt(args[1]);
        int MAX = Integer.parseInt(args[2]);

        Random rnd = new Random(123);
        int[] a = genUniform(N, MAX, rnd);
        BitPacking c = CompressionFactory.create(type);

        // warmups (garder une copie compressée pour info)
        int[] comp = c.compress(a);
        int[] out = new int[N];
        c.decompress(out);

        // si la roundtrip échoue, afficher l'indice et les valeurs concernées
        if (!Arrays.equals(a, out)) {
            int idx = -1;
            for (int i = 0; i < a.length; i++)
                if (a[i] != out[i]) {
                    idx = i;
                    break;
                }
            System.err.println("Roundtrip mismatch at index=" + idx);
            System.err.println("orig=" + a[idx] + "  out=" + out[idx] + "  get=" + c.get(idx));
            System.err.println("size()=" + c.size() + " bitsPerValue()=" + c.bitsPerValue());
            int from = Math.max(0, idx - 5), to = Math.min(a.length - 1, idx + 5);
            for (int i = from; i <= to; i++)
                System.err.println(i + ": orig=" + a[i] + " out=" + out[i] + " get=" + c.get(i));

            // Diagnostic: reconstruire la valeur à partir du tableau compressé
            try {
                int k = c.bitsPerValue();
                long startBit = (long) idx * k;
                int word = (int) (startBit / 32);
                int bit = (int) (startBit % 32);
                System.err.println(
                        "DEBUG: idx=" + idx + " k=" + k + " startBit=" + startBit + " word=" + word + " bit=" + bit);
                if (word >= 0 && word < comp.length) {
                    long w0 = comp[word] & 0xFFFFFFFFL;
                    long recon;
                    if (bit + k <= 32) {
                        recon = (w0 >>> bit) & ((1L << k) - 1L);
                        System.err.println("DEBUG word0=" + Long.toBinaryString(w0) + " recon=" + recon);
                    } else {
                        long w1 = (word + 1 < comp.length) ? (comp[word + 1] & 0xFFFFFFFFL) : 0L;
                        recon = ((w0 >>> bit) | (w1 << (32 - bit))) & ((1L << k) - 1L);
                        System.err.println("DEBUG word0=" + Long.toBinaryString(w0) + " word1="
                                + Long.toBinaryString(w1) + " recon=" + recon);
                    }
                } else {
                    System.err.println("DEBUG: word index out of range for comp");
                }
            } catch (Exception ex) {
                System.err.println("DEBUG: failed to reconstruct: " + ex);
            }
            throw new AssertionError("Roundtrip mismatch at index " + idx);
        }

        long tComp = time(() -> c.compress(a), 5);
        comp = c.compress(a);

        long tDecomp = time(() -> c.decompress(out), 5);
        c.decompress(out);

        Random r = new Random(7);
        int iters = Math.min(100_000, Math.max(10_000, N));
        long tGet = time(() -> {
            long acc = 0;
            for (int i = 0; i < iters; i++)
                acc += c.get(r.nextInt(N));
            if (acc == 42)
                System.out.print("");
        }, 5);

        long unc = (long) N * 4L, compB = (long) comp.length * 4L;
        double ratio = (double) compB / (double) unc;

        System.out.printf(Locale.US,
                "TYPE=%s N=%d MAX=%d  comp=%.3f ms  decomp=%.3f ms  get(1e5)=%.3f ms  size=%d->%d (ratio=%.3f)%n",
                type, N, MAX, tComp / 1e6, tDecomp / 1e6, tGet / 1e6, unc, compB, ratio);
    }
}
