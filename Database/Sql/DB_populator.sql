-- ==================================================
-- SCRIPT DI POPOLAMENTO DATI
-- ==================================================

-- 1. Inserimento Primi 4 Utenti (ID 1-4)
INSERT INTO utenti (username, email, hash_password)
VALUES 
('maikol', 'maikol@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('giulia', 'giulia@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('luca', 'luca@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('marta', 'marta@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q');

-- 2. Inserimento Prime 3 Leghe (ID 1-3)
-- Aggiornato con la colonna moduli_consentiti
INSERT INTO leghe (nome, icona, max_membri, id_creatore, iscrizioni_chiuse, codice_invito, moduli_consentiti)
VALUES
('Serie A Legends', NULL, 10, 1, FALSE, 'A1B2C3', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('Premier Fantasy', NULL, 12, 3, FALSE, 'D4E5F6', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('Liga Master', NULL, 8, 3, TRUE, 'G7H8I9', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2');

-- 3. Associazioni Utenti-Leghe Iniziali
-- Maikol e Giulia nella Serie A Legends (Lega 1)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 1);
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (2, 1);
-- Luca nella Premier Fantasy (Lega 2)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (3, 2);
-- Marta nella Liga Master (Lega 3)
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (4, 3);

-- 4. Richieste di Accesso Iniziali
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (2, 2, 'in_attesa'); -- Giulia -> Premier
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (1, 3, 'rifiutata'); -- Maikol -> Liga
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (3, 1, 'accettata'); -- Luca -> Serie A

-- 5. Inserimento Altre Leghe (ID 4-13)
-- Aggiornato con la colonna moduli_consentiti
INSERT INTO leghe (nome, icona, max_membri, id_creatore, iscrizioni_chiuse, codice_invito, moduli_consentiti) VALUES
('Bundesliga Stars', NULL, 10, 2, FALSE, 'J0K1L2', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('Champions Fantasy', NULL, 12, 3, FALSE, 'M3N4O5', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('Serie B Challenge', NULL, 8, 4, FALSE, 'P6Q7R8', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('Coppa Italia Dream', NULL, 16, 2, FALSE, 'S9T0U1', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('Europa League Heroes', NULL, 10, 3, FALSE, 'V2W3X4', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('World Cup Legends', NULL, 20, 1, FALSE, 'Y5Z6A7', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('Calcio Vintage', NULL, 6, 4, FALSE, 'B8C9D0', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('Super League', NULL, 14, 2, FALSE, 'E1F2G3', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('MLS Fantasy', NULL, 12, 3, FALSE, 'H4I5J6', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2'),
('Friendly Cup', NULL, 8, 1, FALSE, 'K7L8M9', '3-4-3,3-5-2,4-5-1,4-4-2,4-3-3,5-4-1,5-3-2');

-- 6. Iscrizione massiva di Maikol (ID 1) nelle nuove leghe
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 2);  -- Premier
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 3);  -- Liga
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 4);  -- Bundesliga
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 5);  -- Champions
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 6);  -- Serie B
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 7);  -- Coppa Italia
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 8);  -- Europa League
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 9);  -- World Cup
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 10); -- Calcio Vintage
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 11); -- Super League
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 12); -- MLS
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 13); -- Friendly Cup

-- 7. Inserimento Nuovi Utenti (ID 5-12)
INSERT INTO utenti (username, email, hash_password)
VALUES 
('gianluca', 'gianluca@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('tommaso', 'tommaso@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('nicoletta', 'nicoletta@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('jasmine', 'jasmine@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('clotilde', 'clotilde@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('renilde', 'renilde@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('genoveffa', 'genoveffa@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('anacleto', 'anacleto@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q');

-- 8. Richieste di Iscrizione per le leghe di Maikol
-- Richieste per Serie A Legends (Lega 1)
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (2, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (3, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (4, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (5, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (6, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (7, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (8, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (9, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (10, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (11, 1, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (12, 1, 'in_attesa');

-- Richieste per World Cup Legends (Lega 9)
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (2, 9, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (3, 9, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (4, 9, 'in_attesa');

-- Richieste per Friendly Cup (Lega 13)
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (2, 13, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (3, 13, 'in_attesa');
INSERT INTO richieste_accesso (utente_id, lega_id, stato) VALUES (4, 13, 'in_attesa');