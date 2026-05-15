-- ============================================================
-- DROP COMPLET DU SCHÉMA GESTION DE STOCK
-- Supprime les vues, fonctions et tables dans l'ordre inverse
-- ============================================================

\c stock

DROP VIEW IF EXISTS v_analyse_ventes;
DROP VIEW IF EXISTS v_lots_restants;
DROP VIEW IF EXISTS v_historique_mouvements;
DROP VIEW IF EXISTS v_synthese_stock;

DROP FUNCTION IF EXISTS fn_appliquer_cump(INTEGER, DECIMAL);
DROP FUNCTION IF EXISTS fn_appliquer_lifo(INTEGER, DECIMAL);
DROP FUNCTION IF EXISTS fn_appliquer_fifo(INTEGER, DECIMAL);
DROP FUNCTION IF EXISTS fn_etat_stock_date(INTEGER, DATE);
DROP FUNCTION IF EXISTS fn_calcul_cump_produit(INTEGER);

DROP TABLE IF EXISTS vente CASCADE;
DROP TABLE IF EXISTS cump_historique CASCADE;
DROP TABLE IF EXISTS etat_stock CASCADE;
DROP TABLE IF EXISTS ligne_stock CASCADE;
DROP TABLE IF EXISTS mouvement_stock CASCADE;
DROP TABLE IF EXISTS produit CASCADE;
DROP TABLE IF EXISTS configuration_stock CASCADE;
DROP TABLE IF EXISTS methode_valorisation CASCADE;
