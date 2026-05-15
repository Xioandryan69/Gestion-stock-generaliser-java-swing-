

## Quoi ??


## Pourquoi ???
chiffre d ' affaire vola $ ???
ohatrinina vidina beksa


### Gestion article 

#### FIFO 

#### LIFO
#### CUMP



| date | qté | PU                    | Voleur                    | Stok | VAleurStock     | CUMP                                                     |
| ---- | --- | --------------------- | ------------------------- | ---- | --------------- | -------------------------------------------------------- |
| 01/5 | 5   | 100                   | 500                       | 5    | 500             | VAleur/qté 100                                           |
| 2    | 10  | 110                   | 1100                      | 15   | 1600            | 106                                                      |
| 3    | 5   | 95                    | 475                       | 20   | 2075            | (VAleurStock1 +VAleur2/Stock2) cout moyen pondere=103.75 |
| 4    | -13 | Dernier CUMP-1=103.75 | 13*Dernier CUMP-1=1348,75 | 7    | 726.5           | 103.75                                                   |
| 5    | 3   | 115                   | 345                       | 10   | CUMP*qt=1071.25 | 107.125                                                  |
|      |     |                       |                           |      |                 |                                                          |


## Sortie

| Date | quantite |
| ---- | -------- |
| 1    | 5        |
| 2    | 8 \|10   |
| 3    | -13      |


FIFO :First IN First OUT 
	BEX

article 
	nom 
		FIFO
	Lasting First Info 

| Date |     | ordre |
| ---- | --- | ----- |
| 2    | 8   | 2     |
| 3    | 5   | 1     |
| 4    |     | -13   |
calcul pour chaque article 
valeur stock global =valeur stock article



CUMP = cumul Valeur stock /quantite stock 

## Demandes 

FEnetre 
+ [ ] saisier produit 
	+ [ ] nom 
	+ [ ] FIFO/...
+ [ ] Mouvement produit 
	+ [ ] Produit 
	+ [ ] type
		+ [ ] Entre
			+ [ ] Prix_Unitaire 
		+ [ ] Sortie 
	+ [ ] quantite 
	+ [ ] VAleur Solde
+ [ ] Etat Stock 
	+ [ ] Date en question comprise ou egale <= 
	+ [ ] Produit 
``` sql


idSource idProduit articele naka 


java.swing 
```


Comment generaliser ?????

String 

generaliser 
technologie ::: java Swing 


``` 
CRUD 
classe 

ajouter 
modifier 
supprimer 


service generaliser 
nouveau 
Liste table ajouter modifier 


```




est ce que c est ok logique 

# Analyse du modèle : Gestion de stock FIFO / LIFO / CUMP en Java Swing

## 1. Quoi ?

Le projet consiste à créer une application de gestion de stock capable de :

- gérer des articles/produits
    
- enregistrer les mouvements d’entrée et sortie
    
- calculer automatiquement :
    
    - FIFO (First In First Out)
        
    - LIFO (Last In First Out)
        
    - CUMP (Coût Unitaire Moyen Pondéré)
        
- afficher l’état du stock à une date donnée
    
- calculer la valeur du stock
    
- calculer les coûts de sortie
    
- produire des états comptables/logistiques
    

Technologie demandée :

- Java Swing
    
- architecture généralisée CRUD
    
- base de données SQL
    

---

# 2. Pourquoi ?

## Objectif métier

Dans une entreprise, le stock représente de l’argent immobilisé.

Exemple :

- achat de marchandises
    
- stockage
    
- vente
    
- calcul du bénéfice réel
    

Sans gestion correcte du stock :

- pertes invisibles
    
- erreurs de prix
    
- faux bénéfices
    
- vols difficiles à détecter
    
- rupture de stock
    
- mauvais calcul du chiffre d’affaires
    

---

## Chiffre d’affaire vs bénéfice

Beaucoup confondent :

- chiffre d’affaire
    
