package com.tybug.carboncopier.listeners;

import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ReactionListener extends ListenerAdapter {
	
	
	@Override 
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		if(Hub.isTargetGuild(event.getGuild().getId())) {
			return;
		}
		JDA jda = event.getJDA();
		String messageID = event.getMessageId();
		String userID = event.getUser().getId();
		String channelID = event.getChannel().getId();
		
        Hub.updateReactions(jda, messageID, userID, channelID);
	}
}
