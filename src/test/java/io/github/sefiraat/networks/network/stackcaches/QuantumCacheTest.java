package io.github.sefiraat.networks.network.stackcaches;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for {@link QuantumCache} to ensure item amounts are tracked
 * correctly and no duplication can occur through arithmetic errors.
 *
 * <p>These tests were added after commit 51171eb4 ("fix: may fix a dupe") which
 * fixed a potential item duplication bug in NetworkQuantumStorage. The cache's
 * amount-tracking methods are a critical part of the fix: if increaseAmount,
 * reduceAmount, or withdrawItem miscalculate, items can be duplicated or lost.
 */
@ExtendWith(MockitoExtension.class)
class QuantumCacheTest {

    private ItemStack mockItem;

    @BeforeEach
    void setUp() {
        mockItem = createMockItemStack();
    }

    /**
     * Creates a mock ItemStack that properly tracks its amount via setAmount/getAmount.
     * This is critical for testing withdrawItem(), which calls clone().setAmount(n)
     * and then reduceAmount(clone.getAmount()).
     */
    private static ItemStack createMockItemStack() {
        ItemStack item = mock(ItemStack.class);
        AtomicInteger amount = new AtomicInteger(1);
        lenient().when(item.getType()).thenReturn(Material.DIAMOND);
        lenient().when(item.getMaxStackSize()).thenReturn(64);
        lenient().when(item.hasItemMeta()).thenReturn(false);
        lenient().when(item.getAmount()).thenAnswer(inv -> amount.get());
        lenient().doAnswer(inv -> {
            amount.set(inv.getArgument(0));
            return null;
        }).when(item).setAmount(anyInt());
        lenient().when(item.clone()).thenAnswer(inv -> createMockItemStack());
        return item;
    }

    private QuantumCache createCache(long amount, long limit) {
        return new QuantumCache(mockItem, amount, limit, false, false);
    }

    @Nested
    @DisplayName("Amount conservation (anti-duplication)")
    class AmountConservation {

        /**
         * Regression: depositing items must increase cache amount by exactly the
         * deposited quantity with no items created from nothing.
         */
        @Test
        @DisplayName("increaseAmount adds exact amount when within capacity")
        void increaseAmountAddsExactAmount() {
            QuantumCache cache = createCache(100, 1000);

            int leftover = cache.increaseAmount(50);

            assertEquals(0, leftover, "No leftover when within capacity");
            assertEquals(150, cache.getAmountLong(),
                    "Cache amount must equal initial + deposited (no duplication)");
        }

        /**
         * Regression: when deposit would exceed capacity, leftover must account
         * for every item -- total of (cache amount + leftover) must equal
         * (initial amount + deposited amount). Violating this invariant causes duplication.
         */
        @Test
        @DisplayName("increaseAmount returns correct leftover at capacity boundary")
        void increaseAmountReturnsLeftoverAtCapacity() {
            long limit = 1000;
            long initial = 990;
            int deposit = 20;

            QuantumCache cache = createCache(initial, limit);
            int leftover = cache.increaseAmount(deposit);

            // Conservation invariant: initial + deposit == cache.amount + leftover
            assertEquals(initial + deposit, cache.getAmountLong() + leftover,
                    "Total items must be conserved: cache amount + leftover == initial + deposited");
            assertEquals(limit, cache.getAmountLong(),
                    "Cache should be filled to capacity");
            assertEquals(10, leftover,
                    "Leftover should be the overflow amount");
        }

        /**
         * Regression: when voidExcess is true, overflow items are destroyed but the
         * cache must still not exceed its limit -- otherwise items are duplicated
         * into the cache beyond capacity.
         */
        @Test
        @DisplayName("increaseAmount with voidExcess does not exceed limit")
        void increaseAmountWithVoidExcessDoesNotExceedLimit() {
            long limit = 100;
            QuantumCache cache = new QuantumCache(mockItem, 90, limit, true, false);

            int leftover = cache.increaseAmount(20);

            assertEquals(0, leftover, "Void excess should consume overflow, returning 0 leftover");
            assertEquals(limit, cache.getAmountLong(),
                    "Cache must not exceed limit even with void excess");
        }

        /**
         * Regression: reduceAmount must decrease cache amount by exactly the withdrawn
         * quantity. Under-reducing would leave phantom items; over-reducing could
         * create negative amounts that wrap around to huge positive values.
         */
        @Test
        @DisplayName("reduceAmount subtracts exact amount")
        void reduceAmountSubtractsExactly() {
            QuantumCache cache = createCache(500, 1000);

            cache.reduceAmount(200);

            assertEquals(300, cache.getAmountLong(),
                    "Cache amount must equal initial - withdrawn (exact subtraction)");
        }

