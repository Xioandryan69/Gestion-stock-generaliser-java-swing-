-- ============================================================
-- BASE DE DONNÉES : stock
-- Projet : Gestion_Stock (framework CRUD dynamique Java)
-- ============================================================

CREATE DATABASE stock OWNER dev1;
\c stock
GRANT ALL ON SCHEMA public TO dev1;


-- ============================================================
-- BASE DE DONNÉES : stock (PostgreSQL)
-- ============================================================

-- Création de la base (à exécuter séparément si besoin)
-- CREATE DATABASE stock WITH ENCODING 'UTF8';

-- Connexion à la base (à faire via votre client ou en ligne de commande)
-- \c stock;

-- ============================================================
-- TABLE : addresse
-- ============================================================
CREATE TABLE IF NOT EXISTS addresse (
    id          SERIAL PRIMARY KEY,
    ville       VARCHAR(100) NOT NULL,
    codePostal  VARCHAR(20)  NOT NULL
);

-- ============================================================
-- TABLE : telephone
-- ============================================================
CREATE TABLE IF NOT EXISTS telephone (
    id          SERIAL PRIMARY KEY,
    numero      VARCHAR(30) NOT NULL,
    type        VARCHAR(20) NOT NULL    -- mobile, fixe, fax
);

-- ============================================================
-- TABLE : personne
-- ============================================================
CREATE TABLE IF NOT EXISTS personne (
    id                    VARCHAR(50) NOT NULL PRIMARY KEY,
    nom                   VARCHAR(100) NOT NULL,
    age                   INTEGER NOT NULL DEFAULT 0,
    role                  VARCHAR(20) NOT NULL DEFAULT 'CLIENT',
    adresse_id            INTEGER DEFAULT NULL,
    infosSupplementaires  JSONB DEFAULT NULL
);

-- ============================================================
-- TABLE : personne_telephone
-- ============================================================
CREATE TABLE IF NOT EXISTS personne_telephone (
    personne_id   VARCHAR(50) NOT NULL,
    telephone_id  INTEGER NOT NULL,
    PRIMARY KEY (personne_id, telephone_id),
    CONSTRAINT fk_pers_tel_personne   FOREIGN KEY (personne_id)  REFERENCES personne(id)   ON DELETE CASCADE,
    CONSTRAINT fk_pers_tel_telephone  FOREIGN KEY (telephone_id) REFERENCES telephone(id)  ON DELETE CASCADE
);

-- ============================================================
-- CONTRAINTE FK : personne → addresse
-- ============================================================
ALTER TABLE personne
    ADD CONSTRAINT fk_personne_adresse
    FOREIGN KEY (adresse_id) REFERENCES addresse(id)
    ON DELETE SET NULL;

-- ============================================================
-- DONNÉES DE TEST
-- ============================================================

INSERT INTO addresse (ville, codePostal) VALUES
    ('Antananarivo', '101'),
    ('Fianarantsoa', '301'),
    ('Toamasina',    '501');

INSERT INTO telephone (numero, type) VALUES
    ('034 00 000 01', 'mobile'),
    ('022 000 001',   'fixe'),
    ('034 00 000 02', 'mobile');

INSERT INTO personne (id, nom, age, role, adresse_id, infosSupplementaires) VALUES
    ('1', 'Alice', 30, 'ADMINISTRATEUR', 1, '{"langue":"fr","niveau":3}'),
    ('2', 'Bob',   25, 'CLIENT',          2, NULL),
    ('3', 'Cara',  22, 'INVITE',          3, '{"notes":"stagiaire"}');

INSERT INTO personne_telephone (personne_id, telephone_id) VALUES
    ('1', 1),
    ('1', 2),
    ('2', 3);

-- ============================================================
-- VÉRIFICATION (équivalent PostgreSQL)
-- ============================================================
SELECT
    p.id,
    p.nom,
    p.age,
    p.role,
    a.ville,
    a.codePostal,
    string_agg(t.numero, ' / ') AS telephones,
    p.infosSupplementaires
FROM personne p
LEFT JOIN addresse         a  ON a.id = p.adresse_id
LEFT JOIN personne_telephone pt ON pt.personne_id = p.id
LEFT JOIN telephone         t  ON t.id = pt.telephone_id
GROUP BY p.id, p.nom, p.age, p.role, a.ville, a.codePostal, p.infosSupplementaires;