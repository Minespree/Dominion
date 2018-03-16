package net.minespree.dominion.servers.data;

/**
 * Denotes the phase the current game is in.
 */
public enum GamePhase {
    /**
     * The server is starting up. No players can be accepted at this time.
     */
    STARTING,
    /**
     * The server is selecting and initializing a map. No players can be accepted at this time.
     */
    SETUP,
    /**
     * The server is awaiting new players.
     */
    WAITING,
    /**
     * The game on this server is now in progress.
     */
    PLAYING,
    /**
     * The game on this server has finished. We will rotate to the setup phase once all players have been kicked.
     */
    ENDGAME
}