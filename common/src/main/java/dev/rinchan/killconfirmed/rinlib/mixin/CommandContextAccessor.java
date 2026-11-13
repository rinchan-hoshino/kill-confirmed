package dev.rinchan.killconfirmed.rinlib.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.execution.ExecutionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Commands.class)
public interface CommandContextAccessor {
    @Accessor("CURRENT_EXECUTION_CONTEXT")
    static ThreadLocal<ExecutionContext<CommandSourceStack>> killConfirmed$currentContext() {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
