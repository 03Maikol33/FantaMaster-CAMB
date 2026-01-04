package it.camb.fantamaster.model;

import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.Assert.*;

public class LeagueTest {

    @Test
    public void testAddParticipant() {
        User creator = new User(1, "creatorUser", "creator@example.com", "hashedPassword", LocalDateTime.now());
        
        // CORREZIONE: Aggiunto il parametro "punti_totali" per rispettare il nuovo costruttore
        // Firma: (name, image, maxMembers, creator, gameMode, createdAt)
        League league = new League("Test League", null, 3, creator, "punti_totali", LocalDateTime.now());
        
        User participant1 = new User(2, "participant1", "participant1@example.com", "hashedPassword", LocalDateTime.now());
        User participant2 = new User(3, "participant2", "participant2@example.com", "hashedPassword", LocalDateTime.now());
        
        league.addParticipant(participant1);
        league.addParticipant(participant2);
        
        // Verifica: Creator + P1 + P2 = 3
        assertEquals(3, league.getParticipants().size());
        
        // Verifica extra: il primo della lista deve essere il creatore
        assertEquals(creator, league.getParticipants().get(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testAddParticipant_LeagueFull() {
        User creator = new User(1, "creatorUser", "creator@example.com", "hashedPassword", LocalDateTime.now());
        
        // CORREZIONE: Aggiunto "punti_totali"
        // MaxMembers = 1. La lega nasce già piena (c'è solo il creatore)!
        League league = new League("Test League", null, 1, creator, "punti_totali", LocalDateTime.now());
        
        User participant1 = new User(2, "participant1", "participant1@example.com", "hashedPassword", LocalDateTime.now());
        
        // Questo deve lanciare l'eccezione perché 1 (creator) + 1 (p1) > 1
        league.addParticipant(participant1);
    }
}