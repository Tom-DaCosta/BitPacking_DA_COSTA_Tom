package BitPacking_DA_COSTA_Tom.Projet;

public interface BitPacking {
    int[] compress(int[] input);     // compresse et retourne le tableau d’ints compressés
    void decompress(int[] output);   // remplit le tableau output avec les valeurs originales
    int get(int i);                  // accès direct à l’élément i
    int bitsPerValue();              // k (nb de bits par valeur)
    int size();                      // nb d’éléments dans le tableau source
}
