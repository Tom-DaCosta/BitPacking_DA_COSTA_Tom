package BitPacking_DA_COSTA_Tom.Projet;

import java.util.Arrays;

public class Main {
    // public static void main(String[] args) {
    // int[] src = {100, 200, 300, 150}; // tu peux changer pour tester
    // BitPacking codec = new BitPackingOverlap();

    // int[] packed = codec.compress(src);

    // // Affichage infos
    // System.out.println("Source: " + Arrays.toString(src));
    // System.out.println("k (bits/valeur): " + codec.bitsPerValue());
    // System.out.println("n (taille): " + codec.size());
    // System.out.println("Mots compressés (int[]): " + Arrays.toString(packed));
    // System.out.println("Taille binaire totale (approx): " + (codec.size() *
    // codec.bitsPerValue()) + " bits");
    // System.out.println("Mots 32 bits utilisés: " + packed.length);

    // // Validation get(i)
    // for (int i = 0; i < src.length; i++) {
    // int gi = codec.get(i);
    // if (gi != src[i]) {
    // throw new AssertionError("get(" + i + ") = " + gi + " != " + src[i]);
    // }
    // System.out.println(gi);
    // }
    // System.out.println("get(i): OK");

    // // Décompression
    // int[] dst = new int[src.length];
    // codec.decompress(dst);
    // System.out.println("Décompressé: " + Arrays.toString(dst));

    // if (!Arrays.equals(src, dst)) {
    // throw new AssertionError("Décompression incorrecte");
    // }
    // System.out.println("Décompression: OK");
    // }

    private static void run(BitPacking codec, int[] src, String label) {
        int[] packed = codec.compress(src);
        System.out.println("== " + label + " ==");
        System.out.println("Source: " + Arrays.toString(src));
        System.out.println("k: " + codec.bitsPerValue() + " bits");
        System.out.println("n: " + codec.size());
        System.out.println("Packed words: " + Arrays.toString(packed));
        System.out.print("Packed words (hex): ");
        for (int w : packed)
            System.out.print(toHex(w) + " ");
        System.out.println();

        System.out.println("Packed words (bin):");
        for (int i = 0; i < packed.length; i++) {
            System.out.println("  word[" + i + "] = " + toBits(packed[i]));
        }

        System.out.println("Positions (OVERLAP):");
        int k = codec.bitsPerValue();
        for (int i = 0; i < src.length; i++) {
            long bitStart = (long) i * k;
            int wIdx = (int) (bitStart >>> 5);
            int off = (int) (bitStart & 31);
            boolean spill = (off + k > 32);
            System.out.printf("  i=%d  -> word=%d  off=%d  span=%s%n",
                    i, wIdx, off, spill ? "CROSSES (split over 2 words)" : "WITHIN");
        }
        System.out.println();

        System.out.println("Words used: " + packed.length);

        // sanity: get(i)
        for (int i = 0; i < src.length; i++) {
            int gi = codec.get(i);
            if (gi != src[i])
                throw new AssertionError(label + " get(" + i + ")=" + gi + " != " + src[i]);
        }
        int[] dst = new int[src.length];
        codec.decompress(dst);
        if (!Arrays.equals(src, dst))
            throw new AssertionError(label + " decompress mismatch");
        System.out.println("OK get/decompress\n");
    }

    public static void main(String[] args) {
        int[] src = { 100, 200, 300, 150 };

        run(new BitPackingOverlap(), src, "OVERLAP");
        run(new BitPackingNonOverlap(), src, "NON_OVERLAP");
        run(CompressionFactory.create(CompressionType.OVERFLOW),
                new int[] { 100, 200, 300, 2047, 1027, 2 }, "OVERFLOW");

    }

    static String toHex(int x) {
        return String.format("0x%08X", x);
    }

    static String toBits(int x) {
        return String.format("%32s", Integer.toBinaryString(x)).replace(' ', '0');
    }

}
