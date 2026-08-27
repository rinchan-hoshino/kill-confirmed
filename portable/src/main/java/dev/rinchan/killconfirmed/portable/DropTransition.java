package dev.rinchan.killconfirmed.portable;

public record DropTransition(DropAction action, PendingState next) {}
