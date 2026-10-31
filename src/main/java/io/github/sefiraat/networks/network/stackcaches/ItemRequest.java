package io.github.sefiraat.networks.network.stackcaches;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
public class ItemRequest extends ItemStackCache {
    @Getter
    private final int originalAmount;

    private int amount;

    public ItemRequest(@NotNull ItemStack itemStack, int amount) {
        super(itemStack);
        this.originalAmount = amount;
        this.amount = amount;
    }

    public void receiveAmount(int amount) {
        this.amount -= amount;
    }

    public int getReceivedAmount() {
        return originalAmount - amount;
    }

    public @NotNull String toString() {
        return "ItemRequest{" + "itemStack=" + getItemStack() + ", originalAmount=" + originalAmount + ", amount=" + amount + '}';
    }
}
