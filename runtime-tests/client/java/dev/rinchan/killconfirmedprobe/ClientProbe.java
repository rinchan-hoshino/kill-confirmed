package dev.rinchan.killconfirmedprobe;
import dev.rinchan.killconfirmed.KillConfirmed;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Files;
import java.nio.file.Path;
public final class ClientProbe {
    private static int ticks;
    private static boolean finished;
    public static void tick() {
        var client = Minecraft.getInstance();
        if (finished || ++ticks < 40 || !client.isGameLoadFinished()) return;
        finished = true;
        try {
            if (!BuiltInRegistries.ITEM.getKey(KillConfirmed.dogTag()).toString().equals("kill_confirmed:dog_tag"))
                throw new AssertionError("dog tag is not registered");
            if (!KillConfirmed.class.getResource("KillConfirmed.class").toString().contains(".jar"))
                throw new AssertionError("client is not loading the release jar");
            var resource = client.getResourceManager().getResource(KillConfirmed.id("textures/item/dog_tag.png")).orElseThrow();
            try (var stream = resource.open(); var image = NativeImage.read(stream)) {
                if (image.getWidth() < 1 || image.getHeight() < 1) throw new AssertionError("invalid dog tag texture");
            }
            client.getResourceManager().getResource(KillConfirmed.id("models/item/dog_tag.json")).orElseThrow();
            Files.writeString(Path.of("client.pass"), "registered item, loaded release jar, texture and model resources\n");
            System.out.println("KC_CLIENT_PASS");
        } catch (Throwable error) {
            error.printStackTrace();
            System.out.println("KC_CLIENT_FAIL " + error);
        } finally { client.stop(); }
    }
}
