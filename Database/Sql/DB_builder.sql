-- ==================================================
-- SCRIPT DI CREAZIONE DEL DATABASE
-- ==================================================

-- 1. Disabilita temporaneamente i controlli sulle chiavi esterne
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Elimina le tabelle se esistono (pulizia totale)
DROP TABLE IF EXISTS richieste_accesso;
DROP TABLE IF EXISTS utenti_leghe;
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
CREATE TABLE leghe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    icona mediumblob, /* Fino a 16MB */
    max_membri INT NOT NULL,
    id_creatore INT NOT NULL,
    iscrizioni_chiuse BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    codice_invito VARCHAR(10) UNIQUE,
    -- NUOVA COLONNA: Moduli consentiti (CSV)
    moduli_consentiti VARCHAR(255) DEFAULT '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2',
    FOREIGN KEY (id_creatore) REFERENCES utenti(id) ON DELETE CASCADE
);

-- 6. Creazione Tabella Utenti_Leghe (relazione molti-a-molti)
CREATE TABLE utenti_leghe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utente_id INT NOT NULL,
    lega_id INT NOT NULL,
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE,
    UNIQUE (utente_id, lega_id)
);

-- 7. Creazione Tabella Richieste di Accesso
CREATE TABLE richieste_accesso (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utente_id INT NOT NULL,
    lega_id INT NOT NULL,
    data_richiesta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    stato ENUM('in_attesa','accettata','rifiutata') DEFAULT 'in_attesa',
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE
);