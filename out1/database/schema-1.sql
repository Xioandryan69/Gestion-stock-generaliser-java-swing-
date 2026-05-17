-- ============================================================
-- GESTION DE STOCK - FIFO / LIFO / CUMP
-- Schéma : tables, contraintes, fonctions et vues
-- Base de données PostgreSQL
-- ============================================================

--\c stock
-- ============================================================
-- TABLE DE RÉFÉRENCE : methode_valorisation
-- ============================================================
CREATE TABLE IF NOT EXISTS methode_valorisation (
    code VARCHAR(20) PRIMARY KEY,
    label VARCHAR(50) NOT NULL
);

-- ============================================================
-- TABLE 1 : produit
-- ============================================================
CREATE TABLE IF NOT EXISTS produit (
    id                   SERIAL PRIMARY KEY,
    nom                  VARCHAR(255) NOT NULL UNIQUE,
    description          TEXT,
    methode_valorisation VARCHAR(20) NOT NULL DEFAULT 'CUMP',
    code_interne         VARCHAR(50) UNIQUE,
    poids_kg             DECIMAL(10, 2),
    volume_m3            DECIMAL(10, 2),
    actif                BOOLEAN DEFAULT TRUE,
    date_creation        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_produit_methode CHECK (methode_valorisation IN ('FIFO', 'LIFO', 'CUMP')),
    CONSTRAINT ck_produit_nom_non_vide CHECK (nom <> '')
);

CREATE INDEX IF NOT EXISTS idx_produit_nom ON produit(nom);
CREATE INDEX IF NOT EXISTS idx_produit_code_interne ON produit(code_interne);

-- ============================================================
-- TABLE 2 : mouvement_stock
-- ============================================================
CREATE TABLE IF NOT EXISTS mouvement_stock (
    id              SERIAL PRIMARY KEY,
    id_produit      INTEGER NOT NULL,
    type_mouvement  VARCHAR(20) NOT NULL,
    quantite        DECIMAL(15, 4) NOT NULL CHECK (quantite > 0),
    prix_unitaire   DECIMAL(15, 4) NOT NULL CHECK (prix_unitaire >= 0),
    valeur_total    DECIMAL(18, 2) GENERATED ALWAYS AS (quantite * prix_unitaire) STORED,
    reference_achat VARCHAR(100),
    reference_vente VARCHAR(100),
    date_mouvement  DATE NOT NULL DEFAULT CURRENT_DATE,
    date_creation   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut          VARCHAR(20) DEFAULT 'VALIDÉ',
    notes           TEXT,
    CONSTRAINT fk_mvt_produit FOREIGN KEY (id_produit) REFERENCES produit(id) ON DELETE RESTRICT,
    CONSTRAINT ck_mvt_type CHECK (type_mouvement IN ('ENTREE', 'SORTIE')),
    CONSTRAINT ck_mvt_date CHECK (date_mouvement <= CURRENT_DATE)
);

CREATE INDEX IF NOT EXISTS idx_mouvement_produit ON mouvement_stock(id_produit);
CREATE INDEX IF NOT EXISTS idx_mouvement_date ON mouvement_stock(date_mouvement);
CREATE INDEX IF NOT EXISTS idx_mouvement_type ON mouvement_stock(type_mouvement);

