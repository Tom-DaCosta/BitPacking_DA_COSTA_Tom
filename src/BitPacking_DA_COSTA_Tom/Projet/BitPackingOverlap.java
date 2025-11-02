package BitPacking_DA_COSTA_Tom.Projet;

public class BitPackingOverlap implements BitPacking {
    private int[] data;       // buffer compressé
    private int n;            // taille logique (nb d'éléments)
    private int k;            // bits par valeur
    private int mask;         // (1<<k) - 1

    @Override
    public int[] compress(int[] input) {
        this.n = input.length;
        this.k = computeK(input);
        this.mask = (k == 32) ? -1 : ((1 << k) - 1);

        long totalBits = (long) n * k;
        int words = (int) ((totalBits + 31) >>> 5); // ceil(totalBits/32)
        data = new int[Math.max(1, words)];

        long bitPos = 0;
        for (int i = 0; i < n; i++) {
            int v = input[i] & mask;
            int wordIndex = (int) (bitPos >>> 5);
            int bitOffset = (int) (bitPos & 31);

            data[wordIndex] |= (v << bitOffset);

            int spill = bitOffset + k - 32;
            if (spill > 0) {
                // la partie haute déborde dans le mot suivant
                data[wordIndex + 1] |= (v >>> (k - spill));
            }
            bitPos += k;
        }
        return data;
    }

    @Override
    public void decompress(int[] output) {
        if (output.length != n) throw new IllegalArgumentException("bad size");
        for (int i = 0; i < n; i++) output[i] = get(i);
    }

    @Override
    public int get(int i) {
        long bitStart = (long) i * k;
        int wordIndex = (int) (bitStart >>> 5);
        int bitOffset = (int) (bitStart & 31);

        int value = (data[wordIndex] >>> bitOffset);
        int need = bitOffset + k - 32;
        if (need > 0) {
            int hi = data[wordIndex + 1] & ((1 << need) - 1);
            value |= (hi << (k - need));
        }
        return value & mask;
    }

    @Override public int bitsPerValue() { return k; }
    @Override public int size() { return n; }

    private static int computeK(int[] a) {
        int max = 0;
        for (int v : a) if (v > max) max = v;
        if (max == 0) return 1;
        return 32 - Integer.numberOfLeadingZeros(max);
    }
    
}

