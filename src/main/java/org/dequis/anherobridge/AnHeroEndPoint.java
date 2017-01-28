package org.dequis.anherobridge;

import java.util.List;
import java.util.LinkedList;

import org.bukkit.plugin.Plugin;

import com.dthielke.api.Channel;
import com.dthielke.api.Chatter;
import com.dthielke.ChatterManager;
import com.dthielke.Herochat;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.ensifera.animosity.craftirc.EndPoint;
import com.ensifera.animosity.craftirc.RelayedMessage;

public class AnHeroEndPoint implements EndPoint {

    private CraftIRC craftirc;
    public String herotag;
    public String irctag;
    private List<String> playerCommandAliases;

    private Channel herochatChannel;
    private ChatterManager chatterManager;

    public AnHeroEndPoint(CraftIRC craftirc, String herotag, String irctag, List<String> playerCommandAliases) {
        this.craftirc = craftirc;
        this.herotag = herotag;
        this.irctag = irctag;
        this.playerCommandAliases = playerCommandAliases;

        this.herochatChannel = Herochat.getChannelManager().getChannel(herotag);
        this.chatterManager = Herochat.getChatterManager();
    }

    public boolean register() {
        boolean success = this.craftirc.registerEndPoint(this.irctag, this);
        for (final String cmd : this.playerCommandAliases) {
            // if the user specifies names for our players command, they get priority
            // so unregister the built-in ones (it returns false silently on failure)
            this.craftirc.unregisterCommand(cmd);
        }
        return success;
    }

    public boolean unregister() {
        // TODO: should i reregister removed craftirc commands?
        // This function should never get called except before a reload
        return this.craftirc.unregisterEndPoint(this.irctag);
    }

    @Override
    public Type getType() {
        return EndPoint.Type.MINECRAFT;
    }

    @Override
    public void messageIn(RelayedMessage msg) {
        // CraftIRC3 sucks at command extensibility by design
        // Let's handle stuff our own way here
        final String cmdPrefix = this.craftirc.cCommandPrefix(0); // can't even get the bot id
        final String rawMessage = msg.getField("message");

        if (msg.getEvent() == "chat" && rawMessage.startsWith(cmdPrefix)) {
            int firstSpace = rawMessage.contains(" ") ? rawMessage.indexOf(" ") : rawMessage.length();
            final String commandName = rawMessage.substring(cmdPrefix.length(), firstSpace);
            if (playerCommandAliases.contains(commandName)) {
                this.sendPlayersResponse(msg.getField("source"));
                return;
            }
        }

        // Everything else
        this.herochatChannel.announce(msg.getMessage(this));
    }

    @Override
    public boolean userMessageIn(String username, RelayedMessage msg) {
        return false;
    }

    @Override
    public boolean adminMessageIn(RelayedMessage msg) {
        return false;
    }

    @Override
    public List<String> listUsers() {
        final LinkedList<String> users = new LinkedList<String>();
        for (Chatter c : this.chatterManager.getChatters()) {
            if (c.hasChannel(this.herochatChannel)) {
                users.add(c.getName());
            }
        }
        return users;
    }

    @Override
    public List<String> listDisplayUsers() {
        return this.listUsers();
    }

    private void sendPlayersResponse(String source) {
        final List<String> users = this.listUsers();
        final int playerCount = users.size();
        String result;
        if (playerCount > 0) {
            final StringBuilder builder = new StringBuilder();
            builder.append(String.format("Players in channel %s (%d): ", this.herotag, playerCount));
            for (String user : users) {
                builder.append(user).append(" ");
            }
            builder.setLength(builder.length() - 1);
            result = builder.toString();
        } else {
            result = "Nobody is in " + this.herotag + " right now.";
        }

        final RelayedMessage response = this.craftirc.newMsgToTag(this, source, "");
        response.setField("message", result);
        response.post();
    }
}
