# Carbon-Copier
A discord bot to copy every action performed on one server to another - for historic purposes, viewing deleted images/messages, or in case a channel ever gets deleted and you want to save the message history.


## Setup
* This repo contains multiple personal hacks (such as automatic rebuilding) that can make setting it up yourself a tedious process. If you insist, a (by no means comprehensive) guide follows:
	* Copy example.db to db/info.db
	* Add authorized users to the db
	* Modify the channel CommandListener.java listens to, or remove automatic rebuilding entirely

## Showcase
![Bot in Action](/showcase/ex1.png?raw=true)

## Commands
* !link \[source] \[target] - Links two guilds together, copying all past and future message history


## Dependencies
* [JDA](https://github.com/DV8FromTheWorld/JDA)
* [JDA-Utilities](https://github.com/JDA-Applications/JDA-Utilities)
* [SQLite JDBC driver](https://github.com/xerial/sqlite-jdbc)
* [Logback Classic](https://mvnrepository.com/artifact/ch.qos.logback/logback-classic/0.9.26)


## Events
A brief overview of supported events:

* **Message** send delete, edit, reaction add
* **Role** create, modify, delete
* **Category** create, delete, modify
* **TextChannel** create, delete, modify
* **VoiceChannel** create, delete, modify

For a comprehensive list, see the classes in the listeners package.