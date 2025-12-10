-- ==================================================
-- SCRIPT DI POPOLAMENTO DATI (MERGED VERSION)
-- ==================================================

-- 1. Inserimento Primi 4 Utenti "Reali" (Team di Sviluppo)
-- Uso i nomi dello script della tua amica, ma mantengo Maikol come ID 1
INSERT INTO utenti (username, email, hash_password)
VALUES 
('maikol', 'maikol@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),   -- ID 1
('chiara', 'chiara@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),   -- ID 2
('alessio', 'alessio@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),  -- ID 3
('bassma', 'bassma@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q');   -- ID 4

-- 2. Inserimento Altri Utenti "Fake" (per testare le richieste massive)
-- Continuano da ID 5 in poi (dallo script tuo)
INSERT INTO utenti (username, email, hash_password)
VALUES 
('gianluca', 'gianluca@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'), -- ID 5
('tommaso', 'tommaso@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),  -- ID 6
('nicoletta', 'nicoletta@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),-- ID 7
('jasmine', 'jasmine@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),  -- ID 8
('clotilde', 'clotilde@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'), -- ID 9
('renilde', 'renilde@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),  -- ID 10
('genoveffa', 'genoveffa@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),-- ID 11
('anacleto', 'anacleto@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'); -- ID 12


-- 3. Inserimento Tutte le Leghe (1-13)
-- Includo sia i codici invito (tuoi) che lo stato (suo)
INSERT INTO leghe (nome, icona, max_membri, id_creatore, iscrizioni_chiuse, codice_invito) VALUES
('Serie A Legends', NULL, 10, 1, FALSE, 'A1B2C3'), -- ID 1 (Maikol)
('Premier Fantasy', NULL, 12, 3, FALSE, 'D4E5F6'), -- ID 2 (Alessio)
('Liga Master', NULL, 8, 3, TRUE, 'G7H8I9'),       -- ID 3 (Alessio) - Chiusa
('Bundesliga Stars', NULL, 10, 2, FALSE, 'J0K1L2'), -- ID 4 (Chiara)
('Champions Fantasy', NULL, 12, 3, FALSE, 'M3N4O5'),-- ID 5 (Alessio)
('Serie B Challenge', NULL, 8, 4, FALSE, 'P6Q7R8'), -- ID 6 (Bassma)
('Coppa Italia Dream', NULL, 16, 2, FALSE, 'S9T0U1'),-- ID 7 (Chiara)
('Europa League Heroes', NULL, 10, 3, FALSE, 'V2W3X4'),-- ID 8 (Alessio)
('World Cup Legends', NULL, 20, 1, FALSE, 'Y5Z6A7'), -- ID 9 (Maikol)
('Calcio Vintage', NULL, 6, 4, FALSE, 'B8C9D0'),    -- ID 10 (Bassma)
('Super League', NULL, 14, 2, FALSE, 'E1F2G3'),     -- ID 11 (Chiara)
('MLS Fantasy', NULL, 12, 3, FALSE, 'H4I5J6'),      -- ID 12 (Alessio)
('Friendly Cup', NULL, 8, 1, FALSE, 'K7L8M9');      -- ID 13 (Maikol)


-- 4. Inserimento REGOLE per TUTTE le leghe (Feature della tua amica)
INSERT INTO regole (lega_id, budget_iniziale) VALUES 
(1, 500),  -- Serie A
(2, 1000), -- Premier
(3, 500),  -- Liga
(4, 500), (5, 500), (6, 500), (7, 500), (8, 500), 
(9, 800), (10, 500), (11, 500), (12, 500), (13, 250);


-- 5. Associazioni Utenti-Leghe (Chi è dentro chi)
-- Maikol, Chiara nella Serie A (1)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 1);
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (2, 1);
-- Alessio nella Premier (2)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (3, 2);
-- Bassma nella Liga (3)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (4, 3);

-- Maikol partecipa a quasi tutto (dal tuo script)
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
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 13); -- Friendly


-- 6. Richieste di Iscrizione
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
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (4, 13, 'in_attesa');