package com.tybug.carboncopier.listeners;

import java.time.OffsetDateTime;
import java.util.List;

import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.MessageEmbed;
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
		
		String profileURL = author.getEffectiveAvatarUrl();
		String username = author.getName();
		String content = message.getContentRaw();
		List<Attachment> attachments = message.getAttachments();
		List<MessageEmbed> embeds = message.getEmbeds();
		OffsetDateTime timestamp = message.getCreationTime();
		String messageID = message.getId();
		String channelID = event.getChannel().getId();
		String guildID = event.getGuild().getId();
		
		if(message.getType().equals(MessageType.GUILD_MEMBER_JOIN)) {
			content = author.getAsMention() + " joined the guild!";
		}
		
        Hub.sendMessage(jda, profileURL, username, content, attachments, embeds, timestamp, messageID, channelID, guildID);
    }
	
	
	@Override
	public void onMessageUpdate(MessageUpdateEvent event) {
		if(!Hub.isSourceGuild(event.getGuild().getId())) {
			return;
		}
		
		JDA jda = event.getJDA();
		String messageID = event.getMessage().getId();
		String channelID = event.getChannel().getId();
		
		
        Hub.editMessage(jda, messageID, channelID);
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
		String userID = event.getUser().getId();
		String channelID = event.getChannel().getId();
		
        Hub.updateReactions(jda, messageID, userID, channelID);
	}
	
	
	
	@Override 
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
		if(!Hub.isSourceGuild(event.getGuild().getId())) {
			return;
		}
		JDA jda = event.getJDA();
		String messageID = event.getMessageId();
		String userID = event.getUser().getId();
		String channelID = event.getChannel().getId();
		
        Hub.updateReactions(jda, messageID, userID, channelID);
	}
	
}