- bénéfice
    

### Chiffre d’affaire

C’est :

```text
Somme totale des ventes
```

Exemple :

- vente = 10 000 000 Ar
    

Alors :

```text
CA = 10 000 000 Ar
```

Mais cela ne veut PAS dire bénéfice.

---

## Bénéfice réel

Le bénéfice dépend du coût réel du stock.

Formule :

```text
Bénéfice = Vente - Coût réel des articles vendus
```

Le problème :

```text
Quel est le coût réel ?
```

C’est là que FIFO / LIFO / CUMP deviennent importants.

---

# 3. Exemple concret

## Entrées

|Date|Quantité|PU|Valeur|
|---|---|---|---|
|01/05|5|100|500|
|02/05|10|110|1100|
|03/05|5|95|475|

Total :

```text
Stock = 20
Valeur = 2075
```

---

## Sortie

Le 04/05 :

```text
Sortie = 13 unités
```

Question :

```text
Quel coût utiliser ?
```

Réponse dépend de la méthode.

---

# 4. FIFO

## Définition

FIFO = First In First Out

Premier entré = premier sorti.

---

## Logique

Les anciens stocks sortent d’abord.

Exemple :

Entrées :

|Ordre|Quantité|PU|
|---|---|---|
|1|5|100|
|2|10|110|
|3|5|95|

Sortie = 13

Alors :

- prendre 5 à 100
    
- puis 8 à 110
    

Calcul :

```text
5×100 = 500
8×110 = 880
Total sortie = 1380
```

Stock restant :

```text
2 à 110
5 à 95
```

Valeur stock :

```text
220 + 475 = 695
```

---

# 5. LIFO

## Définition

LIFO = Last In First Out

Dernier entré = premier sorti.

---

## Exemple

Sortie = 13

On prend :

- 5 à 95
    
- puis 8 à 110
    

Calcul :

```text
5×95 = 475
8×110 = 880
Total = 1355
```

---

# 6. CUMP

## Définition

CUMP = Coût Unitaire Moyen Pondéré

Formule :

genui{"math_block_widget_always_prefetch_v2":{"content":"CUMP=\frac{ValeurStock}{QuantiteStock}"}}

---

## Exemple

Après les 3 entrées :

```text
Valeur = 2075
Quantité = 20
```

Donc :

genui{"math_block_widget_always_prefetch_v2":{"content":"CUMP=\frac{2075}{20}=103.75"}}

---

## Sortie

Sortie = 13

Coût sortie :

genui{"math_block_widget_always_prefetch_v2":{"content":"13\times103.75=1348.75"}}

Stock restant :

```text
20 - 13 = 7
```

Valeur restante :

```text
2075 - 1348.75 = 726.25
```

---

# 7. Structure métier importante

## Produit

Chaque produit possède :

- id
    
- nom
    
- methodeStock
    
    - FIFO
        
    - LIFO
        
    - CUMP
        

---

## Mouvement

Chaque mouvement possède :

- id
    
- produit
    
- type
    
    - ENTREE
        
    - SORTIE
        
- quantité
    
- prixUnitaire
    
- date
    

---

## Règle importante

### Entrée

Une entrée augmente :

- quantité
    
- valeur stock
    

---

### Sortie

Une sortie diminue :

- quantité
    
- valeur stock
    

mais le calcul dépend de la méthode.

---

# 8. Architecture recommandée

## Packages

```text
src/
 ├── model/
 ├── service/
 ├── dao/
 ├── ui/
 ├── util/
 ├── listener/
 └── main/
```

---

# 9. Modèles Java

## Produit.java

```java
public class Produit {
    private int id;
    private String nom;
    private String methodeStock;
}
```

---

## MouvementStock.java

```java
public class MouvementStock {
    private int id;
    private Produit produit;
    private String type;
    private double quantite;
    private double prixUnitaire;
    private Date dateMouvement;
}
```

---

