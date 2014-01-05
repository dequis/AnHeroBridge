package org.dequis.anherobridge;

import java.util.Random;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginEnableEvent;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.ensifera.animosity.craftirc.EndPoint;
import com.ensifera.animosity.craftirc.RelayedMessage;

import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Chatter.Result;
import com.dthielke.herochat.Channel;

public class AnHeroBridge extends JavaPlugin implements Listener {

    private static final String BRIDGES_NODE = "AnHeroBridge.bridges";
    private static final String SETTINGS_NODE = "AnHeroBridge.settings";
    private static final String UIRCBRIDGE_MESSAGE = "You have uIRCBridge. I don't know why you would want to use both at the same time.";
    private static final String BRIDGES_MESSAGE = "You didn't define any bridges.";
    private static final String INVALID_BRIDGES_MESSAGE = "None of the bridges you defined were valid.";
    private static final String AUTOPATHS_MESSAGE = "You have autopaths enabled in the craftirc config.\n" +
        "Why would you do this? Have you not read the docs that tells you to disable it?\n";

    // irc tag to endpoint
    private final HashMap<String, AnHeroEndPoint> bridges = new HashMap<String, AnHeroEndPoint>();
    // herochat tag to irc tag (this reverse mapping looks asymmetrical...)
    private final HashMap<String, String> heroTagMap = new HashMap<String, String>();

    private CraftIRC craftirc;

    // still need to listen for plugin enable events
    private boolean kindaDisabled = false;

    public void onEnable() { 
        final PluginManager pm = this.getServer().getPluginManager();

        if (pm.isPluginEnabled("uIRCBridge")) {
            this.getLogger().warning(UIRCBRIDGE_MESSAGE);
        }

        this.craftirc = (CraftIRC) pm.getPlugin("CraftIRC");

        try {
            if (this.craftirc.cAutoPaths()) {
                throw new DerpException(AUTOPATHS_MESSAGE);
            }

            this.saveDefaultConfig();

            if (!this.getConfig().isConfigurationSection(BRIDGES_NODE)) {
                throw new DerpException(BRIDGES_MESSAGE);
            }

            boolean shitHappened = false;

            for (String irctag : this.getConfig().getConfigurationSection(BRIDGES_NODE).getKeys(false)) {
                String herotag = this.getConfig().getString(BRIDGES_NODE + "." + irctag);
                List<String> playerCommandAliases = this.getConfig().getStringList(SETTINGS_NODE + ".players-irc-commands");
                AnHeroEndPoint endpoint = new AnHeroEndPoint(this.craftirc, herotag, irctag, playerCommandAliases);
                if (!endpoint.register()) {
                    this.getLogger().warning("Couldn't register craftirc tag " + irctag + " for herochat channel " + herotag);
                    shitHappened = true;
                } else {
                    this.bridges.put(irctag, endpoint);
                    this.heroTagMap.put(herotag, irctag);
                }
            }

            if (this.bridges.isEmpty()) {
                throw new DerpException(shitHappened ? INVALID_BRIDGES_MESSAGE : BRIDGES_MESSAGE);
            }

            this.kindaDisabled = false;
            this.getLogger().info("Registered tags for: " + this.heroTagMap.toString());
            this.getLogger().info("Enabled");

        } catch (DerpException e) {
            this.getLogger().severe(e.getMessage());
            this.nukeEverything();
            this.getLogger().severe("Disabled"); // not true, but shhh
        }

        // Init may have failed, but I need the PluginEnableEvent at least
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    private void nukeEverything() {
        for (AnHeroEndPoint endpoint : this.bridges.values()) {
            endpoint.unregister();
        }
        this.bridges.clear();
        this.kindaDisabled = true;
    }

    public void onDisable() {
        this.nukeEverything();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        String name = event.getPlugin().getDescription().getName();
        if (name.equals("CraftIRC") || name.equals("Herochat")) {
            this.reloadConfig();
            final PluginManager pm = this.getServer().getPluginManager();
            pm.disablePlugin(this);
            pm.enablePlugin(this);
        }
        if (!this.kindaDisabled && name.equals("uIRCBridge")) {
            this.getLogger().warning(UIRCBRIDGE_MESSAGE);
        }
    }

    @EventHandler
    public void onHeroChannelChat(ChannelChatEvent e) {
        if (this.kindaDisabled || e.getResult() != Result.ALLOWED) {
            return;
        }

        String channelName = e.getChannel().getName();
        if (!this.heroTagMap.containsKey(channelName)) {
            return;
        }
        AnHeroEndPoint endpoint = this.bridges.get(this.heroTagMap.get(channelName));

        RelayedMessage msg = this.craftirc.newMsg(endpoint, null, "chat");
        if (msg == null) {
            return;
        }
        
        Player player = e.getSender().getPlayer();
        msg.setField("sender", player.getDisplayName());
        msg.setField("message", e.getMessage());
        msg.setField("world", player.getWorld().getName());
        msg.setField("realSender", player.getName());
        msg.setField("prefix", this.craftirc.getPrefix(player));
        msg.setField("suffix", this.craftirc.getSuffix(player));
        msg.setField("channelName", e.getChannel().getName());
        msg.setField("channelNick", e.getChannel().getNick());
        msg.doNotColor("message");
        msg.post();
    }

    // Handle cases of user derp.
    @SuppressWarnings("serial")
    private static class DerpException extends Exception {
        static final String[] messages = {
            "I'm getting out of here. Have fun.",
            "This plugin is going to be REALLY useful now. Good job.",
            "I don't think this is going to work...",
            "This is not how you're supposed to do it.",
        };

        static final Random random = new Random();

        public DerpException(String prefix) {
            super((prefix != null ? (prefix + " ") : "") + messages[random.nextInt(messages.length)]);
        }
        public DerpException() {
            this((String) null);
        }
    }
}
