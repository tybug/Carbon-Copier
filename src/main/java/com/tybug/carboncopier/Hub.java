package com.tybug.carboncopier;

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
		targetMessage.editMessage(eb.build()).queue();


	}


	
	
	

	public static boolean isTargetGuild(String id) {
		if(targetGuilds.contains(id)) {
			return true;
		}

		return false;
	}

	private static String parseTime(OffsetDateTime timestamp) {
		return timestamp.getMonthValue() + "/" + timestamp.getDayOfMonth() + "/" + timestamp.getYear() + " " + timestamp.getHour() + ":" + timestamp.getMinute();
	}


}
