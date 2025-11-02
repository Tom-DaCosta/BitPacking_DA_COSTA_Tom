===============================
Projet Bit Packing - M1 2025
Auteur : Tom DA COSTA
===============================

Langage : Java 8+

Compilation :

  javac -d bin src/BitPacking_DA_COSTA_Tom/Projet/*.java

Exécution :
  java -cp bin BitPacking_DA_COSTA_Tom.Projet.Main
  java -cp bin BitPacking_DA_COSTA_Tom.Projet.QuickCheckMain
  java -cp bin BitPacking_DA_COSTA_Tom.Projet.MicroBenchMain OVERLAP 10000 4095
  java -cp bin BitPacking_DA_COSTA_Tom.Projet.MicroBenchMain NON_OVERLAP 10000 4095
  java -cp bin BitPacking_DA_COSTA_Tom.Projet.MicroBenchMain OVERFLOW 10000 4095

Description :
  Implémentation de trois variantes de Bit Packing :
    - OVERLAP : entiers peuvent chevaucher deux mots de 32 bits.
    - NON_OVERLAP : pas de chevauchement, mais perte potentielle de bits.
    - OVERFLOW : combine kSmall et kBig avec une zone séparée pour les valeurs trop grandes.

Fichiers :
  BitPacking.java                Interface
  BitPackingOverlap.java         Implémentation OVERLAP
  BitPackingNonOverlap.java      Implémentation NON_OVERLAP
  BitPackingWithOverflow.java    Implémentation OVERFLOW
  CompressionType.java           Enum
  CompressionFactory.java        Fabrique
  Main.java                      Démonstration et validation
  QuickCheckMain.java            Tests automatiques
  MicroBenchMain.java            Benchmark de performance

Résultats typiques :
  OVERLAP : ratio 0.375, comp 0.7 ms
  NON_OVERLAP : ratio 0.5, comp 0.15 ms
  OVERFLOW : ratio 0.375, comp 0.4 ms

Tous les tests (QuickCheck) réussissent.
