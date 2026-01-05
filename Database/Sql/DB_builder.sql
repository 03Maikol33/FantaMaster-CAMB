-- ==========================================================
-- SCRIPT DATABASE FANTAMASTER v5.5
-- ==========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. PULIZIA
DROP TABLE IF EXISTS eventi;
DROP TABLE IF EXISTS voti; -- ELIMINATA PER SEMPRE
DROP TABLE IF EXISTS partite;
DROP TABLE IF EXISTS dettaglio_formazione;
DROP TABLE IF EXISTS formazioni;
DROP TABLE IF EXISTS scambi;
DROP TABLE IF EXISTS offerte_asta;
DROP TABLE IF EXISTS giocatori_rose;
DROP TABLE IF EXISTS rosa;
DROP TABLE IF EXISTS utenti_leghe;
DROP TABLE IF EXISTS giornate;
DROP TABLE IF EXISTS messaggi;
DROP TABLE IF EXISTS richieste_accesso;
DROP TABLE IF EXISTS regole;
DROP TABLE IF EXISTS leghe;
DROP TABLE IF EXISTS giocatori;
DROP TABLE IF EXISTS utenti;

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================================
-- SEZIONE A: ANAGRAFICHE
-- ==========================================================

CREATE TABLE utenti (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    hash_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    avatar BLOB
);

CREATE TABLE giocatori (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_esterno INT NOT NULL UNIQUE, 
    nome VARCHAR(100) NOT NULL,
    squadra_reale VARCHAR(50) NOT NULL,
    ruolo VARCHAR(5) NOT NULL,
    quotazione_iniziale INT DEFAULT 1,
    attivo BOOLEAN DEFAULT TRUE,
    INDEX (id_esterno)
);

-- ==========================================================
-- SEZIONE B: STRUTTURA LEGA
-- ==========================================================

CREATE TABLE leghe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    icona mediumblob, 
    max_membri INT NOT NULL,
    id_creatore INT NOT NULL,
    iscrizioni_chiuse BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    codice_invito VARCHAR(10) UNIQUE,
    modalita VARCHAR(30) DEFAULT 'punti_totali',
    moduli_consentiti VARCHAR(255) DEFAULT '3-4-3;3-5-2;4-3-3;4-4-2;4-5-1;5-3-2;5-4-1',
    
    mercato_aperto BOOLEAN DEFAULT FALSE,--avvia e chiude il periodo di scambi--
    asta_aperta BOOLEAN DEFAULT TRUE, --apre e chiude l'asta iniziale--
    turno_asta_utente_id INT,
    giocatore_chiamato_id INT,
    
    FOREIGN KEY (id_creatore) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (turno_asta_utente_id) REFERENCES utenti(id) ON DELETE SET NULL,
    FOREIGN KEY (giocatore_chiamato_id) REFERENCES giocatori(id) ON DELETE SET NULL
);

CREATE TABLE regole (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lega_id INT NOT NULL,
    budget_iniziale INT NOT NULL DEFAULT 500,
    
    -- === NUOVI CAMPI: LIMITI ROSA (STEP 0) ===
    max_portieri INT DEFAULT 3,
    max_difensori INT DEFAULT 8,
    max_centrocampisti INT DEFAULT 8,
    max_attaccanti INT DEFAULT 6,
    
    -- CONFIGURAZIONE PUNTEGGI
    voto_base DECIMAL(4,1) DEFAULT 6.0, 
    
    bonus_gol DECIMAL(4,1) DEFAULT 3.0,
    bonus_assist DECIMAL(4,1) DEFAULT 1.0,
    bonus_imbattibilita DECIMAL(4,1) DEFAULT 1.0,
    bonus_rigore_parato DECIMAL(4,1) DEFAULT 3.0,
    bonus_fattore_campo DECIMAL(4,1) DEFAULT 1.0,
    malus_gol_subito DECIMAL(4,1) DEFAULT 1.0,
    malus_ammonizione DECIMAL(4,1) DEFAULT 0.5,
    malus_espulsione DECIMAL(4,1) DEFAULT 1.0,
    malus_rigore_sbagliato DECIMAL(4,1) DEFAULT 3.0,
    malus_autogol DECIMAL(4,1) DEFAULT 2.0,
    usa_modificatore_difesa BOOLEAN DEFAULT FALSE,
    
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE,
    UNIQUE (lega_id)
);

-- ==========================================================
-- SEZIONE C: PARTITE E GIORNATE
-- ==========================================================

CREATE TABLE giornate (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero_giornata INT NOT NULL,
    data_inizio TIMESTAMP NOT NULL,
    stato ENUM('da_giocare', 'in_corso', 'calcolata') DEFAULT 'da_giocare'
);

CREATE TABLE partite (
    id INT AUTO_INCREMENT PRIMARY KEY,
    giornata_id INT NOT NULL,
    squadra_casa VARCHAR(50) NOT NULL,
    squadra_ospite VARCHAR(50) NOT NULL,
    gol_casa INT DEFAULT 0,
    gol_ospite INT DEFAULT 0,
    data_partita TIMESTAMP,
    FOREIGN KEY (giornata_id) REFERENCES giornate(id) ON DELETE CASCADE
);

-- ==========================================================
-- SEZIONE D: ROSA E FORMAZIONI
-- ==========================================================

CREATE TABLE utenti_leghe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utente_id INT NOT NULL,
    lega_id INT NOT NULL,
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE,
    UNIQUE(utente_id, lega_id)
);

