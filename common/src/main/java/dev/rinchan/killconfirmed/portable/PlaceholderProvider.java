package dev.rinchan.killconfirmed.portable;

import com.google.gson.JsonElement;
import java.util.Optional;

@FunctionalInterface
public interface PlaceholderProvider<C> {
    Optional<JsonElement> resolve(C context) throws Exception;
}
