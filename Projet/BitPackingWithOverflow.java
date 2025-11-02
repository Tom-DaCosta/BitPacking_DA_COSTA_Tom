package BitPacking_DA_COSTA_Tom.Projet;

import java.util.Arrays;

public class BitPackingWithOverflow implements BitPacking {
    private int[] data; // flux principal bit-packé (flag + payload/index)
    private int[] overflowData; // zone overflow bit-packée
    private int[] overflowVals; // copie brute (utile debug)
    private int n;

    private int kSmall; // bits pour "petits" éléments
    private int p; // bits pour l'index overflow
    private int m; // nb d'éléments en overflow
    private int kBig; // bits par valeur dans overflowData

    // positions pour accès direct au flux principal
    private int[] wordIdx, bitOff, width;
    private boolean[] isOv;
    private int[] ovIndex;

    @Override
    public int[] compress(int[] input) {
        this.n = input.length;
        if (n == 0) {
            kSmall = 1;
            p = 0;
            m = 0;
            kBig = 0;
            data = new int[1];
            overflowData = new int[1];
            overflowVals = new int[0];
            wordIdx = bitOff = width = ovIndex = new int[0];
            isOv = new boolean[0];
            return data;
        }

        // 0) garde-fou (bonus négatifs non traité ici)
        for (int v : input)
            if (v < 0)
                throw new IllegalArgumentException("Négatifs non gérés ici (faire ZigZag ou décalage minValue).");

        int kMax = computeKMax(input);

        // 1) choisir kSmall en minimisant bits totaux (main + overflow compressé)
        int bestK = 1;
        long bestBits = Long.MAX_VALUE;
        for (int k = 1; k <= Math.max(1, kMax); k++) {
            int limit = 1 << Math.min(k, 30);
            int mTmp = 0, maxOv = 0, smallCount = 0;
            long mainBits = 0;

            for (int v : input) {
                if (v < limit) { // small
                    smallCount++;
                    mainBits += 1L + k; // flag(0) + value(k)
                } else { // overflow
                    mTmp++;
                    maxOv = Math.max(maxOv, v);
                    // flag(1) + index(pTmp), pTmp inconnu pour l'instant
                }
            }
            int pTmp = ceilLog2(Math.max(1, mTmp)); // index taille
            mainBits += (long) mTmp * (1 + pTmp);

            int kBigTmp = (mTmp == 0) ? 0 : (32 - Integer.numberOfLeadingZeros(maxOv));
            long overflowBits = (long) mTmp * kBigTmp; // <-- CHANGEMENT: on compresse l'overflow
            long totalBits = mainBits + overflowBits;

            if (totalBits < bestBits) {
                bestBits = totalBits;
                bestK = k;
            }
        }
        this.kSmall = bestK;

        // 2) séparer petits/gros, déterminer p, kBig, remplir tables
        int limit = 1 << Math.min(kSmall, 30);
        this.m = 0;
        int maxOv = 0;
        for (int v : input)
            if (v >= limit) {
                m++;
                maxOv = Math.max(maxOv, v);
            }
        this.p = ceilLog2(Math.max(1, m));
        this.kBig = (m == 0) ? 0 : (32 - Integer.numberOfLeadingZeros(maxOv));

        this.wordIdx = new int[n];
        this.bitOff = new int[n];
        this.width = new int[n];
        this.isOv = new boolean[n];
        this.ovIndex = new int[n];
        Arrays.fill(ovIndex, -1);
        this.overflowVals = new int[m];

        int cursorOv = 0;
        for (int i = 0; i < n; i++) {
            int v = input[i];
            if (v >= limit) { // overflow
                isOv[i] = true;
                ovIndex[i] = cursorOv;
                overflowVals[cursorOv++] = v;
                width[i] = 1 + p; // flag + index
            } else { // small
                isOv[i] = false;
                width[i] = 1 + kSmall; // flag + value
            }
        }

        // 3) allouer/écrire flux principal (chevauchement autorisé)
        long totalBitsMain = 0;
        for (int w : width)
            totalBitsMain += w;
        int wordsMain = (int) ((totalBitsMain + 31) >>> 5);
        data = new int[Math.max(1, wordsMain)];

        long bitPos = 0;
        for (int i = 0; i < n; i++) {
            wordIdx[i] = (int) (bitPos >>> 5);
            bitOff[i] = (int) (bitPos & 31);

            if (!isOv[i]) {
                // flag=0 (implicite) + value(kSmall)
                int v = input[i] & ((kSmall == 32) ? -1 : ((1 << kSmall) - 1));
                writeBits(data, bitPos + 1, kSmall, v);
            } else {
                // flag=1 + index p
                writeBits(data, bitPos, 1, 1);
                if (p > 0)
                    writeBits(data, bitPos + 1, p, ovIndex[i]);
            }
            bitPos += width[i];
        }

        // 4) compresser la zone overflow (kBig bits par valeur)
        if (m == 0) {
            overflowData = new int[1]; // vide mais non nul
        } else {
            long bitsOv = (long) m * kBig;
            int wordsOv = (int) ((bitsOv + 31) >>> 5);
            overflowData = new int[Math.max(1, wordsOv)];

            long pos = 0;
            for (int j = 0; j < m; j++) {
                writeBits(overflowData, pos, kBig, overflowVals[j]);
                pos += kBig;
            }
        }

        return data;
    }

