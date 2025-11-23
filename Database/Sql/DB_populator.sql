-- Popolamento Utenti
INSERT INTO utenti (username, email, hash_password)
/* La password è: Password1234! per ogni utente */
VALUES 
('maikol', 'maikol@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('giulia', 'giulia@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('luca', 'luca@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
('marta', 'marta@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q');

-- Popolamento Leghe
INSERT INTO leghe (nome, icona, max_membri, id_creatore, iscrizioni_chiuse)
VALUES
('Serie A Legends', NULL, 10,1, FALSE),
('Premier Fantasy', NULL, 12,3, FALSE),
('Liga Master', NULL, 8,3, TRUE);

-- Popolamento Utenti_Leghe (relazioni molti-a-molti)
-- Maikol e Giulia nella Serie A Legends
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 1);
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (2, 1);

-- Luca nella Premier Fantasy
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (3, 2);

-- Marta nella Liga Master
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (4, 3);

-- Popolamento Richieste di Accesso
-- Giulia chiede di entrare nella Premier Fantasy
INSERT INTO richieste_accesso (utente_id, lega_id, stato)
VALUES (2, 2, 'in_attesa');

-- Maikol chiede di entrare nella Liga Master (ma iscrizioni chiuse)
INSERT INTO richieste_accesso (utente_id, lega_id, stato)
VALUES (1, 3, 'rifiutata');

-- Luca chiede di entrare nella Serie A Legends
INSERT INTO richieste_accesso (utente_id, lega_id, stato)
VALUES (3, 1, 'accettata');


-- Popolamento Leghe aggiuntive
INSERT INTO leghe (nome, icona, max_membri, id_creatore, iscrizioni_chiuse) VALUES
('Bundesliga Stars', NULL, 10, 2, FALSE),
('Champions Fantasy', NULL, 12, 3, FALSE),
('Serie B Challenge', NULL, 8, 4, FALSE),
('Coppa Italia Dream', NULL, 16, 2, FALSE),
('Europa League Heroes', NULL, 10, 3, FALSE),
('World Cup Legends', NULL, 20, 1, FALSE),
('Calcio Vintage', NULL, 6, 4, FALSE),
('Super League', NULL, 14, 2, FALSE),
('MLS Fantasy', NULL, 12, 3, FALSE),
('Friendly Cup', NULL, 8, 1, FALSE);

-- Popolamento Utenti_Leghe: iscrivi Maikol (id=1) in tutte queste leghe
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 2);  -- Premier Fantasy
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 3);  -- Liga Master
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 4);  -- Bundesliga Stars
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 5);  -- Champions Fantasy
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 6);  -- Serie B Challenge
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 7);  -- Coppa Italia Dream
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 8);  -- Europa League Heroes
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 9);  -- World Cup Legends
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 10); -- Calcio Vintage
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 11); -- Super League
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 12); -- MLS Fantasy
INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (1, 13); -- Friendly Cup