-- ============================================================
-- TABLE 3 : ligne_stock
-- ============================================================
CREATE TABLE IF NOT EXISTS ligne_stock (
    id                  SERIAL PRIMARY KEY,
    id_produit          INTEGER NOT NULL,
    id_mouvement_entree INTEGER NOT NULL,
    quantite_initiale   DECIMAL(15, 4) NOT NULL CHECK (quantite_initiale > 0),
    quantite_restante   DECIMAL(15, 4) NOT NULL CHECK (quantite_restante >= 0),
    prix_unitaire       DECIMAL(15, 4) NOT NULL CHECK (prix_unitaire >= 0),
    valeur_restante     DECIMAL(18, 2) GENERATED ALWAYS AS (quantite_restante * prix_unitaire) STORED,
    date_entree         DATE NOT NULL,
    date_creation       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_consommation   TIMESTAMP,
    CONSTRAINT fk_ligne_produit FOREIGN KEY (id_produit) REFERENCES produit(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ligne_mouvement FOREIGN KEY (id_mouvement_entree) REFERENCES mouvement_stock(id) ON DELETE RESTRICT,
    CONSTRAINT ck_ligne_qte CHECK (quantite_restante <= quantite_initiale)
);

CREATE INDEX IF NOT EXISTS idx_ligne_produit ON ligne_stock(id_produit);
CREATE INDEX IF NOT EXISTS idx_ligne_date_entree ON ligne_stock(date_entree);
CREATE INDEX IF NOT EXISTS idx_ligne_qte_restante ON ligne_stock(quantite_restante);

-- ============================================================
-- TABLE 4 : etat_stock
-- ============================================================
CREATE TABLE IF NOT EXISTS etat_stock (
    id                SERIAL PRIMARY KEY,
    id_produit        INTEGER NOT NULL,
    date_etat         DATE NOT NULL,
    quantite_total    DECIMAL(15, 4) DEFAULT 0,
    valeur_stock_total DECIMAL(18, 2) DEFAULT 0,
    cump_jour         DECIMAL(15, 4),
    date_creation     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    calculé_à         TIMESTAMP,
    CONSTRAINT fk_etat_produit FOREIGN KEY (id_produit) REFERENCES produit(id) ON DELETE CASCADE,
    CONSTRAINT uk_etat_produit_date UNIQUE (id_produit, date_etat)
);

CREATE INDEX IF NOT EXISTS idx_etat_produit_date ON etat_stock(id_produit, date_etat);

-- ============================================================
-- TABLE 5 : cump_historique
-- ============================================================
CREATE TABLE IF NOT EXISTS cump_historique (
    id             SERIAL PRIMARY KEY,
    id_produit     INTEGER NOT NULL,
    id_mouvement   INTEGER NOT NULL,
    cump_ancien    DECIMAL(15, 4),
    cump_nouveau   DECIMAL(15, 4) NOT NULL,
    quantite_stock DECIMAL(15, 4) NOT NULL,
    valeur_stock   DECIMAL(18, 2) NOT NULL,
    date_changement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    type_mouvement VARCHAR(20) NOT NULL,
    CONSTRAINT fk_cump_produit FOREIGN KEY (id_produit) REFERENCES produit(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cump_mouvement FOREIGN KEY (id_mouvement) REFERENCES mouvement_stock(id) ON DELETE RESTRICT,
    CONSTRAINT ck_cump_type CHECK (type_mouvement IN ('ENTREE', 'SORTIE'))
);

CREATE INDEX IF NOT EXISTS idx_cump_produit ON cump_historique(id_produit);
CREATE INDEX IF NOT EXISTS idx_cump_date ON cump_historique(date_changement);

-- ============================================================
-- TABLE 6 : vente
-- ============================================================
CREATE TABLE IF NOT EXISTS vente (
    id                  SERIAL PRIMARY KEY,
    id_produit          INTEGER NOT NULL,
    quantite            DECIMAL(15, 4) NOT NULL CHECK (quantite > 0),
    prix_vente_unitaire DECIMAL(15, 4) NOT NULL CHECK (prix_vente_unitaire > 0),
    cout_unitaire_reel  DECIMAL(15, 4) NOT NULL,
    montant_vente       DECIMAL(18, 2) GENERATED ALWAYS AS (quantite * prix_vente_unitaire) STORED,
    cout_reel_total     DECIMAL(18, 2) GENERATED ALWAYS AS (quantite * cout_unitaire_reel) STORED,
    benefice            DECIMAL(18, 2) GENERATED ALWAYS AS ((quantite * prix_vente_unitaire) - (quantite * cout_unitaire_reel)) STORED,
    date_vente          DATE NOT NULL DEFAULT CURRENT_DATE,
    n_facture           VARCHAR(50),
    date_creation       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vente_produit FOREIGN KEY (id_produit) REFERENCES produit(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_vente_produit ON vente(id_produit);
CREATE INDEX IF NOT EXISTS idx_vente_date ON vente(date_vente);

-- ============================================================
-- TABLE 7 : configuration_stock
-- ============================================================
CREATE TABLE IF NOT EXISTS configuration_stock (
    id                SERIAL PRIMARY KEY,
    cle               VARCHAR(100) NOT NULL UNIQUE,
    valeur            VARCHAR(255),
    description       TEXT,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- INSERT INITIAL DE RÉFÉRENCE
-- ============================================================
INSERT INTO methode_valorisation (code, label) VALUES
    ('FIFO', 'First In First Out'),
    ('LIFO', 'Last In First Out'),
    ('CUMP', 'Coût Unitaire Moyen Pondéré')
ON CONFLICT (code) DO NOTHING;

INSERT INTO configuration_stock (cle, valeur, description) VALUES
    ('devise', 'Ar', 'Devise utilisée (Ariary)'),
    ('precision_decimal', '2', 'Nombre de décimales pour les prix'),
    ('methode_defaut', 'CUMP', 'Méthode de valorisation par défaut'),
    ('approche_fifo_auto', 'true', 'Calcul automatique FIFO/LIFO au mouvement')
ON CONFLICT (cle) DO NOTHING;

-- ============================================================
-- FONCTIONS
-- ============================================================
CREATE OR REPLACE FUNCTION fn_calcul_cump_produit(p_id_produit INTEGER)
RETURNS TABLE (
    cump_courant DECIMAL,
    quantite_totale DECIMAL,
    valeur_totale DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        CASE
            WHEN SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN ms.quantite ELSE -ms.quantite END) = 0 THEN 0
            ELSE SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN (ms.quantite * ms.prix_unitaire)
                         ELSE -(ms.quantite * ms.prix_unitaire) END)
                 / NULLIF(SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN ms.quantite
                                   ELSE -ms.quantite END), 0)
        END,
        SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN ms.quantite ELSE -ms.quantite END),
        SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN (ms.quantite * ms.prix_unitaire)
                 ELSE -(ms.quantite * ms.prix_unitaire) END)
    FROM mouvement_stock ms
    WHERE ms.id_produit = p_id_produit
      AND ms.statut = 'VALIDÉ'
      AND ms.date_mouvement <= CURRENT_DATE;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_etat_stock_date(
    p_id_produit INTEGER,
    p_date DATE DEFAULT CURRENT_DATE
)
RETURNS TABLE (
    quantite DECIMAL,
    valeur DECIMAL,
    cump DECIMAL,
    methode VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    WITH stock_calc AS (
        SELECT
            SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN ms.quantite ELSE -ms.quantite END) AS qty,
            SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN (ms.quantite * ms.prix_unitaire)
                     ELSE -(ms.quantite * ms.prix_unitaire) END) AS val
        FROM mouvement_stock ms
        WHERE ms.id_produit = p_id_produit
          AND ms.statut = 'VALIDÉ'
          AND ms.date_mouvement <= p_date
    )
    SELECT
        COALESCE(qty, 0)::DECIMAL,
        COALESCE(val, 0)::DECIMAL,
        CASE WHEN COALESCE(qty, 0) = 0 THEN 0 ELSE COALESCE(val, 0) / COALESCE(qty, 0) END::DECIMAL,
        p.methode_valorisation
    FROM stock_calc, produit p
    WHERE p.id = p_id_produit;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_appliquer_fifo(
    p_id_produit INTEGER,
    p_quantite_sortie DECIMAL
)
RETURNS TABLE (
    cout_total DECIMAL,
    details TEXT
) AS $$
DECLARE
    v_quantite_a_consommer DECIMAL := p_quantite_sortie;
    v_cout_total DECIMAL := 0;
    v_details TEXT := '';
    r RECORD;
