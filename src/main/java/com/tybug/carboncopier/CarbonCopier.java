package com.tybug.carboncopier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tybug.carboncopier.listeners.ChannelListener;
import com.tybug.carboncopier.listeners.CommandListener;
import com.tybug.carboncopier.listeners.MessageListener;
import com.tybug.carboncopier.listeners.RoleListener;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;

/**
 * A discord bot to copy every action performed on one server to another server.
 * <p>
 * The bot itself is loaded here, and listeners added. The waiter listener is from JDA-Utilities, to wait for events before taking an action. <br>
 * Event logic takes place in the listener classes (ex: MessageListener), 
 * and copying logic in the Hub class. <br>
 * Database interactions are handled entire by the DBFunctions class. <br>
 * MessageInfo is simply a convenience class to pass to Hub methods, to reduce method signature length.
 * <p>
 * Regarding log style: <br>
 * Where possible, logs are put in the method call, not before the method call. <br>
 * So LOG.info("Creating textchannel") would be in the method {@link Hub#createChannel(net.dv8tion.jda.core.entities.Channel)}, 
 * not right above Hub.createChannel in whatever method is calling Hub.
 * @author Liam DeVoe
 *
 */
public class CarbonCopier {
	final static Logger LOG = LoggerFactory.getLogger(Hub.class);

	public static void main(String[] args) {
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
			.addEventListener(new MessageListener(), new RoleListener(), new ChannelListener(), new CommandListener(), Utils.waiter) // add all the listeners
			.setGame(Game.playing("Backing up your server")) //set "Playing..." display message
			.buildBlocking();  //build the whole thing, blocking guarantees it will be completely loaded vs async which does not
			Hub.setup();
			
		}
		catch (LoginException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
