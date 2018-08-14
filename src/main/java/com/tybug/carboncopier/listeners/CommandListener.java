package com.tybug.carboncopier.listeners;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tybug.carboncopier.DBFunctions;
import com.tybug.carboncopier.Hub;
import com.tybug.carboncopier.Utils;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.HierarchyException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Listens for events (Currently only MessageReceived events), but not to copy them - to enact commands.
 * @author Liam DeVoe
 *
 */
public class CommandListener extends ListenerAdapter {
	final static Logger LOG = LoggerFactory.getLogger(Hub.class);

	private static final String SCRIPT_RESTART = "./compiler.sh"; 
	private static final String GITHUB_CC = "477476287540232202"; // Channel the github webhook is linked to for the Carbon Copier repo

	private static final String EMOJI_ACCEPT = "\u2705";
	private static final String EMOJI_DENY = "\u274C";

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


		if(content.startsWith("!test")) {

		}


		if(content.startsWith("!link")) {
			LOG.info("Link command executed by {}", event.getAuthor().getName());
			String[] parts = content.split(" ");
			if(parts.length != 3) {
				LOG.debug("Incorrect command format! parts contained: {}", String.join(", ", parts));
				event.getChannel().sendMessage("!link [source] [target]").queue();
				return;
			}

			Message m = event.getChannel().sendMessage("Would you like to copy previous message history from the guild?").complete();
			m.addReaction(EMOJI_ACCEPT).queue();
			m.addReaction(EMOJI_DENY).queue();

			String messageID = m.getId();
			Utils.waiter.waitForEvent(MessageReactionAddEvent.class, 
					e -> e.getMessageId().equals(messageID) && // Only trigger if it's the accept or deny reactions
					!e.getUser().isBot() &&
					(e.getReaction().getReactionEmote().getName().equals(EMOJI_ACCEPT) || e.getReaction().getReactionEmote().getName().equals(EMOJI_DENY)), 
					e -> {
						if(e.getReaction().getReactionEmote().getName().equals(EMOJI_ACCEPT)) { 
							event.getChannel().sendMessage("Linking guilds and copying history").queue();
							Hub.linkGuilds(event.getJDA(), event.getChannel(), parts[1], parts[2], true);
						} else if(e.getReaction().getReactionEmote().getName().equals(EMOJI_DENY)) {
							event.getChannel().sendMessage("Linking guilds without copying history").queue();
							Hub.linkGuilds(event.getJDA(), event.getChannel(), parts[1], parts[2], false);

						}
					},
					-1, null, null);
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
