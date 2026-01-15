--
-- PostgreSQL database dump
--

\restrict JtImdiK6n0jxrWQ1IwGM2WdVzWeoGlKFdmInup2d840qFeBssVpE8b3YSoPP5vE

-- Dumped from database version 18.0 (Homebrew)
-- Dumped by pg_dump version 18.0 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.rejestrwejsc DROP CONSTRAINT IF EXISTS rejestrwejsc_karta_fkey;
ALTER TABLE IF EXISTS ONLY public.pomiary DROP CONSTRAINT IF EXISTS fk_pomiar_komponent;
ALTER TABLE IF EXISTS ONLY public.ustawieniamanualne DROP CONSTRAINT IF EXISTS fk_komponent_stan;
DROP TRIGGER IF EXISTS trg_sprawdz_harmonogram ON public.harmonogramtemperatur;
ALTER TABLE IF EXISTS ONLY public.ustawieniamanualne DROP CONSTRAINT IF EXISTS ustawieniamanualne_pkey;
ALTER TABLE IF EXISTS ONLY public.pracownicy DROP CONSTRAINT IF EXISTS unique_karta;
ALTER TABLE IF EXISTS ONLY public.stansystemu DROP CONSTRAINT IF EXISTS stansystemu_pkey;
ALTER TABLE IF EXISTS ONLY public.rejestrwejsc DROP CONSTRAINT IF EXISTS rejestrwejsc_pkey;
ALTER TABLE IF EXISTS ONLY public.pracownicy DROP CONSTRAINT IF EXISTS pracownicy_pkey;
ALTER TABLE IF EXISTS ONLY public.powiadomienia DROP CONSTRAINT IF EXISTS powiadomienia_pkey;
ALTER TABLE IF EXISTS ONLY public.pomiary DROP CONSTRAINT IF EXISTS pomiary_pkey;
ALTER TABLE IF EXISTS ONLY public.konfiguracjasystemu DROP CONSTRAINT IF EXISTS konfiguracjasystemu_pkey;
ALTER TABLE IF EXISTS ONLY public.harmonogramtemperatur DROP CONSTRAINT IF EXISTS harmonogramtemperatur_pkey;
ALTER TABLE IF EXISTS public.powiadomienia ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.harmonogramtemperatur ALTER COLUMN id DROP DEFAULT;
DROP TABLE IF EXISTS public.ustawieniamanualne;
DROP SEQUENCE IF EXISTS public.powiadomienia_id_seq;
DROP TABLE IF EXISTS public.pomiary;
DROP SEQUENCE IF EXISTS public.harmonogramtemperatur_id_seq;
DROP VIEW IF EXISTS api.stan_systemu;
DROP TABLE IF EXISTS public.stansystemu;
DROP VIEW IF EXISTS api.pracownicy;
DROP VIEW IF EXISTS api.powiadomienia;
DROP TABLE IF EXISTS public.powiadomienia;
DROP VIEW IF EXISTS api.konfiguracja;
DROP TABLE IF EXISTS public.konfiguracjasystemu;
DROP VIEW IF EXISTS api.historia_wejsc;
DROP TABLE IF EXISTS public.rejestrwejsc;
DROP TABLE IF EXISTS public.pracownicy;
DROP VIEW IF EXISTS api.harmonogram;
DROP TABLE IF EXISTS public.harmonogramtemperatur;
DROP FUNCTION IF EXISTS public.url_encode(data bytea);
DROP FUNCTION IF EXISTS public.url_decode(data text);
DROP FUNCTION IF EXISTS public.sprawdz_pokrywanie_terminow();
DROP FUNCTION IF EXISTS public.sign(payload json, secret text, algorithm text);
DROP FUNCTION IF EXISTS public.algorithm_sign(signables text, secret text, algorithm text);
DROP FUNCTION IF EXISTS api.ustaw_sterowanie(komponent text, wartosc numeric);
DROP FUNCTION IF EXISTS api.pobierz_pomiary(komponent text, zakres text);
DROP FUNCTION IF EXISTS api.login_as_admin(pass text);
DROP FUNCTION IF EXISTS api.login(email text, pass text);
DROP EXTENSION IF EXISTS pgcrypto;
DROP SCHEMA IF EXISTS api;
--
-- Name: api; Type: SCHEMA; Schema: -; Owner: mateuszsterma
--

CREATE SCHEMA api;


ALTER SCHEMA api OWNER TO mateuszsterma;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: login(text, text); Type: FUNCTION; Schema: api; Owner: postgres
--

CREATE FUNCTION api.login(email text, pass text) RETURNS json
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
  _role text;
  result json;
begin
  -- Sprawdź email i hasło (tu przykładowo pgcrypto crypt, dostosuj do swojej metody hashowania)
  select role into _role
  from auth.users
  where users.email = login.email
    and users.password_hash = crypt(login.pass, users.password_hash);

  if _role is null then
    raise invalid_password using message = 'invalid user or password';
  end if;

  -- Wygeneruj token JWT
  select json_build_object(
    'token', sign(
        json_build_object(
          'role', _role,
          'email', login.email,
          'exp', extract(epoch from now())::integer + 3600 -- ważny 1h
        ), 
        'twoj_sekret_jwt_z_konfiguracji_postgrest' -- WAŻNE: To musi być ten sam sekret co w postgrest.conf
    )
  ) into result;

  return result;
end;
$$;


ALTER FUNCTION api.login(email text, pass text) OWNER TO postgres;

--
-- Name: login_as_admin(text); Type: FUNCTION; Schema: api; Owner: postgres
--

CREATE FUNCTION api.login_as_admin(pass text) RETURNS json
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
  result json;
  secret_password text := 'admin123'; 
  jwt_secret text := 'to_jest_moj_sekretny_klucz_do_testow_min_32_znaki';
