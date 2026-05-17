-- ============================================================
-- RESET COMPLET DE LA BASE stock (PostgreSQL)
-- Désactive les contraintes FK, vide les tables, réinitialise les séquences
-- ============================================================

-- Désactiver les triggers de FK (alternative à FOREIGN_KEY_CHECKS)
-- Nécessite des privilèges superuser, sinon supprimer dans l'ordre.
SET session_replication_role = 'replica';

-- Vider les tables (l'ordre n'a plus d'importance grâce à replication role)
TRUNCATE TABLE personne_telephone CASCADE;
TRUNCATE TABLE telephone CASCADE;
TRUNCATE TABLE personne CASCADE;
TRUNCATE TABLE addresse CASCADE;

-- Réinitialiser les séquences (équivalent AUTO_INCREMENT = 1)
ALTER SEQUENCE addresse_id_seq RESTART WITH 1;
ALTER SEQUENCE telephone_id_seq RESTART WITH 1;

-- Réactiver les contraintes FK
SET session_replication_role = 'origin';