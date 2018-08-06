package com.tybug.carboncopier;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Querying and updating the database. <br>
 * References to "source" are the guild that the action occurs on first, which is then carbon copied to the "target".
 * @author Liam DeVoe
 */
public class DBFunctions {

	
	
	
	
	
	/**
	 * @return {@literal HashMap<String, String> } <br>
	 * For each linked guild: <br>
	 * [SOURCE ID :: TARGET ID]
	 */
	public static HashMap<String, String> getLinkedGuilds(){
		return getMapFromDatabase("info", new String[] {"SOURCE", "TARGET"}, "SELECT * from 'GUILDS'");
	}
	
	
	
	/**
	 * @return {@literal HashMap<String, String> } <br>
	 * For each linked text channel: <br>
	 * [SOURCE ID :: TARGET ID]
	 */
	public static HashMap<String, String> getLinkedCategories() {
		return getMapFromDatabase("info", new String[] {"SOURCE", "TARGET"}, "SELECT * from 'CATEGORIES'");
	}
	
	
	
	/**
	 * @return {@literal HashMap<String, String> } <br>
	 * For each linked text channel: <br>
	 * [SOURCE ID :: TARGET ID]
	 */
	public static HashMap<String, String> getLinkedChannels() {
		return getMapFromDatabase("info", new String[] {"SOURCE", "TARGET"}, "SELECT * from 'CHANNELS'");
	}
	
	
	
	
	/**
	 * @return {@literal HashMap<String, String> } <br>
	 * For each linked role: <br>
	 * [SOURCE ID :: TARGET ID]
	 */
	public static HashMap<String, String> getLinkedRoles(){
		return getMapFromDatabase("info", new String[] {"SOURCE", "TARGET"}, "SELECT * FROM 'ROLES'");
	}
	
	
	
	/**
	 * @return {@literal List<String> } A list containing the ids of the target guilds
	 */
	public static List<String> getTargetGuilds() {
		return getListFromDatabase("info", "TARGET", "SELECT * FROM 'GUILDS'");
	}
	
	
	
	/**
	 * @return {@literal List<String> } A list containing the ids of users who can use commands
	 */
	public static List<String> getAuthorizedUsers(){
		return getListFromDatabase("info", "ID", "SELECT * FROM 'AUTHORIZED'");
	}
	
	
	
	/**
	 * @param id The SOURCE message id
	 * @return The TARGET id linked to the SOURCE id
	 */
	public static String getLinkedMessage(String id) {
		return getStringFromDatabase("info", "TARGET", "SELECT * FROM `MESSAGES` WHERE `SOURCE` = \"" + id + "\"");
	}
	
	
	
	
	
	/**
	 * @param id The SOURCE role id
	 * @return The TARGET id linked to the SOURCE id
	 */
	public static String getLinkedRole(String id) {
		return getStringFromDatabase("info", "TARGET", "SELECT * FROM `ROLES` WHERE `SOURCE` = \"" + id + "\"");
	}
	

	
	
	
	
	/**
	 * Links two messages
	 * <p>
	 * Inserts values (source, target) into the MESSAGES table in the INFO database
	 * @param source The source message id
	 * @param target The target message id
	 */
	public static void linkMessage(String source, String target) {
		modifyDatabase("info", Arrays.asList(source, target), "INSERT INTO 'MESSAGES' ('SOURCE', 'TARGET') VALUES (?, ?)");
	}
	
	
	
	
	
	/**
	 * Links two roles
	 * <p>
	 * Inserts values (source, target) into the ROLES table in the INFO database
	 * @param source The source role id
	 * @param target The target role id
	 */
	public static void linkRole(String source, String target) {
		modifyDatabase("info", Arrays.asList(source, target), "INSERT INTO 'ROLES' ('SOURCE', 'TARGET') VALUES (?, ?)");
	}
	
	
	
	
	/**
	 * Links two channels
	 * <p>
	 * Inserts values (source, target) into the CHANNELS table in the INFO database
	 * @param source The source channel id
	 * @param target The target channel id
	 */
	public static void linkChannel(String source, String target) {
		modifyDatabase("info", Arrays.asList(source, target), "INSERT INTO 'CHANNELS' ('SOURCE', 'TARGET') VALUES (?, ?)");
	}
	
	
	
	
	/**
	 * Links two guilds
	 * <p>
	 * Inserts values (source, target) into the GUILDS table in the INFO database
	 * @param source The source guild id
	 * @param target The target guild id
	 */
	public static void linkGuild(String source, String target) {
		modifyDatabase("info", Arrays.asList(source, target), "INSERT INTO 'GUILDS' ('SOURCE', 'TARGET') VALUES (?, ?)");
	}
	
	
	
