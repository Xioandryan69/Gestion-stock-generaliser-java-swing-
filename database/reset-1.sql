-- ============================================================
-- VIDAGE COMPLET DES TABLES ET RÉINITIALISATION DES SÉQUENCES
-- Module : Gestion de stock (FIFO/LIFO/CUMP)
-- Base   : PostgreSQL
-- ============================================================

-- Désactiver temporairement les contraintes FK (optionnel mais pratique)
--\c stock;
SET session_replication_role = 'replica';

-- ============================================================
-- VIDER LES TABLES DANS L'ORDRE INVERSE DES DÉPENDANCES FK
-- (TRUNCATE ... CASCADE peut aussi fonctionner, mais plus sûr)
-- ============================================================
TRUNCATE TABLE vente CASCADE;
TRUNCATE TABLE cump_historique CASCADE;
TRUNCATE TABLE etat_stock CASCADE;
TRUNCATE TABLE ligne_stock CASCADE;
TRUNCATE TABLE mouvement_stock CASCADE;
TRUNCATE TABLE produit CASCADE;
TRUNCATE TABLE configuration_stock CASCADE;
TRUNCATE TABLE methode_valorisation CASCADE;

-- ============================================================
-- RÉINITIALISER LES SÉQUENCES (SERIAL) À 1
-- ============================================================
ALTER SEQUENCE vente_id_seq RESTART WITH 1;
ALTER SEQUENCE cump_historique_id_seq RESTART WITH 1;
ALTER SEQUENCE etat_stock_id_seq RESTART WITH 1;
ALTER SEQUENCE ligne_stock_id_seq RESTART WITH 1;
ALTER SEQUENCE mouvement_stock_id_seq RESTART WITH 1;
ALTER SEQUENCE produit_id_seq RESTART WITH 1;
ALTER SEQUENCE configuration_stock_id_seq RESTART WITH 1;
ALTER SEQUENCE methode_valorisation_id_seq RESTART WITH 1;

-- ============================================================
-- RÉACTIVER LES CONTRAINTES
-- ============================================================
SET session_replication_role = 'origin';

-- ============================================================
-- FIN : toutes les tables sont vides et les IDs repartent de 1
-- ============================================================