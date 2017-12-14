package uk.ac.exeter.QuinCe.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A utility class providing useful methods for dealing with
 * database-related objects
 * 
 * @author Steve Jones
 */
public class DatabaseUtils {

	/**
	 * Indicates that this item does not have a database ID, implying
	 * that it is not (yet) stored in the database.
	 */
	public static final int NO_DATABASE_RECORD = -1;
	
	/**
	 * Token to mark the place where the parameters for an IN clause should be in an SQL string.
	 * @see #makeInStatementSql(String, int)
	 */
	public static final String IN_PARAMS_TOKEN = "%%IN_PARAMS%%";
	
	/**
	 * Close a set of {@link java.sql.ResultSet} objects, ignoring any errors
	 * @param results The ResultSets
	 */
	public static void closeResultSets(List<ResultSet> results) {
		for (ResultSet result : results) {
			if (null != result) {
				try {
					result.close();
				} catch(SQLException e) {
					// Do nothing
				}
			}
		}			
	}
	
	/**
	 * Close a set of {@link java.sql.ResultSet} objects, ignoring any errors
	 * @param results The ResultSets
	 */
	public static void closeResultSets(ResultSet... results) {
		closeResultSets(Arrays.asList(results));
	}
	
	/**
	 * Close a set of {@link java.sql.PreparedStatement} objects, ignoring any errors
	 * @param statements The PreparedStatements
	 */
	public static void closeStatements(List<PreparedStatement> statements) {
		for (PreparedStatement stmt : statements) {
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}
	}
	
	/**
	 * Close a set of {@link java.sql.PreparedStatement} objects, ignoring any errors
	 * @param statements The statements
	 */
	public static void closeStatements(PreparedStatement... statements) {
		closeStatements(Arrays.asList(statements));
	}
	
	/**
	 * Close a database connection, ignoring any errors.
	 * All connections have their auto-commit flag set to true.
	 * @param conn The database connection
	 */
	public static void closeConnection(Connection conn) {
		if (null != conn) {
			try {
				if (!conn.getAutoCommit()) {
					conn.rollback();
				}
				conn.close();
			} catch (SQLException e) {
				// Do nothing
			}
		}
	}
	
	/**
	 * Roll back an open transaction
	 * @param conn The database connection
	 */
	public static void rollBack(Connection conn) {
		if (null != conn) {
			try {
				conn.rollback();
			} catch (SQLException e) {
				// DO nothing
			}
		}
	}
	
	/**
	 * Retrieve a date/time from the database. For the actual data, all times are recorded in UTC,
	 * so this method ensures that the retrieved {@link java.util.Calendar} object is in UTC.
	 * 
	 * @param records The results from which the date/time must be retrieved
	 * @param columnIndex The colum index of the date/time
	 * @return The date/time in UTC
	 * @throws SQLException If an error occurs while reading from the database record
	 * @see java.util.Calendar#getInstance(TimeZone, Locale)
	 */
	@Deprecated
	public static Calendar getUTCDateTime(ResultSet records, int columnIndex) throws SQLException {
		Calendar result = DateTimeUtils.getUTCCalendarInstance();
		result.setTimeInMillis(records.getTimestamp(columnIndex).getTime());
		return result;
	}
	
	/**
	 * Create an insert statement for a table and list of fields
	 * @param conn A database connection
	 * @param table The table
	 * @param fields The fields
	 * @return The query
	 * @throws MissingParamException If any required parameters are missing
	 * @throws SQLException If the statement cannot be created
	 */
	public static PreparedStatement createInsertStatement(Connection conn, String table, List<String> fields) throws MissingParamException, SQLException {
		return createInsertStatement(conn, table, fields, Statement.NO_GENERATED_KEYS);
	}

	/**
	 * Create an insert statement for a table and list of fields
	 * @param conn A database connection
	 * @param table The table
	 * @param fields The fields
	 * @param autoGeneratedKeys Indicates whether generated keys should be available from the query
	 * @return The query
	 * @throws MissingParamException If any required parameters are missing
	 * @throws SQLException If the statement cannot be created
	 */
	public static PreparedStatement createInsertStatement(Connection conn, String table, List<String> fields, int autoGeneratedKeys) throws MissingParamException, SQLException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkMissing(table, "table");
		MissingParam.checkMissing(fields, "fields");
		
		StringBuilder fieldList = new StringBuilder();
		StringBuilder parameterList = new StringBuilder();
		
