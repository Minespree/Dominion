package net.minespree.dominion.punishments;

import java.util.UUID;

/**
 * @since 12/09/2017
 */
public class Punishment {
    private PunishmentType type;
    private UUID source;
    private UUID target;
    private String reason;
    private long timestamp;
    private long until;
    private String appealCode;

    public Punishment(PunishmentType type, UUID source, UUID target, String reason, long timestamp, long until, String appealCode) {
        this.type = type;
        this.source = source;
        this.target = target;
        this.reason = reason;
        this.timestamp = timestamp;
        this.until = until;
        this.appealCode = appealCode;
    }

    public PunishmentType getType() {
        return type;
    }

    public UUID getSource() {
        return source;
    }

    public UUID getTarget() {
        return target;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getUntil() {
        return until;
    }

    public String getAppealCode() {
        return appealCode;
    }

    public void setUntil(long until) {
        this.until = until;
    }

    public boolean hasExpired() {
        return until != -1 && until <= System.currentTimeMillis();
    }
}