	/**
	 * Modifies a pre-existing link between two roles
	 * <p>
	 * Updates the previous TARGET value to the passed TARGET value where they both share the SOURCE value
	 * @param source The source role id
	 * @param target The target role id
	 */
	public static void updateRoleLink(String source, String target) {
		modifyDatabase("info", Arrays.asList(target, source), "UPDATE `ROLES` SET `TARGET` = ? WHERE `SOURCE` = ?");
	}
	
	
	
	public static void deleteRoleLink(String source) {
		modifyDatabase("info", Arrays.asList(source), "DELETE FROM `ROLES` WHERE `SOURCE` = ?");
	}
	
	
	
	
	
	
	/**
	 * Changes the content of the given database according to the given sql statement
	 * @param database The name of the database to modify
	 * @param args The strings to insert into the prepared sql statement
	 * @param sql The sql statement to execute
	 */
	public static void modifyDatabase(String database, List<String> args, String sql) {
		Connection c = null;
		PreparedStatement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:db/" + database + ".db");
			c.setAutoCommit(false);
			stmt = c.prepareStatement(sql); 
			for(String s : args) {
				stmt.setString(args.indexOf(s) + 1, s);
			}
			stmt.executeUpdate();
			c.commit();

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		} finally {
			close(stmt, c);
		}
	}
	
	
	
	/**
	 * Creates a map of String to String from the given database
	 * @param database The name of the database to look through
	 * @param sql The sql statement to execute
	 * @param arg An array, the first element is the column name to get the key value from and the second element is the column name to get the map value from
	 * @return Map<String, String> A map of String to String of the args from the database
	 */
	public static HashMap<String, String> getMapFromDatabase(String database, String[] arg, String sql) {
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:db/" + database + ".db");
			c.setAutoCommit(false);
			
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			HashMap<String, String> ret = new HashMap<String, String>();
			while (rs.next()) {
				ret.put(rs.getString(arg[0]), rs.getString(arg[1]));
			}
			return ret;
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			return null;
		} finally {
			close(stmt, c);
		}
	}
	
	
	
	
	
	/**
	 * Gets a list of Strings with the given condition from a database
	 * @param database The name of the database to look through
	 * @param sql The sql statement to execute
	 * @param arg The name of the column to add contents to the list from
	 * @return ArrayList A list of items returned by the sql statement from the table specified by arg
	 */
	public static ArrayList<String> getListFromDatabase(String database, String arg, String sql) {
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:db/" + database + ".db");
			c.setAutoCommit(false);
			
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			ArrayList<String> ret = new ArrayList<String>();
			while (rs.next()) {
				ret.add(rs.getString(arg));
			}
			return ret;
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			return null;
		} finally {
			close(stmt, c);
		}
	}
	
	
	
	
	/**
	 * Gets a single string with the given condition from a database
	 * @param database The name of the database to look through
	 * @param column The name of the column to add contents to the list from
	 * @param sql The sql statement to execute
	 * @return String The first item returned by the sql statement, selected from the column named
	 */
	public static String getStringFromDatabase(String database, String column, String sql) {
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:db/" + database + ".db");
			c.setAutoCommit(false);
			
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			String ret = null;
			while (rs.next()) {
				ret = rs.getString(column);
			}
			return ret;
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			return null;
		} finally {
			close(stmt, c);
		}
	}
	
	
	
	
	/**
	 * Closes the given statement and connection.
	 * @param stmt The statement to close
	 * @param c The connection to close
	 * @return True if the operation was successful, false otherwise
	 */
	static boolean close(Statement stmt, Connection c) {
		try {
			stmt.close();
			c.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
