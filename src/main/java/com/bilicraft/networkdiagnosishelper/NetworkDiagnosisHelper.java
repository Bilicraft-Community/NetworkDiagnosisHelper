package com.bilicraft.networkdiagnosishelper;

import com.google.gson.Gson;
import lombok.Getter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class NetworkDiagnosisHelper extends Plugin implements Listener {
    @Getter
    private final Set<ProxiedPlayer> modInstalledPlayers =new CopyOnWriteArraySet<>();
    private final Gson gson = new Gson();
    private final CommandContainer commandContainer = CommandContainer.builder()
            .host("mc.bilicraft.com")
            .host("106.55.75.235")
            .host("s4.e7mc.com")
            .host("s2.e7mc.com")
            .ping(true)
            .traceroute(true)
            .dnsLookup(true)
            .checkReachable(true)
            .netCard(true)
            .type("test")
            .build();


    private final Thread thread = new Thread("Ping Scan Thread") {
        @Override
        public void run() {
            while (true){
                getProxy().getPlayers().forEach(player->{
                    if (player.getPing() > 300){
                        sendTestRequest(player,commandContainer);
                        if(!modInstalledPlayers.contains(player)){
                            getLogger().info("玩家 "+player.getName()+" 可能没有安装网络辅助工具，但仍然尝试进行测试中...");
                        }
                    }
                });
                try {
                    Thread.sleep(1000*60*3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onEnable() {
        // Plugin startup logic
        getDataFolder().mkdirs();
        getProxy().registerChannel("networkdiagnosis:command");
        getProxy().getPluginManager().registerListener(this, this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getProxy().unregisterChannel("networkdiagnosis:command");

        thread.stop();
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
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        try {
            if (event.getTag().equals("minecraft:register")) {
                String registering = new String(event.getData(), StandardCharsets.UTF_8);
                if (registering.contains("networkdiagnosis:command")) {
                    getLogger().info("Player " + player.getName() + " have NetworkDiagnosis mod, feature enabled.");
                    this.modInstalledPlayers.add(player);
                }
            }
            if (event.getTag().equals("networkdiagnosis:command")) {
                ResponseContainer responseContainer = gson.fromJson(new String(event.getData(), StandardCharsets.UTF_8), ResponseContainer.class);
                processResponse(player, responseContainer);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void processResponse(ProxiedPlayer player, ResponseContainer responseContainer) {
        if(!responseContainer.getType().equals("test")){
            getLogger().warning("未知响应"+ responseContainer);
            return;
        }
        File file = new File(getDataFolder(), player.getName()+System.currentTimeMillis()+".log");
        try {
            file.createNewFile();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        try(PrintWriter writer = new PrintWriter(new FileWriter(file),true)){
            writer.println("Date: "+ new Date());
            writer.println("Ping: "+ responseContainer.getClientConnectedPing());
            writer.println("Client Connect Address: "+ responseContainer.getClientConnectedAddress());
            writer.println("NetCard: "+ responseContainer.getNetCard());
            writer.println("Hosts: ");
            responseContainer.getResponses().forEach(hostResponse -> {
                writer.println("  Host: "+hostResponse.getHost());
                writer.println("    Ping: \n"+hostResponse.getPing());
                writer.println("    NsLookup: \n"+hostResponse.getDnsLookup());
                writer.println("    Traceroute: \n"+hostResponse.getTraceroute());
            });
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        getLogger().info("数据跟踪完成: "+player.getName());
    }

    public void sendTestRequest(ProxiedPlayer player, CommandContainer commandContainer) {
        player.sendData("networkdiagnosis:command", gson.toJson(commandContainer).getBytes(StandardCharsets.UTF_8));
    }
}
