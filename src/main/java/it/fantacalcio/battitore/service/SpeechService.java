package it.fantacalcio.battitore.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.fantacalcio.battitore.model.Player;

public class SpeechService implements AutoCloseable {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "fantacalcio-speech");
        thread.setDaemon(true);
        return thread;
    });

    public void speak(Player player, boolean includeRole, boolean includeTeam) {
        if (player == null) {
            return;
        }

        StringBuilder text = new StringBuilder(player.getName());
        if (includeRole && !player.getRole().isBlank()) {
            text.append(", ").append(roleName(player.getRole()));
        }
        if (includeTeam && !player.getTeam().isBlank()) {
            text.append(", ").append(player.getTeam());
        }
        speak(text.toString());
    }

    public void speak(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        executor.submit(() -> {
            try {
                speakInternal(text);
            } catch (Exception e) {
                System.err.println(
                        "Impossibile riprodurre la voce: " + e.getMessage()
                );
            }
        });
    }

    private void speakInternal(String text)
            throws IOException, InterruptedException {

        String os = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT);

        if (os.contains("mac")) {
            speakMac(text);
        } else if (os.contains("win")) {
            speakWindows(text);
        } else if (os.contains("linux")) {
            speakLinux(text);
        } else {
            throw new UnsupportedOperationException(
                    "Sistema operativo non supportato: " + os
            );
        }
    }

    private void speakMac(String text)
            throws IOException, InterruptedException {

        execute(List.of(
                "say",
                text
        ));
    }

    private void speakWindows(String text)
            throws IOException, InterruptedException {

        String script =
                    "Add-Type -AssemblyName System.Speech; " +
                    "$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                    "$speaker.Speak($env:FANTACALCIO_TTS_TEXT); " +
                    "$speaker.Dispose();";

        ProcessBuilder processBuilder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                script
        );

        processBuilder.environment().put(
                "FANTACALCIO_TTS_TEXT",
                text
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException(
                    "Il comando TTS è terminato con codice "
                            + exitCode
                            + (output.isBlank() ? "" : ": " + output.trim())
            );
        }
    }

    private void speakLinux(String text)
            throws IOException, InterruptedException {

        if (tryExecute(List.of("spd-say", "--wait", text))) {
            return;
        }

        if (tryExecute(List.of("espeak-ng", text))) {
            return;
        }

        if (tryExecute(List.of("espeak", text))) {
            return;
        }

        throw new IOException(
                "Nessun motore TTS trovato. Installare spd-say, espeak-ng o espeak."
        );
    }

    private void execute(List<String> command)
            throws IOException, InterruptedException {

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException(
                    "Il comando TTS è terminato con codice "
                            + exitCode
                            + (output.isBlank() ? "" : ": " + output.trim())
            );
        }
    }

    private boolean tryExecute(List<String> command)
            throws InterruptedException {

        try {
            execute(command);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String roleName(String role) {
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "P" -> "portiere";
            case "D" -> "difensore";
            case "C" -> "centrocampista";
            case "A" -> "attaccante";
            default -> role;
        };
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