## LigneStock.java

Utilisé surtout pour FIFO/LIFO.

```java
public class LigneStock {
    private int id;
    private Produit produit;
    private double quantiteRestante;
    private double prixUnitaire;
    private Date dateEntree;
}
```

---

# 10. Tables SQL

## produit

```sql
CREATE TABLE produit(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100),
    methode_stock VARCHAR(20)
);
```

---

## mouvement_stock

```sql
CREATE TABLE mouvement_stock(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    id_produit INTEGER,
    type VARCHAR(20),
    quantite DECIMAL(10,2),
    prix_unitaire DECIMAL(10,2),
    date_mouvement DATE
);
```

---

## ligne_stock

```sql
CREATE TABLE ligne_stock(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    id_produit INTEGER,
    quantite_restante DECIMAL(10,2),
    prix_unitaire DECIMAL(10,2),
    date_entree DATE
);
```

---

# 11. Service métier

## Pourquoi Service ?

Le service contient les règles de calcul.

Exemple :

```text
StockService
```

---

## Fonctions importantes

```java
entreeStock()
sortieStock()
calculFIFO()
calculLIFO()
calculCUMP()
getEtatStock()
```

---

# 12. Généralisation CRUD

## Objectif

Éviter de répéter :

- ajouter
    
- modifier
    
- supprimer
    
- liste
    

---

## BaseModel

```java
public abstract class BaseModel {
    protected int id;
}
```

---

## GenericDAO

```java
public interface GenericDAO<T> {
    void save(T obj);
    void update(T obj);
    void delete(int id);
    List<T> findAll();
    T findById(int id);
}
```

---

## Exemple

```java
public class ProduitDAO implements GenericDAO<Produit> {
}
```

---

# 13. Interface Swing

# Fenêtre principale

## Menus

- Produit
    
- Mouvement
    
- Etat stock
    
- Rapport
    

---

# 14. Fenêtre Produit

## Champs

- nom
    
- méthode
    

Méthode :

- FIFO
    
- LIFO
    
- CUMP
    

---

## Boutons

- Ajouter
    
- Modifier
    
- Supprimer
    
- Actualiser
    

---

# 15. Fenêtre Mouvement

## Champs

- produit
    
- type
    
- quantité
    
- prix unitaire
    
- date
    

---

## Cas spécial

### Si sortie

Le prix NE DOIT PAS être saisi.

Car :

```text
le système calcule automatiquement
```

---

# 16. Fenêtre Etat Stock

## Filtres

- date <=
    
- produit
    

---

## Colonnes

- produit
    
- quantité
    
- valeur stock
    
- CUMP
    
- méthode
    

---

# 17. Difficulté importante

## FIFO/LIFO

Le vrai problème est :

```text
comment savoir quelles lignes consommer ?
```

---

## Solution FIFO

```sql
ORDER BY date_entree ASC
```

---

## Solution LIFO

```sql
ORDER BY date_entree DESC
```

---

# 18. Algorithme FIFO

Pseudo-code :

```text
reste = quantité demandée

prendre les lignes stock
ordre ASC

pour chaque ligne :

    si reste == 0
        arrêter

    si ligne.quantité <= reste
        consommer toute la ligne
    sinon
        consommer une partie
```

---

# 19. Etat global du stock

## Formule

genui{"math_block_widget_always_prefetch_v2":{"content":"ValeurStockGlobal=\sum ValeurStockArticle"}}

---

# 20. TO DO détaillé

# Étape 1 — Base de données

## Créer tables

- produit
    
- mouvement_stock
    
- ligne_stock
    

---

# Étape 2 — Modèles Java

Créer :

- Produit
    
- MouvementStock
    
- LigneStock
    

---

# Étape 3 — Connexion DB

Créer :

```java
DBConnection
```

avec :

```java
getConnection()
```

---

# Étape 4 — DAO

Créer :

- GenericDAO
    
