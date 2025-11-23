-- Creazione database
DROP DATABASE IF exists fantamaster;
CREATE DATABASE fantamaster
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE fantamaster;

-- Tabella Utenti
DROP TABLE IF EXISTS utenti;
CREATE TABLE utenti (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    hash_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella Leghe
DROP TABLE IF EXISTS leghe;
CREATE TABLE leghe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    icona mediumblob, /*Fino a 16MB*/
    max_membri INT NOT NULL,
    id_creatore INT NOT NULL,
    iscrizioni_chiuse BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_creatore) REFERENCES utenti(id) ON DELETE CASCADE
);

-- Tabella Utenti_Leghe (relazione molti-a-molti)
DROP TABLE IF EXISTS utenti_leghe;
CREATE TABLE utenti_leghe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utente_id INT NOT NULL,
    lega_id INT NOT NULL,
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE,
    UNIQUE (utente_id, lega_id)
);

-- Tabella Richieste di Accesso
DROP TABLE IF EXISTS richieste_accesso;
CREATE TABLE richieste_accesso (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utente_id INT NOT NULL,
    lega_id INT NOT NULL,
    data_richiesta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    stato ENUM('in_attesa','accettata','rifiutata') DEFAULT 'in_attesa',
    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    FOREIGN KEY (lega_id) REFERENCES leghe(id) ON DELETE CASCADE
);
