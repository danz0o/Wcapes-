package wcape;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class WcapeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(c -> CapeManager.loadSaved());

        // Usage: /Wcape url <direct link to a .png>  |  /Wcape file <path to a .png on your PC>  |  /Wcape off
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("Wcape")
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    CapeManager.clear();
                    ctx.getSource().sendFeedback(Component.literal("[Wcape] Cape removed."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("file")
                    .then(ClientCommandManager.argument("path", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String path = StringArgumentType.getString(ctx, "path");
                            var src = ctx.getSource();
                            src.sendFeedback(Component.literal("[Wcape] Loading..."));
                            CapeManager.loadFile(path, true, msg ->
                                Minecraft.getInstance().execute(() ->
                                    src.sendFeedback(Component.literal("[Wcape] " + msg))));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("url")
                    .then(ClientCommandManager.argument("link", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String link = StringArgumentType.getString(ctx, "link").trim();
                            var src = ctx.getSource();
                            src.sendFeedback(Component.literal("[Wcape] Downloading..."));
                            CapeManager.loadUrl(link, true, msg ->
                                Minecraft.getInstance().execute(() ->
                                    src.sendFeedback(Component.literal("[Wcape] " + msg))));
                            return 1;
                        })))));
    }
}
