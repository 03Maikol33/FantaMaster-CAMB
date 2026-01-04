SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE giocatori_rose;
TRUNCATE TABLE rosa;
TRUNCATE TABLE utenti_leghe;
TRUNCATE TABLE leghe;
TRUNCATE TABLE regole;
TRUNCATE TABLE giocatori;
TRUNCATE TABLE utenti;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. UTENTI
INSERT INTO utenti (username, email, hash_password) VALUES 
('maikol', 'maikol@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('chiara', 'chiara@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('alessio', 'alessio@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('bassma', 'bassma@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q');

-- 2. GIOCATORI (30 finti)
INSERT INTO giocatori (id_esterno, nome, squadra_reale, ruolo, quotazione_iniziale) VALUES
(1, 'P1', 'A', 'P', 1), (2, 'P2', 'A', 'P', 1), (3, 'P3', 'A', 'P', 1),
(4, 'D1', 'A', 'D', 1), (5, 'D2', 'A', 'D', 1), (6, 'D3', 'A', 'D', 1), (7, 'D4', 'A', 'D', 1), (8, 'D5', 'A', 'D', 1), (9, 'D6', 'A', 'D', 1), (10, 'D7', 'A', 'D', 1), (11, 'D8', 'A', 'D', 1),
(12, 'C1', 'A', 'C', 1), (13, 'C2', 'A', 'C', 1), (14, 'C3', 'A', 'C', 1), (15, 'C4', 'A', 'C', 1), (16, 'C5', 'A', 'C', 1), (17, 'C6', 'A', 'C', 1), (18, 'C7', 'A', 'C', 1), (19, 'C8', 'A', 'C', 1),
(20, 'A1', 'A', 'A', 1), (21, 'A2', 'A', 'A', 1), (22, 'A3', 'A', 'A', 1), (23, 'A4', 'A', 'A', 1), (24, 'A5', 'A', 'A', 1), (25, 'A6', 'A', 'A', 1),
(26, 'Extra1', 'A', 'A', 1);

-- 3. LEGHE
INSERT INTO leghe (nome, max_membri, id_creatore, iscrizioni_chiuse, codice_invito, modalita, asta_aperta) VALUES
('Lega Asta APERTA', 4, 1, TRUE, 'OPEN12', 'punti_totali', TRUE),
('Lega Asta CHIUSA', 4, 1, TRUE, 'CLOSE34', 'punti_totali', FALSE);

-- 4. REGOLE
INSERT INTO regole (lega_id, budget_iniziale) VALUES (1, 500), (2, 500);

-- 5. ISCRIZIONI
-- Maikol (Admin)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 1), (1, 2);
INSERT INTO rosa (utenti_leghe_id, nome_rosa) VALUES (1, 'Maikol Team'), (2, 'Maikol Chiuso'); -- ID Rosa 1 e 2 (Attenzione agli ID auto increment)

-- Chiara (Vuota)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (2, 1);
INSERT INTO rosa (utenti_leghe_id, nome_rosa) VALUES (3, 'Chiara City'); -- ID Rosa 3

-- Alessio (PIENO)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (3, 1);
INSERT INTO rosa (utenti_leghe_id, nome_rosa) VALUES (4, 'Alessio Full'); -- ID Rosa 4

-- 6. RIEMPIMENTO ROSA ALESSIO (ID Rosa 4)
INSERT INTO giocatori_rose (rosa_id, giocatore_id, costo_acquisto)
SELECT 4, id, 1 FROM giocatori WHERE id BETWEEN 1 AND 25;