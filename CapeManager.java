package wcape;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

public class CapeManager {
    public static final Identifier CAPE_ID = Identifier.fromNamespaceAndPath("wcape", "custom_cape");
    public static volatile boolean active = false;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10)).build();

    private static Path saveFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("wcape.txt");
    }

    /** Register the image as the cape texture on the render thread and remember the source. */
    private static void apply(NativeImage image, String saveLine, Consumer<String> onDone) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.getTextureManager().register(CAPE_ID, new DynamicTexture(() -> "wcape", image));
            active = true;
            if (saveLine != null) {
                try { Files.writeString(saveFile(), saveLine); } catch (Exception ignored) {}
            }
            onDone.accept("Cape applied!");
        });
    }

    public static void loadUrl(String url, boolean save, Consumer<String> onDone) {
        Thread t = new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent", "Wcape/1.0").build();
                HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (res.statusCode() != 200) {
                    onDone.accept("Error: HTTP " + res.statusCode());
                    return;
                }
                NativeImage img;
                try (InputStream in = res.body()) {
                    img = NativeImage.read(in); // PNG only
                }
                apply(img, save ? "url\n" + url : null, onDone);
            } catch (Exception e) {
                onDone.accept("Error: " + e.getMessage());
            }
        }, "Wcape-Download");
        t.setDaemon(true);
        t.start();
    }

    public static void loadFile(String pathStr, boolean save, Consumer<String> onDone) {
        Thread t = new Thread(() -> {
            try {
                String p = pathStr.trim();
                if (p.length() > 1 && p.startsWith("\"") && p.endsWith("\"")) {
                    p = p.substring(1, p.length() - 1); // Windows "Copy as path" adds quotes
                }
                Path file = Path.of(p);
                if (!Files.isRegularFile(file)) {
                    onDone.accept("Error: file not found: " + p);
                    return;
                }
                NativeImage img;
                try (InputStream in = Files.newInputStream(file)) {
                    img = NativeImage.read(in); // PNG only
                }
                apply(img, save ? "file\n" + p : null, onDone);
            } catch (Exception e) {
                onDone.accept("Error: " + e.getMessage());
            }
        }, "Wcape-File");
        t.setDaemon(true);
        t.start();
    }

    public static void clear() {
        active = false;
        try { Files.deleteIfExists(saveFile()); } catch (Exception ignored) {}
    }

    public static void loadSaved() {
        try {
            Path f = saveFile();
            if (!Files.exists(f)) return;
            String[] lines = Files.readString(f).split("\\R", 2);
            if (lines.length < 2) return;
            if (lines[0].equals("url")) loadUrl(lines[1].trim(), false, s -> {});
            else if (lines[0].equals("file")) loadFile(lines[1], false, s -> {});
        } catch (Exception ignored) {}
    }
}
