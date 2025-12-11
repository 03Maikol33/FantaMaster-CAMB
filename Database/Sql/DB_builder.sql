-- ==================================================
-- SCRIPT DI CREAZIONE DEL DATABASE (Chiara's version)
-- ==================================================

-- 1. Disabilita temporaneamente i controlli sulle chiavi esterne
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Elimina le tabelle se esistono (pulizia totale)
-- L'ordine è importante per evitare errori di vincoli
DROP TABLE IF EXISTS richieste_accesso;
DROP TABLE IF EXISTS utenti_leghe;
DROP TABLE IF EXISTS regole;
DROP TABLE IF EXISTS leghe;
DROP TABLE IF EXISTS utenti;

-- 3. Riabilita i controlli sulle chiavi esterne
SET FOREIGN_KEY_CHECKS = 1;

-- 4. Creazione Tabella Utenti
CREATE TABLE utenti (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    hash_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Creazione Tabella Leghe
-- Aggiornata con la colonna 'modalita' per compatibilità con il codice Java
CREATE TABLE leghe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    icona mediumblob, /* Fino a 16MB */
    max_membri INT NOT NULL,
    id_creatore INT NOT NULL,
    iscrizioni_chiuse BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
<<<<<<< HEAD
<<<<<<< HEAD
    codice_invito VARCHAR(10) UNIQUE,
    -- NUOVA COLONNA: Moduli consentiti (CSV)
    moduli_consentiti VARCHAR(255) DEFAULT '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2',
=======
    codice_invito VARCHAR(10) UNIQUE, -- MANTENUTA: Colonna dal tuo script
>>>>>>> develop
=======
    codice_invito VARCHAR(10) UNIQUE,
    modalita VARCHAR(30) DEFAULT 'punti_totali', -- Default per evitare errori nel Java
>>>>>>> develop
    FOREIGN KEY (id_creatore) REFERENCES utenti(id) ON DELETE CASCADE
);

-- 6. Creazione Tabella Regole
-- Aggiornata con i campi Bonus/Malus e il flag per il modificatore
CREATE TABLE regole (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lega_id INT NOT NULL,
    budget_iniziale INT NOT NULL DEFAULT 500,
    
    -- Opzioni extra
    usa_modificatore_difesa BOOLEAN DEFAULT TRUE,

    -- Bonus (Valori positivi)
    bonus_gol DECIMAL(4,1) DEFAULT 3.0,
    bonus_assist DECIMAL(4,1) DEFAULT 1.0,
    bonus_imbattibilita DECIMAL(4,1) DEFAULT 1.0,
    bonus_rigore_parato DECIMAL(4,1) DEFAULT 3.0,
    bonus_fattore_campo DECIMAL(4,1) DEFAULT 1.0,
    
    -- Malus (Valori positivi che verranno sottratti dal calcolo)
    malus_gol_subito DECIMAL(4,1) DEFAULT 1.0,
    malus_ammonizione DECIMAL(4,1) DEFAULT 0.5,
    malus_espulsione DECIMAL(4,1) DEFAULT 1.0,
    malus_rigore_sbagliato DECIMAL(4,1) DEFAULT 3.0,
    malus_autogol DECIMAL(4,1) DEFAULT 2.0,
    
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE,
    UNIQUE (lega_id) -- Assicura che esista solo un set di regole per ogni lega
);

-- 7. Creazione Tabella Utenti_Leghe (relazione molti-a-molti)
CREATE TABLE utenti_leghe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utente_id INT NOT NULL,
    lega_id INT NOT NULL,
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE,
    UNIQUE (utente_id, lega_id)
);

-- 8. Creazione Tabella Richieste di Accesso
CREATE TABLE richieste_accesso (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utente_id INT NOT NULL,
    lega_id INT NOT NULL,
    data_richiesta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    stato ENUM('in_attesa','accettata','rifiutata') DEFAULT 'in_attesa',
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE
);