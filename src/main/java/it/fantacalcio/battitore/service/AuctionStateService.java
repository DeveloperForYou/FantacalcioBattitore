package it.fantacalcio.battitore.service;

import it.fantacalcio.battitore.model.Player;
import it.fantacalcio.battitore.model.PlayerStatus;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionStateService {

    public void save(Path file, List<Player> players) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Id\tStatus");
            writer.newLine();
            for (Player player : players) {
                writer.write(player.getId() + "\t" + player.getStatus().name());
                writer.newLine();
            }
        }
    }

    public int load(Path file, List<Player> players) throws IOException {
        Map<Integer, PlayerStatus> statuses = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("Id\t")) {
                    continue;
                }

                String[] parts = line.split("\\t");
                if (parts.length != 2) {
                    continue;
                }

                try {
                    int id = Integer.parseInt(parts[0].trim());
                    PlayerStatus status = PlayerStatus.valueOf(parts[1].trim());
                    statuses.put(id, status);
                } catch (IllegalArgumentException ignored) {
                    // Riga non valida: la saltiamo.
                }
            }
        }

        int restored = 0;
        for (Player player : players) {
            PlayerStatus status = statuses.get(player.getId());
            if (status != null) {
                player.setStatus(status);
                restored++;
            }
        }
        return restored;
    }
}
