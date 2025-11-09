package net.le0nia.inconsistentize.mixin;

import net.le0nia.inconsistentize.Constants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    private int pickupDelay;

    @Shadow
    @Nullable
    private UUID target;

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void init(CallbackInfo info) {
        info.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity)(Object)this;
        if (self.level().isClientSide()) return;

        List<Player> players = self.level().getEntitiesOfClass(
                Player.class,
                self.getBoundingBox().inflate(1.0, 0.5, 1.0),
                p -> p.isAlive() && !p.isSpectator()
        );

        long seed = ((long) self.getId() << 32) ^ self.level().getGameTime();
        RandomSource rng = RandomSource.create(seed);
        Util.shuffle(players, rng);

        for (Player p : players) {
            if (!p.getBoundingBox().inflate(1.0, 0.5, 1.0).intersects(self.getBoundingBox()))
                continue;

            ItemStack stack = this.getItem();
            if (stack.isEmpty()) break;

            if (this.pickupDelay == 0 && (this.target == null || this.target.equals(p.getUUID()))) {
                int countBefore = stack.getCount();
                Item item = stack.getItem();

                if (p.getInventory().add(stack)) {
                    p.take(self, countBefore);
                    if (stack.isEmpty()) {
                        self.discard();
                        stack.setCount(countBefore);
                    }
                    p.awardStat(Stats.ITEM_PICKED_UP.get(item), countBefore);
                    p.onItemPickup(self);
                    break;
                }
            }
        }
    }
}
