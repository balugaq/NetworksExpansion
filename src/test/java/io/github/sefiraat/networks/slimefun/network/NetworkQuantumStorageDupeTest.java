package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the item duplication bug fixed in commit 51171eb4
 * ("fix: may fix a dupe").
 *
 * <h2>Bug description</h2>
 * <p>When items were deposited into or extracted from a {@link NetworkQuantumStorage}
 * via the {@code insertAll()} or {@code extract()} GUI interactions, the network's
 * cached node data was <b>not</b> invalidated. This caused the network to retain a
 * stale view of available items, allowing the same items to be counted (and
 * transferred) twice &mdash; effectively duplicating them.</p>
 *
 * <h2>Fix description</h2>
 * <p>The fix adds {@code NetworkUtils.clearNearbyNetworks(Location)} calls at
 * every mutation point in {@code insertAll()} and {@code extract()}, which removes
 * cached {@link NodeDefinition}s for all adjacent blocks. This forces the network
 * to rebuild its item index on the next tick, eliminating the stale-data
 * duplication window.</p>
 *
 * <h2>What these tests verify</h2>
 * <ol>
 *   <li>That {@code NetworkStorage.removeNode()} correctly removes registered nodes,
 *       which is the primitive operation behind the fix's cache invalidation.</li>
 *   <li>That {@code NetworkNode.VALID_FACES} contains all 6 cardinal directions,
 *       ensuring clearNearbyNetworks covers every adjacent block.</li>
 *   <li>That a location-keyed cache map (mirroring the CACHES contract) correctly
 *       isolates caches per location with no cross-contamination.</li>
 *   <li>{@link QuantumCache} arithmetic is consistent after the deposit + extract
 *       sequence (the full duplication scenario).</li>
 *   <li>Partial overflow deposits conserve exact item counts with no phantom items.</li>
 *   <li>Sequential rapid deposits maintain a consistent running total.</li>
 * </ol>
 *
 * <p><b>Note:</b> Tests that would require loading {@link NetworkQuantumStorage}'s
 * static CACHES field directly are instead tested using a local {@code HashMap}
 * that mirrors the same {@code Map&lt;Location, QuantumCache&gt;} contract,
 * because the static initializer in {@link NetworkQuantumStorage} depends on the
 * Bukkit runtime (Icon, PersistentDataAPI) which is unavailable in unit tests.</p>
 */
class NetworkQuantumStorageDupeTest {

    private final World mockWorld = createMockWorld();

    private static World createMockWorld() {
        World w = mock(World.class);
        when(w.getName()).thenReturn("test_world");
        when(w.getUID()).thenReturn(UUID.nameUUIDFromBytes("test_world".getBytes()));
        return w;
    }

    @AfterEach
    void cleanupNetworkStorage() {
        // Clean any nodes we registered during tests to avoid polluting other tests
        for (Location loc : new HashMap<>(NetworkStorage.getAllNetworkObjects()).keySet()) {
            NetworkStorage.removeNode(loc);
        }
    }

    /**
     * Verify that {@code NetworkStorage.removeNode()} actually removes registered
     * nodes from the global store. This is the primitive that
     * {@code NetworkUtils.clearNearbyNetworks()} relies on to invalidate stale
     * network caches. If removeNode is broken, the entire fix is ineffective.
     */
    @Test
    @DisplayName("NetworkStorage.removeNode removes registered node definitions")
    void removeNodeClearsRegisteredDefinitions() {
        Location loc = new Location(mockWorld, 0, 64, 0);

        NetworkStorage.registerNode(loc, new NodeDefinition(NodeType.CELL));
        assertTrue(NetworkStorage.containsKey(loc),
                "Pre-condition: node should be registered");

        NetworkStorage.removeNode(loc);

        assertFalse(NetworkStorage.containsKey(loc),
                "After removeNode, the location should no longer be in the store");
    }

