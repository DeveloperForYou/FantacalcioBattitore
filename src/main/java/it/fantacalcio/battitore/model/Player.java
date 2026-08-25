package it.fantacalcio.battitore.model;

import java.util.Objects;

public class Player {
    private final int id;
    private final String role;
    private final String mantraRole;
    private final String name;
    private final String team;
    private final double currentQuotation;
    private final double initialQuotation;
    private final double fvm;
    private PlayerStatus status;

    public Player(int id,
                  String role,
                  String mantraRole,
                  String name,
                  String team,
                  double currentQuotation,
                  double initialQuotation,
                  double fvm) {
        this.id = id;
        this.role = safe(role);
        this.mantraRole = safe(mantraRole);
        this.name = safe(name);
        this.team = safe(team);
        this.currentQuotation = currentQuotation;
        this.initialQuotation = initialQuotation;
        this.fvm = fvm;
        this.status = PlayerStatus.AVAILABLE;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public int getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getMantraRole() {
        return mantraRole;
    }

    public String getName() {
        return name;
    }

    public String getTeam() {
        return team;
    }

    public double getCurrentQuotation() {
        return currentQuotation;
    }

    public double getInitialQuotation() {
        return initialQuotation;
    }

    public double getFvm() {
        return fvm;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    public boolean isAvailable() {
        return status == PlayerStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return name + " (" + role + ", " + team + ")";
    }
}
