package com.tybug.carboncopier.listeners;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	final static Logger LOG = LoggerFactory.getLogger(Hub.class);

	private static final String SCRIPT_RESTART = "./compiler.sh"; 
	private static final String GITHUB_CC = "477476287540232202"; // Channel the github webhook is linked to for the Carbon Copier repo

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if(event.getChannel().getId().equals(GITHUB_CC)) {
			LOG.info("Recieved github webhook...restarting");
			restart(event.getJDA());
		}
	}


	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		LOG.debug("Private Message received from {}", event.getAuthor().getName());

		if(!DBFunctions.getAuthorizedUsers().contains(event.getAuthor().getId())) {
			LOG.debug("{} was not an authorized user", event.getAuthor().getName());
			return;
		}


		String content = event.getMessage().getContentRaw();

		if(content.startsWith("!link")) {
			LOG.info("Link command executed by {}", event.getAuthor().getName());
			String[] parts = content.split(" ");
			if(parts.length != 3) {
				LOG.debug("Incorrect command format! parts contained: {}", String.join(", ", parts));
				event.getChannel().sendMessage("!link [source] [target]").queue();
				return;
			}

			LOG.debug("Linking guild {} to {}", parts[1], parts[2]);
			Hub.linkGuilds(event.getJDA(), event.getChannel(), parts[1], parts[2]);
		}


		if(content.startsWith("!purge")) {
			LOG.info("Purge command executed by {}", event.getAuthor().getName());
			//Quick and dirty, reset a guild for testing
			Guild guild = event.getJDA().getGuildById(content.split(" ")[1]);

			LOG.debug("Deleting roles in {}", guild.getName());
			for(Role r : guild.getRoles()) {
				LOG.trace("Deleting role {}", r.getName());

				try {
					r.delete().queue();
				} catch (HierarchyException e) { 
					LOG.trace("Hierarchy exception while deleting role {}", r.getName());
					// Just leave the highest role, can be deleted manually (plus he needs it to delete everything else)
				}
			}

			LOG.debug("Deleting text channels in {}", guild.getName());
			guild.getTextChannels().forEach(channel -> channel.delete().queue());
			LOG.debug("Deleting voice channels in {}", guild.getName());
			guild.getVoiceChannels().forEach(channel -> channel.delete().queue());
			LOG.debug("Deleting categories in {}", guild.getName());
			guild.getCategories().forEach(channel -> channel.delete().queue());

		}
	}


	

	private static void restart(JDA jda){
		try {
			LOG.debug("Executing pull/compile script");
			Runtime.getRuntime().exec(SCRIPT_RESTART);
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOG.debug("Shutting down");
		jda.shutdown();

	}

}
