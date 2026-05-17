-- Création de l'utilisateur (à adapter selon votre environnement)
CREATE USER dev1 WITH PASSWORD 'dev';

-- Accorder tous les privilèges sur la base stock

GRANT ALL PRIVILEGES ON SCHEMA public TO dev1;

-- Donner les droits sur toutes les tables existantes et futures
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO dev1;