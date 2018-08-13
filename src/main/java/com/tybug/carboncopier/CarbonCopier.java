package com.tybug.carboncopier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.napstr.logback.DiscordAppender;
import com.tybug.carboncopier.listeners.ChannelListener;
import com.tybug.carboncopier.listeners.CommandListener;
import com.tybug.carboncopier.listeners.MessageListener;
import com.tybug.carboncopier.listeners.RoleListener;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;

public class CarbonCopier {
	final static Logger LOG = LoggerFactory.getLogger(Hub.class);


	public static void main(String[] args) {
		LOG.info("");
		BufferedReader br;
		String token = null;
		try {
			br = new BufferedReader(new FileReader("config.txt"));
			token = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}



		try
		{
			@SuppressWarnings("unused")
			JDA jda = new JDABuilder(AccountType.BOT) 
			.setToken(token) //pass the token (loaded from an ignored file, this time)
			.addEventListener(new MessageListener(), new RoleListener(), new ChannelListener(), new CommandListener()) // add all the listeners
			.setGame(Game.playing("Backing up your server")) //set "Playing..." display message
			.buildBlocking();  //build the whole thing, blocking guarantees it will be completely loaded vs async which does not
			Hub.setup();
		}
		catch (LoginException | InterruptedException e) {
			e.printStackTrace();
		}



	}

}
