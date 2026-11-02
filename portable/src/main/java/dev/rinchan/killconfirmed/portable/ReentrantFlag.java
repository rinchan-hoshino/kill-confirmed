package dev.rinchan.killconfirmed.portable;

/** A thread-confined boolean flag owned by one or more nested lexical scopes. */
public final class ReentrantFlag {
    private final ThreadLocal<Integer> depth = new ThreadLocal<Integer>();

    public Scope enter() {
        Integer current = depth.get();
        depth.set(current == null ? 1 : current + 1);
        return new Scope(this, Thread.currentThread());
    }

    public boolean isSet() {
        Integer current = depth.get();
        return current != null && current > 0;
    }

    private void leave(Thread owner) {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("A reentrant flag scope must close on its owning thread");
        }
        Integer current = depth.get();
        if (current == null || current <= 0) {
            throw new IllegalStateException("Reentrant flag scope underflow");
        }
        if (current == 1) {
            depth.remove();
        } else {
            depth.set(current - 1);
        }
    }

    public static final class Scope implements AutoCloseable {
        private final ReentrantFlag flag;
        private final Thread owner;
        private boolean closed;

        private Scope(ReentrantFlag flag, Thread owner) {
            this.flag = flag;
            this.owner = owner;
        }

        @Override
        public void close() {
            if (closed) {
                throw new IllegalStateException("Reentrant flag scope is already closed");
            }
            flag.leave(owner);
            closed = true;
        }
    }
}
