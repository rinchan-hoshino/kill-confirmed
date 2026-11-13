package dev.rinchan.killconfirmed.rinlib.v2;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
public final class Event<T> {
    private final List<T> listeners = new CopyOnWriteArrayList<>();
    public void register(T listener) { listeners.add(Objects.requireNonNull(listener)); }
    public List<T> listeners() { return listeners; }
}
