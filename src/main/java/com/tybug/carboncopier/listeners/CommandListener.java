package com.tybug.carboncopier.listeners;

import java.io.IOException;

import com.tybug.carboncopier.DBFunctions;
import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.HierarchyException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

	private static final String SCRIPT_RESTART = "./compiler.sh"; 
	private static final String GITHUB_CC = "477476287540232202"; // Channel the github webhook is linked to for the Carbon Copier repo
	private static final String LOG = "477263076706484230";

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if(event.getChannel().getId().equals(GITHUB_CC)) {
			event.getGuild().getTextChannelById(LOG).sendMessage("Received push webhook; restarting").queue();
			restart(event.getJDA());
		}
	}


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


		if(content.startsWith("!purge")) {
			//Quick and dirty, reset a guild for testing
			Guild guild = event.getJDA().getGuildById(content.split(" ")[1]);


			for(Role r : guild.getRoles()) {
				try {
					r.delete().queue();
				} catch (HierarchyException e) { 
					// Just leave the highest role, can be deleted manually (plus he needs it to delete everything else)
				}
			}

			guild.getTextChannels().forEach(channel -> channel.delete().queue());
			guild.getVoiceChannels().forEach(channel -> channel.delete().queue());
			guild.getCategories().forEach(channel -> channel.delete().queue());

		}
	}


	

	private static void restart(JDA jda){
		try {
			Runtime.getRuntime().exec(SCRIPT_RESTART);
		} catch (IOException e) {
			e.printStackTrace();
		}

		jda.shutdown();

	}

}
