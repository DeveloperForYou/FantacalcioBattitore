package it.fantacalcio.battitore.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import it.fantacalcio.battitore.model.Player;
import it.fantacalcio.battitore.model.PlayerStatus;

public class AuctionService {
    public static final String ALL_ROLES = "Tutti";

    private final Random random;
    private final List<Player> players = new ArrayList<>();
    private Player currentPlayer;

    public AuctionService() {
        this(new Random());
    }

    AuctionService(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    public void loadPlayers(Collection<Player> newPlayers) {
        players.clear();
        players.addAll(newPlayers);
        currentPlayer = null;
    }

    public Player drawNext(String role) {
        List<Player> candidates = getAvailable(role);
        if (candidates.isEmpty()) {
            currentPlayer = null;
            return null;
        }

        currentPlayer = candidates.get(random.nextInt(candidates.size()));
        return currentPlayer;
    }

    public Player markCurrentSold() {
        if (currentPlayer != null) {
            currentPlayer.setStatus(PlayerStatus.SOLD);
        }
        return currentPlayer;
    }

    public Player markCurrentUnsold() {
        if (currentPlayer != null) {
            currentPlayer.setStatus(PlayerStatus.UNSOLD);
        }
        return currentPlayer;
    }

    public int reactivateUnsold(String role) {
        int count = 0;
        for (Player player : players) {
            if (player.getStatus() == PlayerStatus.UNSOLD && matchesRole(player, role)) {
                player.setStatus(PlayerStatus.AVAILABLE);
                count++;
            }
        }
        currentPlayer = null;
        return count;
    }

    public int countAvailable(String role) {
        return (int) players.stream()
                .filter(Player::isAvailable)
                .filter(player -> matchesRole(player, role))
                .count();
    }

    public int countSold() {
        return (int) players.stream()
                .filter(player -> player.getStatus() == PlayerStatus.SOLD)
                .count();
    }

    public int countUnsold(String role) {
        return (int) players.stream()
                .filter(player -> player.getStatus() == PlayerStatus.UNSOLD)
                .filter(player -> matchesRole(player, role))
                .count();
    }

    public List<Player> getAvailable(String role) {
        return players.stream()
                .filter(Player::isAvailable)
                .filter(player -> matchesRole(player, role))
                .filter(player -> player.getFvm() > 1)
                .collect(Collectors.toList());
    }

    public List<Player> getAllByStatusAndRole(PlayerStatus status, String role) {
        return players.stream()
                .filter(player -> player.getStatus() == status)
                .filter(player -> matchesRole(player, role))
                .collect(Collectors.toList());
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    private boolean matchesRole(Player player, String role) {
        return role == null
                || role.isBlank()
                || ALL_ROLES.equalsIgnoreCase(role)
                || player.getRole().equalsIgnoreCase(role);
    }
}
