package com.tybug.carboncopier.listeners;

import java.time.OffsetDateTime;
import java.util.List;

import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		JDA jda = event.getJDA();
		User author = event.getAuthor();
		Message message = event.getMessage();
		
		String profileURL = author.getEffectiveAvatarUrl();
		String username = author.getName();
		String content = message.getContentRaw();
		List<Attachment> attachments = message.getAttachments();
		OffsetDateTime timestamp = message.getCreationTime();
		String channelID = event.getChannel().getId();
		
        Hub.sendMessage(jda, profileURL, username, content, attachments, timestamp, channelID);
    }
	
}
