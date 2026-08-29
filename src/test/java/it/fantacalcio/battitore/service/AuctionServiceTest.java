package it.fantacalcio.battitore.service;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import it.fantacalcio.battitore.model.Player;
import it.fantacalcio.battitore.model.PlayerStatus;

class AuctionServiceTest {

    @Test
    void soldPlayerIsNoLongerAvailable() {
        AuctionService service = new AuctionService(new Random(1));
        Player p1 = player(1, "A", "Giocatore 1");
        Player p2 = player(2, "A", "Giocatore 2");
        service.loadPlayers(List.of(p1, p2));

        Player drawn = service.drawNext("A");
        service.markCurrentSold();

        assertEquals(PlayerStatus.SOLD, drawn.getStatus());
        assertEquals(1, service.countAvailable("A"));
    }

    @Test
    void unsoldPlayersCanBeReactivated() {
        AuctionService service = new AuctionService(new Random(1));
        Player player = player(1, "D", "Difensore");
        service.loadPlayers(List.of(player));

        service.drawNext("D");
        service.markCurrentUnsold();

        assertEquals(0, service.countAvailable("D"));
        assertEquals(1, service.countUnsold("D"));
        assertEquals(1, service.reactivateUnsold("D"));
        assertEquals(1, service.countAvailable("D"));
    }

    @Test
    void roleFilterWorks() {
        AuctionService service = new AuctionService(new Random(1));
        service.loadPlayers(List.of(
                player(1, "P", "Portiere"),
                player(2, "A", "Attaccante")
        ));

        assertEquals(1, service.countAvailable("P"));
        assertEquals(1, service.countAvailable("A"));
        assertEquals(2, service.countAvailable(AuctionService.ALL_ROLES));
    }

    private Player player(int id, String role, String name) {
        return new Player(id, role, "", name, "TEST", 0, 0, 5);
    }
}
