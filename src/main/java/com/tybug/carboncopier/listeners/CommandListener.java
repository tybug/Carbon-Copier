package com.tybug.carboncopier.listeners;

import com.tybug.carboncopier.DBFunctions;
import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

	
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if(!DBFunctions.getAuthorizedUsers().contains(event.getAuthor().getId())) {
			return;
		}
		
		
		String content = event.getMessage().getContentRaw();
		if(content.equals("!link")) {
			String[] parts = content.split(" ");
			if(parts.length != 3) {
				event.getChannel().sendMessage("!link [source] [target]").queue();
				return;
			}
			
			DBFunctions.linkGuild(parts[1], parts[2]);
			Guild source = event.getJDA().getGuildById(parts[1]);
			Guild target = event.getJDA().getGuildById(parts[2]);
			
			event.getChannel().sendMessage("Linked `" + source.getName() +"` to `" + target.getName() + "`").queue();
			Hub.updateLinkedGuilds(); // Update our cache
		}
		
		
		
	}
}
