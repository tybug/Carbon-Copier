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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import net.dv8tion.jda.core.entities.MessageHistory;
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

	final static Logger LOG = LoggerFactory.getLogger(Hub.class);


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
	private static final String FIELD_NAME_LIMIT = "Last allowed field";


	private static final String LOG_CATEGORY_NAME = "ADMIN";
	private static final String LOG_CHANNEL_NAME = "log";


	public static void setup() {
		LOG.info("Setting up Hub lists/maps");

		sourceGuilds = DBFunctions.getSourceGuilds();
		linkedGuilds = DBFunctions.getLinkedGuilds();
		linkedCategories = DBFunctions.getLinkedCategories();
		linkedChannels = DBFunctions.getLinkedChannels();
	}


	public static void linkGuilds(JDA jda, PrivateChannel channel, String sourceID, String targetID, boolean copyHistory) {
		LOG.info("Linking guild {} to {}. Copying history? {}", sourceID, targetID, copyHistory);
		

		
		Guild source = jda.getGuildById(sourceID);
		Guild target = jda.getGuildById(targetID);


		GuildController gc = target.getController();
		String logID = gc.createTextChannel(LOG_CHANNEL_NAME).setParent((Category) gc.createCategory(LOG_CATEGORY_NAME).complete()).complete().getId();
		DBFunctions.linkGuild(sourceID, targetID, logID);
		Hub.updateLinkedGuilds(); // Update our cache BEFORE we create any channels so the id is there
		Hub.updateSourceGuilds();

		List<Role> roles = new ArrayList<Role>(source.getRoles()); // Make the list mutable so we can reverse it
		// Reverse so when we create roles we create ones with lower position first and don't get "provided position is out of bounds"
		Collections.reverse(roles);

		LOG.debug("Copying roles");
		for(Role r : roles) {
			Hub.createRole(r);
		}	
		LOG.debug("Copying categories");
		for(Category category : source.getCategories()) {
			LOG.trace("Copying category {}", category.getName());
			Hub.createChannel(category);
		}
		LOG.debug("Copying textchannels");
		for(TextChannel text : source.getTextChannels()) {
			LOG.trace("Copying textchannel {}", text.getName());
			Hub.createChannel(text);
		}
		LOG.debug("Copying voicechannels");
		for(VoiceChannel voice : source.getVoiceChannels()) {
			LOG.trace("Copying voicechannel {}", voice.getName());
			Hub.createChannel(voice);
		}
		
		if(!copyHistory) {
			channel.sendMessage("Finished linking `" + source.getName() +"` to `" + target.getName() + "`").queue();
			return;
		}
		
		
		for(TextChannel text : source.getTextChannels()) {
			List<Message> messages = Hub.loadChannelHistory(text);
			Collections.reverse(messages); // TODO check how expensive this is... O(1) but can we get better overall by changing the type of messages to a stack?
			for(Message m : messages) {
				MessageInfo info = Utils.createMessageInfo(m);
				Hub.sendMessage(jda, info);
				if(m.getReactions().size() > 0) { 
					// Inefficient to do both send and update, but the best we can do without rewriting how messages and reactions are handled
					Hub.updateReactions(jda, info.getMessageID(), info.getChannelID());
				}
			}
		}

		channel.sendMessage("Finished linking `" + source.getName() +"` to `" + target.getName() + "`").queue();
		
	}




	/**
	 * Copies a source message to the linked guild and channel. The source message is then linked to the target message in the db.
	 * <p>
	 * The color of all copied MessageEmbeds will be overriden in favor of the master color scheme
	 * @param jda The JDA
	 * @param MessageInfo The meta info contained in the source message
	 */
	public static void sendMessage(JDA jda, MessageInfo info) {
		LOG.debug("Copying message from guild {} by {}", jda.getGuildById(info.getGuildID()).getName(), info.getUsername());

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
		LOG.info("Copying message edit of {} by {}", info.getMessageID(), info.getUsername());

		TextChannel targetChannel = jda.getTextChannelById(linkedChannels.get(info.getChannelID()));
		Message targetMessage = targetChannel.getMessageById(DBFunctions.getLinkedMessage(info.getMessageID())).complete();

		EmbedBuilder eb = new EmbedBuilder(targetMessage.getEmbeds().get(0)); // Copy the target embed
		eb.setColor(COLOR_EDIT);
		eb.addField(FIELD_NAME_EDIT, info.getContent() + "\n" + parseTime(info.getEditedTime()), false); // Set new content as last field
		targetMessage.editMessage(eb.build()).queue();
	}


	public static void deleteMessage(JDA jda, String messageID, String channelID) {
		LOG.info("Copying message delete of {} ", messageID);

		TextChannel targetChannel = jda.getTextChannelById(linkedChannels.get(channelID));
		Message targetMessage = targetChannel.getMessageById(DBFunctions.getLinkedMessage(messageID)).complete();

		EmbedBuilder eb = new EmbedBuilder(targetMessage.getEmbeds().get(0)); // Copy the target embed
		eb.setColor(COLOR_DELETE); // No need to compare, delete always takes precedence
		eb.addField("Deleted", "~" + parseTime(OffsetDateTime.now()), false);

		targetMessage.editMessage(eb.build()).queue();
	}






	public static void updateReactions(JDA jda, String messageID, String channelID) {
		LOG.info("Copying reactions on message {}", messageID);

		TextChannel sourceChannel = jda.getTextChannelById(channelID);
		Message sourceMessage = sourceChannel.getMessageById(messageID).complete();

		TextChannel targetChannel = jda.getTextChannelById(linkedChannels.get(channelID));
		Message targetMessage = targetChannel.getMessageById(DBFunctions.getLinkedMessage(sourceMessage.getId())).complete();

		HashMap<String, List<String>> emojis = new HashMap<String, List<String>>();


		for(MessageReaction mr : sourceMessage.getReactions()) {
			LOG.debug("Copying users on reaction {} ", mr.getReactionEmote().getName());
			String reactionCode;
			ReactionEmote reaction = mr.getReactionEmote();
			if(reaction.isEmote()) {
				LOG.trace("{} is an emote in guild {}", reaction.getName(), reaction.getEmote().getGuild());
				reactionCode = reaction.getEmote().getAsMention(); // if it is a guild specific reaction
			} else {
				LOG.trace("{} is a default discord reaction", reaction.getName());
				reactionCode = reaction.getName(); // if it is a default, unicode reaction
			}
			emojis.put(reactionCode, new ArrayList<String>());

			LOG.trace("Counting users for reaction {}", reaction.getName());
			List<String> users = emojis.get(reactionCode);
			for(User u : mr.getUsers().complete()){
				LOG.trace("Adding user {} to reaction list for {}", u.getName(), reaction.getName());
				users.add(u.getAsMention());
			}
		}
		MessageEmbed embed = targetMessage.getEmbeds().get(0);
		EmbedBuilder eb = new EmbedBuilder(embed); // Copy the target embed

		StringBuilder sb = new StringBuilder();
		for(String reactionCode : emojis.keySet()) {
			LOG.trace("Appending reactionCode {} from reaction list to stringBuilder", reactionCode);
			sb.append(reactionCode + ": " + emojis.get(reactionCode).stream().collect(Collectors.joining(", ")) + "(" + emojis.get(reactionCode).size() + ")");
			sb.append("\n");
		}

		LOG.debug("Checking for reaction field");
		for(Field f : eb.getFields()) {
			LOG.trace("Checking field {}", f.getName());
			if(f.getName().equals(FIELD_NAME_REACTION)) { // Builder already has a reaction field
				LOG.trace("Found a reaction field; removing from embed"); 
				eb.getFields().remove(f);
				break; // No need to check the rest, guaranteed to only have one
			}
		}


		if(sourceMessage.getReactions().size() != 0) { // If there are any reactions
			LOG.debug("Reactions remain on source message, adding reaction field");
			eb.addField(new Field(FIELD_NAME_REACTION, sb.toString(), false));
			eb.setColor(compareColors(COLOR_REACT, embed.getColor()));
		} else {
			LOG.debug("No reactions left on source message");
			if(embed.getColor().equals(COLOR_REACT)) { // No more reactions? Reset to default color
				LOG.debug("Previously COLOR_REACT, setting to COLOR_MESSAGE");
				eb.setColor(COLOR_MESSAGE);
			}
		}
		targetMessage.editMessage(eb.build()).queue();
	}



	public static void updateRole(Role source) {
		LOG.info("Updating role {} from {}", source.getName(), source.getGuild().getName());

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
		LOG.info("Copying role {} from {}", source.getName(), source.getGuild().getName());

		int pos = source.getPosition();
		Guild sourceGuild = source.getGuild();
		GuildController targetController = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId())).getController();

		Role target = targetController.createCopyOfRole(source).complete();
		target.getManager().setColor(source.getColorRaw()); // Not sure if #getColorRaw vs #getColor does anything, but I'm not taking chances

		targetController.modifyRolePositions().selectPosition(target).moveTo(pos + ROLE_OFFSET).queue();

		DBFunctions.linkRole(source.getId(), target.getId());
	}



	public static void deleteRole(Role source) {
		LOG.info("Copying deletion of role {} from {}", source.getName(), source.getGuild().getName());


		Guild sourceGuild = source.getGuild();
		Guild targetGuild = sourceGuild.getJDA().getGuildById(linkedGuilds.get(sourceGuild.getId()));

		targetGuild.getRoleById(DBFunctions.getLinkedRole(source.getId())).delete().complete();
		DBFunctions.removeRoleLink(source.getId());

	}





	public static void createChannel(Channel source) {
		LOG.info("Copying creation of channel {} from {}", source.getName(), source.getGuild().getName());


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
		LOG.info("Copying deletion of channel {} from {}", source.getName(), source.getGuild().getName());


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
		LOG.info("Updating textchannel {} from {} with action {}", source.getName(), source.getGuild().getName(), action);

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

		LOG.info("Updating voicechannel {} from {} with action {}", source.getName(), source.getGuild().getName(), action);

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
		LOG.info("Updating channel {} from {} with action {}", source.getName(), source.getGuild().getName(), action);

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
		LOG.info("Updating perms of channel {} from {} for roles {}", source.getName(),
				source.getGuild().getName(), sourceRoles.stream().map(r -> r.getName()).collect(Collectors.joining(", ")));


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
		LOG.debug("Creating message embed for message {} by ", info.getMessageID(), info.getUsername());

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

	
	
	public static List<Message> loadChannelHistory(TextChannel channel) {
		List<Message> history = MessageHistory.getHistoryAfter(channel, channel.getId()).complete().getRetrievedHistory();
		return history;
	}


	/**
	 * Updates linkedChannels
	 * <p>
	 * Updates the cache stored in linkedChannels to the most recent version from the db
	 */
	private static void updateLinkedChannels() {
		LOG.debug("Updating linked channels");
		linkedChannels = DBFunctions.getLinkedChannels();
	}



	/**
	 * Updates linkedGuilds
	 * <p>
	 * Updates the cache stored in linkedGuilds to the most recent version from the db
	 */
	public static void updateLinkedGuilds() {
		LOG.debug("Updating linked guilds");
		linkedGuilds = DBFunctions.getLinkedGuilds();
	}


	/**
	 * Updates linkedCategories
	 * <p>
	 * Updates the cache stored in linkedCategories to the most recent version from the db
	 */
	public static void updateLinkedCategories() {
		LOG.debug("Updating linked categories");
		linkedCategories = DBFunctions.getLinkedCategories();
	}





	/**
	 * Updates sourceGuilds
	 * <p>
	 * Updates the cache stored in sourceGuilds to the most recent version from the db
	 */
	public static void updateSourceGuilds() {
		LOG.debug("Updating source guilds");
		sourceGuilds = DBFunctions.getSourceGuilds();
	}




	public static boolean isSourceGuild(String id) {
		LOG.trace("Checking if {} is a source guild", id);
		if(sourceGuilds.contains(id)) {
			LOG.debug("{} is a source guild", id);
			return true;
		}

		LOG.debug("{} is not a source guild", id);
		return false;
	}




	private static Color compareColors(Color c1, Color c2) {
		LOG.debug("Comparing colors {} and {} (RGB values)", c1.getRGB(), c2.getRGB());
		if(getColorPriority(c1) > getColorPriority(c2)) {
			LOG.debug("Color {} has a higher priority", c1.getRGB());
			return c1;
		}
		LOG.debug("Color {} has a higher priority", c2.getRGB());
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
		LOG.trace("Finding priority of color {}", c.getRGB());
		int rgb = c.getRGB();

		if(rgb == COLOR_MESSAGE.getRGB()) {
			LOG.trace("Priority of color {} is 1", c.getRGB());
			return 1;
		}
		if(rgb == COLOR_REACT.getRGB()) {
			LOG.trace("Priority of color {} is 2", c.getRGB());
			return 2;
		}
		if(rgb == COLOR_EDIT.getRGB()) {
			LOG.trace("Priority of color {} is 3", c.getRGB());
			return 3;
		}
		if(rgb == COLOR_DELETE.getRGB()) {
			LOG.trace("Priority of color {} is 4", c.getRGB());
			return 4;
		}

		LOG.trace("Priority of color {} is 0", c.getRGB());
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
		LOG.trace("Parsing timestamp {}", timestamp);
		Date date = Date.from(timestamp.toInstant());
		SimpleDateFormat format = new SimpleDateFormat("M/dd/yyyy h:mm a");
		LOG.trace("Parsed {} to {}", timestamp, format.format(date));
		return format.format(date);
	}


}
