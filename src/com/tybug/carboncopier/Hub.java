package com.tybug.carboncopier;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.TextChannel;

public class Hub {


	private static HashMap<String, String> linkedChannels = null;

	public static void setup() {
		linkedChannels = DBFunctions.getLinkedChannels();
	}

	public static void sendMessage(JDA jda, String profileURL, String username, String content, List<Attachment> attachments, OffsetDateTime timestamp, String channelID) {
		TextChannel channel = jda.getTextChannelById(linkedChannels.get(channelID));
		
		EmbedBuilder eb = new EmbedBuilder();
		eb.setAuthor(username, "https://gmail.com", profileURL);
		eb.setDescription(content);
		eb.setFooter("Blair Discord", "https://images.pexels.com/photos/67636/rose-blue-flower-rose-blooms-67636.jpeg?auto=compress&cs=tinysrgb&h=350");
		
		channel.sendMessage(eb.build()).queue();
		
		for(Attachment a : attachments) {
			try {
				channel.sendFile(a.getInputStream(), a.getFileName()).queue();
			} catch (IOException e) { // because of the input stream
				e.printStackTrace();
			}
		}
	}
}
