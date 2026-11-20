package dev.rinchan.killconfirmed.client;

import static org.junit.jupiter.api.Assertions.*;

import dev.rinchan.killconfirmed.DogTagCaseMenu;
import dev.rinchan.killconfirmed.KillConfirmed;
import java.lang.reflect.Field;
import net.minecraft.SharedConstants;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

/**
 * Headless black-box client Screen lifecycle verification for DogTagCaseScreen and KillConfirmedConfigScreen.
 * Verifies constructor, init(Minecraft, w, h), widget creation, resize, input handling, and close with zero exceptions.
 */
class ScreenLifecycleTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        KillConfirmed.bindRegistries(
                () -> Items.NAME_TAG,
                () -> Items.BUNDLE,
                () -> MenuType.GENERIC_9x6);
    }

    @Test
    void dogTagCaseScreen_initializes_resizes_and_handles_inputs_without_exception() throws Exception {
        Minecraft mc = createHeadlessMinecraft();
        ServerPlayer dummyPlayer = allocate(ServerPlayer.class);
        Inventory inv = new Inventory(dummyPlayer);
        DogTagCaseMenu menu = new DogTagCaseMenu(1, inv);

        DogTagCaseScreen screen = new DogTagCaseScreen(menu, inv, Component.literal("Dog Tag Case"));
        assertDoesNotThrow(() -> screen.init(mc, 427, 240));
        assertFalse(screen.children().isEmpty(), "DogTagCaseScreen must register its search EditBox widget");
        assertDoesNotThrow(() -> screen.resize(mc, 640, 360));
        assertDoesNotThrow(() -> screen.mouseScrolled(200, 100, 0.0, -1.0));
        assertDoesNotThrow(() -> screen.mouseReleased(200, 100, 0));
    }

    @Test
    void killConfirmedConfigScreen_initializes_resizes_and_closes_without_exception() throws Exception {
        Minecraft mc = createHeadlessMinecraft();
        KillConfirmedConfigScreen screen = new KillConfirmedConfigScreen(null);

        assertDoesNotThrow(() -> screen.init(mc, 427, 240));
        assertTrue(screen.children().size() >= 11,
                "KillConfirmedConfigScreen must register all 6 toggles, 2 edit boxes, and 3 action buttons");
        assertDoesNotThrow(() -> screen.resize(mc, 854, 480));
        assertDoesNotThrow(() -> {
            setField(screen, "minecraft", null);
            screen.onClose();
        });
    }

    private static Minecraft createHeadlessMinecraft() throws Exception {
        Minecraft mc = allocate(Minecraft.class);
        Font font = new Font(rl -> null, false);
        setField(font, "splitter", new StringSplitter((codePoint, style) -> 6.0f));
        setField(mc, "font", font);
        setField(mc, "lastInputType", InputType.NONE);
        GameNarrator silentNarrator = new GameNarrator(mc) {
            @Override
            public boolean isActive() {
                return false;
            }
        };
        setField(mc, "narrator", silentNarrator);
        return mc;
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        return (T) unsafe.allocateInstance(type);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> cls = target.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " on " + target.getClass().getName());
    }
}