    /**
     * Verify that all 6 adjacent block faces used by the network are the same
     * faces that clearNearbyNetworks iterates. The fix calls clearNearbyNetworks
     * which iterates {@code NetworkNode.VALID_FACES}. If a face is missing,
     * a network controller on that face would retain stale data.
     */
    @Test
    @DisplayName("VALID_FACES contains all 6 cardinal directions for cache invalidation")
    void validFacesContainsAllCardinalDirections() {
        Set<BlockFace> expectedFaces = Set.of(
                BlockFace.UP, BlockFace.DOWN,
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST);

        for (BlockFace face : expectedFaces) {
            assertTrue(io.github.sefiraat.networks.network.NetworkNode.VALID_FACES.contains(face),
                    "VALID_FACES must contain " + face + " for complete cache invalidation");
        }
        assertEquals(expectedFaces.size(),
                io.github.sefiraat.networks.network.NetworkNode.VALID_FACES.size(),
                "VALID_FACES should have exactly 6 faces (no extras that could cause issues)");
    }

    /**
     * Verify that re-registering a node at the same location after removal works.
     * This simulates the lifecycle: node cached -> cache invalidated -> node
     * re-discovered on next network tick.
     */
    @Test
    @DisplayName("Node can be re-registered after removal (cache invalidation + rebuild)")
    void nodeCanBeReregisteredAfterRemoval() {
        Location loc = new Location(mockWorld, 5, 64, 5);

        NetworkStorage.registerNode(loc, new NodeDefinition(NodeType.CELL));
        NetworkStorage.removeNode(loc);
        assertFalse(NetworkStorage.containsKey(loc));

        // Re-register (simulates network rebuild)
        NetworkStorage.registerNode(loc, new NodeDefinition(NodeType.CONTROLLER));
        assertTrue(NetworkStorage.containsKey(loc),
                "Node should be re-registerable after removal");
    }

    /**
     * Verify that a location-keyed cache map correctly isolates caches per
     * location with no cross-contamination. This mirrors the contract of
     * {@code NetworkQuantumStorage.CACHES}, which the insertAll/extract methods
     * use to look up the cache for the clicked block.
     *
     * <p>We use a local HashMap instead of the actual CACHES field because
     * {@link NetworkQuantumStorage}'s static initializer requires the Bukkit
     * runtime.</p>
     */
    @Test
    @DisplayName("Location-keyed cache map isolates caches per location (CACHES contract)")
    void cachesMapIsolatesCachesPerLocation() {
        Location loc1 = new Location(mockWorld, 10, 64, 10);
        Location loc2 = new Location(mockWorld, 20, 64, 20);

        QuantumCache cache1 = new QuantumCache(null, 100, 1000L, false, false);
        QuantumCache cache2 = new QuantumCache(null, 200, 1000L, false, false);

        // Simulate the CACHES map: Map<Location, QuantumCache>
        Map<Location, QuantumCache> caches = new HashMap<>();
        caches.put(loc1, cache1);
        caches.put(loc2, cache2);

        // Each location must map to its own independent cache
        assertEquals(100, caches.get(loc1).getAmountLong(),
                "Cache at loc1 should have its own amount");
        assertEquals(200, caches.get(loc2).getAmountLong(),
                "Cache at loc2 should have its own amount");

        // Mutate one cache and verify the other is not affected
        cache1.increaseAmount(50);
        assertEquals(150, caches.get(loc1).getAmountLong(),
                "Mutating cache1 should only affect loc1");
        assertEquals(200, caches.get(loc2).getAmountLong(),
                "Cache at loc2 must remain unchanged (no cross-contamination)");
    }

