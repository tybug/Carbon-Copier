package com.tybug.carboncopier;

import java.time.OffsetDateTime;
import java.util.List;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Message.Attachment;

public class MessageInfo {
	
	private OffsetDateTime editedTime;
	private String profileURL;
	private String username;
	private String content;
	private List<Attachment> attachments;
	private List<MessageEmbed> embeds;
	private OffsetDateTime timestamp;
	private String messageID;
	private String channelID;
	private String guildID;
	
	public OffsetDateTime getEditedTime() {
		return editedTime;
	}
	public String getUsername() {
		return username;
	}
	public String getProfileURL() {
		return profileURL;
	}
	public String getContent() {
		return content;
	}
	public List<Attachment> getAttachments() {
		return attachments;
	}
	public List<MessageEmbed> getEmbeds() {
		return embeds;
	}
	public OffsetDateTime getTimestamp() {
		return timestamp;
	}
	public String getMessageID() {
		return messageID;
	}
	public String getChannelID() {
		return channelID;
	}
	public String getGuildID() {
		return guildID;
	}
	
	public void setEditedTime(OffsetDateTime editedTime) {
		this.editedTime = editedTime;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public void setProfileURL(String profileURL) {
		this.profileURL = profileURL;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}
	public void setEmbeds(List<MessageEmbed> embeds) {
		this.embeds = embeds;
	}
	public void setTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
	}
	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}
	public void setChannelID(String channelID) {
		this.channelID = channelID;
	}
	public void setGuildID(String guildID) {
		this.guildID = guildID;
	}
}