CREATE TABLE rosa (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utenti_leghe_id INT NOT NULL,
    nome_rosa VARCHAR(100) DEFAULT 'La mia Rosa',
    crediti_residui INT DEFAULT 500,
    punteggio_totale DECIMAL(5,1) DEFAULT 0.0,
    FOREIGN KEY (utenti_leghe_id) REFERENCES utenti_leghe(id) ON DELETE CASCADE,
    UNIQUE(utenti_leghe_id)
);

CREATE TABLE giocatori_rose (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rosa_id INT NOT NULL,
    giocatore_id INT NOT NULL,
    costo_acquisto INT NOT NULL DEFAULT 1,
    FOREIGN KEY (rosa_id) REFERENCES rosa(id) ON DELETE CASCADE,
    FOREIGN KEY (giocatore_id) REFERENCES giocatori(id) ON DELETE CASCADE,
    UNIQUE(rosa_id, giocatore_id)
);

-- FORMAZIONI
CREATE TABLE formazioni (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rosa_id INT NOT NULL,
    giornata_id INT NOT NULL,
    modulo_schierato VARCHAR(10) NOT NULL,
    capitano_id INT,
    vice_capitano_id INT,
    totale_fantapunti DECIMAL(5,1) DEFAULT 0.0,
    FOREIGN KEY (rosa_id) REFERENCES rosa(id) ON DELETE CASCADE,
    FOREIGN KEY (giornata_id) REFERENCES giornate(id) ON DELETE CASCADE,
    FOREIGN KEY (capitano_id) REFERENCES giocatori(id) ON DELETE SET NULL,
    UNIQUE(rosa_id, giornata_id)
);

-- DETTAGLIO FORMAZIONE (Semplificata)
CREATE TABLE dettaglio_formazione (
    id INT AUTO_INCREMENT PRIMARY KEY,
    formazione_id INT NOT NULL,
    giocatore_id INT NOT NULL,
    stato ENUM('titolare', 'panchina') NOT NULL,
    ordine_panchina INT DEFAULT 0,
    
    -- Semplificato: Solo il risultato finale
    fantavoto DECIMAL(4,1) DEFAULT 0.0, 
    
    entrato_al_posto_di INT DEFAULT NULL,
    
    FOREIGN KEY (formazione_id) REFERENCES formazioni(id) ON DELETE CASCADE,
    FOREIGN KEY (giocatore_id) REFERENCES giocatori(id) ON DELETE CASCADE
);
ALTER TABLE dettaglio_formazione ADD COLUMN fantavoto_calcolato DOUBLE DEFAULT 0.0; --nuovo campo per memorizzare il fantavoto calcolato
-- ==========================================================
-- SEZIONE E: EVENTI (Sostituiscono i Voti)
-- ==========================================================

-- EVENTI DELLA PARTITA
CREATE TABLE eventi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    partita_id INT NOT NULL,
    giocatore_id INT NOT NULL,
    
    tipo_evento ENUM(
        'GOL_FATTO', 
        'GOL_SUBITO', 
        'RIGORE_PARATO', 
        'RIGORE_SBAGLIATO', 
        'ASSIST', 
        'AMMONIZIONE', 
        'ESPULSIONE', 
        'AUTOGOL', 
        'PORTIERE_IMBATTUTO'
        -- Nota: La semplice "Presenza" è implicita se non ci sono eventi,
        -- il giocatore prende comunque il voto base se ha giocato.
    ) NOT NULL,
    
    valore_bonus DECIMAL(4,1) NOT NULL, 
    
    FOREIGN KEY (partita_id) REFERENCES partite(id) ON DELETE CASCADE,
    FOREIGN KEY (giocatore_id) REFERENCES giocatori(id) ON DELETE CASCADE
);

-- ==========================================================
-- SEZIONE F: ASTA, SCAMBI E UTILITÀ
-- ==========================================================

CREATE TABLE offerte_asta (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lega_id INT NOT NULL,
    giocatore_id INT NOT NULL,
    rosa_id INT NOT NULL,
    tipo ENUM('offerta', 'passo') NOT NULL DEFAULT 'offerta', 
    offerta INT DEFAULT NULL,
    timestamp_offerta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE,
    FOREIGN KEY (giocatore_id) REFERENCES giocatori(id) ON DELETE CASCADE,
    FOREIGN KEY (rosa_id) REFERENCES rosa(id) ON DELETE CASCADE
);

CREATE TABLE scambi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lega_id INT NOT NULL,
    rosa_richiedente_id INT NOT NULL, 
    rosa_ricevente_id INT NOT NULL,   
    giocatore_offerto_id INT NOT NULL,
    giocatore_richiesto_id INT NOT NULL,
    stato ENUM('proposto', 'accettato', 'rifiutato', 'annullato') DEFAULT 'proposto',
    data_proposta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE,
    FOREIGN KEY (rosa_richiedente_id) REFERENCES rosa(id) ON DELETE CASCADE,
    FOREIGN KEY (rosa_ricevente_id) REFERENCES rosa(id) ON DELETE CASCADE,
    FOREIGN KEY (giocatore_offerto_id) REFERENCES giocatori(id) ON DELETE CASCADE,
    FOREIGN KEY (giocatore_richiesto_id) REFERENCES giocatori(id) ON DELETE CASCADE
);

CREATE TABLE messaggi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    testo TEXT NOT NULL,
    data_invio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    utente_id INT NOT NULL,
    lega_id INT NOT NULL,
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE
);

CREATE TABLE richieste_accesso (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utente_id INT NOT NULL,
    lega_id INT NOT NULL,
    data_richiesta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    stato ENUM('in_attesa','accettata','rifiutata') DEFAULT 'in_attesa',
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE
);