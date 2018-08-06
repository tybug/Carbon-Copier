# Carbon-Copier
A discord bot to copy every action performed on one server to another

## Events
Currently supported events:

##### Messages
* MessageReceivedEvent
* MessageUpdateEvent
* GuildMessageReactionAddEvent

##### Roles
* GenericRoleUpdateEvent
* RoleCreateEvent
* RoleDeleteEvent

##### Text Channels
* TextChannelCreateEvent
* TextChannelUpdateNameEvent
* TextChannelUpdateTopicEvent
* TextChannelUpdatePositionEvent
* TextChannelUpdatePermissionsEvent
* TextChannelUpdateParentEvent
* TextChannelUpdateNSFWEvent

##### Voice Channels
* VoiceChannelCreateEvent
* VoiceChannelDeleteEvent
* VoiceChannelUpdateNameEvent
* VoiceChannelUpdatePositionEvent
* VoiceChannelUpdateUserLimitEvent
* VoiceChannelUpdateBitrateEvent
* VoiceChannelUpdatePermissionsEvent
* VoiceChannelUpdateParentEvent

## Commands
* !link \[source] \[target] - Links two guilds together