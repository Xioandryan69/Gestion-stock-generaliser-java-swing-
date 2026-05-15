-- ============================================================
-- DONNÉES DE TEST - GESTION DE STOCK
-- À exécuter après schema.sql
-- ============================================================

--\c stock;

INSERT INTO produit (nom, description, methode_valorisation, code_interne, actif) VALUES
	('Coca-Cola 1.5L', 'Boisson gazeuse', 'FIFO', 'COCA-1.5L', TRUE),
	('Riz Blanc 1Kg', 'Riz blanc de qualité', 'LIFO', 'RIZ-1KG', TRUE),
	('Savon Lux 100g', 'Savon de toilette', 'CUMP', 'LUX-100', TRUE),
	('Huile Palme 2L', 'Huile de cuisson', 'FIFO', 'HUILE-2L', TRUE),
	('Sucre Cristal 1Kg', 'Sucre cristallisé', 'CUMP', 'SUCRE-1KG', TRUE)
ON CONFLICT (nom) DO NOTHING;

INSERT INTO mouvement_stock (id_produit, type_mouvement, quantite, prix_unitaire, reference_achat, date_mouvement)
SELECT id, 'ENTREE', 50, 1000, 'FACTURE-001', DATE '2024-01-05' FROM produit WHERE code_interne = 'COCA-1.5L'
UNION ALL
SELECT id, 'ENTREE', 50, 1100, 'FACTURE-001', DATE '2024-01-05' FROM produit WHERE code_interne = 'COCA-1.5L'
UNION ALL
SELECT id, 'ENTREE', 100, 500, 'FACTURE-002', DATE '2024-01-10' FROM produit WHERE code_interne = 'RIZ-1KG'
UNION ALL
SELECT id, 'ENTREE', 75, 8000, 'FACTURE-003', DATE '2024-01-12' FROM produit WHERE code_interne = 'HUILE-2L'
UNION ALL
SELECT id, 'ENTREE', 200, 200, 'FACTURE-004', DATE '2024-01-15' FROM produit WHERE code_interne = 'LUX-100'
UNION ALL
SELECT id, 'ENTREE', 60, 1050, 'FACTURE-001-B', DATE '2024-02-01' FROM produit WHERE code_interne = 'COCA-1.5L';

INSERT INTO ligne_stock (id_produit, id_mouvement_entree, quantite_initiale, quantite_restante, prix_unitaire, date_entree)
SELECT ms.id_produit, ms.id, ms.quantite, ms.quantite, ms.prix_unitaire, ms.date_mouvement
FROM mouvement_stock ms
WHERE ms.type_mouvement = 'ENTREE'
ORDER BY ms.id;

INSERT INTO mouvement_stock (id_produit, type_mouvement, quantite, prix_unitaire, reference_vente, date_mouvement)
SELECT id, 'SORTIE', 30, 0, 'VENTE-001', DATE '2024-02-05' FROM produit WHERE code_interne = 'COCA-1.5L'
UNION ALL
SELECT id, 'SORTIE', 50, 0, 'VENTE-002', DATE '2024-02-10' FROM produit WHERE code_interne = 'RIZ-1KG'
UNION ALL
SELECT id, 'SORTIE', 40, 0, 'VENTE-003', DATE '2024-02-15' FROM produit WHERE code_interne = 'HUILE-2L';

INSERT INTO vente (id_produit, quantite, prix_vente_unitaire, cout_unitaire_reel, date_vente, n_facture)
SELECT id, 30, 1500, 1050, DATE '2024-02-05', 'FACT-VENTE-001' FROM produit WHERE code_interne = 'COCA-1.5L'
UNION ALL
SELECT id, 50, 700, 550, DATE '2024-02-10', 'FACT-VENTE-002' FROM produit WHERE code_interne = 'RIZ-1KG'
UNION ALL
SELECT id, 40, 10000, 8000, DATE '2024-02-15', 'FACT-VENTE-003' FROM produit WHERE code_interne = 'HUILE-2L';
