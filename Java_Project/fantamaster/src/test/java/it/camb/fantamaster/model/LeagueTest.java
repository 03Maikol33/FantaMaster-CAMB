package it.camb.fantamaster.model;

import org.junit.Test;
import static org.junit.Assert.*; // Importante per usare assertEquals

public class LeagueTest {

    @Test
    public void testAddParticipant() {
        User creator = new User(1, "creatorUser", "creator@example.com", "hashedPassword", java.time.LocalDateTime.now());
        
        // CORREZIONE: Imposto maxMembers a 3 (Creator + Part1 + Part2)
        League league = new League("Test League", null, 3, creator, java.time.LocalDateTime.now());
        
        User participant1 = new User(2, "participant1", "participant1@example.com", "hashedPassword", java.time.LocalDateTime.now());
        User participant2 = new User(3, "participant2", "participant2@example.com", "hashedPassword", java.time.LocalDateTime.now());
        
        league.addParticipant(participant1);
        league.addParticipant(participant2);
        
        // Verifica: Creator + P1 + P2 = 3
        assertEquals(3, league.getParticipants().size());
        
        // Verifica extra: il primo della lista deve essere il creatore
        assertEquals(creator, league.getParticipants().get(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testAddParticipant_LeagueFull() {
        User creator = new User(1, "creatorUser", "creator@example.com", "hashedPassword", java.time.LocalDateTime.now());
        
        // MaxMembers = 1. La lega nasce già piena (c'è solo il creatore)!
        League league = new League("Test League", null, 1, creator, java.time.LocalDateTime.now());
        
        User participant1 = new User(2, "participant1", "participant1@example.com", "hashedPassword", java.time.LocalDateTime.now());
        
        // Questo deve lanciare l'eccezione perché 1 (creator) + 1 (p1) > 1
        league.addParticipant(participant1);
    }
}