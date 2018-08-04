package com.tybug.carboncopier;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class Hub {


	private static List<String> targetGuilds = null;
	private static HashMap<String, String> linkedChannels = null;


	private static final String TEMP_URL = "https://gmail.com";
	private static final Color COLOR_MESSAGE = Color.decode("42f450");
	private static final Color COLOR_REACT = Color.decode("f9c131");
	private static final Color COLOR_EDIT = Color.decode("f3f718");
	private static final Color COLOR_DELETE = Color.decode("ff2a00");

	public static void setup() {
		linkedChannels = DBFunctions.getLinkedChannels();
		targetGuilds = DBFunctions.getTargetGuilds();
	}


	public static void sendMessage(JDA jda, String profileURL, String username, String content, 
			List<Attachment> attachments, OffsetDateTime timestamp, String messageID, String channelID, String guildID) {

		Guild guild = jda.getGuildById(guildID);
		TextChannel channel = jda.getTextChannelById(linkedChannels.get(channelID));



		EmbedBuilder eb = new EmbedBuilder();
		eb.setAuthor(username, TEMP_URL, profileURL);
		eb.setDescription(content);

		String time = parseTime(timestamp);
		eb.setFooter("Blair Discord • " + time, guild.getIconUrl());

		if(attachments.size() == 1) {
			eb.setImage(attachments.get(0).getUrl());
		} 

		else if(attachments.size() > 1) {
			jda.getUserById("216008405758771200").openPrivateChannel().complete()
			.sendMessage("wtf someone sent more than one attachment in a message fucking fix it pls").queue();
		} 
		
		eb.setColor(COLOR_MESSAGE);

		String target = channel.sendMessage(eb.build()).complete().getId();





		DBFunctions.linkMessage(messageID, target);

	}


	public static void editMessage(JDA jda, String messageID, String channelID) {

		TextChannel sourceChannel = jda.getTextChannelById(channelID);
		TextChannel targetChannel = jda.getTextChannelById(linkedChannels.get(channelID));

		EmbedBuilder eb = new EmbedBuilder();
		Message targetMessage = targetChannel.getMessageById(DBFunctions.getLinkedMessage(messageID)).complete();
		Message sourceMessage = sourceChannel.getMessageById(messageID).complete();

		MessageEmbed embed = targetMessage.getEmbeds().get(0);

		eb.setAuthor(embed.getAuthor().getName(), TEMP_URL, embed.getAuthor().getIconUrl());
		eb.setDescription(sourceMessage.getContentRaw());
		eb.setFooter(embed.getFooter().getText() + " (Edited " + parseTime(sourceMessage.getEditedTime()) + ")", embed.getFooter().getIconUrl());
		
		eb.setColor(compareColors(COLOR_EDIT, embed.getColor()));
		
		targetMessage.editMessage(eb.build()).queue();
	}


	
	
	

	public static void updateReactions(JDA jda, String messageID, String userID, String channelID) {

		TextChannel sourceChannel = jda.getTextChannelById(channelID);
		Message sourceMessage = sourceChannel.getMessageById(messageID).complete();

		TextChannel targetChannel = jda.getTextChannelById(linkedChannels.get(channelID));
		Message targetMessage = targetChannel.getMessageById(DBFunctions.getLinkedMessage(sourceMessage.getId())).complete();
		
		HashMap<String, List<String>> emojis = new HashMap<String, List<String>>();
		
		for(MessageReaction mr : sourceMessage.getReactions()) {
			
			String reactionCode;
			ReactionEmote reaction = mr.getReactionEmote();
			if(reaction.isEmote()) {
				reactionCode = reaction.getEmote().getAsMention(); // if it is a guild specific reaction
			} else {
				reactionCode = reaction.getName(); // if it is a default, unicode reaction
			}
			emojis.put(reactionCode, new ArrayList<String>());
			
			List<String> users = emojis.get(reactionCode);
			for(User u : mr.getUsers().complete()){
				users.add(u.getAsMention());
			}
		}
		
		EmbedBuilder eb = new EmbedBuilder();
		MessageEmbed embed = targetMessage.getEmbeds().get(0);
		
		eb.setAuthor(embed.getAuthor().getName(), TEMP_URL, embed.getAuthor().getIconUrl());
		eb.setDescription(sourceMessage.getContentRaw());
		eb.setFooter(embed.getFooter().getText(), embed.getFooter().getIconUrl());

		StringBuilder sb = new StringBuilder();
		for(String reactionCode : emojis.keySet()) {
			sb.append(reactionCode + ": " + emojis.get(reactionCode).stream().collect(Collectors.joining(", ")));
			sb.append("\n");
		}
		
		
		eb.addField("Reactions", sb.toString(), false);
		eb.setColor(compareColors(COLOR_REACT, embed.getColor()));
		targetMessage.editMessage(eb.build()).queue();


	}

	
	
	
	
	
	public static boolean isTargetGuild(String id) {
		if(targetGuilds.contains(id)) {
			return true;
		}

		return false;
	}
	
	
	
	
	private static Color compareColors(Color c1, Color c2) {
		if(getColorPriority(c1) > getColorPriority(c2)) {
			return c1;
		}
		
		return c2;
		
	}
	
	private static int getColorPriority(Color c) {
		int rgb = c.getRGB();
		
		if(rgb == COLOR_MESSAGE.getRGB()) return 1;
		if(rgb == COLOR_REACT.getRGB()) return 2;
		if(rgb == COLOR_EDIT.getRGB()) return 3;
		if(rgb == COLOR_DELETE.getRGB()) return 4;
		
		return 0;

	}
//	private static final String TEMP_URL = "https://gmail.com";
//	private static final Color COLOR_MESSAGE = Color.decode("42f450");
//	private static final Color COLOR_REACT = Color.decode("f9c131");
//	private static final Color COLOR_EDIT = Color.decode("f3f718");
//	private static final Color COLOR_DELETE = Color.decode("ff2a00");

	

	private static String parseTime(OffsetDateTime timestamp) {
		return timestamp.getMonthValue() + "/" + timestamp.getDayOfMonth() + "/" + timestamp.getYear() + " " + timestamp.getHour() + ":" + timestamp.getMinute();
	}


}
