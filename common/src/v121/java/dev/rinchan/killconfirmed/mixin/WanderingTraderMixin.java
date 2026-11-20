package dev.rinchan.killconfirmed.mixin;

import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin {
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void killConfirmed$addRareCaseTrade(CallbackInfo ci) {
        if (!KillConfirmed.config().enableWandererTrade()) return;
        WanderingTrader self = (WanderingTrader) (Object) this;
        if (self.getRandom().nextFloat() < 0.25f) {
            self.getOffers().add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, DogTagCaseModel.WANDERER_EMERALD_COST),
                    new ItemStack(KillConfirmed.dogTagCase()),
                    1,
                    15,
                    0.05f));
        }
    }
}
