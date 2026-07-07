package com.ccdelic.tweaks.legendary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LegendaryRecentTracker}. The tracker's max size is driven by an injectable
 * supplier so these run without a loaded NeoForge config.
 */
class LegendaryRecentTrackerTest {

    private final AtomicInteger max = new AtomicInteger(3);

    @BeforeEach
    void setUp() {
        LegendaryRecentTracker.clear();
        LegendaryRecentTracker.setMaxSizeSupplier(max::get);
    }

    @AfterEach
    void tearDown() {
        LegendaryRecentTracker.clear();
        LegendaryRecentTracker.setMaxSizeSupplier(null); // restore default (live config)
    }

    @Test
    void recordsAndDetectsRecentSpecies() {
        LegendaryRecentTracker.record("zacian");
        LegendaryRecentTracker.record("mewtwo");
        LegendaryRecentTracker.record("rayquaza");

        assertTrue(LegendaryRecentTracker.isRecent("zacian"));
        assertTrue(LegendaryRecentTracker.isRecent("mewtwo"));
        assertTrue(LegendaryRecentTracker.isRecent("rayquaza"));
        assertFalse(LegendaryRecentTracker.isRecent("kyogre"));
    }

    @Test
    void evictsOldestBeyondMaxSize() {
        LegendaryRecentTracker.record("zacian");
        LegendaryRecentTracker.record("mewtwo");
        LegendaryRecentTracker.record("rayquaza");
        LegendaryRecentTracker.record("kyogre"); // pushes out zacian (max = 3)

        assertFalse(LegendaryRecentTracker.isRecent("zacian"));
        assertTrue(LegendaryRecentTracker.isRecent("mewtwo"));
        assertTrue(LegendaryRecentTracker.isRecent("rayquaza"));
        assertTrue(LegendaryRecentTracker.isRecent("kyogre"));
        assertEquals(3, LegendaryRecentTracker.snapshot().size());
    }

    @Test
    void zeroMaxSizeDisablesRecency() {
        max.set(0);
        LegendaryRecentTracker.record("zacian");
        assertFalse(LegendaryRecentTracker.isRecent("zacian"));
        assertTrue(LegendaryRecentTracker.snapshot().isEmpty());
    }

    @Test
    void shrinkingMaxSizeIsHonoredOnNextRecord() {
        LegendaryRecentTracker.record("a");
        LegendaryRecentTracker.record("b");
        LegendaryRecentTracker.record("c");
        assertEquals(3, LegendaryRecentTracker.snapshot().size());

        // Shrink to 1; the next record trims the history down.
        max.set(1);
        LegendaryRecentTracker.record("d");

        assertEquals(1, LegendaryRecentTracker.snapshot().size());
        assertTrue(LegendaryRecentTracker.isRecent("d"));
        assertFalse(LegendaryRecentTracker.isRecent("c"));
    }

    @Test
    void isRecentOnlyConsidersMostRecentWindow() {
        // History deeper than the window: only the last 'max' entries count as recent.
        max.set(5);
        for (String s : new String[] {"a", "b", "c", "d", "e"}) {
            LegendaryRecentTracker.record(s);
        }
        // Shrink window without recording; oldest entries fall outside the window.
        max.set(2);
        assertTrue(LegendaryRecentTracker.isRecent("e"));
        assertTrue(LegendaryRecentTracker.isRecent("d"));
        assertFalse(LegendaryRecentTracker.isRecent("c"));
        assertFalse(LegendaryRecentTracker.isRecent("a"));
    }

    @Test
    void nullSpeciesIsIgnored() {
        LegendaryRecentTracker.record(null);
        assertFalse(LegendaryRecentTracker.isRecent(null));
        assertTrue(LegendaryRecentTracker.snapshot().isEmpty());
    }
}
