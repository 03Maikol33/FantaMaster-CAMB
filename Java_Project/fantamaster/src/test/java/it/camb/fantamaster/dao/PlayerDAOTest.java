package it.camb.fantamaster.dao;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import it.camb.fantamaster.model.Player;

public class PlayerDAOTest {

    @Test
    public void testFilteredCountsAndPaging() {
        PlayerDAO dao = new PlayerDAO();

        int total = dao.getTotalCount();
        assertTrue("Total players should be > 0", total > 0);

        int attackers = dao.getFilteredTotalCount("A", null, null);
        assertTrue("There should be at least one attacker", attackers > 0);

        List<Player> attackersPage = dao.getPlayersPageFiltered(0, 10, "A", null, null);
        assertEquals(Math.min(10, attackers), attackersPage.size());

        int expensive = dao.getFilteredTotalCount(null, 80, null);
        assertTrue("There should be at least one player with prezzo >= 80", expensive > 0);

        List<Player> expensivePage = dao.getPlayersPageFiltered(0, 5, null, 80, null);
        assertEquals(Math.min(5, expensive), expensivePage.size());

        // Combined filter
        int combo = dao.getFilteredTotalCount("D", 10, 30);
        assertTrue("Combined filter should produce a non-negative count", combo >= 0);
    }
}
