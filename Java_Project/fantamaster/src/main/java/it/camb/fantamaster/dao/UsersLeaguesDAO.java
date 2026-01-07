package it.camb.fantamaster.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import it.camb.fantamaster.model.League;
import it.camb.fantamaster.model.User;

public class UsersLeaguesDAO {

    private final Connection connection;

    public UsersLeaguesDAO(Connection connection) {
        this.connection = connection;
    }

    // ==========================================================
    // SEZIONE UTENTI E ISCRIZIONI
    // ==========================================================

    public List<User> getUsersInLeagueId(int leagueId) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.* FROM utenti u " +
                     "JOIN utenti_leghe ul ON u.id = ul.utente_id " +
                     "WHERE ul.lega_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setHashPassword(rs.getString("hash_password"));
                    
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        user.setCreatedAt(ts.toLocalDateTime());
                    }
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public List<User> getUsersInLeague(League league) {
        return getUsersInLeagueId(league.getId());
    }

    public boolean subscribeUserToLeague(User user, League league) {
        String sql = "INSERT INTO utenti_leghe (utente_id, lega_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());
            stmt.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isUserSubscribed(User user, League league) {
        String sql = "SELECT 1 FROM utenti_leghe WHERE utente_id = ? AND lega_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean unsubscribeUserFromLeague(User user, League league) {
        String sql = "DELETE FROM utenti_leghe WHERE utente_id = ? AND lega_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, league.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================================
    // SEZIONE CLASSIFICA (CORRETTA)
    // ==========================================================

    /**
     * Aggiorna il punteggio totale nella tabella ROSA (non utenti_leghe).
     */
    public void updateLeagueRanking(int leagueId) {
        // Correzione: Aggiorna la tabella ROSA usando i fantavoti in dettaglio_formazione
        String sql = "UPDATE rosa r " +
                     "SET r.punteggio_totale = ( " +
                     "    SELECT COALESCE(SUM(df.fantavoto), 0) " +  // Usa 'fantavoto' (non fantavoto_calcolato)
                     "    FROM formazioni f " +
                     "    JOIN dettaglio_formazione df ON f.id = df.formazione_id " +
                     "    WHERE f.rosa_id = r.id " +
                     ") " +
                     "WHERE r.utenti_leghe_id IN (SELECT id FROM utenti_leghe WHERE lega_id = ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            stmt.executeUpdate();
            System.out.println("✅ Classifica aggiornata per la lega " + leagueId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Recupera la classifica leggendo 'punteggio_totale' dalla tabella ROSA.
     */
    public List<UserRankingRow> getLeagueRanking(int leagueId) {
        List<UserRankingRow> ranking = new ArrayList<>();
        
        // Correzione: Prende punteggio_totale e nome_rosa dalla tabella ROSA
        String sql = "SELECT u.username, r.nome_rosa, r.punteggio_totale " +
                     "FROM utenti_leghe ul " +
                     "JOIN utenti u ON ul.utente_id = u.id " +
                     "LEFT JOIN rosa r ON r.utenti_leghe_id = ul.id " + 
                     "WHERE ul.lega_id = ? " +
                     "ORDER BY r.punteggio_totale DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                int position = 1;
                while (rs.next()) {
                    String nomeSquadra = rs.getString("nome_rosa");
                    if (nomeSquadra == null) nomeSquadra = "Nessuna Rosa";
                    
                    ranking.add(new UserRankingRow(
                        position++,
                        rs.getString("username"),
                        nomeSquadra,
                        rs.getDouble("punteggio_totale")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ranking;
    }
    
    public static class UserRankingRow {
        private int posizione;
        private String username;
        private String nomeSquadra;
        private double punteggio;

        public UserRankingRow(int posizione, String username, String nomeSquadra, double punteggio) {
            this.posizione = posizione;
            this.username = username;
            this.nomeSquadra = nomeSquadra;
            this.punteggio = punteggio;
        }

        public int getPosizione() { return posizione; }
        public String getUsername() { return username; }
        public String getNomeSquadra() { return nomeSquadra; }
        public double getPunteggio() { return punteggio; }
    }

    // ==========================================================
    // SEZIONE STORICO E SIMULAZIONE
    // ==========================================================

    /**
     * Recupera le giornate giocate facendo JOIN con la tabella giornate.
     */
    public List<Integer> getPlayedMatchdays(int userId, int leagueId) {
        List<Integer> matchdays = new ArrayList<>();
        
        // Correzione: Join con tabella 'giornate' per prendere 'numero_giornata'
        String sql = "SELECT DISTINCT g.numero_giornata " +
                     "FROM formazioni f " +
                     "JOIN giornate g ON f.giornata_id = g.id " + // Usa giornata_id -> g.id
                     "JOIN rosa r ON f.rosa_id = r.id " +
                     "JOIN utenti_leghe ul ON r.utenti_leghe_id = ul.id " +
                     "WHERE ul.utente_id = ? AND ul.lega_id = ? " +
                     "ORDER BY g.numero_giornata DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    matchdays.add(rs.getInt("numero_giornata"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return matchdays;
    }

    /**
     * Recupera i punteggi usando il nome colonna corretto 'fantavoto'.
     */
    public List<PlayerScoreRow> getFormationScores(int userId, int leagueId, int numeroGiornata) {
        List<PlayerScoreRow> scores = new ArrayList<>();
        
        // Correzione: Join con giornate per filtrare per numero, e usa 'df.fantavoto'
        String sql = "SELECT g.cognome, g.ruolo, df.fantavoto, df.stato " +
                     "FROM dettaglio_formazione df " +
                     "JOIN formazioni f ON df.formazione_id = f.id " +
                     "JOIN giornate gio ON f.giornata_id = gio.id " +
                     "JOIN giocatori g ON df.giocatore_id = g.id " +
                     "JOIN rosa r ON f.rosa_id = r.id " +
                     "JOIN utenti_leghe ul ON r.utenti_leghe_id = ul.id " +
                     "WHERE ul.utente_id = ? AND ul.lega_id = ? AND gio.numero_giornata = ? " +
                     "ORDER BY CASE g.ruolo WHEN 'P' THEN 1 WHEN 'D' THEN 2 WHEN 'C' THEN 3 WHEN 'A' THEN 4 END";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);
            stmt.setInt(3, numeroGiornata);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    boolean isTitolare = "titolare".equalsIgnoreCase(rs.getString("stato"));
                    scores.add(new PlayerScoreRow(
                        rs.getString("cognome"),
                        rs.getString("ruolo"),
                        rs.getDouble("fantavoto"), // Nome colonna corretto
                        isTitolare
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return scores;
    }

    public static class PlayerScoreRow {
        private String nome;
        private String ruolo;
        private double fantavoto;
        private boolean titolare;

        public PlayerScoreRow(String nome, String ruolo, double fantavoto, boolean titolare) {
            this.nome = nome;
            this.ruolo = ruolo;
            this.fantavoto = fantavoto;
            this.titolare = titolare;
        }

        public String getNome() { return nome; }
        public String getRuolo() { return ruolo; }
        public double getFantavoto() { return fantavoto; }
        public boolean isTitolare() { return titolare; }
    }

    // --- SIMULATORE ---

    public int getRosaId(int userId, int leagueId) {
        String sql = "SELECT id FROM rosa WHERE utenti_leghe_id = (SELECT id FROM utenti_leghe WHERE utente_id = ? AND lega_id = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int createDummyFormation(int rosaId, int numeroGiornata) throws SQLException {
        // 1. Dobbiamo trovare l'ID della giornata corrispondente al numero
        int giornataId = getGiornataIdByNumero(numeroGiornata);
        if (giornataId == -1) {
            // Se non esiste nel DB, la creiamo al volo per evitare crash del simulatore
            giornataId = createGiornataIfMissing(numeroGiornata);
        }

        // 2. Controlla se esiste già la formazione
        String checkSql = "SELECT id FROM formazioni WHERE rosa_id = ? AND giornata_id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, rosaId);
            checkStmt.setInt(2, giornataId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }

        // 3. Crea formazione (usa giornata_id)
        String sql = "INSERT INTO formazioni (rosa_id, giornata_id, modulo_schierato) VALUES (?, ?, '4-4-2')";
        try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, rosaId);
            stmt.setInt(2, giornataId);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
            }
        }
        throw new SQLException("Errore creazione formazione");
    }

    public void savePlayerScore(int formationId, int playerId, double fantavoto, boolean titolare) {
        // Correzione: usa 'fantavoto' e 'stato' (enum) invece di 'titolare' (boolean)
        String sql = "INSERT INTO dettaglio_formazione (formazione_id, giocatore_id, fantavoto, stato) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE fantavoto = VALUES(fantavoto)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, formationId);
            stmt.setInt(2, playerId);
            stmt.setDouble(3, fantavoto);
            stmt.setString(4, titolare ? "titolare" : "panchina");
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper per gestire l'ID giornata
    private int getGiornataIdByNumero(int numero) {
        String sql = "SELECT id FROM giornate WHERE numero_giornata = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, numero);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    private int createGiornataIfMissing(int numero) {
        String sql = "INSERT INTO giornate (numero_giornata, data_inizio, stato) VALUES (?, NOW(), 'calcolata')";
        try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, numero);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }
}