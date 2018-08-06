package com.tybug.carboncopier.listeners;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.List;

import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageType;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		
		// temporary for debugging linked ids etc
		if(event.getMessage().getContentDisplay().startsWith("!info")) {
			
			
			Guild guild = event.getJDA().getGuildById("475139498506715136");
			EmbedBuilder builder = new EmbedBuilder();
	        builder.setTitle("Server Info for " + guild.getName());
	        builder.setColor(Color.green);
	        StringBuilder sb = new StringBuilder();
	        for(TextChannel c : guild.getTextChannels()) {
	            sb.append(c.getName().substring(0, 1).toUpperCase() + c.getName().substring(1) + " - " + c.getId() + " - position " + c.getPositionRaw() + "\n");
	        }
	        builder.addField("Text Channels", sb.toString(), false);

	        sb = new StringBuilder();
	        for(Role r : guild.getRoles()) {
	            sb.append(r.getName() + " - " + r.getId() + "\n");
	        }
	        builder.addField("Roles", sb.toString(), false);
	        guild.getTextChannelById("475476543343165450").sendMessage(builder.build()).queue();

		}
		
		
		else if (event.getMessage().getContentDisplay().startsWith("!test")) {
			
			Guild guild = event.getJDA().getGuildById("475139498506715136");
			guild.getTextChannelById("475139498506715138").getManager().setPosition(3).queue();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			guild.getTextChannelById("475139498506715138").getManager().setParent(null).queue();
			
		}
		
		if(Hub.isTargetGuild(event.getGuild().getId())) {
			return;
		}
		JDA jda = event.getJDA();
		User author = event.getAuthor();
		Message message = event.getMessage();
		
		String profileURL = author.getEffectiveAvatarUrl();
		String username = author.getName();
		String content = message.getContentRaw();
		List<Attachment> attachments = message.getAttachments();
		OffsetDateTime timestamp = message.getCreationTime();
		String messageID = message.getId();
		String channelID = event.getChannel().getId();
		String guildID = event.getGuild().getId();
		
		if(message.getType().equals(MessageType.GUILD_MEMBER_JOIN)) {
			content = author.getAsMention() + " joined the guild!";
		}
		
        Hub.sendMessage(jda, profileURL, username, content, attachments, timestamp, messageID, channelID, guildID);
    }
	
	@Override
	public void onMessageUpdate(MessageUpdateEvent event) {
		if(Hub.isTargetGuild(event.getGuild().getId())) {
			return;
		}
		
		JDA jda = event.getJDA();
		String messageID = event.getMessage().getId();
		String channelID = event.getChannel().getId();
		
		
        Hub.editMessage(jda, messageID, channelID);
	}
	
}
