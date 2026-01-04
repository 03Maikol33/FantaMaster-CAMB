-- ==================================================
-- SCRIPT DI CREAZIONE DEL DATABASE (Updated for Modules)
-- ==================================================

-- 1. Disabilita temporaneamente i controlli sulle chiavi esterne
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Elimina le tabelle se esistono (pulizia totale per reset)
DROP TABLE IF EXISTS messaggi;
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
-- MODIFICA: Aggiunta colonna 'moduli_consentiti'
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
    
    -- NUOVA COLONNA PER I MODULI
    -- Contiene una stringa con i moduli separati da punto e virgola
    moduli_consentiti VARCHAR(255) DEFAULT '3-4-3;3-5-2;4-3-3;4-4-2;4-5-1;5-3-2;5-4-1',
    
    FOREIGN KEY (id_creatore) REFERENCES utenti(id) ON DELETE CASCADE
);

-- 6. Creazione Tabella Regole
CREATE TABLE regole (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lega_id INT NOT NULL,
    budget_iniziale INT NOT NULL DEFAULT 500,
    
    -- Opzioni extra
    usa_modificatore_difesa BOOLEAN DEFAULT TRUE,

    -- Bonus
    bonus_gol DECIMAL(4,1) DEFAULT 3.0,
    bonus_assist DECIMAL(4,1) DEFAULT 1.0,
    bonus_imbattibilita DECIMAL(4,1) DEFAULT 1.0,
    bonus_rigore_parato DECIMAL(4,1) DEFAULT 3.0,
    bonus_fattore_campo DECIMAL(4,1) DEFAULT 1.0,
    
    -- Malus
    malus_gol_subito DECIMAL(4,1) DEFAULT 1.0,
    malus_ammonizione DECIMAL(4,1) DEFAULT 0.5,
    malus_espulsione DECIMAL(4,1) DEFAULT 1.0,
    malus_rigore_sbagliato DECIMAL(4,1) DEFAULT 3.0,
    malus_autogol DECIMAL(4,1) DEFAULT 2.0,
    
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE,
    UNIQUE (lega_id)
);

-- 7. Creazione Tabella Utenti_Leghe
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

-- 9. Creazione Tabella Messaggi
CREATE TABLE messaggi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    testo TEXT NOT NULL,
    data_invio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    utente_id INT NOT NULL,
    lega_id INT NOT NULL,
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE
);