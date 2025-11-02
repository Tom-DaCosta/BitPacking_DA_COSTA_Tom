package BitPacking_DA_COSTA_Tom.Projet;

public class BitPackingWithOverflow implements BitPacking {
    private int n;
    private int k;
    private int[] comp;

    // constructeur sans argument — utilisé par CompressionFactory.create()
    public BitPackingWithOverflow() {
        this.n = 0;
        this.k = 0;
    }

    public BitPackingWithOverflow(int n, int k) {
        this.n = n;
        this.k = k;
    }

    @Override
    public int[] compress(int[] input) {
        // si nécessaire, déterminer n et k à partir des données
        if (this.n != input.length || this.k == 0) {
            this.n = input.length;
            int max = 0;
            for (int v : input)
                if (v > max)
                    max = v;
            this.k = (max == 0) ? 1 : (32 - Integer.numberOfLeadingZeros(max));
        }

        long totalBits = (long) n * k;
        int words = (int) ((totalBits + 31) / 32);
        comp = new int[words];
        long mask = (k >= 64) ? -1L : ((1L << k) - 1L);

        for (int i = 0; i < n; i++) {
            long val = input[i] & mask;
            long start = (long) i * k;
            int word = (int) (start >>> 5); // start / 32
            int bit = (int) (start & 31); // start % 32

            if (bit + k <= 32) {
                long w = (comp[word] & 0xFFFFFFFFL) | ((val << bit) & 0xFFFFFFFFL);
                comp[word] = (int) w;
            } else {
                long low = (val << bit) & 0xFFFFFFFFL;
                comp[word] = (int) ((comp[word] & 0xFFFFFFFFL) | low);
                long high = (val >>> (32 - bit)) & 0xFFFFFFFFL;
                comp[word + 1] = (int) ((comp[word + 1] & 0xFFFFFFFFL) | high);
            }
        }
        return comp;
    }

    @Override
    public int get(int idx) {
        long start = (long) idx * k;
        int word = (int) (start >>> 5);
        int bit = (int) (start & 31);
        long mask = (k >= 64) ? -1L : ((1L << k) - 1L);

        long w0 = (word < comp.length) ? (comp[word] & 0xFFFFFFFFL) : 0L;
        if (bit + k <= 32) {
            return (int) ((w0 >>> bit) & mask);
        } else {
            long w1 = (word + 1 < comp.length) ? (comp[word + 1] & 0xFFFFFFFFL) : 0L;
            long val = ((w0 >>> bit) | (w1 << (32 - bit))) & mask;
            return (int) val;
        }
    }

    @Override
    public void decompress(int[] output) {
        for (int i = 0; i < n; i++)
            output[i] = get(i);
    }

    @Override
    public int bitsPerValue() {
        return k;
    }

    @Override
    public int size() {
        return n;
    }
}
