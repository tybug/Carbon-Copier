package com.tybug.carboncopier;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.tybug.carboncopier.listeners.ChannelUpdateAction;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.ChannelManager;
import net.dv8tion.jda.core.managers.GuildController;


/**
 * Where Listeners send information so events can be replicated on the target guild.
 * <p>
 * A note on style: <br>
 * Methods recieve as little information as possible, and do a bit more leg work than they otherwise would have to regain references to guilds, textchannels, etc. 
 * <p>
 * This is both so that references to objects with large caches (guilds, jda) don't live as long, and to create an artificial barrier between Listeners (which deal with 
 * the events) and the Hub (which deals primarily with ids)
 * @author Liam DeVoe
 */
public class Hub {


	private static List<String> targetGuilds = null;

	private static HashMap<String, String> linkedGuilds = null;
	private static HashMap<String, String> linkedCategories = null;
	private static HashMap<String, String> linkedChannels = null;


	private static final String TEMP_URL = "https://gmail.com";
	private static final Color COLOR_MESSAGE = Color.decode("#42f450");
	private static final Color COLOR_REACT = Color.decode("#f9c131");
	private static final Color COLOR_EDIT = Color.decode("#f3f718");
	private static final Color COLOR_DELETE = Color.decode("#ff2a00");

	public static void setup() {
		targetGuilds = DBFunctions.getTargetGuilds();

		linkedGuilds = DBFunctions.getLinkedGuilds();
		linkedCategories = DBFunctions.getLinkedCategories();
		linkedChannels = DBFunctions.getLinkedChannels();
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

		List<Field> fields = embed.getFields(); // For the reaction field, if applicable
		if(fields.size() > 0) {
			eb.addField(embed.getFields().get(0));
		}

		targetMessage.editMessage(eb.build()).queue();
	}
	
	
	public static void deleteMessage(JDA jda, String messageID, String channelID) {
		
		TextChannel targetChannel = jda.getTextChannelById(linkedChannels.get(channelID));

		EmbedBuilder eb = new EmbedBuilder();
		Message targetMessage = targetChannel.getMessageById(DBFunctions.getLinkedMessage(messageID)).complete();

		MessageEmbed embed = targetMessage.getEmbeds().get(0);

		eb.setAuthor(embed.getAuthor().getName(), TEMP_URL, embed.getAuthor().getIconUrl());
		eb.setDescription("~~" + targetMessage.getContentRaw() + "~~");
		eb.setFooter(embed.getFooter().getText() + " (Deleted ~" + parseTime(OffsetDateTime.now()) + ")", embed.getFooter().getIconUrl());

		eb.setColor(compareColors(COLOR_DELETE, embed.getColor()));

		List<Field> fields = embed.getFields(); // For the reaction field, if applicable
		if(fields.size() > 0) {
			eb.addField(embed.getFields().get(0));
		}

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



	public static void updateRole(Role source) {
		int pos = source.getPosition();
		Guild sourceGuild = source.getGuild();
		Guild targetGuild = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId()));
		GuildController targetController = targetGuild.getController();

		targetGuild.getRoleById(DBFunctions.getLinkedRole(source.getId())).delete().complete(); 
		//make sure we delete if before we make a new one with potentially the same name

		Role target = targetController.createCopyOfRole(source).complete();

		targetController.modifyRolePositions().selectPosition(target).moveTo(pos).queue();

