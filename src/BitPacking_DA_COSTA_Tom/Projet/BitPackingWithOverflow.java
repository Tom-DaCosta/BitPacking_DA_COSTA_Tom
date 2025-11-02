package BitPacking_DA_COSTA_Tom.Projet;

public class BitPackingWithOverflow implements BitPacking {
    private int n;
    // kSmall : nombre de bits utilisés pour les valeurs directes dans la
    // zoneprincipale
    private int kSmall;
    // kBig : nombre de bits utilisés pour les valeurs de débordement dans la zone
    // de débordement
    private int kBig;
    // bits utilisés pour indexer les entrées de débordement (ceil(log2(m))) peut
    // être 0 si m<=1
    private int idxBits;
    // nombre d'éléments en débordement
    private int mOverflow;
    // décalage en bits où commence la zone de débordement
    private long overflowStartBit;
    // données compactées
    private int[] comp;
    // décalages en bits par entrée dans la zone principale (pour rendre get(i)
    // O(1))
    private long[] entryBitOffsets;

    public BitPackingWithOverflow() {
        this.n = 0;
        this.kSmall = 0;
        this.kBig = 0;
        this.idxBits = 0;
        this.mOverflow = 0;
        this.overflowStartBit = 0L;
        this.comp = new int[0];
        this.entryBitOffsets = null;
    }

    public BitPackingWithOverflow(int n, int k) {
        this();
        this.n = n;
        this.kSmall = k;
    }

    @Override
    public int[] compress(int[] input) {
        if (input == null)
            return null;
        this.n = input.length;
        if (n == 0) {
            this.comp = new int[0];
            this.entryBitOffsets = null;
            return comp;
        }

        // Calcul des paramètres optimaux
        int globalMax = 0;
        for (int v : input)
            if (v > globalMax)
                globalMax = v;
        int globalMaxBits = (globalMax == 0) ? 1 : (32 - Integer.numberOfLeadingZeros(globalMax));

        java.util.function.IntUnaryOperator idxBitsFor = (m -> {
            if (m <= 1)
                return 0;
            return 32 - Integer.numberOfLeadingZeros(m - 1);
        });

        long bestBits = Long.MAX_VALUE;
        int bestKSmall = Math.max(1, globalMaxBits);
        int bestM = 0;
        int bestIdxBits = 0;
        int bestKBig = globalMaxBits;

        for (int ks = 1; ks <= globalMaxBits; ks++) {
            int m = 0;
            int maxOverflowVal = 0;
            for (int v : input) {
                if (v >= (1 << ks)) { // débordement si nécessite plus de ks bits
                    m++;
                    if (v > maxOverflowVal)
                        maxOverflowVal = v;
                }
            }
            int idxB = idxBitsFor.applyAsInt(m);
            int kBig = (m == 0) ? 0 : ((maxOverflowVal == 0) ? 1 : (32 - Integer.numberOfLeadingZeros(maxOverflowVal)));
            long mainBits = 0L;
            mainBits = (long) (n - m) * (1 + ks) + (long) m * (1 + idxB);
            long overflowBits = (long) m * kBig;
            long total = mainBits + overflowBits;
            if (total < bestBits) {
                bestBits = total;
                bestKSmall = ks;
                bestM = m;
                bestIdxBits = idxB;
                bestKBig = kBig;
            }
        }

        // Param choisis
        this.kSmall = bestKSmall;
        this.mOverflow = bestM;
        this.idxBits = bestIdxBits;
        this.kBig = bestKBig;

        // Prép de la liste de débordement + calcul de la taille totale
        java.util.ArrayList<Integer> overflowList = new java.util.ArrayList<>(mOverflow);
        long mainBitsLen = 0;
        for (int v : input) {
            if (v >= (1 << kSmall)) {
                mainBitsLen += 1 + idxBits;
                overflowList.add(v);
            } else {
                mainBitsLen += 1 + kSmall;
            }
        }

        long overflowBitsLen = (long) overflowList.size() * kBig;
        long totalBits = mainBitsLen + overflowBitsLen;
        int words = (int) ((totalBits + 31) / 32);
        if (words < 0)
            words = 0;
        comp = new int[words];

        BitWriter bw = new BitWriter(comp);
        // prép des décalages par entrée
        this.entryBitOffsets = new long[n];
        // écriture de la zone principale
        int overflowCounter = 0;
        for (int i = 0; i < n; i++) {
            int v = input[i];
            // décalage en bits de cette entrée dans la zone principale
            entryBitOffsets[i] = bw.getBitPos();
            if (v >= (1 << kSmall)) {
                // bit indicateur = 1
                bw.writeBits(1L, 1);
                if (idxBits > 0)
                    bw.writeBits(overflowCounter, idxBits);
                overflowCounter++;
            } else {
                // bit indicateur = 0 + valeur
                bw.writeBits(0L, 1);
                bw.writeBits(v & ((1L << kSmall) - 1), kSmall);
            }
        }

        this.overflowStartBit = bw.getBitPos();

        for (int v : overflowList) {
            if (kBig > 0)
                bw.writeBits(v & ((1L << kBig) - 1), kBig);
        }

        return comp;
    }

