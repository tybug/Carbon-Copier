package com.tybug.carboncopier.listeners;

import com.tybug.carboncopier.DBFunctions;
import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

	
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if(!DBFunctions.getAuthorizedUsers().contains(event.getAuthor().getId())) {
			return;
		}
		
		
		String content = event.getMessage().getContentRaw();
		if(content.startsWith("!link")) {
			String[] parts = content.split(" ");
			if(parts.length != 3) {
				event.getChannel().sendMessage("!link [source] [target]").queue();
				return;
			}
			
			Hub.linkGuilds(event.getJDA(), event.getChannel(), parts[1], parts[2]);
		}
	}
	
}
