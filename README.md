Projet Bit Packing - M1 2025
Auteur : Tom DA COSTA
===============================

## Prérequis : Java 8+, Git

## Compilation :
```
  mkdir ./bin
  javac -d bin src/BitPacking_DA_COSTA_Tom/Projet/*.java
```

## Exécution :
```
  java -cp bin BitPacking_DA_COSTA_Tom.Projet.Main
  java -cp bin BitPacking_DA_COSTA_Tom.Projet.QuickCheckMain
  java -cp bin BitPacking_DA_COSTA_Tom.Projet.MicroBenchMain OVERLAP 10000 4095
  java -cp bin BitPacking_DA_COSTA_Tom.Projet.MicroBenchMain NON_OVERLAP 10000 4095
  java -cp bin BitPacking_DA_COSTA_Tom.Projet.MicroBenchMain OVERFLOW 10000 4095
```

---

## Description

Ce projet implémente trois variantes de la technique de **Bit Packing**, une méthode de compression sans perte visant à réduire la taille des tableaux d'entiers en les encodant de manière compacte sur un nombre minimal de bits.

Chaque variante explore une approche différente du compromis entre **densité de stockage**, **simplicité de décodage** et **vitesse d’accès**.

### Variantes implémentées

- **OVERLAP**  
  Les entiers peuvent chevaucher deux mots de 32 bits.  
  → Permet une compression optimale, au prix d’une légère complexité supplémentaire lors du décodage.

- **NON_OVERLAP**  
  Aucun entier ne peut être découpé entre deux mots de 32 bits.  
  → Implémentation plus simple et plus rapide, mais avec un léger gaspillage de bits à la fin de chaque mot.

- **OVERFLOW**  
  Combine deux tailles de codage :  
  - `kSmall` pour les petites valeurs,  
  - `kBig` pour les valeurs trop grandes, stockées dans une zone **overflow** séparée.  
  → Choisit automatiquement la configuration la plus compacte en fonction du jeu de données.

---

## Fichiers du projet

| Fichier | Rôle |
|----------|------|
| **BitPacking.java** | Interface commune (`compress`, `decompress`, `get`) |
| **BitPackingOverlap.java** | Implémentation de la méthode **OVERLAP** |
| **BitPackingNonOverlap.java** | Implémentation de la méthode **NON_OVERLAP** |
| **BitPackingWithOverflow.java** | Implémentation de la méthode **OVERFLOW** |
| **CompressionType.java** | Énumération des trois modes |
| **CompressionFactory.java** | Fabrique d’instances selon le type |
| **Main.java** | Programme principal de démonstration et validation |
| **QuickCheckMain.java** | Tests automatiques de fiabilité |
| **MicroBenchMain.java** | Benchmarks de performance (temps et ratio) |

---

## Résultats typiques

Mesures effectuées sur un tableau de `10 000` entiers avec `MAX = 4095` :

| Type | Ratio | Temps de compression | Temps de décompression |
|------|--------|----------------------|------------------------|
| **OVERLAP** | 0.375 | ~0.7 ms | ~0.2 ms |
| **NON_OVERLAP** | 0.500 | ~0.15 ms | ~0.19 ms |
| **OVERFLOW** | 0.375 | ~0.4 ms | ~0.23 ms |

> **Analyse :**  
> - `OVERLAP` offre la meilleure densité (37,5 % de la taille initiale).  
> - `NON_OVERLAP` est le plus rapide mais un peu moins compact.  
> - `OVERFLOW` devient avantageux pour des données contenant quelques très grandes valeurs (“outliers”).

---

## Validation

Tous les tests automatiques (`QuickCheckMain`) se terminent avec succès :

```
QuickCheck OK — cases=XXX in YYY ms
```

Cela confirme la **correction** du codage/décodage et la **cohérence des valeurs obtenues via `get()`** pour les trois variantes.

---

## Conclusion

Le projet démontre que le **bit packing** permet une réduction significative de la taille mémoire (de 50 % à 62,5 %) tout en maintenant des temps de traitement très faibles.  
Les trois approches couvrent un éventail complet de compromis entre performance, compacité et adaptabilité.

---

2025 — Tom DA COSTA | M1 Software Engineering