- ProduitDAO
    
- MouvementDAO
    
- LigneStockDAO
    

---

# Étape 5 — Service métier

Créer :

```text
StockService
```

Méthodes :

- entreeStock()
    
- sortieStockFIFO()
    
- sortieStockLIFO()
    
- sortieStockCUMP()
    
- getEtatStock()
    

---

# Étape 6 — Swing Produit

Créer :

```text
ProduitFrame
```

Avec :

- JTable
    
- JTextField
    
- JComboBox
    
- JButton
    

---

# Étape 7 — Swing Mouvement

Créer :

```text
MouvementFrame
```

---

# Étape 8 — Swing Etat Stock

Créer :

```text
EtatStockFrame
```

---

# Étape 9 — Calculs automatiques

Faire :

- calcul quantité
    
- calcul valeur stock
    
- calcul CUMP
    
- calcul coût sortie
    

---

# Étape 10 — Rapport

Créer :

- état stock PDF
    
- historique mouvements
    
- valorisation stock
    

---

# 21. Généralisation avancée

Tu peux créer un moteur CRUD générique.

Exemple :

```java
GenericFrame<T>
```

capable de :

- générer formulaire
    
- générer JTable
    
- ajouter
    
- modifier
    
- supprimer
    

à partir de réflexion Java.

---

# 22. Architecture professionnelle

## Version simple

```text
Swing -> Service -> DAO -> SQL
```

---

## Version plus propre

```text
MVC
```

- Model
    
- View
    
- Controller
    

---

# 23. Risques techniques

## Concurrence

Deux utilisateurs peuvent sortir le même stock.

Solution :

- transaction SQL
    
- verrouillage
    

---

## Stock négatif

Interdire :

```text
sortie > stock disponible
```

---

# 24. Conseils importants

## FIFO/LIFO

Ne jamais modifier l’historique.

Toujours :

- ajouter mouvement
    
- recalculer
    

---

## CUMP

Le CUMP change uniquement :

```text
lors des entrées
```

Pas lors des sorties.

---

# 25. Résumé final

Le cœur du projet est :

```text
Valorisation du stock
```

Le vrai niveau difficile n’est PAS Swing.

Le vrai problème est :

- algorithme FIFO
    
- algorithme LIFO
    
- cohérence du stock
    
- calcul valeur
    
- historique
    
- architecture généralisée
    

Swing n’est qu’une interface.

Le cerveau du système est :

```text
StockService
```

comme dit un proverbe industriel :

```text
“Un stock mal calculé crée un bénéfice imaginaire.”
```


------------
Oui, la logique globale est bonne.  
Même très bonne pour un projet universitaire/professionnel de gestion de stock.

Tu touches déjà les vraies problématiques :

- valorisation du stock
    
- comptabilité matière
    
- calcul du coût réel
    
- historique des mouvements
    
- architecture métier
    
- généralisation CRUD
    
- séparation Service / DAO / UI
    

Le projet commence à ressembler à un mini ERP.

Mais il y a quelques corrections importantes à faire pour rendre le modèle solide.

---

# 1. Ce qui est BON dans ton modèle

## A. Séparation Produit / Mouvement

Très bon choix.

Un produit :

- possède une méthode de calcul
    
- possède un stock logique
    

Les mouvements :

- représentent l’historique réel
    

C’est exactement comme dans les logiciels :

- Odoo
    
- Sage
    
- SAP
    
- Dolibarr
    

---

# 2. Très bonne idée : ligne_stock

Ça c’est important :

```java
LigneStock
```

Pourquoi ?

Parce que FIFO/LIFO ont besoin de savoir :

```text
QUEL LOT EST ENCORE DISPONIBLE
```

Exemple :

|Lot|Quantité restante|PU|
|---|---|---|
|Lot1|2|100|
|Lot2|10|110|

Sans cette table :

- FIFO impossible proprement
    
- LIFO impossible proprement
    

