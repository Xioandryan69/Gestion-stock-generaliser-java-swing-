--
-- PostgreSQL database dump
--

\restrict 73dW1pPiz5ezUj9CjchqWAUqGDnpDCZZLMJL3cxvGoRTqKyG3SRaoiVinImv1Xq

-- Dumped from database version 16.13 (Ubuntu 16.13-0ubuntu0.24.04.1)
-- Dumped by pg_dump version 16.13 (Ubuntu 16.13-0ubuntu0.24.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: fn_appliquer_cump(integer, numeric); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_appliquer_cump(p_id_produit integer, p_quantite_sortie numeric) RETURNS TABLE(cout_total numeric, cump_applique numeric)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_cump DECIMAL;
BEGIN
    SELECT (fn_calcul_cump_produit(p_id_produit)).cump_courant INTO v_cump;
    RETURN QUERY SELECT (p_quantite_sortie * v_cump)::DECIMAL, v_cump::DECIMAL;
END;
$$;


ALTER FUNCTION public.fn_appliquer_cump(p_id_produit integer, p_quantite_sortie numeric) OWNER TO postgres;

--
-- Name: fn_appliquer_fifo(integer, numeric); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_appliquer_fifo(p_id_produit integer, p_quantite_sortie numeric) RETURNS TABLE(cout_total numeric, details text)
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.fn_appliquer_fifo(p_id_produit integer, p_quantite_sortie numeric) OWNER TO postgres;

--
-- Name: fn_appliquer_lifo(integer, numeric); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_appliquer_lifo(p_id_produit integer, p_quantite_sortie numeric) RETURNS TABLE(cout_total numeric, details text)
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.fn_appliquer_lifo(p_id_produit integer, p_quantite_sortie numeric) OWNER TO postgres;

--
-- Name: fn_calcul_cump_produit(integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_calcul_cump_produit(p_id_produit integer) RETURNS TABLE(cump_courant numeric, quantite_totale numeric, valeur_totale numeric)
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.fn_calcul_cump_produit(p_id_produit integer) OWNER TO postgres;

--
-- Name: fn_etat_stock_date(integer, date); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_etat_stock_date(p_id_produit integer, p_date date DEFAULT CURRENT_DATE) RETURNS TABLE(quantite numeric, valeur numeric, cump numeric, methode character varying)
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.fn_etat_stock_date(p_id_produit integer, p_date date) OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: addresse; Type: TABLE; Schema: public; Owner: dev1
--

CREATE TABLE public.addresse (
    id integer NOT NULL,
    ville character varying(100) NOT NULL,
    codepostal character varying(20) NOT NULL
);


ALTER TABLE public.addresse OWNER TO dev1;

--
-- Name: addresse_id_seq; Type: SEQUENCE; Schema: public; Owner: dev1
--

CREATE SEQUENCE public.addresse_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.addresse_id_seq OWNER TO dev1;

--
-- Name: addresse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dev1
--

ALTER SEQUENCE public.addresse_id_seq OWNED BY public.addresse.id;


--
-- Name: configuration_stock; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.configuration_stock (
    id integer NOT NULL,
    cle character varying(100) NOT NULL,
    valeur character varying(255),
    description text,
    date_modification timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.configuration_stock OWNER TO postgres;

--
-- Name: configuration_stock_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.configuration_stock_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.configuration_stock_id_seq OWNER TO postgres;

--
-- Name: configuration_stock_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.configuration_stock_id_seq OWNED BY public.configuration_stock.id;


--
-- Name: cump_historique; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.cump_historique (
    id integer NOT NULL,
    id_produit integer NOT NULL,
    id_mouvement integer NOT NULL,
    cump_ancien numeric(15,4),
    cump_nouveau numeric(15,4) NOT NULL,
    quantite_stock numeric(15,4) NOT NULL,
    valeur_stock numeric(18,2) NOT NULL,
    date_changement timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    type_mouvement character varying(20) NOT NULL,
    CONSTRAINT ck_cump_type CHECK (((type_mouvement)::text = ANY ((ARRAY['ENTREE'::character varying, 'SORTIE'::character varying])::text[])))
);


ALTER TABLE public.cump_historique OWNER TO postgres;

--
-- Name: cump_historique_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.cump_historique_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.cump_historique_id_seq OWNER TO postgres;

--
-- Name: cump_historique_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.cump_historique_id_seq OWNED BY public.cump_historique.id;


--
-- Name: etat_stock; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.etat_stock (
    id integer NOT NULL,
    id_produit integer NOT NULL,
    date_etat date NOT NULL,
    quantite_total numeric(15,4) DEFAULT 0,
    valeur_stock_total numeric(18,2) DEFAULT 0,
    cump_jour numeric(15,4),
    date_creation timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    "calculé_à" timestamp without time zone
);


ALTER TABLE public.etat_stock OWNER TO postgres;

--
-- Name: etat_stock_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.etat_stock_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.etat_stock_id_seq OWNER TO postgres;

--
-- Name: etat_stock_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.etat_stock_id_seq OWNED BY public.etat_stock.id;


--
-- Name: ligne_stock; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ligne_stock (
    id integer NOT NULL,
    id_produit integer NOT NULL,
    id_mouvement_entree integer NOT NULL,
    quantite_initiale numeric(15,4) NOT NULL,
    quantite_restante numeric(15,4) NOT NULL,
    prix_unitaire numeric(15,4) NOT NULL,
    valeur_restante numeric(18,2) GENERATED ALWAYS AS ((quantite_restante * prix_unitaire)) STORED,
    date_entree date NOT NULL,
    date_creation timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    date_consommation timestamp without time zone,
    CONSTRAINT ck_ligne_qte CHECK ((quantite_restante <= quantite_initiale)),
    CONSTRAINT ligne_stock_prix_unitaire_check CHECK ((prix_unitaire >= (0)::numeric)),
    CONSTRAINT ligne_stock_quantite_initiale_check CHECK ((quantite_initiale > (0)::numeric)),
    CONSTRAINT ligne_stock_quantite_restante_check CHECK ((quantite_restante >= (0)::numeric))
);


ALTER TABLE public.ligne_stock OWNER TO postgres;

--
-- Name: ligne_stock_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ligne_stock_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.ligne_stock_id_seq OWNER TO postgres;

--
-- Name: ligne_stock_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ligne_stock_id_seq OWNED BY public.ligne_stock.id;


--
-- Name: methode_valorisation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.methode_valorisation (
    code character varying(20) NOT NULL,
    label character varying(50) NOT NULL
);


ALTER TABLE public.methode_valorisation OWNER TO postgres;

--
-- Name: mouvement_stock; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mouvement_stock (
    id integer NOT NULL,
    id_produit integer NOT NULL,
    type_mouvement character varying(20) NOT NULL,
    quantite numeric(15,4) NOT NULL,
    prix_unitaire numeric(15,4) NOT NULL,
    valeur_total numeric(18,2) GENERATED ALWAYS AS ((quantite * prix_unitaire)) STORED,
    reference_achat character varying(100),
    reference_vente character varying(100),
    date_mouvement date DEFAULT CURRENT_DATE NOT NULL,
    date_creation timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    statut character varying(20) DEFAULT 'VALIDÉ'::character varying,
    notes text,
    CONSTRAINT ck_mvt_date CHECK ((date_mouvement <= CURRENT_DATE)),
    CONSTRAINT ck_mvt_type CHECK (((type_mouvement)::text = ANY ((ARRAY['ENTREE'::character varying, 'SORTIE'::character varying])::text[]))),
    CONSTRAINT mouvement_stock_prix_unitaire_check CHECK ((prix_unitaire >= (0)::numeric)),
    CONSTRAINT mouvement_stock_quantite_check CHECK ((quantite > (0)::numeric))
);


ALTER TABLE public.mouvement_stock OWNER TO postgres;

--
-- Name: mouvement_stock_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mouvement_stock_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mouvement_stock_id_seq OWNER TO postgres;

--
-- Name: mouvement_stock_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.mouvement_stock_id_seq OWNED BY public.mouvement_stock.id;


--
-- Name: personne; Type: TABLE; Schema: public; Owner: dev1
--

CREATE TABLE public.personne (
    id character varying(50) NOT NULL,
    nom character varying(100) NOT NULL,
    age integer DEFAULT 0 NOT NULL,
    role character varying(20) DEFAULT 'CLIENT'::character varying NOT NULL,
    adresse_id integer,
    infossupplementaires jsonb
);


ALTER TABLE public.personne OWNER TO dev1;

--
-- Name: personne_telephone; Type: TABLE; Schema: public; Owner: dev1
--

CREATE TABLE public.personne_telephone (
    personne_id character varying(50) NOT NULL,
    telephone_id integer NOT NULL
);


ALTER TABLE public.personne_telephone OWNER TO dev1;

--
-- Name: produit; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.produit (
    id integer NOT NULL,
    nom character varying(255) NOT NULL,
    description text,
    methode_valorisation character varying(20) DEFAULT 'CUMP'::character varying NOT NULL,
    code_interne character varying(50),
    poids_kg numeric(10,2),
    volume_m3 numeric(10,2),
    actif boolean DEFAULT true,
    date_creation timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    date_modification timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_produit_methode CHECK (((methode_valorisation)::text = ANY ((ARRAY['FIFO'::character varying, 'LIFO'::character varying, 'CUMP'::character varying])::text[]))),
    CONSTRAINT ck_produit_nom_non_vide CHECK (((nom)::text <> ''::text))
);


ALTER TABLE public.produit OWNER TO postgres;

--
-- Name: produit_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.produit_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.produit_id_seq OWNER TO postgres;

--
-- Name: produit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.produit_id_seq OWNED BY public.produit.id;


--
-- Name: telephone; Type: TABLE; Schema: public; Owner: dev1
--

CREATE TABLE public.telephone (
    id integer NOT NULL,
    numero character varying(30) NOT NULL,
    type character varying(20) NOT NULL
);


ALTER TABLE public.telephone OWNER TO dev1;

--
-- Name: telephone_id_seq; Type: SEQUENCE; Schema: public; Owner: dev1
--

CREATE SEQUENCE public.telephone_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.telephone_id_seq OWNER TO dev1;

--
-- Name: telephone_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dev1
--

ALTER SEQUENCE public.telephone_id_seq OWNED BY public.telephone.id;


--
-- Name: vente; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.vente (
    id integer NOT NULL,
    id_produit integer NOT NULL,
    quantite numeric(15,4) NOT NULL,
    prix_vente_unitaire numeric(15,4) NOT NULL,
    cout_unitaire_reel numeric(15,4) NOT NULL,
    montant_vente numeric(18,2) GENERATED ALWAYS AS ((quantite * prix_vente_unitaire)) STORED,
    cout_reel_total numeric(18,2) GENERATED ALWAYS AS ((quantite * cout_unitaire_reel)) STORED,
    benefice numeric(18,2) GENERATED ALWAYS AS (((quantite * prix_vente_unitaire) - (quantite * cout_unitaire_reel))) STORED,
    date_vente date DEFAULT CURRENT_DATE NOT NULL,
    n_facture character varying(50),
    date_creation timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT vente_prix_vente_unitaire_check CHECK ((prix_vente_unitaire > (0)::numeric)),
    CONSTRAINT vente_quantite_check CHECK ((quantite > (0)::numeric))
);


ALTER TABLE public.vente OWNER TO postgres;

--
-- Name: v_analyse_ventes; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_analyse_ventes AS
 SELECT p.nom AS produit,
    count(v.id) AS nb_ventes,
    sum(v.quantite) AS quantite_vendue_total,
    sum(v.montant_vente) AS montant_total,
    sum(v.cout_reel_total) AS cout_total,
    sum(v.benefice) AS benefice_total,
    round(avg(v.benefice), 2) AS benefice_moyen,
        CASE
            WHEN (sum(v.montant_vente) = (0)::numeric) THEN (0)::numeric
            ELSE round(((100.0 * sum(v.benefice)) / sum(v.montant_vente)), 2)
        END AS marge_pct,
    max(v.date_vente) AS dernier_vente
   FROM (public.produit p
     LEFT JOIN public.vente v ON ((v.id_produit = p.id)))
  WHERE (p.actif = true)
  GROUP BY p.id, p.nom
  ORDER BY (sum(v.benefice)) DESC NULLS LAST;


ALTER VIEW public.v_analyse_ventes OWNER TO postgres;

--
-- Name: v_historique_mouvements; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_historique_mouvements AS
 SELECT ms.id,
    p.nom AS produit,
    p.methode_valorisation,
    ms.type_mouvement,
    ms.quantite,
    ms.prix_unitaire,
    ms.valeur_total,
    ms.reference_achat,
    ms.reference_vente,
    ms.date_mouvement,
    ms.statut,
    ms.notes
   FROM (public.mouvement_stock ms
     JOIN public.produit p ON ((p.id = ms.id_produit)))
  ORDER BY ms.date_mouvement DESC, ms.id DESC;


ALTER VIEW public.v_historique_mouvements OWNER TO postgres;

--
-- Name: v_lots_restants; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_lots_restants AS
 SELECT ls.id,
    p.nom AS produit,
    p.methode_valorisation,
    ls.quantite_restante,
    ls.prix_unitaire,
    ls.valeur_restante,
    ls.date_entree,
        CASE
            WHEN (ls.quantite_restante = (0)::numeric) THEN 'CONSOMMÉ'::text
            ELSE 'ACTIF'::text
        END AS statut,
    age((CURRENT_DATE)::timestamp with time zone, (ls.date_entree)::timestamp with time zone) AS anciennete
   FROM (public.ligne_stock ls
     JOIN public.produit p ON ((p.id = ls.id_produit)))
  WHERE (ls.quantite_restante > (0)::numeric)
  ORDER BY ls.date_entree;


ALTER VIEW public.v_lots_restants OWNER TO postgres;

--
-- Name: v_synthese_stock; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_synthese_stock AS
 SELECT p.id,
    p.nom,
    p.code_interne,
    p.methode_valorisation,
    COALESCE(sum(
        CASE
            WHEN ((ms.type_mouvement)::text = 'ENTREE'::text) THEN ms.quantite
            ELSE (- ms.quantite)
        END), (0)::numeric) AS quantite_total,
    COALESCE(sum(
        CASE
            WHEN ((ms.type_mouvement)::text = 'ENTREE'::text) THEN (ms.quantite * ms.prix_unitaire)
            ELSE (- (ms.quantite * ms.prix_unitaire))
        END), (0)::numeric) AS valeur_stock,
        CASE
            WHEN (COALESCE(sum(
            CASE
                WHEN ((ms.type_mouvement)::text = 'ENTREE'::text) THEN ms.quantite
                ELSE (- ms.quantite)
            END), (0)::numeric) = (0)::numeric) THEN (0)::numeric
            ELSE (COALESCE(sum(
            CASE
                WHEN ((ms.type_mouvement)::text = 'ENTREE'::text) THEN (ms.quantite * ms.prix_unitaire)
                ELSE (- (ms.quantite * ms.prix_unitaire))
            END), (0)::numeric) / NULLIF(COALESCE(sum(
            CASE
                WHEN ((ms.type_mouvement)::text = 'ENTREE'::text) THEN ms.quantite
                ELSE (- ms.quantite)
            END), (0)::numeric), (0)::numeric))
        END AS cump_courant,
    max(ms.date_mouvement) AS dernier_mouvement,
    p.date_creation,
    p.date_modification
   FROM (public.produit p
     LEFT JOIN public.mouvement_stock ms ON (((ms.id_produit = p.id) AND ((ms.statut)::text = 'VALIDÉ'::text) AND (ms.date_mouvement <= CURRENT_DATE))))
  WHERE (p.actif = true)
  GROUP BY p.id, p.nom, p.code_interne, p.methode_valorisation, p.date_creation, p.date_modification
  ORDER BY p.nom;


ALTER VIEW public.v_synthese_stock OWNER TO postgres;

--
-- Name: vente_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.vente_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.vente_id_seq OWNER TO postgres;

--
-- Name: vente_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.vente_id_seq OWNED BY public.vente.id;


--
-- Name: addresse id; Type: DEFAULT; Schema: public; Owner: dev1
--

ALTER TABLE ONLY public.addresse ALTER COLUMN id SET DEFAULT nextval('public.addresse_id_seq'::regclass);


--
-- Name: configuration_stock id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_stock ALTER COLUMN id SET DEFAULT nextval('public.configuration_stock_id_seq'::regclass);


--
-- Name: cump_historique id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cump_historique ALTER COLUMN id SET DEFAULT nextval('public.cump_historique_id_seq'::regclass);


--
-- Name: etat_stock id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.etat_stock ALTER COLUMN id SET DEFAULT nextval('public.etat_stock_id_seq'::regclass);


--
-- Name: ligne_stock id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ligne_stock ALTER COLUMN id SET DEFAULT nextval('public.ligne_stock_id_seq'::regclass);


--
-- Name: mouvement_stock id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mouvement_stock ALTER COLUMN id SET DEFAULT nextval('public.mouvement_stock_id_seq'::regclass);


--
-- Name: produit id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.produit ALTER COLUMN id SET DEFAULT nextval('public.produit_id_seq'::regclass);


--
-- Name: telephone id; Type: DEFAULT; Schema: public; Owner: dev1
--

ALTER TABLE ONLY public.telephone ALTER COLUMN id SET DEFAULT nextval('public.telephone_id_seq'::regclass);


--
-- Name: vente id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vente ALTER COLUMN id SET DEFAULT nextval('public.vente_id_seq'::regclass);


--
-- Data for Name: addresse; Type: TABLE DATA; Schema: public; Owner: dev1
--

COPY public.addresse (id, ville, codepostal) FROM stdin;
\.


--
-- Data for Name: configuration_stock; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.configuration_stock (id, cle, valeur, description, date_modification) FROM stdin;
\.


--
-- Data for Name: cump_historique; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.cump_historique (id, id_produit, id_mouvement, cump_ancien, cump_nouveau, quantite_stock, valeur_stock, date_changement, type_mouvement) FROM stdin;
\.


--
-- Data for Name: etat_stock; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.etat_stock (id, id_produit, date_etat, quantite_total, valeur_stock_total, cump_jour, date_creation, "calculé_à") FROM stdin;
\.


--
-- Data for Name: ligne_stock; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ligne_stock (id, id_produit, id_mouvement_entree, quantite_initiale, quantite_restante, prix_unitaire, date_entree, date_creation, date_consommation) FROM stdin;
1	1	1	100.0000	100.0000	500.0000	2026-05-01	2026-05-21 11:20:52.438064	\N
2	1	2	80.0000	80.0000	550.0000	2026-05-03	2026-05-21 11:23:49.963398	\N
3	1	3	60.0000	60.0000	600.0000	2026-05-05	2026-05-21 11:24:31.577603	\N
4	2	4	200.0000	200.0000	50.0000	2026-05-01	2026-05-21 11:25:18.942735	\N
5	2	5	200.0000	200.0000	50.0000	2026-05-01	2026-05-21 11:26:18.494533	\N
6	2	6	100.0000	100.0000	60.0000	2026-05-05	2026-05-21 14:33:20.204009	\N
7	3	7	50.0000	50.0000	1200.0000	2026-05-01	2026-05-21 14:33:46.41143	\N
8	3	8	40.0000	40.0000	1250.0000	2026-05-03	2026-05-21 14:34:07.851933	\N
\.


--
-- Data for Name: methode_valorisation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.methode_valorisation (code, label) FROM stdin;
\.


--
-- Data for Name: mouvement_stock; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.mouvement_stock (id, id_produit, type_mouvement, quantite, prix_unitaire, reference_achat, reference_vente, date_mouvement, date_creation, statut, notes) FROM stdin;
1	1	ENTREE	100.0000	500.0000	Biscuit-1	Biscuit-1	2026-05-01	2026-05-21 11:20:52.438064	VALIDÉ	1
2	1	ENTREE	80.0000	550.0000	Biscuit-2	\N	2026-05-03	2026-05-21 11:23:49.963398	VALIDÉ	2
4	2	ENTREE	200.0000	50.0000	Bonbon-1	\N	2026-05-01	2026-05-21 11:25:18.942735	VALIDÉ	1
3	1	ENTREE	60.0000	600.0000	Biscuit-3	Biscuit-3	2026-05-05	2026-05-21 11:24:31.577603	VALIDÉ	3
6	2	ENTREE	100.0000	60.0000	Bobom-3	\N	2026-05-05	2026-05-21 14:33:20.204009	VALIDÉ	3
7	3	ENTREE	50.0000	1200.0000	Soda-1	\N	2026-05-01	2026-05-21 14:33:46.41143	VALIDÉ	1
8	3	ENTREE	40.0000	1250.0000	Soda-2	\N	2026-05-03	2026-05-21 14:34:07.851933	VALIDÉ	2
5	2	ENTREE	150.0000	55.0000	Bonbon-2	Bonbon-1	2026-05-03	2026-05-21 11:26:18.494533	VALIDÉ	\N
\.


--
-- Data for Name: personne; Type: TABLE DATA; Schema: public; Owner: dev1
--

COPY public.personne (id, nom, age, role, adresse_id, infossupplementaires) FROM stdin;
\.


--
-- Data for Name: personne_telephone; Type: TABLE DATA; Schema: public; Owner: dev1
--

COPY public.personne_telephone (personne_id, telephone_id) FROM stdin;
\.


--
-- Data for Name: produit; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.produit (id, nom, description, methode_valorisation, code_interne, poids_kg, volume_m3, actif, date_creation, date_modification) FROM stdin;
1	Biscuit	biscuit	FIFO	Biscuit-FIFO	\N	\N	t	2026-05-21 11:18:39.518415	2026-05-21 11:18:39.518415
2	Bobon	Bonbon	LIFO	Bonbon-LIFO	\N	\N	t	2026-05-21 11:19:12.899333	2026-05-21 11:19:12.899333
3	Soda	Soda	CUMP	Soda-CUMO	\N	\N	t	2026-05-21 11:19:34.316388	2026-05-21 11:19:34.316388
\.


--
-- Data for Name: telephone; Type: TABLE DATA; Schema: public; Owner: dev1
--

COPY public.telephone (id, numero, type) FROM stdin;
\.


--
-- Data for Name: vente; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.vente (id, id_produit, quantite, prix_vente_unitaire, cout_unitaire_reel, date_vente, n_facture, date_creation) FROM stdin;
\.


--
-- Name: addresse_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dev1
--

SELECT pg_catalog.setval('public.addresse_id_seq', 1, false);


--
-- Name: configuration_stock_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.configuration_stock_id_seq', 1, false);


--
-- Name: cump_historique_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.cump_historique_id_seq', 1, false);


--
-- Name: etat_stock_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.etat_stock_id_seq', 1, false);


--
-- Name: ligne_stock_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ligne_stock_id_seq', 8, true);


--
-- Name: mouvement_stock_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.mouvement_stock_id_seq', 8, true);


--
-- Name: produit_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.produit_id_seq', 1, false);


--
-- Name: telephone_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dev1
--

SELECT pg_catalog.setval('public.telephone_id_seq', 1, false);


--
-- Name: vente_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.vente_id_seq', 1, false);


--
-- Name: addresse addresse_pkey; Type: CONSTRAINT; Schema: public; Owner: dev1
--

ALTER TABLE ONLY public.addresse
    ADD CONSTRAINT addresse_pkey PRIMARY KEY (id);


--
-- Name: configuration_stock configuration_stock_cle_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_stock
    ADD CONSTRAINT configuration_stock_cle_key UNIQUE (cle);


--
-- Name: configuration_stock configuration_stock_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_stock
    ADD CONSTRAINT configuration_stock_pkey PRIMARY KEY (id);


--
-- Name: cump_historique cump_historique_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cump_historique
    ADD CONSTRAINT cump_historique_pkey PRIMARY KEY (id);


--
-- Name: etat_stock etat_stock_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.etat_stock
    ADD CONSTRAINT etat_stock_pkey PRIMARY KEY (id);


--
-- Name: ligne_stock ligne_stock_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ligne_stock
    ADD CONSTRAINT ligne_stock_pkey PRIMARY KEY (id);


--
-- Name: methode_valorisation methode_valorisation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.methode_valorisation
    ADD CONSTRAINT methode_valorisation_pkey PRIMARY KEY (code);


--
-- Name: mouvement_stock mouvement_stock_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mouvement_stock
    ADD CONSTRAINT mouvement_stock_pkey PRIMARY KEY (id);


--
-- Name: personne personne_pkey; Type: CONSTRAINT; Schema: public; Owner: dev1
--

ALTER TABLE ONLY public.personne
    ADD CONSTRAINT personne_pkey PRIMARY KEY (id);


--
-- Name: personne_telephone personne_telephone_pkey; Type: CONSTRAINT; Schema: public; Owner: dev1
--

ALTER TABLE ONLY public.personne_telephone
    ADD CONSTRAINT personne_telephone_pkey PRIMARY KEY (personne_id, telephone_id);


--
-- Name: produit produit_code_interne_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.produit
    ADD CONSTRAINT produit_code_interne_key UNIQUE (code_interne);


--
-- Name: produit produit_nom_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.produit
    ADD CONSTRAINT produit_nom_key UNIQUE (nom);


--
-- Name: produit produit_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.produit
    ADD CONSTRAINT produit_pkey PRIMARY KEY (id);


--
-- Name: telephone telephone_pkey; Type: CONSTRAINT; Schema: public; Owner: dev1
--

ALTER TABLE ONLY public.telephone
    ADD CONSTRAINT telephone_pkey PRIMARY KEY (id);


--
-- Name: etat_stock uk_etat_produit_date; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.etat_stock
    ADD CONSTRAINT uk_etat_produit_date UNIQUE (id_produit, date_etat);


--
-- Name: vente vente_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vente
    ADD CONSTRAINT vente_pkey PRIMARY KEY (id);


--
-- Name: idx_cump_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_cump_date ON public.cump_historique USING btree (date_changement);


--
-- Name: idx_cump_produit; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_cump_produit ON public.cump_historique USING btree (id_produit);


--
-- Name: idx_etat_produit_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_etat_produit_date ON public.etat_stock USING btree (id_produit, date_etat);


--
-- Name: idx_ligne_date_entree; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ligne_date_entree ON public.ligne_stock USING btree (date_entree);


--
-- Name: idx_ligne_produit; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ligne_produit ON public.ligne_stock USING btree (id_produit);


--
-- Name: idx_ligne_qte_restante; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ligne_qte_restante ON public.ligne_stock USING btree (quantite_restante);


--
-- Name: idx_mouvement_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_mouvement_date ON public.mouvement_stock USING btree (date_mouvement);


--
-- Name: idx_mouvement_produit; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_mouvement_produit ON public.mouvement_stock USING btree (id_produit);


--
-- Name: idx_mouvement_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_mouvement_type ON public.mouvement_stock USING btree (type_mouvement);


--
-- Name: idx_produit_code_interne; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_produit_code_interne ON public.produit USING btree (code_interne);


--
-- Name: idx_produit_nom; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_produit_nom ON public.produit USING btree (nom);


--
-- Name: idx_vente_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_vente_date ON public.vente USING btree (date_vente);


--
-- Name: idx_vente_produit; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_vente_produit ON public.vente USING btree (id_produit);


--
-- Name: cump_historique fk_cump_mouvement; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cump_historique
    ADD CONSTRAINT fk_cump_mouvement FOREIGN KEY (id_mouvement) REFERENCES public.mouvement_stock(id) ON DELETE RESTRICT;


--
-- Name: cump_historique fk_cump_produit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cump_historique
    ADD CONSTRAINT fk_cump_produit FOREIGN KEY (id_produit) REFERENCES public.produit(id) ON DELETE RESTRICT;


--
-- Name: etat_stock fk_etat_produit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.etat_stock
    ADD CONSTRAINT fk_etat_produit FOREIGN KEY (id_produit) REFERENCES public.produit(id) ON DELETE CASCADE;


--
-- Name: ligne_stock fk_ligne_mouvement; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ligne_stock
    ADD CONSTRAINT fk_ligne_mouvement FOREIGN KEY (id_mouvement_entree) REFERENCES public.mouvement_stock(id) ON DELETE RESTRICT;


--
-- Name: ligne_stock fk_ligne_produit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ligne_stock
    ADD CONSTRAINT fk_ligne_produit FOREIGN KEY (id_produit) REFERENCES public.produit(id) ON DELETE RESTRICT;


--
-- Name: mouvement_stock fk_mvt_produit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mouvement_stock
    ADD CONSTRAINT fk_mvt_produit FOREIGN KEY (id_produit) REFERENCES public.produit(id) ON DELETE RESTRICT;


--
-- Name: personne_telephone fk_pers_tel_personne; Type: FK CONSTRAINT; Schema: public; Owner: dev1
--

ALTER TABLE ONLY public.personne_telephone
    ADD CONSTRAINT fk_pers_tel_personne FOREIGN KEY (personne_id) REFERENCES public.personne(id) ON DELETE CASCADE;


--
-- Name: personne_telephone fk_pers_tel_telephone; Type: FK CONSTRAINT; Schema: public; Owner: dev1
--

ALTER TABLE ONLY public.personne_telephone
    ADD CONSTRAINT fk_pers_tel_telephone FOREIGN KEY (telephone_id) REFERENCES public.telephone(id) ON DELETE CASCADE;


--
-- Name: personne fk_personne_adresse; Type: FK CONSTRAINT; Schema: public; Owner: dev1
--

ALTER TABLE ONLY public.personne
    ADD CONSTRAINT fk_personne_adresse FOREIGN KEY (adresse_id) REFERENCES public.addresse(id) ON DELETE SET NULL;


--
-- Name: vente fk_vente_produit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vente
    ADD CONSTRAINT fk_vente_produit FOREIGN KEY (id_produit) REFERENCES public.produit(id) ON DELETE RESTRICT;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: pg_database_owner
--

GRANT ALL ON SCHEMA public TO dev1;


--
-- Name: FUNCTION fn_appliquer_cump(p_id_produit integer, p_quantite_sortie numeric); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.fn_appliquer_cump(p_id_produit integer, p_quantite_sortie numeric) TO dev1;


--
-- Name: FUNCTION fn_appliquer_fifo(p_id_produit integer, p_quantite_sortie numeric); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.fn_appliquer_fifo(p_id_produit integer, p_quantite_sortie numeric) TO dev1;


--
-- Name: FUNCTION fn_appliquer_lifo(p_id_produit integer, p_quantite_sortie numeric); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.fn_appliquer_lifo(p_id_produit integer, p_quantite_sortie numeric) TO dev1;


--
-- Name: FUNCTION fn_calcul_cump_produit(p_id_produit integer); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.fn_calcul_cump_produit(p_id_produit integer) TO dev1;


--
-- Name: FUNCTION fn_etat_stock_date(p_id_produit integer, p_date date); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.fn_etat_stock_date(p_id_produit integer, p_date date) TO dev1;


--
-- Name: TABLE configuration_stock; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.configuration_stock TO dev1;


--
-- Name: SEQUENCE configuration_stock_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.configuration_stock_id_seq TO dev1;


--
-- Name: TABLE cump_historique; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.cump_historique TO dev1;


--
-- Name: SEQUENCE cump_historique_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.cump_historique_id_seq TO dev1;


--
-- Name: TABLE etat_stock; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.etat_stock TO dev1;


--
-- Name: SEQUENCE etat_stock_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.etat_stock_id_seq TO dev1;


--
-- Name: TABLE ligne_stock; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.ligne_stock TO dev1;


--
-- Name: SEQUENCE ligne_stock_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.ligne_stock_id_seq TO dev1;


--
-- Name: TABLE methode_valorisation; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.methode_valorisation TO dev1;


--
-- Name: TABLE mouvement_stock; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.mouvement_stock TO dev1;


--
-- Name: SEQUENCE mouvement_stock_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.mouvement_stock_id_seq TO dev1;


--
-- Name: TABLE produit; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.produit TO dev1;


--
-- Name: SEQUENCE produit_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.produit_id_seq TO dev1;


--
-- Name: TABLE vente; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.vente TO dev1;


--
-- Name: TABLE v_analyse_ventes; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.v_analyse_ventes TO dev1;


--
-- Name: TABLE v_historique_mouvements; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.v_historique_mouvements TO dev1;


--
-- Name: TABLE v_lots_restants; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.v_lots_restants TO dev1;


--
-- Name: TABLE v_synthese_stock; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.v_synthese_stock TO dev1;


--
-- Name: SEQUENCE vente_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.vente_id_seq TO dev1;


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO dev1;


--
-- Name: DEFAULT PRIVILEGES FOR FUNCTIONS; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO dev1;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO dev1;


--
-- PostgreSQL database dump complete
--

\unrestrict 73dW1pPiz5ezUj9CjchqWAUqGDnpDCZZLMJL3cxvGoRTqKyG3SRaoiVinImv1Xq

