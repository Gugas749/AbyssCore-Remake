package com.gugas749.abysscore.api.command;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.List;

public abstract class AbyssCommandRegistrar {


    // In the mod's main class constructor:
    //    MinecraftForge.EVENT_BUS.register(new ModCommands());
    /*
    *
    *       EXAMPLE OF USE
    *
    *    public class ModCommands extends AbyssCommandRegistrar {
    *        @Override
    *        protected List<AbyssCommand> commands() {
    *            return List.of(
    *                SomeCommand::register,
    *                OtherCommand::register
    *            );
    *        }
    *    }
    *
    *
    *
    * */

    protected abstract List<AbyssCommand> commands();

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        commands().forEach(cmd -> cmd.register(event.getDispatcher()));
    }
}