    @Override
    public void decompress(int[] output) {
        if (output.length != n)
            throw new IllegalArgumentException("Output size must be " + n);
        for (int i = 0; i < n; i++)
            output[i] = get(i);
    }

    @Override
    public int get(int i) {
        if (i < 0 || i >= n)
            throw new IndexOutOfBoundsException("i=" + i);
        int w = wordIdx[i], off = bitOff[i];

        int flag = readBits(data, w, off, 1);
        if (flag == 0) {
            return (kSmall == 0) ? 0 : readBits(data, w, off + 1, kSmall);
        } else {
            int idx = (p == 0) ? 0 : readBits(data, w, off + 1, p);
            return readOverflowAt(idx);
        }
    }

    @Override
    public int bitsPerValue() {
        return kSmall;
    } // info: k' (petits)

    @Override
    public int size() {
        return n;
    }

    // --------- helpers bas niveau (écriture/lecture chevauchante) ---------

    private static void writeBits(int[] buf, long bitPos, int width, int value) {
        if (width == 0)
            return;
        int wIdx = (int) (bitPos >>> 5);
        int off = (int) (bitPos & 31);
        buf[wIdx] |= (value << off);
        int spill = off + width - 32;
        if (spill > 0) {
            buf[wIdx + 1] |= (value >>> (width - spill));
        }
    }

    private static int readBits(int[] buf, int wordIdx, int bitOff, int width) {
        if (width == 0)
            return 0;
        int part = (buf[wordIdx] >>> bitOff);
        int spill = bitOff + width - 32;
        if (spill > 0) {
            int hi = buf[wordIdx + 1] & ((1 << spill) - 1);
            part |= (hi << (width - spill));
        }
        int mask = (width == 32) ? -1 : ((1 << width) - 1);
        return part & mask;
    }

    private int readOverflowAt(int idx) {
        if (m == 0)
            return 0;
        long bit = (long) idx * kBig;
        int w = (int) (bit >>> 5);
        int off = (int) (bit & 31);
        return readBits(overflowData, w, off, kBig);
    }

    private static int computeKMax(int[] a) {
        int max = 0;
        for (int v : a)
            max = Math.max(max, v);
        if (max == 0)
            return 1;
        return 32 - Integer.numberOfLeadingZeros(max);
    }

    private static int ceilLog2(int x) {
        if (x <= 1)
            return 0;
        return 32 - Integer.numberOfLeadingZeros(x - 1);
    }

    // --- DEBUG (optionnel) pour le Main
    int[] _debugData() {
        return data;
    }

    int[] _debugOverflowData() {
        return overflowData;
    }

    int[] _debugOverflowVals() {
        return overflowVals;
    }

    int[] _debugWordIdx() {
        return wordIdx;
    }

    int[] _debugBitOff() {
        return bitOff;
    }

    int[] _debugWidth() {
        return width;
    }

    boolean[] _debugIsOv() {
        return isOv;
    }

    int[] _debugOvIndex() {
        return ovIndex;
    }

    int _debugKSmall() {
        return kSmall;
    }

    int _debugP() {
        return p;
    }

    int _debugKBig() {
        return kBig;
    }

    int _debugM() {
        return m;
    }
}
