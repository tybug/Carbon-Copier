package com.tybug.carboncopier.listeners;

public enum ChannelUpdateAction {
	
	NAME, POSITION, PARENT, // Channel
	TOPIC, NSFW, // TextChannel
	USER_LIMIT, BITRATE // VoiceChannel
}