		DBFunctions.updateRoleLink(source.getId(), target.getId());
	}


	public static void createRole(Role source) {

		int pos = source.getPosition();

		Guild sourceGuild = source.getGuild();

		GuildController targetController = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId())).getController();

		Role target = targetController.createCopyOfRole(source).complete();
		target.getManager().setColor(Role.DEFAULT_COLOR_RAW).queue(); // It doesn't have the default color when copied for some reason
		targetController.modifyRolePositions().selectPosition(target).moveTo(pos).queue();


		DBFunctions.linkRole(source.getId(), target.getId());
	}



	public static void deleteRole(Role source) {
		Guild sourceGuild = source.getGuild();
		Guild targetGuild = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId()));

		targetGuild.getRoleById(DBFunctions.getLinkedRole(source.getId())).delete().complete();
		DBFunctions.removeRoleLink(source.getId());

	}





	public static void createChannel(Channel source) {
		Guild sourceGuild = source.getGuild();
		Guild targetGuild = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId()));
		Channel target = targetGuild.getController().createCopyOfChannel(source).complete();
		DBFunctions.linkChannel(source.getId(), target.getId());

		updateLinkedChannels();
	}
	
	
	public static void deleteChannel(Channel source) {
		Guild sourceGuild = source.getGuild();
		Guild targetGuild = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId()));
		Channel target = null;
		if(source.getType().equals(ChannelType.TEXT)) {
			target = targetGuild.getTextChannelById(linkedChannels.get(source.getId()));
		}
		
		else if (source.getType().equals(ChannelType.VOICE)) {
			target = targetGuild.getVoiceChannelById(linkedChannels.get(source.getId()));
		}
		target.delete().queue();
		
		DBFunctions.removeChannelLink(source.getId());
	}

	
	

	public static void updateTextChannel(TextChannel source, ChannelUpdateAction action) {
		Guild sourceGuild = source.getGuild();
		Guild targetGuild = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId()));
		TextChannel target = targetGuild.getTextChannelById(linkedChannels.get(source.getId()));
		ChannelManager manager = target.getManager();
		
		switch(action) {
		case TOPIC:
			manager.setTopic(source.getTopic()).queue();
			break;
		case NSFW:
			manager.setNSFW(source.isNSFW()).queue();
			break;
		default:
			break;
		}
	}
	
	
	
	public static void updateVoiceChannel(VoiceChannel source, ChannelUpdateAction action) {
		Guild sourceGuild = source.getGuild();
		Guild targetGuild = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId()));
		VoiceChannel target = targetGuild.getVoiceChannelById(linkedChannels.get(source.getId()));
		ChannelManager manager = target.getManager();
		
		switch(action) {
		case USER_LIMIT:
			manager.setUserLimit(source.getUserLimit()).queue();
			break;
		case BITRATE:
			manager.setBitrate(source.getBitrate()).queue();
			break;
		default:
			break;
		}
	}
	
	

	public static void updateChannel(Channel source, ChannelUpdateAction action) {
		Guild sourceGuild = source.getGuild();
		Guild targetGuild = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId()));
		Channel target = null;
		
		if(source.getType().equals(ChannelType.TEXT)) {
			target = targetGuild.getTextChannelById(linkedChannels.get(source.getId()));
		}
		
		else if (source.getType().equals(ChannelType.VOICE)) {
			target = targetGuild.getVoiceChannelById(linkedChannels.get(source.getId()));
		}
		
		ChannelManager manager = target.getManager();
		
		switch(action) {
		case NAME:
			manager.setName(source.getName()).queue();
			break;

		case POSITION:
			manager.setPosition(source.getPosition()).queue();
			break;

		case PARENT:
			Category parent = source.getParent();
			if(parent == null) {
				manager.setParent(null).queue();
			} 
			else {
				manager.setParent(targetGuild.getCategoryById(linkedCategories.get(parent.getId()))).queue();
			}
			break;
		default:
			break;
		}
	}

	
	

	public static void updateChannelPerms(Channel source, Collection<Role> sourceRoles) {
		Guild sourceGuild = source.getGuild();
		Guild targetGuild = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId()));
		Channel target = null;
		
		if(source.getType().equals(ChannelType.TEXT)) {
			target = targetGuild.getTextChannelById(linkedChannels.get(source.getId()));
		}
		
		else if (source.getType().equals(ChannelType.VOICE)) {
			target = targetGuild.getVoiceChannelById(linkedChannels.get(source.getId()));
		}
		
		ChannelManager manager = target.getManager();
		
		
		// Convert source roles to target roles
		Collection<Role> targetRoles = sourceRoles.stream().map(role -> targetGuild.getRoleById(DBFunctions.getLinkedRole(role.getId()))).collect(Collectors.toList());
		
		for(Role r : targetRoles) {
			List<Permission> allowed = source.getPermissionOverride(r).getAllowed();
			List<Permission> denied = source.getPermissionOverride(r).getDenied();

			manager.putPermissionOverride(targetGuild.getRoleById(DBFunctions.getLinkedRole(r.getId())), allowed, denied).queue();
		}
	}





	/**
	 * Updates linkedChannels
	 * <p>
	 * Updates the cache stored in linkedChannels to the most recent version from the db
	 */
	private static void updateLinkedChannels() {
		linkedChannels = DBFunctions.getLinkedChannels();
	}

	/**
	 * Updates linkedGuilds
	 * <p>
	 * Updates the cache stored in linkedGuilds to the most recent version from the db
	 */
	public static void updateLinkedGuilds() {
		linkedGuilds = DBFunctions.getLinkedGuilds();
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


	/**
	 * Determines the priority of the given color
	 * <p>
	 * In the context of embeds for sent messages, this determines which color should be displayed when a message has been both reacted to and edited, for example.
	 * The color with the higher priority will be displayed. No two states have the same priority.
	 * <p>
	 * If the passed color is not one of the special embed colors, the priority is returned as zero.
	 * 
	 * @param c The color
	 * @return The priority of this color
	 */
	private static int getColorPriority(Color c) {
		int rgb = c.getRGB();

		if(rgb == COLOR_MESSAGE.getRGB()) return 1;
		if(rgb == COLOR_REACT.getRGB()) return 2;
		if(rgb == COLOR_EDIT.getRGB()) return 3;
		if(rgb == COLOR_DELETE.getRGB()) return 4;

		return 0;

	}



	/**
	 * Parses to human readable time
	 * <p>
	 * Given an OffsetDateTime object, returns a String in the form M/dd/yyyy h:mm a (as specified by SimpleDateFormat)
	 * <p>
	 * EX: 8/05/2018 10:23 PM
	 * 
	 * @param timestamp The timestamp
	 * @return The parsed date
	 */
	private static String parseTime(OffsetDateTime timestamp) {
		Date date = Date.from(timestamp.toInstant());
		SimpleDateFormat format = new SimpleDateFormat("M/dd/yyyy h:mm a");
		return format.format(date);
	}



}
