package dev.rinchan.killconfirmed.portable;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.Map;

public record FunctionParseResult(Map<PlaceholderKey, JsonElement> values, List<String> errors) {}