BEGIN
  IF pass != secret_password THEN
     RAISE EXCEPTION 'Nieprawidłowe hasło administratora';
  END IF;

  -- Generowanie tokena BEZ parametru 'exp' (ważny bezterminowo)
  SELECT json_build_object(
    'token', sign(
        json_build_object(
          'role', 'app_admin'
          -- Usunęliśmy linię z 'exp'
        ), 
        jwt_secret
    )
  ) INTO result;

  RETURN result;
END;
$$;


ALTER FUNCTION api.login_as_admin(pass text) OWNER TO postgres;

--
-- Name: pobierz_pomiary(text, text); Type: FUNCTION; Schema: api; Owner: mateuszsterma
--

CREATE FUNCTION api.pobierz_pomiary(komponent text, zakres text) RETURNS TABLE(data timestamp without time zone, wartosc numeric)
    LANGUAGE plpgsql STABLE
    AS $$
BEGIN
    RETURN QUERY
    SELECT data_pomiaru, p.wartosc
    FROM Pomiary p
    WHERE p.nazwa_komponentu = komponent
    AND data_pomiaru >= CASE 
        WHEN zakres = '1h' THEN NOW() - INTERVAL '1 hour'
        WHEN zakres = '24h' THEN NOW() - INTERVAL '24 hours'
        WHEN zakres = '72h' THEN NOW() - INTERVAL '72 hours'
        ELSE NOW() - INTERVAL '1 hour'
    END
    ORDER BY data_pomiaru DESC;
END;
$$;


ALTER FUNCTION api.pobierz_pomiary(komponent text, zakres text) OWNER TO mateuszsterma;

--
-- Name: ustaw_sterowanie(text, numeric); Type: FUNCTION; Schema: api; Owner: mateuszsterma
--

CREATE FUNCTION api.ustaw_sterowanie(komponent text, wartosc numeric) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
    INSERT INTO public.UstawieniaManualne (nazwa_komponentu, wartosc_manualna)
    VALUES (komponent, wartosc)
    ON CONFLICT (nazwa_komponentu) 
    DO UPDATE SET wartosc_manualna = EXCLUDED.wartosc_manualna;
END;
$$;


ALTER FUNCTION api.ustaw_sterowanie(komponent text, wartosc numeric) OWNER TO mateuszsterma;

