package de.jxdev.legendarycraft.v3.data.db;

import de.jxdev.legendarycraft.v3.data.db.util.RowMapper;
import de.jxdev.legendarycraft.v3.data.db.util.SQLFunction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public interface IDatabaseService {

    /**
     * Initializes the database.
     */
    void init() throws SQLException;

    /**
     * Closes the database.
     */
    void close() throws SQLException;


    /**
     * Executes a database operation within a transaction.
     *
     * @param work Function that performs database operations using the connection
     * @param <T>  The type of result returned by the transaction
     * @return Result of type T from the transaction
     */
    <T> T inTransaction(SQLFunction<Connection, T> work) throws SQLException;

    /**
     * Executes a SQL query and maps a single result row.
     *
     * @param sql    The SQL query to execute
     * @param mapper RowMapper to convert result set to object
     * @param params Query parameters to be set
     * @param <T>    The type of object being mapped to
     * @return Optional containing the mapped object if found, empty if no results
     * @throws SQLException if a database error occurs
     */
    <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) throws SQLException;

    /**
     * Executes a SQL query and maps multiple result rows to a Set.
     *
     * @param sql    The SQL query to execute
     * @param mapper RowMapper to convert result set to objects
     * @param params Query parameters to be set
     * @param <T>    The type of objects being mapped to
     * @return Set of mapped objects
     * @throws SQLException if a database error occurs
     */
    <T> Set<T> querySet(String sql, RowMapper<T> mapper, Object... params) throws SQLException;

    /**
     * Executes a SQL query and maps multiple result rows to a List.
     *
     * @param sql    The SQL query to execute
     * @param mapper RowMapper to convert result set to objects
     * @param params Query parameters to be set
     * @param <T>    The type of objects being mapped to
     * @return List of mapped objects
     * @throws SQLException if a database error occurs
     */
    <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) throws SQLException;

    /**
     * Executes an update SQL statement (INSERT, UPDATE, DELETE).
     *
     * @param sql    The SQL statement to execute
     * @param params Statement parameters to be set
     * @return Number of rows affected
     * @throws SQLException if a database error occurs
     */
    int update(String sql, Object... params) throws SQLException;

    /**
     * Executes an insert SQL statement and returns the generated ID.
     *
     * @param sql    The SQL insert statement to execute
     * @param params Statement parameters to be set
     * @return Generated ID from the insert
     * @throws SQLException if a database error occurs
     */
    long insertAndReturnId(String sql, Object... params) throws SQLException;

    /**
     * Executes a batch of SQL statements with different parameters.
     *
     * @param sql    The SQL statement to execute as batch
     * @param params List of parameter arrays for each batch statement
     * @return Array containing the update counts for each statement
     * @throws SQLException if a database error occurs
     */
    int[] batch(String sql, List<Object[]> params) throws SQLException;

}