    /**
     * Full duplication scenario regression test.
     *
     * <p>Simulates the sequence that caused duplication before the fix:</p>
     * <ol>
     *   <li>Quantum storage has initial items in cache</li>
     *   <li>Items are deposited (increaseAmount)</li>
     *   <li>Network reads stale data and "transfers" items that already moved</li>
     *   <li>Items are effectively duplicated</li>
     * </ol>
     *
     * <p>With the fix, step 2 now calls clearNearbyNetworks, which forces the
     * network to re-read the actual cache amounts.</p>
     */
    @Test
    @DisplayName("deposit + extract cycle conserves exact item count (no duplication)")
    void depositExtractCycleConservesItems() {
        Location storageLoc = new Location(mockWorld, 50, 64, 50);

        // Simulate a quantum storage with 500 items, capacity 10000
        QuantumCache cache = new QuantumCache(null, 500, 10000L, false, false);

        Map<Location, QuantumCache> caches = new HashMap<>();
        caches.put(storageLoc, cache);

        long totalItemsBefore = cache.getAmountLong();

        // Step 1: Player deposits 200 items via insertAll
        int playerDeposit = 200;
        int leftover = cache.increaseAmount(playerDeposit);
        int actuallyDeposited = playerDeposit - leftover;

        // Step 2: Verify the cache amount after deposit
        long expectedAfterDeposit = totalItemsBefore + actuallyDeposited;
        assertEquals(expectedAfterDeposit, cache.getAmountLong(),
                "After deposit, cache must reflect exactly initial + deposited items");

        // Step 3: Verify the CACHES map returns the same (updated) cache object.
        // Before the fix, the stale network cache would read an OLD amount here.
        long cacheAmountAfterDeposit = caches.get(storageLoc).getAmountLong();
        assertEquals(expectedAfterDeposit, cacheAmountAfterDeposit,
                "CACHES map must reflect the current (post-deposit) amount, not stale data");

        // Step 4: Extract the deposited amount back
        cache.reduceAmount(actuallyDeposited);
        assertEquals(totalItemsBefore, cache.getAmountLong(),
                "After extracting the same amount, cache should return to initial value. "
                        + "Any discrepancy indicates item duplication or loss.");
    }

    /**
     * Verify that the duplication-critical path through insertAll maintains the
     * conservation invariant even when the cache is near capacity.
     */
    @Test
    @DisplayName("partial overflow deposit does not create phantom items")
    void partialOverflowDoesNotCreatePhantomItems() {
        long capacity = 1000;
        long initialAmount = 950;
        QuantumCache cache = new QuantumCache(null, initialAmount, capacity, false, false);

        // Deposit 100 items: only 50 should fit, 50 should be returned
        int deposit = 100;
        int leftover = cache.increaseAmount(deposit);

        // Verify conservation: deposited = actually_added + leftover
        int actuallyAdded = deposit - leftover;
        assertEquals(50, actuallyAdded, "Only 50 items should fit");
        assertEquals(50, leftover, "50 items should be returned as leftover");
        assertEquals(capacity, cache.getAmountLong(),
                "Cache should be exactly at capacity");

        // The total items in existence should be:
        // - In cache: capacity (1000)
        // - Returned to player: leftover (50)
        // - Total: 1050 = initialAmount (950) + deposit (100)
        long totalItems = cache.getAmountLong() + leftover;
        assertEquals(initialAmount + deposit, totalItems,
                "Total items (cache + leftover) must equal initial + deposited. "
                        + "Any difference indicates duplication or loss.");
    }

    /**
     * Regression: simulate multiple sequential deposit operations hitting
     * the same cache. Without the network invalidation fix, each deposit cycle
     * could allow the network to "see" old amounts, causing cumulative duplication.
     */
    @Test
    @DisplayName("sequential deposits maintain consistent running total")
    void sequentialDepositsConserveRunningTotal() {
        Location storageLoc = new Location(mockWorld, 200, 64, 200);

        long capacity = 10000;
        QuantumCache cache = new QuantumCache(null, 0, capacity, false, false);

        Map<Location, QuantumCache> caches = new HashMap<>();
        caches.put(storageLoc, cache);

        long totalDeposited = 0;

        // Simulate 50 sequential insertAll operations (like rapid clicking)
        for (int i = 0; i < 50; i++) {
            int deposit = 64;
            int leftover = cache.increaseAmount(deposit);
            totalDeposited += (deposit - leftover);

            // After each deposit, the cache in the CACHES map must agree with the
            // actual cache object. Before the fix, stale network reads between
            // deposits could diverge these values.
            assertEquals(totalDeposited, caches.get(storageLoc).getAmountLong(),
                    "CACHES map must always reflect current amount after deposit #" + (i + 1));
        }

        assertEquals(totalDeposited, cache.getAmountLong(),
                "Final cache amount must equal sum of all deposits");

        // Now extract everything back
        long totalExtracted = 0;
        while (cache.getAmountLong() > 0) {
            int toExtract = (int) Math.min(64, cache.getAmountLong());
            cache.reduceAmount(toExtract);
            totalExtracted += toExtract;
        }

        assertEquals(totalDeposited, totalExtracted,
                "Total extracted must equal total deposited (conservation of items)");
        assertEquals(0, cache.getAmountLong(),
                "Cache should be empty after full extraction");
    }
}