BEGIN
    FOR r IN
        SELECT id, quantite_restante, prix_unitaire
        FROM ligne_stock
        WHERE id_produit = p_id_produit
          AND quantite_restante > 0
        ORDER BY date_entree ASC
    LOOP
        IF v_quantite_a_consommer <= 0 THEN EXIT; END IF;
        IF r.quantite_restante >= v_quantite_a_consommer THEN
            v_cout_total := v_cout_total + (v_quantite_a_consommer * r.prix_unitaire);
            v_details := v_details || 'Lot ' || r.id || ': ' || v_quantite_a_consommer || ' u à ' || r.prix_unitaire || E'\n';
            UPDATE ligne_stock SET quantite_restante = quantite_restante - v_quantite_a_consommer WHERE id = r.id;
            v_quantite_a_consommer := 0;
        ELSE
            v_cout_total := v_cout_total + (r.quantite_restante * r.prix_unitaire);
            v_details := v_details || 'Lot ' || r.id || ': ' || r.quantite_restante || ' u à ' || r.prix_unitaire || E'\n';
            UPDATE ligne_stock SET quantite_restante = 0, date_consommation = CURRENT_TIMESTAMP WHERE id = r.id;
            v_quantite_a_consommer := v_quantite_a_consommer - r.quantite_restante;
        END IF;
    END LOOP;
    RETURN QUERY SELECT v_cout_total, v_details;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_appliquer_lifo(
    p_id_produit INTEGER,
    p_quantite_sortie DECIMAL
)
RETURNS TABLE (
    cout_total DECIMAL,
    details TEXT
) AS $$
DECLARE
    v_quantite_a_consommer DECIMAL := p_quantite_sortie;
    v_cout_total DECIMAL := 0;
    v_details TEXT := '';
    r RECORD;
