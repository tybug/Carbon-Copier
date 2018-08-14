package com.tybug.carboncopier.listeners;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tybug.carboncopier.Hub;
import com.tybug.carboncopier.MessageInfo;
import com.tybug.carboncopier.Utils;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageType;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Listens for events related to Messages
 * <p>
 * This includes MessageReceived events and Reaction events
 * @author Liam DeVoe
 *
 */
public class MessageListener extends ListenerAdapter {
	final static Logger LOG = LoggerFactory.getLogger(Hub.class);

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		LOG.debug("Guild message received in guild {} from user {}", event.getGuild().getName(), event.getAuthor().getName());
		if(!Hub.isSourceGuild(event.getGuild().getId())) {
			return;
		}
		
		JDA jda = event.getJDA();
		User author = event.getAuthor();
		Message message = event.getMessage();
		MessageInfo info = Utils.createMessageInfo(message);
		
		if(message.getType().equals(MessageType.GUILD_MEMBER_JOIN)) {
			info.setContent(author.getAsMention() + " joined the guild!");
		}
		
        Hub.sendMessage(jda, info);
    }
	
	
	@Override
	public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
		if(!Hub.isSourceGuild(event.getGuild().getId())) {
			return;
		}
		
		JDA jda = event.getJDA();
		Message message = event.getMessage();
		MessageInfo info = Utils.createMessageInfo(message);
        Hub.editMessage(jda, info);
	}
	
	
	@Override
	public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
		if(!Hub.isSourceGuild(event.getGuild().getId())) {
			return;
		}
		
		JDA jda = event.getJDA();
		String messageID = event.getMessageId();
		String channelID = event.getChannel().getId();
		
		Hub.deleteMessage(jda, messageID, channelID);
	}
	

	
	@Override 
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		if(!Hub.isSourceGuild(event.getGuild().getId())) {
			return;
		}
		JDA jda = event.getJDA();
		String messageID = event.getMessageId();
		String channelID = event.getChannel().getId();
		
        Hub.updateReactions(jda, messageID, channelID);
	}
	
	
	
	@Override 
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
		if(!Hub.isSourceGuild(event.getGuild().getId())) {
			return;
		}
		JDA jda = event.getJDA();
		String messageID = event.getMessageId();
		String channelID = event.getChannel().getId();
		
        Hub.updateReactions(jda, messageID, channelID);
	}
	
}