Donc :

- excellente idée
    

---

# 3. Correction IMPORTANTE sur CUMP

Tu as presque bon.

Mais attention :

Le CUMP ne se recalcule PAS lors d’une sortie.

Seulement lors des entrées.

Donc :

Après sortie :

- quantité diminue
    
- valeur diminue
    
- MAIS le CUMP reste identique
    

Tu l’as presque compris dans ton tableau.

---

# 4. Ton tableau CUMP corrigé

Après entrée 1 :

CUMP=\frac{500}{5}=100

Après entrée 2 :

CUMP=\frac{1600}{15}=106.67

Après entrée 3 :

CUMP=\frac{2075}{20}=103.75

Sortie 13 :

Coût sortie :

13\times103.75=1348.75

Valeur restante :

2075-1348.75=726.25

Nouveau stock :

```text
7 unités
```

Le CUMP reste :

```text
103.75
```

Puis nouvelle entrée :

```text
3 × 115 = 345
```

Nouveau calcul :

```text
Valeur = 726.25 + 345
       = 1071.25

Quantité = 7 + 3
         = 10
```

Nouveau CUMP :

CUMP=\frac{1071.25}{10}=107.125

Donc :

- ton raisonnement est correct
    

---

# 5. Erreur conceptuelle à corriger

Tu mélanges parfois :

```text
valeur stock
```

et

```text
valeur sortie
```

Important :

|Élément|Signification|
|---|---|
|valeur stock|argent restant dans stock|
|valeur sortie|coût des articles vendus|
|chiffre affaire|argent gagné vente|
|bénéfice|vente - coût sortie|

---

# 6. Très IMPORTANT : prix vente ≠ coût stock

Exemple :

Tu achètes :

- Coca à 1000 Ar
    

Tu vends :

- à 1500 Ar
    

Alors :

|Élément|Valeur|
|---|---|
|achat|1000|
|vente|1500|
|bénéfice|500|

Ton système doit donc séparer :

## A. Stock

```text
prix achat
```

## B. Vente

```text
prix vente
```

Sinon :

- bénéfice faux
    
- chiffre affaire faux
    

---

# 7. Architecture métier correcte

Très bonne architecture :

```text
Swing -> Service -> DAO -> SQL
```

Ça :

- propre
    
- maintenable
    
- extensible
    

---

# 8. Ce qu’il MANQUE encore

## A. Table Vente

Tu as :

- produit
    
- mouvement stock
    

Mais pas encore :

```sql
vente
ligne_vente
```

Or :

- chiffre affaire
    
- bénéfice
    
- facture
    
- client
    

dépendent de ça.

---

# 9. Ce qu’il faut ajouter

## Table vente

```sql
CREATE TABLE vente(
    id INTEGER PRIMARY KEY,
    date_vente DATE,
    total DECIMAL(10,2)
);
```

---

## ligne_vente

```sql
CREATE TABLE ligne_vente(
    id INTEGER PRIMARY KEY,
    id_vente INTEGER,
    id_produit INTEGER,
    quantite DECIMAL(10,2),
    prix_vente DECIMAL(10,2),
    cout_stock DECIMAL(10,2)
);
```

---

# 10. Pourquoi `cout_stock` ?

Parce qu’au moment de la vente :

- FIFO change
    
- LIFO change
    
- CUMP change
    

Donc :

- il faut sauvegarder le coût réel historique
    

Sinon :

- bénéfice historique faux
    

---

# 11. Ce que ton professeur cherche probablement

Le vrai niveau du projet :

PAS :

- JTable
    
- JButton
    
- Swing
    

Mais :

## logique métier

notamment :

- valorisation stock
    
- algorithme FIFO
    
- historique
    
- cohérence SQL
    
- architecture généralisée
    

---

# 12. Généralisation CRUD

Ton idée est excellente.

Tu peux aller jusqu’à :

```java
GenericFrame<T>
GenericDAO<T>
GenericService<T>
```

