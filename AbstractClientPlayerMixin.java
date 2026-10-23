package wcape.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wcape.CapeManager;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void wcape$replaceCape(CallbackInfoReturnable<PlayerSkin> cir) {
        if (!CapeManager.active) return;
        if ((Object) this != Minecraft.getInstance().player) return;
        PlayerSkin s = cir.getReturnValue();
        ClientAsset.Texture cape = new ClientAsset.ResourceTexture(CapeManager.CAPE_ID, CapeManager.CAPE_ID);
        cir.setReturnValue(new PlayerSkin(s.body(), cape, cape, s.model(), s.secure()));
    }
}
