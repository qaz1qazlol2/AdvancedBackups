package computer.heather.advancedbackups.client;

import java.time.Instant;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import computer.heather.advancedbackups.core.CoreCommandSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages.Update;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraftforge.client.ClientCommandSourceStack;
import net.minecraftforge.server.ServerLifecycleHooks;

public class AdvancedBackupsClientCommand {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(literal("backup").requires((runner) -> {
            return true; //no point fucking with the check
        }).then(literal("start").executes((runner) -> {
            Update acknowledgment = Minecraft.getInstance().player.connection.lastSeenMessages.generateAndApplyUpdate().update();
            Minecraft.getInstance().player.connection.send(new ServerboundChatCommandPacket("backup start", Instant.now(), 0, 
                ArgumentSignatures.EMPTY, acknowledgment));
            return 1;
         }))

         .then(literal("reload-config").executes((runner) -> {            
            Update acknowledgment = Minecraft.getInstance().player.connection.lastSeenMessages.generateAndApplyUpdate().update();
            Minecraft.getInstance().player.connection.send(new ServerboundChatCommandPacket("backup reload-config", Instant.now(), 0, 
                ArgumentSignatures.EMPTY, acknowledgment));
            return 1;
         }))

         .then(literal("reset-chain").executes((runner) -> {
            Update acknowledgment = Minecraft.getInstance().player.connection.lastSeenMessages.generateAndApplyUpdate().update();
            Minecraft.getInstance().player.connection.send(new ServerboundChatCommandPacket("backup reset-chain", Instant.now(), 0, 
                ArgumentSignatures.EMPTY, acknowledgment));
            return 1;
         }))

         .then(literal("snapshot").executes((runner) -> {
            Update acknowledgment = Minecraft.getInstance().player.connection.lastSeenMessages.generateAndApplyUpdate().update();
            Minecraft.getInstance().player.connection.send(new ServerboundChatCommandPacket("backup snapshot", Instant.now(), 0, 
                ArgumentSignatures.EMPTY, acknowledgment));
            return 1;
         })

            .then(Commands.argument("name", StringArgumentType.greedyString()).executes((runner) -> {
                String name = StringArgumentType.getString(runner, "name");
                Update acknowledgment = Minecraft.getInstance().player.connection.lastSeenMessages.generateAndApplyUpdate().update();
                Minecraft.getInstance().player.connection.send(new ServerboundChatCommandPacket("backup snapshot " + name, Instant.now(), 0, 
                    ArgumentSignatures.EMPTY, acknowledgment));
                return 1;
            })))

         .then(literal("cancel").executes((runner) -> {
            Update acknowledgment = Minecraft.getInstance().player.connection.lastSeenMessages.generateAndApplyUpdate().update();
            Minecraft.getInstance().player.connection.send(new ServerboundChatCommandPacket("backup cancel", Instant.now(), 0, 
                ArgumentSignatures.EMPTY, acknowledgment));
            return 1;
         }))

         .then(literal("reload-client-config").executes((runner) -> {
            CoreCommandSystem.reloadClientConfig((response) -> {
                runner.getSource().sendSuccess(() -> {
                    return Component.literal(response);
                }, true);
            });
            return 1;
         }))
    
        );
    }

    //I'm no fan of having this here but it seems required
    public static LiteralArgumentBuilder<CommandSourceStack> literal(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }


}