---

# 13. Niveau supérieur : réflexion Java

Tu peux même générer automatiquement :

- formulaire
    
- JTable
    
- champs
    

à partir des attributs.

Exemple :

```java
Field[] fields = clazz.getDeclaredFields();
```

Là tu entres dans :

- framework
    
- mini ORM
    
- CRUD generator
    

---

# 14. Attention à un problème énorme

## FIFO/LIFO et suppression

NE JAMAIS :

- supprimer un mouvement ancien
    

Sinon :

- tout l’historique devient faux
    

Donc :

- utiliser annulation logique
    
- ou recalcul complet
    

---

# 15. Solution professionnelle

## Mouvement immuable

Principe comptable :

```text
Un mouvement validé ne se modifie plus.
```

On ajoute :

- correction
    
- contre écriture
    

comme en comptabilité réelle.

---

# 16. Structure SQL idéale

Tu es proche de ceci :

```text
Produit
    ↓
MouvementStock
    ↓
LigneStock
    ↓
Valorisation
```

et plus tard :

```text
Vente
Facture
Client
Fournisseur
```

---

# 17. Conclusion technique

Ton modèle est :

- logique
    
- cohérent
    
- extensible
    
- proche d’un vrai ERP
    

Le plus difficile maintenant sera :

1. FIFO/LIFO propre
    
2. historique correct
    
3. éviter stock négatif
    
4. synchronisation SQL
    
5. architecture générique
    

Comme disent souvent les gens en logistique :

> “Le stock est de l’argent qui dort.”

Et en informatique de gestion :

> “Un mauvais stock peut créer un faux bénéfice pendant des années.”


---------------
Oui.  
Cette logique suit bien le calcul du CUMP mobile (CUMP après chaque entrée).  
Globalement ton tableau est cohérent.

Mais il faut corriger quelques détails mathématiques et métier pour éviter les erreurs futures dans le code.

---

# 1. Ton modèle correspond à QUOI exactement ?

Ton calcul correspond à :

```text
CUMP mobile après chaque entrée
```

et non :

```text
CUMP fin de période
```

Ça change beaucoup.

---

# 2. Ton calcul est-il logique ?

Oui.

Tu fais :

- entrée  
    → recalcul CUMP
    
- sortie  
    → utiliser dernier CUMP connu
    

C’est exactement la logique professionnelle.

---

# 3. Analyse ligne par ligne

---

# Ligne 1

|Qté|PU|Valeur|
|---|---|---|
|5|100|500|

CUMP :

CUMP=\frac{500}{5}=100

Correct.

---

# Ligne 2

Nouvelle entrée :

|Qté|PU|Valeur|
|---|---|---|
|10|110|1100|

Total :

```text
Quantité = 15
Valeur = 1600
```

CUMP :

CUMP=\frac{1600}{15}=106.67

Tu as mis :

```text
106
```

Techniquement :

- faux mathématiquement
    
- acceptable si arrondi entier
    

Mais en gestion :

- utiliser DECIMAL(10,2)
    

Donc :

```text
106.67
```

---

# Ligne 3

Nouvelle entrée :

|Qté|PU|Valeur|
|---|---|---|
|5|95|475|

Total :

```text
Valeur = 2075
Quantité = 20
```

CUMP :

CUMP=\frac{2075}{20}=103.75

Correct.

---

# Ligne 4 — Sortie

Sortie :

```text
13 unités
```

Règle CUMP :

```text
on utilise le dernier CUMP connu
```

Donc :

```text
13 × 103.75 = 1348.75
```

Correct.

---

# Nouveau stock

```text
20 - 13 = 7
```

Correct.

---

# Valeur restante

Tu as mis :

```text
726.5
```

Mais le vrai calcul :

2075-1348.75=726.25

Donc :

```text
726.25
```

et NON :

```text
726.5
```

Petite erreur de calcul.

