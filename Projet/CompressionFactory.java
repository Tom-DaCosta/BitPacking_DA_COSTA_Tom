package BitPacking_DA_COSTA_Tom.Projet;

public final class CompressionFactory {
    private CompressionFactory() {
    }

    public static BitPacking create(CompressionType type) {
        switch (type) {
            case OVERLAP:
                return new BitPackingOverlap();
            case NON_OVERLAP:
                return new BitPackingNonOverlap();
            case OVERFLOW:
                return new BitPackingWithOverflow();
            default:
                throw new IllegalArgumentException("Unknown CompressionType: " + type);
        }
    }
}
