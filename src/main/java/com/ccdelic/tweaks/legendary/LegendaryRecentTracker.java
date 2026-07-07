package com.ccdelic.tweaks.legendary;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;

import com.ccdelic.tweaks.config.CCDelicTweaksConfig;

/**
 * Holds the internal registry names of the most recently spawned legendary Pokemon.
 *
 * <p>Only <em>successful</em> legendary spawns are recorded (see {@link LegendarySpawnListener}).
 * The number of remembered entries is driven live by {@link CCDelicTweaksConfig#recentCount()} so a
 * {@code /reload} takes effect immediately.
 *
 * <p>All spawn work in Pixelmon is serialized on a single-threaded executor
 * ({@code SpawnerCoordinator.PROCESSOR}), so contention is not expected. The methods are nonetheless
 * {@code synchronized} as cheap defense-in-depth in case Pixelmon's threading model changes.
 */
public final class LegendaryRecentTracker {

    /** Oldest entries at the head, newest at the tail. */
    private static final Deque<String> RECENT = new ArrayDeque<>();

    /** Hard cap matching the config's upper bound, to bound memory regardless of config. */
    private static final int HARD_CAP = 10;

    /**
     * Source of the "remember this many" value. Defaults to the live server config; overridable in
     * unit tests via {@link #setMaxSizeSupplier(IntSupplier)} so the tracker can be exercised without
     * a loaded NeoForge config.
     */
    private static volatile IntSupplier maxSizeSupplier = CCDelicTweaksConfig::recentCount;

    private LegendaryRecentTracker() {
    }

    private static int maxSize() {
        return Math.min(Math.max(maxSizeSupplier.getAsInt(), 0), HARD_CAP);
    }

    /**
     * @return {@code true} if {@code speciesName} is among the most recent {@code recentCount()}
     *         recorded legendary spawns. Always {@code false} when the recent count is 0.
     */
    public static synchronized boolean isRecent(String speciesName) {
        if (speciesName == null) {
            return false;
        }
        int max = maxSize();
        if (max <= 0) {
            return false;
        }
        int checked = 0;
        Iterator<String> it = RECENT.descendingIterator();
        while (it.hasNext() && checked < max) {
            if (speciesName.equals(it.next())) {
                return true;
            }
            checked++;
        }
        return false;
    }

    /**
     * Records a successful legendary spawn, trimming the history to the current configured size.
     */
    public static synchronized void record(String speciesName) {
        if (speciesName == null) {
            return;
        }
        RECENT.addLast(speciesName);
        int cap = maxSize();
        while (RECENT.size() > cap && !RECENT.isEmpty()) {
            RECENT.removeFirst();
        }
    }

    /** Clears all recorded history. */
    public static synchronized void clear() {
        RECENT.clear();
    }

    /** @return a snapshot copy of the current history (oldest first). For testing/diagnostics. */
    public static synchronized List<String> snapshot() {
        return new ArrayList<>(RECENT);
    }

    /** Test hook: override the source of the "remember this many" value. */
    static synchronized void setMaxSizeSupplier(IntSupplier supplier) {
        maxSizeSupplier = supplier == null ? CCDelicTweaksConfig::recentCount : supplier;
    }
}
