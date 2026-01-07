-- ==========================================================
-- SCRIPT DI POPOLAMENTO FANTAMASTER v5.6 (Dati Reali)
-- ==========================================================

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE giocatori_rose;
TRUNCATE TABLE rosa;
TRUNCATE TABLE utenti_leghe;
TRUNCATE TABLE leghe;
TRUNCATE TABLE regole;
TRUNCATE TABLE giocatori;
TRUNCATE TABLE utenti;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. UTENTI (ID 1 a 4)
INSERT INTO utenti (id, username, email, hash_password) VALUES 
(1, 'maikol', 'maikol@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
(2, 'chiara', 'chiara@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
(3, 'alessio', 'alessio@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q'),
(4, 'bassma', 'bassma@example.com', '$2a$12$PeT2j./oA8l0k3Euwu5wUuAL4IWrZy8iT2qsTyQwWAF2qo7KkK62q');

-- 2. GIOCATORI REALI (Presi dal listone.json)
-- Abbiamo inserito solo i 25 necessari per Alessio per brevità, 
-- gli altri verranno aggiunti dal Lazy Loading durante l'asta.
INSERT INTO giocatori (id, id_esterno, nome, squadra_reale, ruolo, quotazione_iniziale) VALUES
-- Portieri (3)
(1, 1, 'Yann Sommer', 'Inter', 'P', 15),
(2, 2, 'Josep Martinez', 'Inter', 'P', 1),
(21, 21, 'Mike Maignan', 'Milan', 'P', 42),
-- Difensori (8)
(3, 3, 'Alessandro Bastoni', 'Inter', 'D', 40),
(4, 4, 'Benjamin Pavard', 'Inter', 'D', 32),
(5, 5, 'Federico Dimarco', 'Inter', 'D', 45),
(6, 6, 'Stefan de Vrij', 'Inter', 'D', 12),
(7, 7, 'Denzel Dumfries', 'Inter', 'D', 28),
(8, 8, 'Francesco Acerbi', 'Inter', 'D', 18),
(9, 9, 'Yann Bisseck', 'Inter', 'D', 14),
(10, 10, 'Matteo Darmian', 'Inter', 'D', 10),
-- Centrocampisti (8)
(11, 11, 'Nicolò Barella', 'Inter', 'C', 55),
(12, 12, 'Hakan Calhanoglu', 'Inter', 'C', 60),
(13, 13, 'Henrikh Mkhitaryan', 'Inter', 'C', 25),
(14, 14, 'Davide Frattesi', 'Inter', 'C', 35),
(15, 15, 'Piotr Zielinski', 'Inter', 'C', 20),
(16, 16, 'Kristjan Asllani', 'Inter', 'C', 8),
(29, 29, 'Christian Pulisic', 'Milan', 'C', 75),
(30, 30, 'Tijjani Reijnders', 'Milan', 'C', 38),
-- Attaccanti (6)
(17, 17, 'Lautaro Martinez', 'Inter', 'A', 95),
(18, 18, 'Marcus Thuram', 'Inter', 'A', 82),
(19, 19, 'Mehdi Taremi', 'Inter', 'A', 40),
(20, 20, 'Marko Arnautovic', 'Inter', 'A', 5),
(35, 35, 'Rafael Leao', 'Milan', 'A', 88),
(36, 36, 'Alvaro Morata', 'Milan', 'A', 60);

-- 3. LEGHE
INSERT INTO leghe (id, nome, max_membri, id_creatore, iscrizioni_chiuse, codice_invito, modalita, asta_aperta) VALUES
(1, 'Lega Asta APERTA', 4, 1, TRUE, 'OPEN12', 'punti_totali', TRUE),
(2, 'Lega Asta CHIUSA', 4, 1, TRUE, 'CLOSE34', 'punti_totali', FALSE);

-- 4. REGOLE
INSERT INTO regole (lega_id, budget_iniziale) VALUES (1, 500), (2, 500);

-- 5. ISCRIZIONI (Tabella utenti_leghe)
-- Maikol (1) -> Lega 1 (ID 1) e Lega 2 (ID 2)
-- Chiara (2) -> Lega 1 (ID 3)
-- Alessio (3) -> Lega 1 (ID 4)
-- Bassma (4) -> Lega 1 (ID 5)
INSERT INTO utenti_leghe (id, utente_id, lega_id) VALUES 
(1, 1, 1), (2, 1, 2), 
(3, 2, 1), 
(4, 3, 1), 
(5, 4, 1);

-- 6. ROSE (ID 1 a 5)
INSERT INTO rosa (id, utenti_leghe_id, nome_rosa, crediti_residui) VALUES 
(1, 1, 'Maikol Team', 500), 
(2, 2, 'Maikol Chiuso', 500), 
(3, 3, 'Chiara City', 500), 
(4, 4, 'Alessio Full', 100), -- Budget ridotto perché ha già 25 giocatori
(5, 5, 'Bassma Squad', 500);

-- 7. RIEMPIMENTO ROSA ALESSIO (ID Rosa 4)
-- Rispettiamo i limiti: 3 P, 8 D, 8 C, 6 A (Totale 25)
INSERT INTO giocatori_rose (rosa_id, giocatore_id, costo_acquisto) VALUES
-- Portieri
(4, 1, 15), (4, 2, 1), (4, 21, 42),
-- Difensori
(4, 3, 40), (4, 4, 32), (4, 5, 45), (4, 6, 12), (4, 7, 28), (4, 8, 18), (4, 9, 14), (4, 10, 10),
-- Centrocampisti
(4, 11, 55), (4, 12, 60), (4, 13, 25), (4, 14, 35), (4, 15, 20), (4, 16, 8), (4, 29, 75), (4, 30, 38),
-- Attaccanti
(4, 17, 95), (4, 18, 82), (4, 19, 40), (4, 20, 5), (4, 35, 88), (4, 36, 60);