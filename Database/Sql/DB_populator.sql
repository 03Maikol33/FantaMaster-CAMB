-- ==================================================
-- SCRIPT DI POPOLAMENTO DATI (UPDATED FOR MODULES & RULES)
-- ==================================================

-- 1. Inserimento Utenti (Team + Fake Users)
INSERT INTO utenti (username, email, hash_password)
VALUES 
('maikol', 'maikol@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),   -- ID 1
('chiara', 'chiara@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),   -- ID 2
('alessio', 'alessio@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),  -- ID 3
('bassma', 'bassma@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),   -- ID 4
('gianluca', 'gianluca@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'), -- ID 5
('tommaso', 'tommaso@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),  -- ID 6
('nicoletta', 'nicoletta@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),-- ID 7
('jasmine', 'jasmine@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),  -- ID 8
('clotilde', 'clotilde@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'), -- ID 9
('renilde', 'renilde@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),  -- ID 10
('genoveffa', 'genoveffa@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),-- ID 11
('anacleto', 'anacleto@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'); -- ID 12 [cite: 1, 2, 3]

-- 2. Inserimento Leghe
-- NOTA: I moduli_consentiti vengono inseriti automaticamente tramite il DEFAULT del DB.
-- Ho aggiunto 'modalita' per differenziare: la Lega ID 3 sarà a scontri diretti.
INSERT INTO leghe (nome, icona, max_membri, id_creatore, iscrizioni_chiuse, codice_invito, modalita) VALUES
('Serie A Legends', NULL, 10, 1, FALSE, 'A1B2C3', 'punti_totali'),      -- ID 1
('Premier Fantasy', NULL, 12, 3, FALSE, 'D4E5F6', 'punti_totali'),      -- ID 2
('Liga Master', NULL, 8, 3, TRUE, 'G7H8I9', 'scontri_diretti'),         -- ID 3 (Speciale)
('Bundesliga Stars', NULL, 10, 2, FALSE, 'J0K1L2', 'punti_totali'),     -- ID 4
('Champions Fantasy', NULL, 12, 3, FALSE, 'M3N4O5', 'punti_totali'),    -- ID 5
('Serie B Challenge', NULL, 8, 4, FALSE, 'P6Q7R8', 'punti_totali'),     -- ID 6
('Coppa Italia Dream', NULL, 16, 2, FALSE, 'S9T0U1', 'punti_totali'),   -- ID 7
('Europa League Heroes', NULL, 10, 3, FALSE, 'V2W3X4', 'punti_totali'), -- ID 8
('World Cup Legends', NULL, 20, 1, FALSE, 'Y5Z6A7', 'punti_totali'),    -- ID 9
('Calcio Vintage', NULL, 6, 4, FALSE, 'B8C9D0', 'punti_totali'),        -- ID 10
('Super League', NULL, 14, 2, FALSE, 'E1F2G3', 'punti_totali'),         -- ID 11
('MLS Fantasy', NULL, 12, 3, FALSE, 'H4I5J6', 'punti_totali'),          -- ID 12
('Friendly Cup', NULL, 8, 1, FALSE, 'K7L8M9', 'punti_totali');          -- ID 13 [cite: 3, 4, 31]

-- 3. Inserimento Regole
-- NOTA: Inseriamo solo budget e ID. Tutti i bonus/malus verranno popolati 
-- automaticamente con i valori DEFAULT definiti nel builder.txt (es. bonus_gol=3.0).
INSERT INTO regole (lega_id, budget_iniziale) VALUES 
(1, 500),  -- Serie A
(2, 1000), -- Premier
(3, 500),  -- Liga
(4, 500), (5, 500), (6, 500), (7, 500), (8, 500), 
(9, 800), (10, 500), (11, 500), (12, 500), (13, 250); -- [cite: 5, 33, 34]

-- 4. Associazioni Utenti-Leghe
-- Maikol e Chiara nella Serie A (1)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 1);
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (2, 1);
-- Alessio nella Premier (2)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (3, 2);
-- Bassma nella Liga (3)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (4, 3);

-- Maikol partecipa a quasi tutto
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 2);  -- Premier
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 3);  -- Liga
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 4);  -- Bundesliga
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 5);  -- Champions
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 6);  -- Serie B
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 7);  -- Coppa Italia
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 8);  -- Europa
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 9);  -- World Cup
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 10); -- Vintage
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 11); -- Super
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 12); -- MLS
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 13); -- Friendly [cite: 6-15]

-- 5. Richieste di Iscrizione
-- Chiara vuole entrare in Premier (Lega 2)
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (2, 2, 'in_attesa');
-- Alessio è stato accettato in Serie A (Lega 1)
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (3, 1, 'accettata');
-- I Fake Users vogliono entrare in Serie A (Lega 1)
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (5, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (6, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (7, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (8, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (9, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (10, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (11, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (12, 1, 'in_attesa');

-- Richieste per World Cup (Lega 9)
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (2, 9, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (3, 9, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (4, 9, 'in_attesa');

-- Richieste per Friendly Cup (Lega 13)
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (2, 13, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (3, 13, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (4, 13, 'in_attesa'); -- [cite: 16-25]