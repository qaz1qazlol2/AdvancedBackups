package computer.heather.advancedbackups.network;

import java.io.IOException;

import computer.heather.advancedbackups.core.ABCore;
import computer.heather.advancedbackups.core.CoreCommandSystem;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

//I hate this.
public class PacketClientReload implements IMessage{
    
    
    public PacketClientReload(boolean enable) {
        
    }

    public PacketClientReload () { 

    }

    @Override
    public void fromBytes(ByteBuf buf) {
        
    }

    @Override
    public void toBytes(ByteBuf buf) {
        
    }


    public static class Handler implements IMessageHandler<PacketClientReload, IMessage> {

        @Override
        public IMessage onMessage(PacketClientReload message, MessageContext ctx) {

            try {
                CoreCommandSystem.reloadClientConfig((response) -> Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(response)));
            } catch (IOException e) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Command failed to execute! Check log for error"));
                ABCore.errorLogger.accept("Error reloading config :");
                ABCore.logStackTrace(e);
            }
            return null;

        }
        
    }
    
}
