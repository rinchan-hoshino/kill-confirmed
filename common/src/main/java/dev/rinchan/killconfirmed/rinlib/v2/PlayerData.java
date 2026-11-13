package dev.rinchan.killconfirmed.rinlib.v2;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import dev.rinchan.killconfirmed.rinlib.v2.internal.PlayerDataAccess;
public final class PlayerData {
    private static final Map<String, Slot> SLOTS = new ConcurrentHashMap<>();
    private PlayerData() {}
    public static Slot slot(String rootKey) { return SLOTS.computeIfAbsent(rootKey, Slot::new); }
    public static java.util.Set<String> keys() { return SLOTS.keySet(); }
    public static final class Slot {
        private final String key;
        private Slot(String key) { this.key = key; }
        public Optional<CompoundTag> get(ServerPlayer p) {
            return Optional.ofNullable(((PlayerDataAccess) p).killConfirmed$data().get(key)).map(CompoundTag::copy);
        }
        public void set(ServerPlayer p, @Nullable CompoundTag value) {
            var data = ((PlayerDataAccess) p).killConfirmed$data();
            if (value == null) data.remove(key);
            else data.put(key, value.copy());
        }
    }
}
