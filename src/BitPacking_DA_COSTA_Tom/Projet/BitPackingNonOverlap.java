package BitPacking_DA_COSTA_Tom.Projet;

public class BitPackingNonOverlap implements BitPacking {
    private int[] data;      // buffer compressé (mots 32 bits)
    private int n;           // nb d'éléments
    private int k;           // bits par valeur
    private int mask;        // (1<<k)-1
    // positions pour accès direct
    private int[] wordIdx;   // mot où commence l'élément i
    private int[] bitOff;    // offset (0..31) où commence l'élément i

    @Override
    public int[] compress(int[] input) {
        this.n = input.length;
        this.k = computeK(input);
        this.mask = (k == 32) ? -1 : ((1 << k) - 1);
        this.wordIdx = new int[n];
        this.bitOff  = new int[n];

        // PASS 1 : calcul des positions + nb de mots nécessaires
        int w = 0;           // index du mot courant
        int off = 0;         // bit offset dans le mot courant (0..31)
        for (int i = 0; i < n; i++) {
            int space = 32 - off;
            if (space < k) { // pas assez de place -> on passe au mot suivant
                w++;
                off = 0;
            }
            wordIdx[i] = w;
            bitOff[i]  = off;

            off += k;
            if (off == 32) { // pile à la fin
                w++;
                off = 0;
            }
        }
        int words = w + (off > 0 ? 1 : 0);
        data = new int[Math.max(1, words)];

        // PASS 2 : écriture des valeurs (aucun débordement par construction)
        for (int i = 0; i < n; i++) {
            int v = input[i] & mask;
            data[wordIdx[i]] |= (v << bitOff[i]);
        }
        return data;
    }

    @Override
    public void decompress(int[] output) {
        if (output.length != n)
            throw new IllegalArgumentException("Output size must be " + n + " but is " + output.length);
        for (int i = 0; i < n; i++) {
            output[i] = get(i);
        }
    }

    @Override
    public int get(int i) {
        if (i < 0 || i >= n) throw new IndexOutOfBoundsException("i=" + i);
        int v = data[wordIdx[i]] >>> bitOff[i];
        return v & mask;
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