    @Override
    public int get(int idx) {
        if (idx < 0 || idx >= n)
            throw new IndexOutOfBoundsException();
        BitReader br = new BitReader(comp);
        if (entryBitOffsets != null) {
            long pos = entryBitOffsets[idx];
            long flag = br.readBitsAt(pos, 1);
            if (flag == 0) {
                return (int) br.readBitsAt(pos + 1, kSmall);
            } else {
                int index = (idxBits > 0) ? (int) br.readBitsAt(pos + 1, idxBits) : 0;
                long overflowBitPos = overflowStartBit + (long) index * kBig;
                return (int) br.readBitsAt(overflowBitPos, kBig);
            }
        }
        // méthode naïve O(n) si entryBitOffsets non initialisé
        int overflowIndex = 0;
        for (int i = 0; i <= idx; i++) {
            long flag = br.readBits(1);
            if (i == idx) {
                if (flag == 0) {
                    long val = br.readBits(kSmall);
                    return (int) val;
                } else {
                    int index = (idxBits > 0) ? (int) br.readBits(idxBits) : 0;
                    long overflowBitPos = overflowStartBit + (long) index * kBig;
                    return (int) br.readBitsAt(overflowBitPos, kBig);
                }
            } else {
                if (flag == 0) {
                    br.skipBits(kSmall);
                } else {
                    br.skipBits(idxBits);
                    overflowIndex++;
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }

    @Override
    public void decompress(int[] output) {
        if (output == null)
            return;
        if (output.length < n)
            throw new IllegalArgumentException("output too small");
        //
        for (int i = 0; i < n; i++)
            output[i] = get(i);
    }

    @Override
    public int bitsPerValue() {
        if (n == 0)
            return 0;
        long m = mOverflow;
        // mainBits = (n-m)*(1 + kSmall) + m*(1 + idxBits)
        long mainBits = (long) (n - m) * (1 + kSmall) + (long) m * (1 + idxBits);
        long overflowBits = (long) m * kBig;
        long totalBits = mainBits + overflowBits;
        return (int) Math.ceil((double) totalBits / n);
    }

    @Override
    public int size() {
        return n;
    }

    // Classes internes pour écriture/lecture de bits
    private static class BitWriter {
        private final int[] data;
        private long bitPos = 0;

        BitWriter(int[] data) {
            this.data = data;
        }

        void writeBits(long value, int bits) {
            if (bits == 0)
                return;
            int pos = (int) (bitPos >>> 5); // index du mot
            int off = (int) (bitPos & 31);
            bitPos += bits;
            while (bits > 0) {
                int space = 32 - off;
                int take = Math.min(space, bits);
                long part = value & ((1L << take) - 1);
                // placer la partie à l'offset off dans data[pos]
                long cur = (data[pos] & 0xFFFFFFFFL);
                cur |= (part << off) & 0xFFFFFFFFL;
                data[pos] = (int) cur;
                value >>>= take;
                bits -= take;
                pos++;
                off = 0;
            }
        }

        long getBitPos() {
            return bitPos;
        }
    }

    private static class BitReader {
        private final int[] data;
        private long bitPos = 0;

        BitReader(int[] data) {
            this.data = data;
        }

        long readBits(int bits) {
            if (bits == 0)
                return 0L;
            long val = 0L;
            int shift = 0;
            while (bits > 0) {
                int pos = (int) (bitPos >>> 5);
                int off = (int) (bitPos & 31);
                int avail = Math.min(32 - off, bits);
                long w = (pos < data.length) ? (data[pos] & 0xFFFFFFFFL) : 0L;
                long part = (w >>> off) & ((1L << avail) - 1);
                val |= (part << shift);
                bitPos += avail;
                bits -= avail;
                shift += avail;
            }
            return val;
        }

        long readBitsAt(long atBitPos, int bits) {
            long saved = bitPos;
            bitPos = atBitPos;
            long v = readBits(bits);
            bitPos = saved;
            return v;
        }

        void skipBits(int bits) {
            bitPos += bits;
        }
    }
}
