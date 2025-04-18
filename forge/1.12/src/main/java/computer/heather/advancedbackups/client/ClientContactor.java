package computer.heather.advancedbackups.client;

import java.util.List;

import computer.heather.advancedbackups.AdvancedBackups;
import computer.heather.advancedbackups.interfaces.IClientContactor;
import computer.heather.advancedbackups.network.NetworkHandler;
import computer.heather.advancedbackups.network.PacketBackupStatus;
import net.minecraft.entity.player.EntityPlayerMP;

public class ClientContactor implements IClientContactor {

    @Override
    public void backupComplete(boolean all) {
        List<EntityPlayerMP> players = AdvancedBackups.server.getPlayerList().getPlayers();
        PacketBackupStatus packet = new PacketBackupStatus(false, false, false, true, false, 0, 0);
        for (EntityPlayerMP player : players) {
            if (!AdvancedBackups.players.contains(player.getGameProfile().getId().toString())) continue;
            if (!AdvancedBackups.server.isDedicatedServer() || player.canUseCommand(3, "backup") || all) {
                NetworkHandler.HANDLER.sendTo(packet, player);
            }
        }
    }

    @Override
    public void backupFailed(boolean all) {
        List<EntityPlayerMP> players = AdvancedBackups.server.getPlayerList().getPlayers();
        PacketBackupStatus packet = new PacketBackupStatus(false, false, true, false, false, 0, 0);
        for (EntityPlayerMP player : players) {
            if (!AdvancedBackups.players.contains(player.getGameProfile().getId().toString())) continue;
            if (!AdvancedBackups.server.isDedicatedServer() || player.canUseCommand(3, "backup") || all) {
                NetworkHandler.HANDLER.sendTo(packet, player);
            }
        }
    }

    @Override
    public void backupProgress(int progress, int max, boolean all) {
        List<EntityPlayerMP> players = AdvancedBackups.server.getPlayerList().getPlayers();
        PacketBackupStatus packet = new PacketBackupStatus(false, true, false, false, false, progress, max);
        for (EntityPlayerMP player : players) {
            if (!AdvancedBackups.players.contains(player.getGameProfile().getId().toString())) continue;
            if (!AdvancedBackups.server.isDedicatedServer() || player.canUseCommand(3, "backup") || all) {
                NetworkHandler.HANDLER.sendTo(packet, player);
            }
        }
    }

    @Override
    public void backupStarting(boolean all) {
        List<EntityPlayerMP> players = AdvancedBackups.server.getPlayerList().getPlayers();
        PacketBackupStatus packet = new PacketBackupStatus(true, false, false, false, false, 0, 0);
        for (EntityPlayerMP player : players) {
            if (!AdvancedBackups.players.contains(player.getGameProfile().getId().toString())) continue;
            if (!AdvancedBackups.server.isDedicatedServer() || player.canUseCommand(3, "backup") || all) {
                NetworkHandler.HANDLER.sendTo(packet, player);
            }
        }
    }

    @Override
    public void backupCancelled(boolean all) {
        List<EntityPlayerMP> players = AdvancedBackups.server.getPlayerList().getPlayers();
        PacketBackupStatus packet = new PacketBackupStatus(false, false, false, false, true, 0, 0);
        for (EntityPlayerMP player : players) {
            if (!AdvancedBackups.players.contains(player.getGameProfile().getId().toString())) continue;
            if (!AdvancedBackups.server.isDedicatedServer() || player.canUseCommand(3, "backup") || all) {
                NetworkHandler.HANDLER.sendTo(packet, player);
            }
        }
    }
    
}
