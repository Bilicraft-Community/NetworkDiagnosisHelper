package com.bilicraft.networkdiagnosishelper;

import com.google.gson.Gson;
import lombok.Getter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class NetworkDiagnosisHelper extends Plugin implements Listener {
    @Getter
    private final Set<ProxiedPlayer> modInstalledPlayers = new HashSet<>();
    private final Gson gson = new Gson();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getProxy().registerChannel("networkdiagnosis:command");
        getProxy().getPluginManager().registerListener(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getProxy().unregisterChannel("networkdiagnosis:command");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDisconnect(PlayerDisconnectEvent event) {
        this.modInstalledPlayers.remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMessage(PluginMessageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        try {
            if (event.getTag().equals("minecraft:register")) {
                String registering = new String(event.getData(), StandardCharsets.UTF_8);
                if (event.getSender() instanceof ProxiedPlayer) {
                    if (registering.contains("networkdiagnosis:command")) {
                        getLogger().info("Player " + ((ProxiedPlayer) event.getSender()).getName() + " have NetworkDiagnosis mod, feature enabled.");
                        this.modInstalledPlayers.add((ProxiedPlayer) event.getSender());
                    }else{
                        getLogger().info("Player " + ((ProxiedPlayer) event.getSender()).getName() + " had no NetworkDiagnosis mod installed, feature disabled.");
                    }
                }
            }
            if (event.getTag().equals("networkdiagnosis:command")) {
                ResponseContainer responseContainer = gson.fromJson(new String(event.getData(), StandardCharsets.UTF_8), ResponseContainer.class);
                processResponse(responseContainer);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void processResponse(ResponseContainer responseContainer) {

    }

    public void sendTestRequest(ProxiedPlayer player, CommandContainer commandContainer) {
        player.sendData("networkdiagnosis:command", gson.toJson(commandContainer).getBytes(StandardCharsets.UTF_8));
    }
}
