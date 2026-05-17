-- Connexion à la base stock
\c stock;

-- Donner tous les droits sur toutes les tables existantes du schéma public
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO dev1;

-- Donner tous les droits sur toutes les séquences existantes
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO dev1;

-- (Optionnel) Donner les droits sur les fonctions
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO dev1;

-- Définir les privilèges par défaut pour les futures tables créées par n'importe quel utilisateur
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO dev1;

-- Idem pour les séquences futures
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO dev1;

-- (Optionnel) Pour les fonctions futures
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO dev1;

GRANT CREATE ON SCHEMA public TO dev1;