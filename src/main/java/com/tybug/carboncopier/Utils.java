package com.tybug.carboncopier;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

/**
 * Information declared once but used throughout, or helper methods
 * @author Liam DeVoe
 *
 */
public class Utils {
	public static final EventWaiter waiter = new EventWaiter();
	
	
	public static MessageInfo createMessageInfo(Message m) {
		
		User author = m.getAuthor();
		MessageInfo info = new MessageInfo();
		
		info.setProfileURL(author.getEffectiveAvatarUrl());
		info.setUsername(author.getName());
		info.setContent(m.getContentRaw());
		info.setAttachments(m.getAttachments());
		info.setEmbeds(m.getEmbeds());
		info.setTimestamp(m.getCreationTime());
		info.setMessageID(m.getId());
		info.setChannelID(m.getChannel().getId());
		info.setGuildID(m.getGuild().getId());
		
		return info;
	}
}
