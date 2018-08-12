package com.tybug.carboncopier;

import java.awt.Color;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.ChannelManager;
import net.dv8tion.jda.core.managers.GuildController;
import net.dv8tion.jda.core.requests.restaction.MessageAction;


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


	private static List<String> sourceGuilds = null;

	private static HashMap<String, String> linkedGuilds = null;
	private static HashMap<String, String> linkedCategories = null;
	private static HashMap<String, String> linkedChannels = null;


	private static final int ROLE_OFFSET = 1; //how many roles are above the carbon copied roles
	
	private static final String TEMP_URL = "https://gmail.com";
	private static final Color COLOR_MESSAGE = Color.decode("#42f450");
	private static final Color COLOR_REACT = Color.decode("#f9c131");
	private static final Color COLOR_EDIT = Color.decode("#f3f718");
	private static final Color COLOR_DELETE = Color.decode("#ff2a00");

	private static final String FIELD_NAME_REACTION = "Reactions";
	private static final String FIELD_NAME_EDIT = "Edited";

	
	public static void setup() {
		sourceGuilds = DBFunctions.getSourceGuilds();

		linkedGuilds = DBFunctions.getLinkedGuilds();
		linkedCategories = DBFunctions.getLinkedCategories();
		linkedChannels = DBFunctions.getLinkedChannels();
	}


	public static void linkGuilds(JDA jda, PrivateChannel channel, String sourceID, String targetID) {
		DBFunctions.linkGuild(sourceID, targetID);
		Hub.updateLinkedGuilds(); // Update our cache BEFORE we create any channels so the id is there
		Guild source = jda.getGuildById(sourceID);
		Guild target = jda.getGuildById(targetID);
		
		
		List<Role> roles = new ArrayList<Role>(source.getRoles()); // Make the list mutable so we can reverse it
		// Reverse so when we create roles we create ones with lower position first and don't get "provided position is out of bounds"
		Collections.reverse(roles);
		for(Role r : roles) {
			Hub.createRole(r);
		}	
		
		for(Category category : source.getCategories()) {
			Hub.createChannel(category);
		}
		for(TextChannel text : source.getTextChannels()) {
			Hub.createChannel(text);
		}
		for(VoiceChannel voice : source.getVoiceChannels()) {
			Hub.createChannel(voice);
		}
		
			
		
		Hub.updateSourceGuilds();
		channel.sendMessage("Linked `" + source.getName() +"` to `" + target.getName() + "`").queue();
		
	}
	
	
	
	
	/**
	 * Copies a source message to the linked guild and channel. The source message is then linked to the target message in the db.
	 * <p>
	 * The color of all copied MessageEmbeds will be overriden in favor of the master color scheme
	 * @param jda The JDA
	 * @param MessageInfo The meta info contained in the source message
	 */
	public static void sendMessage(JDA jda, MessageInfo info) {

		MessageEmbed embed = createMessage(jda, info);
		TextChannel targetChannel = jda.getTextChannelById(linkedChannels.get(info.getChannelID()));
		MessageAction action = targetChannel.sendMessage(embed);
		
		List<Attachment> attachments = info.getAttachments();
		if(attachments.size() > 0 && attachments.get(0).isImage() == false) {
			try {
				action.addFile(attachments.get(0).getInputStream(), attachments.get(0).getFileName());
			} catch (IOException e) {
				System.err.println("IOException while trying to add the file attachment when creating a linked message!");
			}
		}
		
		
		DBFunctions.linkMessage(info.getMessageID(), action.complete().getId());
		
	}


	public static void editMessage(JDA jda, MessageInfo info) {
		
		TextChannel targetChannel = jda.getTextChannelById(linkedChannels.get(info.getChannelID()));
		Message targetMessage = targetChannel.getMessageById(DBFunctions.getLinkedMessage(info.getMessageID())).complete();

		EmbedBuilder eb = new EmbedBuilder(targetMessage.getEmbeds().get(0)); // Copy the target embed
		eb.setColor(COLOR_EDIT);
		eb.addField(FIELD_NAME_EDIT, info.getContent() + "\n" + parseTime(info.getEditedTime()), false); // Set new content as last field
		// TODO check if adding the field would put us over the field limit

		targetMessage.editMessage(eb.build()).queue();
	}


	public static void deleteMessage(JDA jda, String messageID, String channelID) {

		TextChannel targetChannel = jda.getTextChannelById(linkedChannels.get(channelID));
		Message targetMessage = targetChannel.getMessageById(DBFunctions.getLinkedMessage(messageID)).complete();

		EmbedBuilder eb = new EmbedBuilder(targetMessage.getEmbeds().get(0)); // Copy the target embed
		eb.setColor(COLOR_DELETE); // No need to compare, delete always takes precedence
		eb.addField("Deleted", "~" + parseTime(OffsetDateTime.now()) + ")", false);

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
		MessageEmbed embed = targetMessage.getEmbeds().get(0);
		EmbedBuilder eb = new EmbedBuilder(embed); // Copy the target embed

		StringBuilder sb = new StringBuilder();
		for(String reactionCode : emojis.keySet()) {
			sb.append(reactionCode + ": " + emojis.get(reactionCode).stream().collect(Collectors.joining(", ")));
			sb.append("\n");
		}
		
		Field reactionField = null;
		for(Field f : eb.getFields()) {
			if(f.getName().equals(FIELD_NAME_REACTION)) { // Builder already has a reaction field
				reactionField = f;
				break; // No need to check the rest, guaranteed to only have one
			}
		}
		
		if(sourceMessage.getReactions().size() != 0) { // If there are any reactions
			reactionField = new Field(FIELD_NAME_REACTION, sb.toString(), false);	
			eb.addField(reactionField);
			eb.setColor(compareColors(COLOR_REACT, embed.getColor()));
		} else {
			if(embed.getColor().equals(COLOR_REACT)) { // No more reactions? Reset to default color
				eb.setColor(COLOR_MESSAGE);
				eb.getFields().remove(reactionField);
//				reactionField = null; // Clear field
			}
		}
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

		targetController.modifyRolePositions().selectPosition(target).moveTo(pos + ROLE_OFFSET).queue();

		DBFunctions.updateRoleLink(source.getId(), target.getId());
	}


	public static void createRole(Role source) {

		int pos = source.getPosition();

		Guild sourceGuild = source.getGuild();

		GuildController targetController = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId())).getController();

		Role target = targetController.createCopyOfRole(source).complete();
		target.getManager().setColor(source.getColorRaw()); // Not sure if #getColorRaw vs #getColor does anything, but I'm not taking chances
		
		targetController.modifyRolePositions().selectPosition(target).moveTo(pos + ROLE_OFFSET).queue();

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
		
		for(PermissionOverride override : source.getRolePermissionOverrides()) {
			Role targetRole = targetGuild.getRoleById(DBFunctions.getLinkedRole(override.getRole().getId()));
			target.getManager().putPermissionOverride(targetRole, override.getAllowed(), override.getDenied()).queue();
		}
		
		// Public role (@everyone) not included in Channel#getRolePermissionOverrides, so we add it manually
		PermissionOverride publicOverride = source.getPermissionOverride(sourceGuild.getPublicRole());
		if(publicOverride != null) { // Null if no overrides on @everyone
			target.getManager().putPermissionOverride(targetGuild.getPublicRole(), publicOverride.getAllowed(), publicOverride.getDenied()).queue();
		}
		
		
		if(source.getParent() != null) {
			 // I never want to write something so disgusting in my life again
			target.getManager().setParent(targetGuild.getCategoryById(linkedCategories.get(source.getParent().getId()))).queue();
		}
		
		if(source.getType().equals(ChannelType.CATEGORY)) {
			DBFunctions.linkCategory(source.getId(), target.getId());
			updateLinkedCategories(); 
		} else {
			DBFunctions.linkChannel(source.getId(), target.getId());
			updateLinkedChannels();
		}
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
	 * Creates a MessageEmbed representing the target message, with information copied over from the given MessageInfo
	 * @param jda The source JDA
	 * @param info The source MessageInfo
	 * @return
	 */
	private static MessageEmbed createMessage(JDA jda, MessageInfo info) {
		Guild guild = jda.getGuildById(info.getGuildID());
		
		EmbedBuilder eb = new EmbedBuilder();
		
		// Keep these fields in the sent embed
		eb.setDescription(info.getContent());

		if(info.getEmbeds().size() == 1) {
			eb = new EmbedBuilder(info.getEmbeds().get(0));
		}
		
		// Overwrite these fields from the sent embed
		eb.setAuthor(info.getUsername(), TEMP_URL, info.getProfileURL());
		String time = parseTime(info.getTimestamp());
		eb.setFooter(guild.getName() + " • " + time, guild.getIconUrl());
		
		if(info.getEditedTime() == null) { // If it hasn't been edited
			eb.setColor(COLOR_MESSAGE);
		} 
		
		else {
			eb.setColor(COLOR_EDIT);
		}

		List<Attachment> attachments = info.getAttachments();
		if(attachments.size() == 1) {
			if(attachments.get(0).isImage()) {
				eb.setImage(attachments.get(0).getUrl());
			}
		} 

		else if(attachments.size() > 1) {
			eb.addField("Images", attachments.stream().map(a -> a.toString()).collect(Collectors.joining("\n")), false);
		} 

		
		return eb.build();
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
	
	
	/**
	 * Updates linkedCategories
	 * <p>
	 * Updates the cache stored in linkedCategories to the most recent version from the db
	 */
	public static void updateLinkedCategories() {
		linkedCategories = DBFunctions.getLinkedCategories();
	}

	
	
	
	
	/**
	 * Updates sourceGuilds
	 * <p>
	 * Updates the cache stored in sourceGuilds to the most recent version from the db
	 */
	public static void updateSourceGuilds() {
		sourceGuilds = DBFunctions.getSourceGuilds();
	}
	
	
	

	public static boolean isSourceGuild(String id) {
		if(sourceGuilds.contains(id)) {
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