BEGIN
    FOR r IN
        SELECT id, quantite_restante, prix_unitaire
        FROM ligne_stock
        WHERE id_produit = p_id_produit
          AND quantite_restante > 0
        ORDER BY date_entree DESC
    LOOP
        IF v_quantite_a_consommer <= 0 THEN EXIT; END IF;
        IF r.quantite_restante >= v_quantite_a_consommer THEN
            v_cout_total := v_cout_total + (v_quantite_a_consommer * r.prix_unitaire);
            v_details := v_details || 'Lot ' || r.id || ': ' || v_quantite_a_consommer || ' u à ' || r.prix_unitaire || E'\n';
            UPDATE ligne_stock SET quantite_restante = quantite_restante - v_quantite_a_consommer WHERE id = r.id;
            v_quantite_a_consommer := 0;
        ELSE
            v_cout_total := v_cout_total + (r.quantite_restante * r.prix_unitaire);
            v_details := v_details || 'Lot ' || r.id || ': ' || r.quantite_restante || ' u à ' || r.prix_unitaire || E'\n';
            UPDATE ligne_stock SET quantite_restante = 0, date_consommation = CURRENT_TIMESTAMP WHERE id = r.id;
            v_quantite_a_consommer := v_quantite_a_consommer - r.quantite_restante;
        END IF;
    END LOOP;
    RETURN QUERY SELECT v_cout_total, v_details;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_appliquer_cump(
    p_id_produit INTEGER,
    p_quantite_sortie DECIMAL
)
RETURNS TABLE (
    cout_total DECIMAL,
    cump_applique DECIMAL
) AS $$
DECLARE
    v_cump DECIMAL;
BEGIN
    SELECT (fn_calcul_cump_produit(p_id_produit)).cump_courant INTO v_cump;
    RETURN QUERY SELECT (p_quantite_sortie * v_cump)::DECIMAL, v_cump::DECIMAL;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- VUES
-- ============================================================
CREATE OR REPLACE VIEW v_synthese_stock AS
SELECT
    p.id,
    p.nom,
    p.code_interne,
    p.methode_valorisation,
    COALESCE(SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN ms.quantite ELSE -ms.quantite END), 0) AS quantite_total,
    COALESCE(SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN (ms.quantite * ms.prix_unitaire)
                      ELSE -(ms.quantite * ms.prix_unitaire) END), 0) AS valeur_stock,
    CASE
        WHEN COALESCE(SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN ms.quantite ELSE -ms.quantite END), 0) = 0 THEN 0
        ELSE COALESCE(SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN (ms.quantite * ms.prix_unitaire)
                               ELSE -(ms.quantite * ms.prix_unitaire) END), 0)
             / NULLIF(COALESCE(SUM(CASE WHEN ms.type_mouvement = 'ENTREE' THEN ms.quantite ELSE -ms.quantite END), 0), 0)
    END AS cump_courant,
    MAX(ms.date_mouvement) AS dernier_mouvement,
    p.date_creation,
    p.date_modification
FROM produit p
LEFT JOIN mouvement_stock ms ON ms.id_produit = p.id AND ms.statut = 'VALIDÉ' AND ms.date_mouvement <= CURRENT_DATE
WHERE p.actif = TRUE
GROUP BY p.id, p.nom, p.code_interne, p.methode_valorisation, p.date_creation, p.date_modification
ORDER BY p.nom;

CREATE OR REPLACE VIEW v_historique_mouvements AS
SELECT ms.id, p.nom AS produit, p.methode_valorisation, ms.type_mouvement, ms.quantite,
       ms.prix_unitaire, ms.valeur_total, ms.reference_achat, ms.reference_vente,
       ms.date_mouvement, ms.statut, ms.notes
FROM mouvement_stock ms
JOIN produit p ON p.id = ms.id_produit
ORDER BY ms.date_mouvement DESC, ms.id DESC;

CREATE OR REPLACE VIEW v_lots_restants AS
SELECT ls.id, p.nom AS produit, p.methode_valorisation, ls.quantite_restante,
       ls.prix_unitaire, ls.valeur_restante, ls.date_entree,
       CASE WHEN ls.quantite_restante = 0 THEN 'CONSOMMÉ' ELSE 'ACTIF' END AS statut,
       AGE(CURRENT_DATE, ls.date_entree) AS anciennete
FROM ligne_stock ls
JOIN produit p ON p.id = ls.id_produit
WHERE ls.quantite_restante > 0
ORDER BY ls.date_entree ASC;

CREATE OR REPLACE VIEW v_analyse_ventes AS
SELECT p.nom AS produit, COUNT(v.id) AS nb_ventes, SUM(v.quantite) AS quantite_vendue_total,
       SUM(v.montant_vente) AS montant_total, SUM(v.cout_reel_total) AS cout_total,
       SUM(v.benefice) AS benefice_total, ROUND(AVG(v.benefice), 2) AS benefice_moyen,
       CASE WHEN SUM(v.montant_vente) = 0 THEN 0 ELSE ROUND(100.0 * SUM(v.benefice) / SUM(v.montant_vente), 2) END AS marge_pct,
       MAX(v.date_vente) AS dernier_vente
FROM produit p
LEFT JOIN vente v ON v.id_produit = p.id
WHERE p.actif = TRUE
GROUP BY p.id, p.nom
ORDER BY benefice_total DESC NULLS LAST;
