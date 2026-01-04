package it.camb.fantamaster.dao;

import it.camb.fantamaster.model.Player;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class PlayerDAOTest {

    private PlayerDAO playerDAO;

    @Before
    public void setUp() {
        playerDAO = new PlayerDAO();
    }

    @Test
    public void testGetAllPlayers() {
        List<Player> players = playerDAO.getAllPlayers();
        assertNotNull(players);
        // Assumiamo che il file JSON non sia vuoto
        if (!players.isEmpty()) {
            Player p = players.get(0);
            assertNotNull(p.getNome());
            assertNotNull(p.getRuolo());
        }
    }

    @Test
    public void testGetPlayersByRole() {
        List<Player> players = playerDAO.getAllPlayers();
        if (players.isEmpty()) return; // Skip se json vuoto

        String role = players.get(0).getRuolo();
        List<Player> filtered = playerDAO.getPlayersByRole(role);
        
        assertFalse(filtered.isEmpty());
        for (Player p : filtered) {
            assertEquals(role, p.getRuolo());
        }
    }

    @Test
    public void testGetTotalCount() {
        int count = playerDAO.getTotalCount();
        assertTrue("Il count totale deve essere >= 0", count >= 0);
    }
}