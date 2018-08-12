package com.tybug.carboncopier.listeners;


import com.tybug.carboncopier.Hub;
import com.tybug.carboncopier.MessageInfo;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageType;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		
		if(!Hub.isSourceGuild(event.getGuild().getId())) {
			return;
		}
		
		JDA jda = event.getJDA();
		User author = event.getAuthor();
		Message message = event.getMessage();
		MessageInfo info = new MessageInfo();
		info.setProfileURL(author.getEffectiveAvatarUrl());
		info.setUsername(author.getName());
		info.setContent(message.getContentRaw());
		info.setAttachments(message.getAttachments());
		info.setEmbeds(message.getEmbeds());
		info.setTimestamp(message.getCreationTime());
		info.setMessageID(message.getId());
		info.setChannelID(message.getChannel().getId());
		info.setGuildID(event.getGuild().getId());
		
		if(message.getType().equals(MessageType.GUILD_MEMBER_JOIN)) {
			info.setContent(author.getAsMention() + " joined the guild!");
		}
		
        Hub.sendMessage(jda, info);
    }
	
	
	@Override
	public void onMessageUpdate(MessageUpdateEvent event) {
		if(!Hub.isSourceGuild(event.getGuild().getId())) {
			return;
		}
		
		JDA jda = event.getJDA();
		Message message = event.getMessage();
		MessageInfo info = new MessageInfo();
		info.setMessageID(message.getId());
		info.setChannelID(message.getChannel().getId());
		info.setEditedTime(message.getEditedTime());
		info.setContent(message.getContentRaw());
		info.setUsername(event.getAuthor().getName()); //for debugging
        Hub.editMessage(jda, info);
	}
	
	
	@Override
	public void onMessageDelete(MessageDeleteEvent event) {
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