        /**
         * Core duplication scenario: deposit N items, then withdraw N items.
         * The cache must return to its original amount. If the cache ends up
         * with more items than it started with, items were duplicated.
         */
        @Test
        @DisplayName("deposit then withdraw preserves total item count (no duplication)")
        void depositThenWithdrawPreservesTotal() {
            long initialAmount = 500;
            long limit = 1000;
            QuantumCache cache = createCache(initialAmount, limit);

            // Deposit 200 items
            int depositLeftover = cache.increaseAmount(200);
            assertEquals(0, depositLeftover);
            assertEquals(700, cache.getAmountLong());

            // Withdraw 200 items
            ItemStack withdrawn = cache.withdrawItem(200);
            assertNotNull(withdrawn);

            // Cache should be back to initial amount
            assertEquals(initialAmount, cache.getAmountLong(),
                    "After depositing and withdrawing the same amount, "
                            + "cache must return to initial value (no duplication)");
        }

        /**
         * Simulate a rapid sequence of deposits and withdrawals to ensure
         * the running total always matches the expected conservation equation.
         * This catches off-by-one errors that could lead to gradual duplication.
         */
        @Test
        @DisplayName("multiple deposit/withdraw cycles conserve items")
        void multipleDepositWithdrawCyclesConserveItems() {
            long limit = 10000;
            QuantumCache cache = createCache(0, limit);
            long expectedAmount = 0;

            // Simulate 100 deposit cycles
            for (int i = 0; i < 100; i++) {
                int deposit = 64;
                int leftover = cache.increaseAmount(deposit);
                expectedAmount += (deposit - leftover);
            }
            assertEquals(expectedAmount, cache.getAmountLong(),
                    "After deposits, cache amount must equal sum of (deposit - leftover)");

            // Simulate 50 withdraw cycles
            for (int i = 0; i < 50; i++) {
                long beforeWithdraw = cache.getAmountLong();
                ItemStack withdrawn = cache.withdrawItem(64);
                if (withdrawn != null) {
                    int withdrawnAmount = withdrawn.getAmount();
                    expectedAmount -= withdrawnAmount;
                    assertEquals(beforeWithdraw - withdrawnAmount, cache.getAmountLong(),
                            "Each withdrawal must reduce cache by exactly the withdrawn amount");
                }
            }
            assertEquals(expectedAmount, cache.getAmountLong(),
                    "After withdrawals, cache amount must equal deposits minus withdrawals");
        }
    }

    @Nested
    @DisplayName("Withdraw behavior")
    class WithdrawBehavior {

        @Test
        @DisplayName("withdrawItem returns null when no item is set")
        void withdrawReturnsNullWhenNoItem() {
            QuantumCache cache = new QuantumCache(null, 0, 1000, false, false);

            ItemStack result = cache.withdrawItem(64);

            assertNull(result, "Should return null when no item type is set");
        }

        @Test
        @DisplayName("withdrawItem clamps to available amount")
        void withdrawClampsToAvailable() {
            QuantumCache cache = createCache(10, 1000);

            ItemStack withdrawn = cache.withdrawItem(64);

            assertNotNull(withdrawn);
            assertEquals(10, withdrawn.getAmount(),
                    "Withdrawn amount should be clamped to available stock");
            assertEquals(0, cache.getAmountLong(),
                    "Cache should be empty after full withdrawal");
        }

        /**
         * Regression: withdrawing more than available must not create items out
         * of thin air or leave a negative cache amount.
         */
        @Test
        @DisplayName("withdraw does not create items when cache is empty")
        void withdrawDoesNotCreateItemsWhenEmpty() {
            QuantumCache cache = createCache(0, 1000);

            ItemStack withdrawn = cache.withdrawItem(64);

            assertNotNull(withdrawn);
            assertEquals(0, withdrawn.getAmount(),
                    "Should not create items from an empty cache");
            assertEquals(0, cache.getAmountLong(),
                    "Cache amount should remain 0");
        }
    }

    @Nested
    @DisplayName("Capacity boundary conditions")
    class CapacityBoundary {

        @Test
        @DisplayName("increaseAmount at exact capacity returns all as leftover")
        void increaseAtExactCapacity() {
            QuantumCache cache = createCache(1000, 1000);

            int leftover = cache.increaseAmount(50);

            assertEquals(50, leftover, "All items should be returned as leftover");
            assertEquals(1000, cache.getAmountLong(), "Amount should stay at limit");
        }

        @Test
        @DisplayName("increaseAmount to exact capacity leaves zero leftover")
        void increaseToExactCapacity() {
            QuantumCache cache = createCache(950, 1000);

            int leftover = cache.increaseAmount(50);

            assertEquals(0, leftover, "No leftover when filling to exact capacity");
            assertEquals(1000, cache.getAmountLong(), "Amount should be at limit");
        }

        /**
         * Regression: large amounts near Long.MAX_VALUE must not overflow and
         * wrap around, which would reset the counter and allow massive duplication.
         */
        @Test
        @DisplayName("large amount deposits do not cause overflow duplication")
        void largeAmountNoOverflow() {
            long limit = 140_737_488_355_328L; // NetworkQuantumStorage.MAX_AMOUNT
            QuantumCache cache = createCache(limit - 100, limit);

            int leftover = cache.increaseAmount(200);

            assertEquals(100, leftover, "Should return 100 as leftover");
            assertEquals(limit, cache.getAmountLong(),
                    "Cache must not exceed max amount (no overflow duplication)");
        }
    }
}
