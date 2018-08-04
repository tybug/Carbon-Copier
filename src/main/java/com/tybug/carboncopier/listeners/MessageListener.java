package com.tybug.carboncopier.listeners;

import java.time.OffsetDateTime;
import java.util.List;

import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(Hub.isTargetGuild(event.getGuild().getId())) {
			return;
		}
		Guild guild = event.getGuild();
		User author = event.getAuthor();
		Message message = event.getMessage();
		
		String profileURL = author.getEffectiveAvatarUrl();
		String username = author.getName();
		String content = message.getContentRaw();
		List<Attachment> attachments = message.getAttachments();
		OffsetDateTime timestamp = message.getCreationTime();
		String messageID = message.getId();
		String channelID = event.getChannel().getId();
		
        Hub.sendMessage(guild, profileURL, username, content, attachments, timestamp, messageID, channelID);
    }
	
	@Override
	public void onMessageUpdate(MessageUpdateEvent event) {
		if(Hub.isTargetGuild(event.getGuild().getId())) {
			return;
		}
		
		Guild guild = event.getGuild();
		String messageID = event.getMessage().getId();
		String channelID = event.getChannel().getId();
		
		
        Hub.editMessage(guild, messageID, channelID);
	}
	
}
