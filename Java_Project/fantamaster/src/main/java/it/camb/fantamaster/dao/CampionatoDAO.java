package it.camb.fantamaster.dao;

import java.sql.*;
import java.util.List;
import java.util.function.Consumer;

import it.camb.fantamaster.util.CampionatoUtil;
import it.camb.fantamaster.model.Player;
import it.camb.fantamaster.model.Rules;
import it.camb.fantamaster.model.campionato.MatchData;
import it.camb.fantamaster.model.MatchPerformance;
import it.camb.fantamaster.model.FantavotoCalculator;
import it.camb.fantamaster.util.RealDataGenerator;


public class CampionatoDAO {
    private Connection conn;

    public CampionatoDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Verifica se la riga di stato globale esiste (tabella stato_campionato).
     */
    public boolean existsStatoCampionato() throws SQLException {
        String sql = "SELECT COUNT(*) FROM stato_campionato";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /**
     * Ritorna l'ultima giornata conclusa (es: se ritorna 1, la giornata 1 è finita).
     */
    public int getGiornataCorrente() {
        String sql = "SELECT giornata_corrente FROM stato_campionato WHERE id = 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt("giornata_corrente");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Resetta il campionato allo stato zero e azzera i contatori degli ID (AUTO_INCREMENT).
     * Risolve il problema del 'giornata_id' che cresce anche dopo il reset.
     */
    public void inizializzaCampionato() throws SQLException {
        // TRUNCATE resetta automaticamente i contatori AUTO_INCREMENT a 1.
        // Dobbiamo disabilitare i check delle chiavi esterne per poterlo eseguire.
        try (Statement st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            st.executeUpdate("TRUNCATE TABLE dettaglio_formazione");
            st.executeUpdate("TRUNCATE TABLE formazioni");
            st.executeUpdate("TRUNCATE TABLE giornate");
            st.executeUpdate("TRUNCATE TABLE stato_campionato");
            st.execute("SET FOREIGN_KEY_CHECKS = 1");

            // Inserimento stato iniziale: giornata 0 passata (nessuna ancora giocata)
            st.executeUpdate("INSERT INTO stato_campionato (id, giornata_corrente) VALUES (1, 0)");
            
            System.out.println("✅ Campionato inizializzato e contatori ID azzerati.");
        }
    }

    /**
     * Verifica se esiste una riga fisica nella tabella 'giornate' per il numero specificato.
     */
    public boolean existsGiornataFisica(int n) throws SQLException {
        String sql = "SELECT COUNT(*) FROM giornate WHERE numero_giornata = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, n);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Inserisce la riga fisica per permettere agli utenti di schierare la formazione.
     */
    public void programmaGiornata(int n) throws SQLException {
        // Verifica se la giornata esiste nel JSON prima di programmarla
        CampionatoUtil.load("/api/campionato.json");
        if (CampionatoUtil.getMatchesByDay(n).isEmpty()) {
            throw new SQLException("Impossibile programmare: la giornata " + n + " non esiste nel file JSON.");
        }

        String sql = "INSERT INTO giornate (numero_giornata, data_inizio, stato) VALUES (?, CURRENT_TIMESTAMP, 'da_giocare')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, n);
            ps.executeUpdate();
            System.out.println("📅 Giornata " + n + " programmata. Ora è possibile schierare le formazioni.");
        }
    }

    /**
     * Chiude la giornata N, aggiorna il progresso e programma la N+1 se presente nel JSON.
     */
    public void eseguiGiornataEProgrammaSuccessiva(int n) throws SQLException {
        conn.setAutoCommit(false);
        try {
            // 1. Segna la giornata n come 'calcolata'
            String sqlUpdateGiornata = "UPDATE giornate SET stato = 'calcolata' WHERE numero_giornata = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateGiornata)) {
                ps.setInt(1, n);
                ps.executeUpdate();
            }

            // 2. Avanza il contatore globale (n giornate concluse)
            String sqlUpdateStato = "UPDATE stato_campionato SET giornata_corrente = ? WHERE id = 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateStato)) {
                ps.setInt(1, n);
                ps.executeUpdate();
            }

            // 3. Programma la successiva SOLO SE esiste nel JSON
            CampionatoUtil.load("/api/campionato.json");
            int prossima = n + 1;
            if (!CampionatoUtil.getMatchesByDay(prossima).isEmpty()) {
                programmaGiornata(prossima);
                System.out.println("🚀 Giornata " + n + " conclusa. Giornata " + prossima + " aperta.");
            } else {
                System.out.println("🏆 Campionato terminato con la giornata " + n + ". Nessun'altra giornata da programmare.");
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * LOGICA AUTO-RIPARANTE: Calcola i voti mancanti solo per questa lega.
     */
    public void sincronizzaPunteggiLega(int leagueId, Consumer<String> progressReporter) throws SQLException {
        int ultimaConclusa = getGiornataCorrente();
        if (ultimaConclusa == 0) return; // Campionato non iniziato

        Rules rules = new RulesDAO(conn).getRulesByLeagueId(leagueId);
        PlayerDAO playerDAO = new PlayerDAO(conn);
        UsersLeaguesDAO ulDAO = new UsersLeaguesDAO(conn);
        RealDataGenerator generator = new RealDataGenerator();

        // 1. Cerchiamo le formazioni della lega per le giornate concluse che hanno ancora punteggio 0
        String sqlCheck = "SELECT f.id, g.numero_giornata FROM formazioni f " +
                          "JOIN giornate g ON f.giornata_id = g.id " +
                          "JOIN rosa r ON f.rosa_id = r.id " +
                          "JOIN utenti_leghe ul ON r.utenti_leghe_id = ul.id " +
                          "WHERE ul.lega_id = ? AND g.numero_giornata <= ? AND f.totale_fantapunti = 0.0";

        try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
            ps.setInt(1, leagueId);
            ps.setInt(2, ultimaConclusa);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int formationId = rs.getInt("id");
                int nGiorno = rs.getInt("numero_giornata");

                //notifica di caricamento
                if (progressReporter != null) progressReporter.accept("Ottengo i punteggi per la giornata " + nGiorno + "...");

                // 2. Recupero i calciatori schierati
                List<Player> team = playerDAO.getPlayersByFormationId(formationId);
                
                // 3. Ottengo dati reali dal JSON (Classe di Chiara)
                CampionatoUtil.load("/api/campionato.json");
                List<MatchPerformance> perfs = generator.getPrestazioniReali(team, nGiorno);

                double totale = 0.0;
                for (MatchPerformance mp : perfs) {
                    // 4. Calcolo voto singolo con regole lega (Classe di Chiara)
                    double voto = FantavotoCalculator.calcolaFantavoto(mp, rules);
                    ulDAO.savePlayerScore(formationId, mp.getPlayer().getId(), voto, true);
                    totale += voto;
                }

                // 5. Salvo il totale sulla formazione per segnarla come "calcolata"
                conn.createStatement().executeUpdate("UPDATE formazioni SET totale_fantapunti = " + totale + " WHERE id = " + formationId);
            }
        }
        // Notifica: Aggiorno le classifiche
        if (progressReporter != null) progressReporter.accept("Aggiorno le classifiche...");
        // 6. Aggiorno la classifica generale della lega
        ulDAO.updateLeagueRanking(leagueId);
    }
}