---

# CUMP après sortie

Tu as mis :

```text
103.75
```

Correct.

Le CUMP ne change PAS après sortie.

Très important.

---

# Ligne 5 — Nouvelle entrée

Entrée :

```text
3 × 115 = 345
```

Nouveau stock :

```text
7 + 3 = 10
```

Nouvelle valeur :

726.25+345=1071.25

Correct.

---

# Nouveau CUMP

CUMP=\frac{1071.25}{10}=107.125

Correct.

Souvent on arrondit :

```text
107.13
```

---

# 4. Ce que ton système doit faire EXACTEMENT

Ton moteur doit suivre ceci :

---

# SI ENTREE

## Faire :

```text
nouvelleValeur =
ancienneValeur + (quantité × prix)
```

---

## Puis :

```text
nouvelleQuantité =
ancienneQuantité + quantitéEntrée
```

---

## Puis :

```text
nouveauCUMP =
nouvelleValeur / nouvelleQuantité
```

---

# SI SORTIE

## Faire :

```text
coutSortie =
quantitéSortie × dernierCUMP
```

---

## Puis :

```text
nouvelleValeur =
ancienneValeur - coutSortie
```

---

## Puis :

```text
nouvelleQuantité =
ancienneQuantité - quantitéSortie
```

---

## IMPORTANT

Le CUMP :

```text
reste identique
```

jusqu’à prochaine entrée.

---

# 5. Ton modèle suit-il FIFO ?

NON.

Attention.

Ton tableau suit :

```text
CUMP
```

et PAS FIFO.

---

# 6. Pourquoi FIFO est différent ?

FIFO utilise :

```text
les anciens lots
```

Donc :

|Lot|Quantité|Prix|
|---|---|---|
|lot1|5|100|
|lot2|10|110|
|lot3|5|95|

Sortie 13 :

FIFO :

```text
5 à 100
8 à 110
```

Donc :

```text
500 + 880 = 1380
```

Mais CUMP donnait :

```text
1348.75
```

Donc :

- résultats différents
    
- bénéfices différents
    
- stock restant différent
    

---

# 7. Ton architecture doit donc gérer 3 moteurs différents

## A. FIFO

Utilise :

```text
ligne_stock
```

ordre :

```sql
ASC
```

---

## B. LIFO

Utilise :

```sql
DESC
```

---

## C. CUMP

Utilise :

```text
valeur cumulée
```

sans lignes consommées.

---

# 8. Architecture idéale

## Produit

```java
public class Produit {
    int id;
    String nom;
    MethodeValorisation methode;
}
```

---

# Enum

```java
public enum MethodeValorisation {
    FIFO,
    LIFO,
    CUMP
}
```

Très important.

Éviter :

```java
String
```

car :

- fautes frappe
    
- bugs
    
- sécurité faible
    

---

# 9. Service intelligent

Ton système doit faire :

```java
switch(produit.getMethode()) {

    case FIFO:
        sortieFIFO();
        break;

    case LIFO:
        sortieLIFO();
        break;

    case CUMP:
        sortieCUMP();
        break;
}
```

Ça c’est la vraie architecture métier.

---

# 10. Ce que ton prof veut probablement voir

Pas seulement :

- Swing
    
- JTable
    
- CRUD
    

Mais surtout :

## réflexion métier

et :

## abstraction

Tu commences déjà à faire :

- mini ERP
    
- mini Sage
    
- mini Odoo
    

---

# 11. Conclusion

Oui :

- ton calcul CUMP est logique
    
- la méthode est correcte
    
- l’architecture suit la bonne direction
    

Corrections importantes :

|Élément|Correction|
|---|---|
|106|106.67|
|726.5|726.25|
|String méthode|utiliser Enum|
|FIFO ≠ CUMP|séparer moteurs|

Ton raisonnement devient maintenant :

- comptable
    
- algorithmique
    
- orienté architecture logicielle
    
