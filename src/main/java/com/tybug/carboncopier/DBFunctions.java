package com.tybug.carboncopier;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DBFunctions {

	
	
	
	
	
	
	
	/**
	 * @return HashMap<String, String> <br>
	 * For each text channel: <br>
	 * [ID :: carbon ID]
	 */
	public static HashMap<String, String> getLinkedChannels() {
		
		return getMapFromDatabase("info", new String[] {"SOURCE", "TARGET"}, "SELECT * from 'CHANNELS'");
		
	}
	
	
	public static List<String> getTargetGuilds() {
		return getListFromDatabase("info", "ID", "SELECT * FROM 'TARGETS'");
	}
	
	public static String getLinkedMessage(String id) {
		return getStringFromDatabase("info", "TARGET", "SELECT * WHERE 'SOURCE' = " + id);
	}
	
	
	
	public static void linkMessage(String source, String target) {
		//TODO
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
	 * @param arg The table to add contents to the list from
	 * @param sql The sql statement to execute
	 * @return String The first item returned by the sql statement, selected from the table with name arg
	 */
	public static String getStringFromDatabase(String database, String arg, String sql) {
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:db/" + database + ".db");
			c.setAutoCommit(false);
			
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				return rs.getString(arg);
			}
			return null;
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