--
-- Name: algorithm_sign(text, text, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.algorithm_sign(signables text, secret text, algorithm text) RETURNS text
    LANGUAGE sql
    AS $$
WITH
  alg AS (
    SELECT CASE
      -- TU BYŁ BŁĄD: pgrypto oczekuje 'sha256', a nie 'hmac-sha256'
      WHEN algorithm = 'HS256' THEN 'sha256'
      WHEN algorithm = 'HS384' THEN 'sha384'
      WHEN algorithm = 'HS512' THEN 'sha512'
      ELSE '' END AS id)
SELECT url_encode(hmac(signables::bytea, secret::bytea, alg.id)) FROM alg;
$$;


ALTER FUNCTION public.algorithm_sign(signables text, secret text, algorithm text) OWNER TO postgres;

--
-- Name: sign(json, text, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.sign(payload json, secret text, algorithm text DEFAULT 'HS256'::text) RETURNS text
    LANGUAGE sql
    AS $$
WITH
  header AS (
    SELECT url_encode(convert_to('{"alg":"' || algorithm || '","typ":"JWT"}', 'utf8')) AS data
    ),
  payload AS (
    SELECT url_encode(convert_to(payload::text, 'utf8')) AS data
    ),
  signables AS (
    SELECT header.data || '.' || payload.data AS data FROM header, payload
    )
SELECT
    signables.data || '.' ||
    algorithm_sign(signables.data, secret, algorithm) FROM signables;
$$;


ALTER FUNCTION public.sign(payload json, secret text, algorithm text) OWNER TO postgres;

--
-- Name: sprawdz_pokrywanie_terminow(); Type: FUNCTION; Schema: public; Owner: mateuszsterma
--

CREATE FUNCTION public.sprawdz_pokrywanie_terminow() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Sprawdzamy czy istnieje wpis, który spełnia warunki konfliktu
    IF EXISTS (
        SELECT 1 
        FROM public.HarmonogramTemperatur
        WHERE 
            -- 1. Ten sam dzień tygodnia
            dzien_tygodnia = NEW.dzien_tygodnia
            
            -- 2. Wykluczamy ten sam rekord (przydatne przy edycji/UPDATE)
            AND id IS DISTINCT FROM NEW.id
            
            -- 3. Logika nakładania się czasów (Overlap):
            -- (Start A < Koniec B) ORAZ (Koniec A > Start B)
            AND (NEW.godzina_rozpoczecia < godzina_zakonczenia)
            AND (NEW.godzina_zakonczenia > godzina_rozpoczecia)
    ) THEN
        -- Jeśli znaleziono taki wpis, rzuć błąd i przerwij operację
        RAISE EXCEPTION 'Konflikt harmonogramu! W dniu % istnieje już wpis pokrywający się z godzinami % - %.', 
            NEW.dzien_tygodnia, NEW.godzina_rozpoczecia, NEW.godzina_zakonczenia
            USING ERRCODE = 'P0001'; -- Kod błędu aplikacji
    END IF;

    -- Jeśli brak konfliktów, zatwierdź
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.sprawdz_pokrywanie_terminow() OWNER TO mateuszsterma;

--
-- Name: url_decode(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.url_decode(data text) RETURNS bytea
    LANGUAGE sql
    AS $$
WITH t AS (SELECT translate(data, '-_', '+/') AS trans),
     rem AS (SELECT length(t.trans) % 4 AS remainder FROM t) -- compute padding size
    SELECT decode(
        t.trans ||
        CASE WHEN rem.remainder > 0
           THEN repeat('=', (4 - rem.remainder))
           ELSE '' END,
    'base64') FROM t, rem;
$$;


ALTER FUNCTION public.url_decode(data text) OWNER TO postgres;

--
-- Name: url_encode(bytea); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.url_encode(data bytea) RETURNS text
    LANGUAGE sql
    AS $$
    SELECT translate(encode(data, 'base64'), '+/=', '-_');
$$;


ALTER FUNCTION public.url_encode(data bytea) OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: harmonogramtemperatur; Type: TABLE; Schema: public; Owner: mateuszsterma
--

CREATE TABLE public.harmonogramtemperatur (
    id integer NOT NULL,
    dzien_tygodnia integer NOT NULL,
    godzina_rozpoczecia time without time zone NOT NULL,
    godzina_zakonczenia time without time zone NOT NULL,
    zadana_temperatura numeric(4,1) NOT NULL,
    CONSTRAINT poprawny_czas CHECK ((godzina_rozpoczecia < godzina_zakonczenia)),
    CONSTRAINT poprawny_dzien CHECK (((dzien_tygodnia >= 1) AND (dzien_tygodnia <= 7)))
);


ALTER TABLE public.harmonogramtemperatur OWNER TO mateuszsterma;

--
-- Name: COLUMN harmonogramtemperatur.dzien_tygodnia; Type: COMMENT; Schema: public; Owner: mateuszsterma
--

COMMENT ON COLUMN public.harmonogramtemperatur.dzien_tygodnia IS '1=Poniedziałek, 7=Niedziela';


--
-- Name: harmonogram; Type: VIEW; Schema: api; Owner: mateuszsterma
--

CREATE VIEW api.harmonogram AS
 SELECT id,
    dzien_tygodnia,
    godzina_rozpoczecia,
    godzina_zakonczenia,
    zadana_temperatura
   FROM public.harmonogramtemperatur;


ALTER VIEW api.harmonogram OWNER TO mateuszsterma;

--
-- Name: pracownicy; Type: TABLE; Schema: public; Owner: mateuszsterma
--

CREATE TABLE public.pracownicy (
    id integer NOT NULL,
    imie text,
    nazwisko text,
    karta text,
    aktywny boolean DEFAULT true NOT NULL
);


ALTER TABLE public.pracownicy OWNER TO mateuszsterma;

--
-- Name: rejestrwejsc; Type: TABLE; Schema: public; Owner: mateuszsterma
--

CREATE TABLE public.rejestrwejsc (
    data timestamp without time zone DEFAULT now() NOT NULL,
    karta text NOT NULL,
    typ text NOT NULL,
    CONSTRAINT sprawdz_typ CHECK ((typ = ANY (ARRAY['we'::text, 'wy'::text])))
);


ALTER TABLE public.rejestrwejsc OWNER TO mateuszsterma;

--
-- Name: historia_wejsc; Type: VIEW; Schema: api; Owner: mateuszsterma
--

CREATE VIEW api.historia_wejsc AS
 SELECT p.imie,
    p.nazwisko,
    r.data,
    r.typ
   FROM (public.rejestrwejsc r
     JOIN public.pracownicy p ON ((r.karta = p.karta)));


ALTER VIEW api.historia_wejsc OWNER TO mateuszsterma;

--
-- Name: konfiguracjasystemu; Type: TABLE; Schema: public; Owner: mateuszsterma
--

CREATE TABLE public.konfiguracjasystemu (
    id integer DEFAULT 1 NOT NULL,
    tryb_manualny boolean DEFAULT false NOT NULL,
    koniec_trybu_manualnego timestamp without time zone,
    CONSTRAINT czymoznadodacdate CHECK (((tryb_manualny = true) OR (koniec_trybu_manualnego IS NULL))),
    CONSTRAINT konfiguracjasystemu_id_check CHECK ((id = 1))
);


ALTER TABLE public.konfiguracjasystemu OWNER TO mateuszsterma;

--
-- Name: konfiguracja; Type: VIEW; Schema: api; Owner: mateuszsterma
--

CREATE VIEW api.konfiguracja AS
 SELECT id,
    tryb_manualny,
    koniec_trybu_manualnego
   FROM public.konfiguracjasystemu;


ALTER VIEW api.konfiguracja OWNER TO mateuszsterma;

--
-- Name: powiadomienia; Type: TABLE; Schema: public; Owner: mateuszsterma
--

CREATE TABLE public.powiadomienia (
    id integer NOT NULL,
    data_wystapienia timestamp without time zone DEFAULT now(),
    kod_bledu text NOT NULL,
    komunikat text NOT NULL
);


ALTER TABLE public.powiadomienia OWNER TO mateuszsterma;

--
-- Name: powiadomienia; Type: VIEW; Schema: api; Owner: mateuszsterma
--

CREATE VIEW api.powiadomienia AS
 SELECT id,
    data_wystapienia,
    kod_bledu,
    komunikat
   FROM public.powiadomienia;


ALTER VIEW api.powiadomienia OWNER TO mateuszsterma;

--
-- Name: pracownicy; Type: VIEW; Schema: api; Owner: postgres
--

CREATE VIEW api.pracownicy AS
 SELECT id,
    imie,
    nazwisko,
    karta,
    aktywny
   FROM public.pracownicy;


ALTER VIEW api.pracownicy OWNER TO postgres;

--
-- Name: stansystemu; Type: TABLE; Schema: public; Owner: mateuszsterma
--

CREATE TABLE public.stansystemu (
    nazwa_komponentu text NOT NULL,
    wartosc numeric(4,1) NOT NULL
);


ALTER TABLE public.stansystemu OWNER TO mateuszsterma;

--
-- Name: stan_systemu; Type: VIEW; Schema: api; Owner: mateuszsterma
--

CREATE VIEW api.stan_systemu AS
 SELECT nazwa_komponentu,
    wartosc
   FROM public.stansystemu;


ALTER VIEW api.stan_systemu OWNER TO mateuszsterma;

--
-- Name: harmonogramtemperatur_id_seq; Type: SEQUENCE; Schema: public; Owner: mateuszsterma
--

CREATE SEQUENCE public.harmonogramtemperatur_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.harmonogramtemperatur_id_seq OWNER TO mateuszsterma;

--
-- Name: harmonogramtemperatur_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: mateuszsterma
--

ALTER SEQUENCE public.harmonogramtemperatur_id_seq OWNED BY public.harmonogramtemperatur.id;


--
-- Name: pomiary; Type: TABLE; Schema: public; Owner: mateuszsterma
--

CREATE TABLE public.pomiary (
    data_pomiaru timestamp without time zone DEFAULT now() NOT NULL,
    nazwa_komponentu text NOT NULL,
    wartosc numeric(4,1) NOT NULL
);


ALTER TABLE public.pomiary OWNER TO mateuszsterma;

--
-- Name: powiadomienia_id_seq; Type: SEQUENCE; Schema: public; Owner: mateuszsterma
--

CREATE SEQUENCE public.powiadomienia_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.powiadomienia_id_seq OWNER TO mateuszsterma;

--
-- Name: powiadomienia_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: mateuszsterma
--

ALTER SEQUENCE public.powiadomienia_id_seq OWNED BY public.powiadomienia.id;


--
-- Name: pracownicy_id_seq; Type: SEQUENCE; Schema: public; Owner: mateuszsterma
--

ALTER TABLE public.pracownicy ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pracownicy_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: ustawieniamanualne; Type: TABLE; Schema: public; Owner: mateuszsterma
--

CREATE TABLE public.ustawieniamanualne (
    nazwa_komponentu text NOT NULL,
    wartosc_manualna numeric(4,1) NOT NULL
);


ALTER TABLE public.ustawieniamanualne OWNER TO mateuszsterma;

--
-- Name: harmonogramtemperatur id; Type: DEFAULT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.harmonogramtemperatur ALTER COLUMN id SET DEFAULT nextval('public.harmonogramtemperatur_id_seq'::regclass);


--
-- Name: powiadomienia id; Type: DEFAULT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.powiadomienia ALTER COLUMN id SET DEFAULT nextval('public.powiadomienia_id_seq'::regclass);


--
-- Data for Name: harmonogramtemperatur; Type: TABLE DATA; Schema: public; Owner: mateuszsterma
--

COPY public.harmonogramtemperatur (id, dzien_tygodnia, godzina_rozpoczecia, godzina_zakonczenia, zadana_temperatura) FROM stdin;
3	6	10:00:00	23:00:00	23.0
6	2	08:00:00	16:00:00	21.0
5	1	16:00:00	23:00:00	21.0
7	3	08:00:00	16:00:00	21.0
14	1	08:00:00	16:00:00	21.0
\.


--
-- Data for Name: konfiguracjasystemu; Type: TABLE DATA; Schema: public; Owner: mateuszsterma
--

COPY public.konfiguracjasystemu (id, tryb_manualny, koniec_trybu_manualnego) FROM stdin;
1	f	\N
\.


--
-- Data for Name: pomiary; Type: TABLE DATA; Schema: public; Owner: mateuszsterma
--

COPY public.pomiary (data_pomiaru, nazwa_komponentu, wartosc) FROM stdin;
2026-01-10 12:59:38.999276	temperatura_pomieszczenia	19.2
2026-01-10 12:59:38.999276	temperatura_hali	21.1
2026-01-10 12:59:38.999276	temperatura_pieca	106.9
2026-01-10 12:59:38.999276	piec	0.0
2026-01-10 12:59:38.999276	wiatrak	0.0
2026-01-10 12:59:38.999276	grzejnik_1	1.6
2026-01-10 12:59:38.999276	grzejnik_2	4.8
2026-01-10 12:59:38.999276	grzejnik_3	4.6
2026-01-10 12:29:38.999276	temperatura_pomieszczenia	21.3
2026-01-10 12:29:38.999276	temperatura_hali	22.9
2026-01-10 12:29:38.999276	temperatura_pieca	101.5
2026-01-10 12:29:38.999276	piec	1.0
2026-01-10 12:29:38.999276	wiatrak	1.0
2026-01-10 12:29:38.999276	grzejnik_1	1.0
2026-01-10 12:29:38.999276	grzejnik_2	0.9
2026-01-10 12:29:38.999276	grzejnik_3	3.6
2026-01-10 11:59:38.999276	temperatura_pomieszczenia	19.0
2026-01-10 11:59:38.999276	temperatura_hali	20.4
2026-01-10 11:59:38.999276	temperatura_pieca	126.6
2026-01-10 11:59:38.999276	piec	1.0
2026-01-10 11:59:38.999276	wiatrak	0.0
2026-01-10 11:59:38.999276	grzejnik_1	1.2
2026-01-10 11:59:38.999276	grzejnik_2	1.5
2026-01-10 11:59:38.999276	grzejnik_3	4.1
2026-01-10 11:29:38.999276	temperatura_pomieszczenia	22.7
2026-01-10 11:29:38.999276	temperatura_hali	19.0
2026-01-10 11:29:38.999276	temperatura_pieca	119.1
2026-01-10 11:29:38.999276	piec	0.0
2026-01-10 11:29:38.999276	wiatrak	0.0
2026-01-10 11:29:38.999276	grzejnik_1	4.3
2026-01-10 11:29:38.999276	grzejnik_2	4.3
2026-01-10 11:29:38.999276	grzejnik_3	1.4
2026-01-10 10:59:38.999276	temperatura_pomieszczenia	21.6
2026-01-10 10:59:38.999276	temperatura_hali	19.2
2026-01-10 10:59:38.999276	temperatura_pieca	106.6
2026-01-10 10:59:38.999276	piec	1.0
2026-01-10 10:59:38.999276	wiatrak	1.0
2026-01-10 10:59:38.999276	grzejnik_1	0.3
2026-01-10 10:59:38.999276	grzejnik_2	1.7
2026-01-10 10:59:38.999276	grzejnik_3	4.4
2026-01-10 10:29:38.999276	temperatura_pomieszczenia	20.1
2026-01-10 10:29:38.999276	temperatura_hali	22.6
2026-01-10 10:29:38.999276	temperatura_pieca	103.6
2026-01-10 10:29:38.999276	piec	0.0
2026-01-10 10:29:38.999276	wiatrak	1.0
2026-01-10 10:29:38.999276	grzejnik_1	4.6
2026-01-10 10:29:38.999276	grzejnik_2	1.8
2026-01-10 10:29:38.999276	grzejnik_3	4.2
2026-01-10 09:59:38.999276	temperatura_pomieszczenia	18.5
2026-01-10 09:59:38.999276	temperatura_hali	19.7
2026-01-10 09:59:38.999276	temperatura_pieca	119.6
2026-01-10 09:59:38.999276	piec	0.0
2026-01-10 09:59:38.999276	wiatrak	1.0
2026-01-10 09:59:38.999276	grzejnik_1	0.6
2026-01-10 09:59:38.999276	grzejnik_2	2.1
2026-01-10 09:59:38.999276	grzejnik_3	0.3
2026-01-10 09:29:38.999276	temperatura_pomieszczenia	18.8
2026-01-10 09:29:38.999276	temperatura_hali	22.5
2026-01-10 09:29:38.999276	temperatura_pieca	102.9
2026-01-10 09:29:38.999276	piec	1.0
2026-01-10 09:29:38.999276	wiatrak	1.0
2026-01-10 09:29:38.999276	grzejnik_1	0.6
2026-01-10 09:29:38.999276	grzejnik_2	2.6
2026-01-10 09:29:38.999276	grzejnik_3	1.4
2026-01-10 08:59:38.999276	temperatura_pomieszczenia	22.1
2026-01-10 08:59:38.999276	temperatura_hali	20.1
2026-01-10 08:59:38.999276	temperatura_pieca	119.8
2026-01-10 08:59:38.999276	piec	0.0
2026-01-10 08:59:38.999276	wiatrak	1.0
2026-01-10 08:59:38.999276	grzejnik_1	2.1
2026-01-10 08:59:38.999276	grzejnik_2	2.9
2026-01-10 08:59:38.999276	grzejnik_3	2.8
2026-01-10 08:29:38.999276	temperatura_pomieszczenia	22.0
2026-01-10 08:29:38.999276	temperatura_hali	22.3
2026-01-10 08:29:38.999276	temperatura_pieca	105.0
2026-01-10 08:29:38.999276	piec	1.0
2026-01-10 08:29:38.999276	wiatrak	0.0
2026-01-10 08:29:38.999276	grzejnik_1	1.8
2026-01-10 08:29:38.999276	grzejnik_2	2.1
2026-01-10 08:29:38.999276	grzejnik_3	3.0
2026-01-10 07:59:38.999276	temperatura_pomieszczenia	18.8
2026-01-10 07:59:38.999276	temperatura_hali	19.8
2026-01-10 07:59:38.999276	temperatura_pieca	112.3
2026-01-10 07:59:38.999276	piec	0.0
2026-01-10 07:59:38.999276	wiatrak	1.0
2026-01-10 07:59:38.999276	grzejnik_1	1.3
2026-01-10 07:59:38.999276	grzejnik_2	4.7
2026-01-10 07:59:38.999276	grzejnik_3	2.6
2026-01-10 07:29:38.999276	temperatura_pomieszczenia	20.9
2026-01-10 07:29:38.999276	temperatura_hali	18.3
2026-01-10 07:29:38.999276	temperatura_pieca	121.5
2026-01-10 07:29:38.999276	piec	0.0
2026-01-10 07:29:38.999276	wiatrak	0.0
2026-01-10 07:29:38.999276	grzejnik_1	4.0
2026-01-10 07:29:38.999276	grzejnik_2	4.7
2026-01-10 07:29:38.999276	grzejnik_3	2.5
2026-01-10 06:59:38.999276	temperatura_pomieszczenia	18.8
2026-01-10 06:59:38.999276	temperatura_hali	21.0
2026-01-10 06:59:38.999276	temperatura_pieca	122.1
2026-01-10 06:59:38.999276	piec	1.0
2026-01-10 06:59:38.999276	wiatrak	1.0
2026-01-10 06:59:38.999276	grzejnik_1	4.1
2026-01-10 06:59:38.999276	grzejnik_2	0.8
2026-01-10 06:59:38.999276	grzejnik_3	2.4
2026-01-10 06:29:38.999276	temperatura_pomieszczenia	21.0
2026-01-10 06:29:38.999276	temperatura_hali	18.9
2026-01-10 06:29:38.999276	temperatura_pieca	105.8
2026-01-10 06:29:38.999276	piec	1.0
2026-01-10 06:29:38.999276	wiatrak	0.0
2026-01-10 06:29:38.999276	grzejnik_1	1.8
2026-01-10 06:29:38.999276	grzejnik_2	1.2
2026-01-10 06:29:38.999276	grzejnik_3	3.3
2026-01-10 05:59:38.999276	temperatura_pomieszczenia	21.5
2026-01-10 05:59:38.999276	temperatura_hali	22.0
2026-01-10 05:59:38.999276	temperatura_pieca	116.3
2026-01-10 05:59:38.999276	piec	1.0
2026-01-10 05:59:38.999276	wiatrak	0.0
2026-01-10 05:59:38.999276	grzejnik_1	1.4
2026-01-10 05:59:38.999276	grzejnik_2	0.6
2026-01-10 05:59:38.999276	grzejnik_3	4.5
2026-01-10 20:26:16.870891	temperatura_pomieszczenia	22.0
2026-01-10 20:26:16.870891	temperatura_hali	18.8
2026-01-10 20:26:16.870891	temperatura_pieca	102.7
2026-01-10 20:26:16.870891	piec	1.0
2026-01-10 20:26:16.870891	wiatrak	0.0
2026-01-10 20:26:16.870891	grzejnik_1	0.6
2026-01-10 20:26:16.870891	grzejnik_2	0.6
2026-01-10 20:26:16.870891	grzejnik_3	2.8
2026-01-10 20:21:16.870891	temperatura_pomieszczenia	24.6
2026-01-10 20:21:16.870891	temperatura_hali	24.4
2026-01-10 20:21:16.870891	temperatura_pieca	96.7
2026-01-10 20:21:16.870891	piec	0.0
2026-01-10 20:21:16.870891	wiatrak	0.0
2026-01-10 20:21:16.870891	grzejnik_1	3.3
2026-01-10 20:21:16.870891	grzejnik_2	1.8
2026-01-10 20:21:16.870891	grzejnik_3	1.3
2026-01-10 20:16:16.870891	temperatura_pomieszczenia	23.7
2026-01-10 20:16:16.870891	temperatura_hali	22.9
2026-01-10 20:16:16.870891	temperatura_pieca	97.8
2026-01-10 20:16:16.870891	piec	0.0
2026-01-10 20:16:16.870891	wiatrak	0.0
2026-01-10 20:16:16.870891	grzejnik_1	4.2
2026-01-10 20:16:16.870891	grzejnik_2	3.8
2026-01-10 20:16:16.870891	grzejnik_3	4.2
2026-01-10 20:06:16.870891	temperatura_pomieszczenia	22.0
2026-01-10 20:06:16.870891	temperatura_hali	20.3
2026-01-10 20:06:16.870891	temperatura_pieca	109.9
2026-01-10 20:06:16.870891	piec	0.0
2026-01-10 20:06:16.870891	wiatrak	0.0
2026-01-10 20:06:16.870891	grzejnik_1	3.9
2026-01-10 20:06:16.870891	grzejnik_2	0.3
2026-01-10 20:06:16.870891	grzejnik_3	0.4
2026-01-10 19:56:16.870891	temperatura_pomieszczenia	21.6
2026-01-10 19:56:16.870891	temperatura_hali	20.8
2026-01-10 19:56:16.870891	temperatura_pieca	113.0
2026-01-10 19:56:16.870891	piec	1.0
2026-01-10 19:56:16.870891	wiatrak	1.0
2026-01-10 19:56:16.870891	grzejnik_1	0.0
2026-01-10 19:56:16.870891	grzejnik_2	2.9
2026-01-10 19:56:16.870891	grzejnik_3	4.7
2026-01-10 19:41:16.870891	temperatura_pomieszczenia	23.0
2026-01-10 19:41:16.870891	temperatura_hali	24.5
2026-01-10 19:41:16.870891	temperatura_pieca	113.9
2026-01-10 19:41:16.870891	piec	1.0
2026-01-10 19:41:16.870891	wiatrak	0.0
2026-01-10 19:41:16.870891	grzejnik_1	0.2
2026-01-10 19:41:16.870891	grzejnik_2	2.6
2026-01-10 19:41:16.870891	grzejnik_3	2.8
2026-01-10 18:26:16.870891	temperatura_pomieszczenia	23.0
2026-01-10 18:26:16.870891	temperatura_hali	19.0
2026-01-10 18:26:16.870891	temperatura_pieca	101.8
2026-01-10 18:26:16.870891	piec	1.0
2026-01-10 18:26:16.870891	wiatrak	1.0
2026-01-10 18:26:16.870891	grzejnik_1	2.3
2026-01-10 18:26:16.870891	grzejnik_2	4.8
2026-01-10 18:26:16.870891	grzejnik_3	0.6
2026-01-10 15:26:16.870891	temperatura_pomieszczenia	24.4
2026-01-10 15:26:16.870891	temperatura_hali	24.6
2026-01-10 15:26:16.870891	temperatura_pieca	95.2
2026-01-10 15:26:16.870891	piec	1.0
2026-01-10 15:26:16.870891	wiatrak	0.0
2026-01-10 15:26:16.870891	grzejnik_1	3.2
2026-01-10 15:26:16.870891	grzejnik_2	3.8
2026-01-10 15:26:16.870891	grzejnik_3	4.8
2026-01-10 10:26:16.870891	temperatura_pomieszczenia	23.8
2026-01-10 10:26:16.870891	temperatura_hali	24.1
2026-01-10 10:26:16.870891	temperatura_pieca	99.4
2026-01-10 10:26:16.870891	piec	0.0
2026-01-10 10:26:16.870891	wiatrak	1.0
2026-01-10 10:26:16.870891	grzejnik_1	1.0
2026-01-10 10:26:16.870891	grzejnik_2	4.5
2026-01-10 10:26:16.870891	grzejnik_3	2.7
2026-01-10 05:26:16.870891	temperatura_pomieszczenia	21.6
2026-01-10 05:26:16.870891	temperatura_hali	20.5
2026-01-10 05:26:16.870891	temperatura_pieca	105.0
2026-01-10 05:26:16.870891	piec	0.0
2026-01-10 05:26:16.870891	wiatrak	1.0
2026-01-10 05:26:16.870891	grzejnik_1	4.4
2026-01-10 05:26:16.870891	grzejnik_2	4.5
2026-01-10 05:26:16.870891	grzejnik_3	2.0
2026-01-09 19:26:16.870891	temperatura_pomieszczenia	18.3
2026-01-09 19:26:16.870891	temperatura_hali	19.4
2026-01-09 19:26:16.870891	temperatura_pieca	117.0
2026-01-09 19:26:16.870891	piec	1.0
2026-01-09 19:26:16.870891	wiatrak	1.0
2026-01-09 19:26:16.870891	grzejnik_1	0.7
2026-01-09 19:26:16.870891	grzejnik_2	2.7
2026-01-09 19:26:16.870891	grzejnik_3	4.4
2026-01-08 21:46:16.870891	temperatura_pomieszczenia	18.1
2026-01-08 21:46:16.870891	temperatura_hali	20.0
2026-01-08 21:46:16.870891	temperatura_pieca	90.7
2026-01-08 21:46:16.870891	piec	0.0
2026-01-08 21:46:16.870891	wiatrak	1.0
2026-01-08 21:46:16.870891	grzejnik_1	4.1
2026-01-08 21:46:16.870891	grzejnik_2	3.9
2026-01-08 21:46:16.870891	grzejnik_3	0.1
2026-01-08 01:46:16.870891	temperatura_pomieszczenia	22.4
2026-01-08 01:46:16.870891	temperatura_hali	19.6
2026-01-08 01:46:16.870891	temperatura_pieca	97.1
2026-01-08 01:46:16.870891	piec	0.0
2026-01-08 01:46:16.870891	wiatrak	1.0
2026-01-08 01:46:16.870891	grzejnik_1	4.5
2026-01-08 01:46:16.870891	grzejnik_2	3.7
2026-01-08 01:46:16.870891	grzejnik_3	4.2
\.


--
-- Data for Name: powiadomienia; Type: TABLE DATA; Schema: public; Owner: mateuszsterma
--

COPY public.powiadomienia (id, data_wystapienia, kod_bledu, komunikat) FROM stdin;
12	2026-01-14 21:34:54.90643	ERR_TEMP_HIGH	Temperatura pieca przekroczyła 120 stopni!
13	2026-01-13 23:34:54.90643	INFO_LOGIN	Udane logowanie administratora.
14	2026-01-11 23:34:54.90643	WARN_SENSOR	Czujnik nr 3 nie odpowiada.
\.


--
-- Data for Name: pracownicy; Type: TABLE DATA; Schema: public; Owner: mateuszsterma
--

COPY public.pracownicy (id, imie, nazwisko, karta, aktywny) FROM stdin;
20	Piotr	Wiśniewski	KARTA_003	t
21	Admin	Systemu	MASTER_KEY	t
19	Anna	Nowak	KARTA_002	f
18	Jan	rzezniik	KARTA_001	t
\.


--
-- Data for Name: rejestrwejsc; Type: TABLE DATA; Schema: public; Owner: mateuszsterma
--

COPY public.rejestrwejsc (data, karta, typ) FROM stdin;
2026-01-10 09:25:11.513543	KARTA_001	we
2026-01-10 12:25:11.513543	KARTA_002	we
2026-01-10 13:25:11.513543	KARTA_001	wy
2026-01-10 09:25:43.635716	KARTA_001	we
2026-01-10 12:25:43.635716	KARTA_002	we
2026-01-10 13:25:43.635716	KARTA_001	wy
\.


--
-- Data for Name: stansystemu; Type: TABLE DATA; Schema: public; Owner: mateuszsterma
--

COPY public.stansystemu (nazwa_komponentu, wartosc) FROM stdin;
temperatura_pomieszczenia	21.5
temperatura_hali	18.2
temperatura_pieca	115.0
piec	1.0
wiatrak	0.0
grzejnik_1	2.5
grzejnik_2	3.0
grzejnik_3	0.0
\.


--
-- Data for Name: ustawieniamanualne; Type: TABLE DATA; Schema: public; Owner: mateuszsterma
--

COPY public.ustawieniamanualne (nazwa_komponentu, wartosc_manualna) FROM stdin;
piec	1.0
wiatrak	1.0
grzejnik_1	1.7
grzejnik_2	1.7
grzejnik_3	4.2
\.


--
-- Name: harmonogramtemperatur_id_seq; Type: SEQUENCE SET; Schema: public; Owner: mateuszsterma
--

SELECT pg_catalog.setval('public.harmonogramtemperatur_id_seq', 20, true);


--
-- Name: powiadomienia_id_seq; Type: SEQUENCE SET; Schema: public; Owner: mateuszsterma
--

SELECT pg_catalog.setval('public.powiadomienia_id_seq', 14, true);


--
-- Name: pracownicy_id_seq; Type: SEQUENCE SET; Schema: public; Owner: mateuszsterma
--

SELECT pg_catalog.setval('public.pracownicy_id_seq', 22, true);


--
-- Name: harmonogramtemperatur harmonogramtemperatur_pkey; Type: CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.harmonogramtemperatur
    ADD CONSTRAINT harmonogramtemperatur_pkey PRIMARY KEY (id);


--
-- Name: konfiguracjasystemu konfiguracjasystemu_pkey; Type: CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.konfiguracjasystemu
    ADD CONSTRAINT konfiguracjasystemu_pkey PRIMARY KEY (id);


--
-- Name: pomiary pomiary_pkey; Type: CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.pomiary
    ADD CONSTRAINT pomiary_pkey PRIMARY KEY (data_pomiaru, nazwa_komponentu);


--
-- Name: powiadomienia powiadomienia_pkey; Type: CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.powiadomienia
    ADD CONSTRAINT powiadomienia_pkey PRIMARY KEY (id);


--
-- Name: pracownicy pracownicy_pkey; Type: CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.pracownicy
    ADD CONSTRAINT pracownicy_pkey PRIMARY KEY (id);


--
-- Name: rejestrwejsc rejestrwejsc_pkey; Type: CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.rejestrwejsc
    ADD CONSTRAINT rejestrwejsc_pkey PRIMARY KEY (data, karta);


--
-- Name: stansystemu stansystemu_pkey; Type: CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.stansystemu
    ADD CONSTRAINT stansystemu_pkey PRIMARY KEY (nazwa_komponentu);


--
-- Name: pracownicy unique_karta; Type: CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.pracownicy
    ADD CONSTRAINT unique_karta UNIQUE (karta);


--
-- Name: ustawieniamanualne ustawieniamanualne_pkey; Type: CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.ustawieniamanualne
    ADD CONSTRAINT ustawieniamanualne_pkey PRIMARY KEY (nazwa_komponentu);


--
-- Name: harmonogramtemperatur trg_sprawdz_harmonogram; Type: TRIGGER; Schema: public; Owner: mateuszsterma
--

CREATE TRIGGER trg_sprawdz_harmonogram BEFORE INSERT OR UPDATE ON public.harmonogramtemperatur FOR EACH ROW EXECUTE FUNCTION public.sprawdz_pokrywanie_terminow();


--
-- Name: ustawieniamanualne fk_komponent_stan; Type: FK CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.ustawieniamanualne
    ADD CONSTRAINT fk_komponent_stan FOREIGN KEY (nazwa_komponentu) REFERENCES public.stansystemu(nazwa_komponentu) ON DELETE CASCADE;


--
-- Name: pomiary fk_pomiar_komponent; Type: FK CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.pomiary
    ADD CONSTRAINT fk_pomiar_komponent FOREIGN KEY (nazwa_komponentu) REFERENCES public.stansystemu(nazwa_komponentu);


--
-- Name: rejestrwejsc rejestrwejsc_karta_fkey; Type: FK CONSTRAINT; Schema: public; Owner: mateuszsterma
--

ALTER TABLE ONLY public.rejestrwejsc
    ADD CONSTRAINT rejestrwejsc_karta_fkey FOREIGN KEY (karta) REFERENCES public.pracownicy(karta) ON UPDATE CASCADE;


--
-- Name: SCHEMA api; Type: ACL; Schema: -; Owner: mateuszsterma
--

GRANT USAGE ON SCHEMA api TO app_user;
GRANT USAGE ON SCHEMA api TO app_admin;
GRANT USAGE ON SCHEMA api TO web_anon;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: pg_database_owner
--

GRANT USAGE ON SCHEMA public TO app_user;
GRANT USAGE ON SCHEMA public TO app_admin;
GRANT USAGE ON SCHEMA public TO web_anon;


--
-- Name: FUNCTION login_as_admin(pass text); Type: ACL; Schema: api; Owner: postgres
--

GRANT ALL ON FUNCTION api.login_as_admin(pass text) TO web_anon;


--
-- Name: FUNCTION pobierz_pomiary(komponent text, zakres text); Type: ACL; Schema: api; Owner: mateuszsterma
--

GRANT ALL ON FUNCTION api.pobierz_pomiary(komponent text, zakres text) TO app_user;
GRANT ALL ON FUNCTION api.pobierz_pomiary(komponent text, zakres text) TO app_admin;


--
-- Name: FUNCTION ustaw_sterowanie(komponent text, wartosc numeric); Type: ACL; Schema: api; Owner: mateuszsterma
--

GRANT ALL ON FUNCTION api.ustaw_sterowanie(komponent text, wartosc numeric) TO app_admin;


--
-- Name: TABLE harmonogramtemperatur; Type: ACL; Schema: public; Owner: mateuszsterma
--

GRANT ALL ON TABLE public.harmonogramtemperatur TO app_admin;


--
-- Name: TABLE harmonogram; Type: ACL; Schema: api; Owner: mateuszsterma
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE api.harmonogram TO app_admin;
GRANT SELECT ON TABLE api.harmonogram TO app_user;


--
-- Name: TABLE pracownicy; Type: ACL; Schema: public; Owner: mateuszsterma
--

GRANT ALL ON TABLE public.pracownicy TO app_admin;


--
-- Name: TABLE rejestrwejsc; Type: ACL; Schema: public; Owner: mateuszsterma
--

GRANT ALL ON TABLE public.rejestrwejsc TO app_admin;
GRANT SELECT ON TABLE public.rejestrwejsc TO app_user;


--
-- Name: TABLE historia_wejsc; Type: ACL; Schema: api; Owner: mateuszsterma
--

GRANT SELECT ON TABLE api.historia_wejsc TO app_user;
GRANT SELECT ON TABLE api.historia_wejsc TO app_admin;


--
-- Name: TABLE konfiguracjasystemu; Type: ACL; Schema: public; Owner: mateuszsterma
--

GRANT ALL ON TABLE public.konfiguracjasystemu TO app_admin;


--
-- Name: TABLE konfiguracja; Type: ACL; Schema: api; Owner: mateuszsterma
--

GRANT SELECT ON TABLE api.konfiguracja TO app_user;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE api.konfiguracja TO app_admin;


--
-- Name: TABLE powiadomienia; Type: ACL; Schema: api; Owner: mateuszsterma
--

GRANT SELECT,INSERT,DELETE ON TABLE api.powiadomienia TO app_admin;
GRANT SELECT,DELETE ON TABLE api.powiadomienia TO app_user;


--
-- Name: TABLE pracownicy; Type: ACL; Schema: api; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE api.pracownicy TO app_admin;
GRANT SELECT ON TABLE api.pracownicy TO app_user;


--
-- Name: TABLE stan_systemu; Type: ACL; Schema: api; Owner: mateuszsterma
--

GRANT SELECT ON TABLE api.stan_systemu TO app_user;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE api.stan_systemu TO app_admin;


--
-- Name: SEQUENCE harmonogramtemperatur_id_seq; Type: ACL; Schema: public; Owner: mateuszsterma
--

GRANT SELECT,USAGE ON SEQUENCE public.harmonogramtemperatur_id_seq TO app_admin;


--
-- Name: TABLE pomiary; Type: ACL; Schema: public; Owner: mateuszsterma
--

GRANT SELECT ON TABLE public.pomiary TO app_admin;
GRANT SELECT ON TABLE public.pomiary TO app_user;


--
-- Name: SEQUENCE powiadomienia_id_seq; Type: ACL; Schema: public; Owner: mateuszsterma
--

GRANT SELECT,USAGE ON SEQUENCE public.powiadomienia_id_seq TO app_admin;


--
-- Name: TABLE ustawieniamanualne; Type: ACL; Schema: public; Owner: mateuszsterma
--

GRANT ALL ON TABLE public.ustawieniamanualne TO app_admin;


--
-- PostgreSQL database dump complete
--

\unrestrict JtImdiK6n0jxrWQ1IwGM2WdVzWeoGlKFdmInup2d840qFeBssVpE8b3YSoPP5vE

