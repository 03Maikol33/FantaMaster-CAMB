package it.camb.fantamaster.dao;

import java.sql.*;
import java.util.List;
import it.camb.fantamaster.util.CampionatoUtil;
import it.camb.fantamaster.model.campionato.MatchData;

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
}