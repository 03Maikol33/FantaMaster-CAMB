package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.Message;
import it.camb.fantamaster.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private final Connection conn;

    public MessageDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Inserisce un nuovo messaggio nel DB.
     */
    public boolean insertMessage(Message message) {
        String sql = "INSERT INTO messaggi (testo, data_invio, utente_id, lega_id) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.getText());
            stmt.setTimestamp(2, Timestamp.valueOf(message.getTimestamp()));
            stmt.setInt(3, message.getSender().getId());
            stmt.setInt(4, message.getLeagueId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        message.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Recupera SOLO gli ultimi 20 messaggi di una lega.
     * Logica: Prende i 20 più recenti (DESC) e poi li riordina cronologicamente (ASC).
     */
    public List<Message> getMessagesByLeagueId(int leagueId) {
        List<Message> messages = new ArrayList<>();
        
        // QUERY OTTIMIZZATA:
        // 1. Subquery: Prende gli ultimi 20 (DESC)
        // 2. Query esterna: Li rimette in ordine cronologico (ASC) per la chat
        String sql = "SELECT * FROM (" +
                     "    SELECT m.*, u.username, u.email " +
                     "    FROM messaggi m " +
                     "    JOIN utenti u ON m.utente_id = u.id " +
                     "    WHERE m.lega_id = ? " +
                     "    ORDER BY m.data_invio DESC " +
                     "    LIMIT 20" +
                     ") AS ultimi_messaggi " +
                     "ORDER BY ultimi_messaggi.data_invio ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // 1. Costruiamo l'utente (Senza Avatar per ora, per evitare errori colonna mancante)
                    User sender = new User();
                    sender.setId(rs.getInt("utente_id"));
                    sender.setUsername(rs.getString("username"));
                    sender.setEmail(rs.getString("email"));
                    
                    // 2. Costruiamo il messaggio
                    Message msg = new Message(
                        rs.getInt("id"),
                        rs.getString("testo"),
                        rs.getTimestamp("data_invio").toLocalDateTime(),
                        sender,
                        leagueId
                    );
                    
                    messages.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public boolean deleteMessage(int messageId) {
        String sql = "DELETE FROM messaggi WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}