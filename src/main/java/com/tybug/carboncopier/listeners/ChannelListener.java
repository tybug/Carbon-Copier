package com.tybug.carboncopier.listeners;

import com.tybug.carboncopier.Hub;

import net.dv8tion.jda.core.events.channel.text.GenericTextChannelEvent;
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateNSFWEvent;
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateNameEvent;
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateParentEvent;
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdatePermissionsEvent;
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdatePositionEvent;
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateTopicEvent;
import net.dv8tion.jda.core.events.channel.voice.GenericVoiceChannelEvent;
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent;
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.core.events.channel.voice.update.VoiceChannelUpdateBitrateEvent;
import net.dv8tion.jda.core.events.channel.voice.update.VoiceChannelUpdateNameEvent;
import net.dv8tion.jda.core.events.channel.voice.update.VoiceChannelUpdateParentEvent;
import net.dv8tion.jda.core.events.channel.voice.update.VoiceChannelUpdatePermissionsEvent;
import net.dv8tion.jda.core.events.channel.voice.update.VoiceChannelUpdatePositionEvent;
import net.dv8tion.jda.core.events.channel.voice.update.VoiceChannelUpdateUserLimitEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ChannelListener extends ListenerAdapter {

	@Override
	public void onGenericTextChannel(GenericTextChannelEvent event) {
		if(Hub.isTargetGuild(event.getGuild().getId())) {
			return;
		}


		if(event instanceof TextChannelCreateEvent) {
			Hub.createChannel(event.getChannel());
		}

		//		else if(event instanceof TextChannelDeleteEvent) {
		//			Hub.deleteTextChannel(event.getChannel());
		//		}
		//		We don't want to actually delete the copy of the channel, but how do we deal with name conflicts? Discuss later.

		
		else if(event instanceof TextChannelUpdateNameEvent) {
			Hub.updateChannel(event.getChannel(), ChannelUpdateAction.NAME);
		}
		else if(event instanceof TextChannelUpdateTopicEvent) {
			Hub.updateTextChannel(event.getChannel(), ChannelUpdateAction.TOPIC);
		}
		else if(event instanceof TextChannelUpdatePositionEvent) {
			Hub.updateChannel(event.getChannel(), ChannelUpdateAction.POSITION);
		}
		else if(event instanceof TextChannelUpdatePermissionsEvent) {
			Hub.updateChannelPerms(event.getChannel(), ((TextChannelUpdatePermissionsEvent) event).getChangedRoles());
		}
		else if(event instanceof TextChannelUpdateNSFWEvent) {
			Hub.updateTextChannel(event.getChannel(), ChannelUpdateAction.NSFW);
		}
		else if(event instanceof TextChannelUpdateParentEvent) {
			Hub.updateChannel(event.getChannel(), ChannelUpdateAction.PARENT);
		}

	}







	public void onGenericVoiceChannel(GenericVoiceChannelEvent event) {

		if(Hub.isTargetGuild(event.getGuild().getId())) {
			return;
		}
		
		
		
		if(event instanceof VoiceChannelCreateEvent) {
			Hub.createChannel(event.getChannel());
		}
		else if(event instanceof VoiceChannelDeleteEvent) {
			Hub.deleteChannel(event.getChannel());
		}
		
		
		else if(event instanceof VoiceChannelUpdateNameEvent) {
			Hub.updateChannel(event.getChannel(), ChannelUpdateAction.NAME);
		}
		else if(event instanceof VoiceChannelUpdatePositionEvent) {
			Hub.updateChannel(event.getChannel(), ChannelUpdateAction.POSITION);
		}
		else if(event instanceof VoiceChannelUpdateUserLimitEvent) {
			Hub.updateVoiceChannel(event.getChannel(), ChannelUpdateAction.USER_LIMIT);
		}
		else if(event instanceof VoiceChannelUpdateBitrateEvent) {
			Hub.updateVoiceChannel(event.getChannel(), ChannelUpdateAction.BITRATE);
		}
		else if(event instanceof VoiceChannelUpdatePermissionsEvent) {
			Hub.updateChannelPerms(event.getChannel(), ((VoiceChannelUpdatePermissionsEvent) event).getChangedRoles());
		}
		else if(event instanceof VoiceChannelUpdateParentEvent) {
			Hub.updateChannel(event.getChannel(), ChannelUpdateAction.PARENT);
		}

	}
	
	
	
	

}
