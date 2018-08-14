# Carbon-Copier
A discord bot to copy every action performed on one server to another


## Setup
* Git clone, or download option of your preference
* Create a db/ directory directly under the project directory, and copy the db from example.db into it, renaming it to info.db
* Add users you want to be able to execute commands to the AUTHORIZED table in the db (don't forget your own id!)
* There is a calendar hack located at the bottom of src/main/java/com/tybug/carboncopier/Hub, under Hub#parseTime. Should this not produce timestamps to your liking in the embeds, change the value added to the calendar or remove it entirely
* Add a config.txt file directly under the project directory (or in the same directory as the jar file, if executing from a jar) and put the bot's token as the first line
* Should you still want your project to be recompiled whenever you push to your repository..
    * Change the GITHUB_CC value located in src/main/java/com/tybug/carboncopier/listeners/CommandListener to the channel where the github webhook on your repository links to
        * Note that currently any message in this channel, not just one from a webhook, will cause a restart 
    * Create a /sources/ directory directly under the project directory
    * Make sure the project directory is a git directory, and that it links to your repository
* If not, simply change RESTART\_ON_PUSH in src/main/java/com/tybug/carboncopier/listeners/CommandListener to false
    * Note that leaving the GITHUB_CC value as is would work just as well


## Commands
AUTHORIZED users only:

* !link \[source] \[target] - Links two guilds together
    * When prompted, a "check" reaction copies all previous messages in the source guild, while an "x" reaction does not.
    * Note that the bot must be in both guilds, and have at least MANAGE\_ROLES, MANAGE\_CHANNELS, MESSAGE\_SEND, MESSAGE\_READ, and its own role above any others in the target guild.
    * In the source guild, it only needs MESSAGE_READ for all channels you wish to copy history from, both past and future. 


## Dependencies
* [JDA](https://github.com/DV8FromTheWorld/JDA)
* [JDA-Utilities](https://github.com/JDA-Applications/JDA-Utilities)
* [SQLite JDBC driver](https://github.com/xerial/sqlite-jdbc)
* [Logback Classic](https://mvnrepository.com/artifact/ch.qos.logback/logback-classic/0.9.26)


## Events
Currently supported events:

##### Messages
* MessageReceivedEvent
* MessageUpdateEvent
* MessageDeleteEvent
* GuildMessageReactionAddEvent

##### Roles
* GenericRoleUpdateEvent (And all of its sub events)
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


