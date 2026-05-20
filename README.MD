Je vais créer un petit plan de travail pour structurer l'explication et les étapes suivantes.

Created 5 todos

Progress: j'ai créé un plan et je commence par expliquer les modèles et la couche gestion. Voici l'explication concise et ciblée sur `gestion` et `modele`.

**Modèles (emplacement)**  
- `Produit` : Produit.java  
  - Représente un article (id, nom, éventuellement méthode de valorisation : FIFO/LIFO/CUMP, prix d'achat par défaut, etc.).  
  - Sert d'entité principale référencée par mouvements et lignes de stock.  
- `MouvementStock` : MouvementStock.java  
  - Représente une opération (ENTRÉE ou SORTIE) sur un produit, avec quantité, prixUnitaire (utile pour entrées), date, et référence produit.  
  - Pour une sortie, le prixUnitaire est calculé par la méthode choisie (FIFO/LIFO/CUMP).  
- `LigneStock` : LigneStock.java  
  - Représente un lot restant (quantité disponible d'une entrée), utilisé pour FIFO/LIFO.  
  - Contient : id, produit, qtéRestante, prixUnitaire, dateEntree (permets d'ordonner).

**DAO (emplacement)**  
- ProduitDao.java — CRUD produit.  
- MouvementStockDao.java — insert/find mouvements.  
- LigneStockDao.java — gérer les lignes/lot (rechercher lignes disponibles, mettre à jour qtéRestante).  

Rôle des DAO : isoler accès SQL/DB. Service appelle DAO pour lire/écrire et appliquer logique métier atomique (transaction).

**Service principal — `StockService`**  
- Emplacement : StockService.java  
- Responsabilités :  
  - `entreeStock(MouvementStock m)` : créer mouvement + créer une nouvelle `LigneStock` (qté = mouvement.qty, prix = mouvement.prixUnitaire) ; mettre à jour CUMP si méthode = CUMP.  
  - `sortieStock(MouvementStock m)` : selon méthode du produit, déterminer coût et consommer des `LigneStock` (FIFO/LIFO) ou appliquer CUMP ; créer mouvement sortie et mettre à jour/insérer lignes modifiées.  
  - `getEtatStock(Date d, Produit p)` : somme des `LigneStock` jusqu'à la date (ou appliquer mouvements filtrés) pour obtenir qté et valeur.

Explication des méthodes de valorisation (logique + mapping code)

- FIFO (First-In First-Out)  
  - Logique : consommer d'abord les `LigneStock` les plus anciennes (ORDER BY dateEntree ASC).  
  - Implémentation : DAO retourne liste de lignes triées ASC ; boucle en enlevant qté de chaque ligne jusqu'à satisfaire la sortie ; pour chaque portion consommée, calculer coût = portionQty * ligne.prixUnitaire ; mettre à jour ligne.qteRestante (ou supprimer si 0).  
  - Pseudocode :
    - reste = demandeQty  
    - lignes = LigneStockDao.findByProduitOrderByDateAsc(product)  
    - for l in lignes:
      - prendre = min(l.reste, reste)
      - cout += prendre * l.prixUnitaire
      - l.reste -= prendre
      - resta -= prendre
      - LigneStockDao.update(l)
      - if reste == 0 break
- LIFO (Last-In First-Out)  
  - Logique : consommer d'abord les lignes les plus récentes (ORDER BY dateEntree DESC).  
  - Même algorithme que FIFO mais tri inverse.
- CUMP (Coût Unitaire Moyen Pondéré)  
  - Logique : conserver un coût moyen par produit recalculé seulement lors des entrées. Pour les sorties, utiliser ce coût moyen.  
  - Implémentation : StockService maintient ou calcule CUMP = (valeurStockTotale) / (quantitéStock). À chaque entrée : valeurStock += qté*prix; quantitéStock += qté; CUMP = valeurStock/quantitéStock. À chaque sortie : coûtSortie = qté * CUMP ; quantitéStock -= qté; valeurStock -= coûtSortie. Ne pas modifier CUMP sur sortie.  
  - Pseudocode entrée :
    - valeur += qte * prix
    - qteStock += qte
    - CUMP = valeur / qteStock

Transactions et cohérence  
- Toute sortie doit être faite dans une transaction (read-modify-write) pour éviter concurrence et stock négatif.  
- Vérifier qté disponible avant consommation ; rejeter ou ajuster si insuffisant.

Exemples SQL utiles (DAO)  
- Récupérer lignes dispo FIFO :
  SELECT * FROM ligne_stock WHERE produit_id = ? AND qte_restante > 0 ORDER BY date_entree ASC;
- Mettre à jour ligne :
  UPDATE ligne_stock SET qte_restante = ? WHERE id = ?;
- Supprimer ligne si qte_restante = 0.

Points importants et recommandations  
- Conserver l’historique : ne pas modifier ou supprimer mouvements historiques ; seulement mettre à jour `ligne_stock.qte_restante`.  
- Atomicité : encapsuler `entreeStock` et `sortieStock` dans une transaction DB.  
- Tests : écrire tests unitaires pour chaque méthode (FIFO/LIFO/CUMP) et cas limites (stock insuffisant, sorties partielles, plusieurs lots).  
- Performance : indexer `ligne_stock(produit_id, date_entree)` pour parcours rapide.

Souhaitez-vous que :  
- j’ouvre et commente précisément les fichiers `Produit`, `MouvementStock`, `LigneStock` et `StockService` dans votre repo ?  
- ou que j’implémente un test unitaire d’exemple pour `sortieStock` FIFO ?