		for (int i = 0; i < fields.size(); i++) {
			fieldList.append(fields.get(i));
			parameterList.append('?');
			
			if (i < fields.size() - 1) {
				fieldList.append(',');
				parameterList.append(',');
			}
		}
		
		StringBuilder query = new StringBuilder();
		
		query.append("INSERT INTO ");
		query.append(table);
		query.append(" (");
		query.append(fieldList);
		query.append(") VALUES (");
		query.append(parameterList);
		query.append(')');
		
		return conn.prepareStatement(query.toString(), autoGeneratedKeys);
	}
	
	/**
	 * Prepare a select statement
	 * @param conn A database connection
	 * @param table The table name
	 * @param queryFields The fields to retrieve
	 * @param andFields The WHERE parameters (ANDed together)
	 * @return The prepared query
	 * @throws MissingParamException If any required parameters are missing
	 * @throws SQLException If the query cannot be created
	 */
	public static PreparedStatement createSelectStatement(Connection conn, String table, List<String> queryFields, List<String> andFields) throws MissingParamException, SQLException {
		return createSelectStatement(conn, table, queryFields, andFields, -1, -1);
	}

	/**
	 * Prepare a select statement
	 * @param conn A database connection
	 * @param table The table name
	 * @param queryFields The fields to retrieve
	 * @param andFields The WHERE parameters (ANDed together)
	 * @return The prepared query
	 * @throws MissingParamException If any required parameters are missing
	 * @throws SQLException If the query cannot be created
	 */
	public static PreparedStatement createSelectStatement(Connection conn, String table, List<String> queryFields, List<String> andFields, int start, int length) throws MissingParamException, SQLException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkMissing(table, "table");
		MissingParam.checkMissing(queryFields, "queryFields");

		StringBuilder queryFieldList = new StringBuilder();
		
		for (int i = 0; i < queryFields.size(); i++) {
			queryFieldList.append(queryFields.get(i));
			
			if (i < queryFields.size() - 1) {
				queryFieldList.append(',');
			}
		}
		
		StringBuilder andFieldList = new StringBuilder();
		StringBuilder andParameterList = new StringBuilder();

		if (null != andFields) {
			for (int i = 0; i < andFields.size(); i++) {
				andFieldList.append(andFields.get(i));
				andParameterList.append('?');
				
				if (i < andFields.size() - 1) {
					andFieldList.append(',');
					andParameterList.append(',');
				}
			}
		}

		StringBuilder query = new StringBuilder();
		
		query.append("SELECT ");
		query.append(queryFieldList.toString());
		query.append(" FROM ");
		query.append(table);
		if (null != andFields) {
			query.append(" WHERE ");
			
			for (int i = 0; i < andFields.size(); i++) {
				if (i > 0) {
					query.append(" AND ");
				}
				
				query.append(andFields.get(i));
				query.append(" = ?");
			}
			
		}
		
		if (length > 0) {
			query.append(" LIMIT ");
			query.append(start);
			query.append(',');
			query.append(length);
		}
		
		return conn.prepareStatement(query.toString());
	}

	/**
	 * Construct an SQL Prepared Statement string that contains an IN parameter
	 * with the given number of parameters.
	 * 
	 * The query must contain an {@link #IN_PARAMS_TOKEN}.
	 * 
	 * @param query The query
	 * @param inSize The number of items in the IN parameter
	 * @return The generated SQL statement
	 */
	public static String makeInStatementSql(String query, int inSize) {
		
		StringBuilder inParams = new StringBuilder();
		inParams.append('(');
		for (int i = 0; i < inSize; i++) {
			inParams.append("?,");
		}
		inParams.deleteCharAt(inParams.length() - 1);

		inParams.append(')');
		
		return query.replaceAll(IN_PARAMS_TOKEN, inParams.toString());
	}


	/**
	 * Get the database field name for a human-readable data field name
	 * 
	 * <p>
	 *   The database field name is the full name
	 *   converted to lower case and with spaces replaced by
	 *   underscores. Brackets and other odd characters that
	 *   upset MySQL are removed.
	 * </p>
	 *
	 * @return The database field name
	 */
	public static String getDatabaseFieldName(String fullName) {
		String result = null;
		if (null != fullName) {
			result = fullName.replaceAll(" ", "_").replaceAll("[\\(\\)]", "").toLowerCase();
		}

		return result;
	}
}
