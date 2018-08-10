# Carbon-Copier
A discord bot to copy every action performed on one server to another

## Events
Currently supported events:

##### Messages
* MessageReceivedEvent
* MessageUpdateEvent
* MessageDeleteEvent
* GuildMessageReactionAddEvent

##### Roles
* GenericRoleUpdateEvent
* RoleCreateEvent
* RoleDeleteEvent


##### Categories
* CategoryCreateEvent
* CategoryDeleteEvent
* CategoryUpdateNameEvent
* CategoryUpdatePositionEvent
* CategoryUpdatePermissionsEvent

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
*
