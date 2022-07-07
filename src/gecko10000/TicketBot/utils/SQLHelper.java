package gecko10000.TicketBot.utils;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.Closeable;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Wraps a {@link Connection} and offers helpful methods that don't need to be surrounded in a try/catch
 * @author Redempt
 * Credit where due: https://github.com/Redempt/RedLib/blob/master/src/redempt/redlib/sql/SQLHelper.java
 */
public class SQLHelper implements Closeable {

    /**
     * Opens a SQLite database file
     * @param file The path to the SQLite database file
     * @return The Connection to this SQLite database
     */
    public static Connection openSQLite(Path file) {
        try {
            Class.forName("org.sqlite.JDBC");

            final Properties properties = new Properties();
            properties.setProperty("foreign_keys", "on");
            properties.setProperty("busy_timeout", "1000");

            return DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath(), properties);
        } catch (ClassNotFoundException | SQLException e) {
            sneakyThrow(e);
            return null;
        }
    }

    /**
     * Opens a connection to a MySQL database
     * @param ip The IP address to connect to
     * @param port The port to connect to
     * @param username The username to log in with
     * @param password The password to log in with
     * @param database The database to use, will be created if it doesn't exist
     * @return The Connection to the MySQL database
     */
    public static Connection openMySQL(String ip, int port, String username, String password, String database) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://" + ip + ":" + port + "/?user=" + username + "&password=" + password);
            connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS " + database + ";");
            connection.createStatement().execute("USE " + database + ";");
            return connection;
        } catch (ClassNotFoundException | SQLException e) {
            sneakyThrow(e);
            return null;
        }
    }

    /**
     * Opens a connection to a MySQL database at localhost:3306
     * @param username The username to log in with
     * @param password The password to log in with
     * @param database The database to use, will be created if it doesn't exist
     * @return The Connection to the MySQL database
     */
    public static Connection openMySQL(String username, String password, String database) {
        return openMySQL("localhost", 3306, username, password, database);
    }

    private static <T extends Exception> void sneakyThrow(Exception e) throws T {
        throw (T) e;
    }

    private Connection connection;
    private Disposable commitTask = null;

    /**
     * Constructs a SQLHelper from a Connection. Get the Connection using one of the static SQLHelper open methods.
     * @param connection The SQL Connection to wrap
     */
    public SQLHelper(Connection connection) {
        this.connection = connection;
    }

    /**
     * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed
     * @param command The SQL command to execute
     * @param fields A vararg of the fields to set in the prepared statement
     */
    public void execute(String command, Object... fields) {
        try {
            PreparedStatement statement = prepareStatement(command, fields);
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            sneakyThrow(e);
        }
    }

    /**
     * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed
     * @author U9G
     * @param command The SQL command to execute
     * @param fields A vararg of the fields to set in the prepared statement
     * @return The number of updated rows
     */
    public int executeUpdate(String command, Object... fields) {
        int updatedRows = 0;
        try {
            PreparedStatement statement = prepareStatement(command, fields);
            updatedRows = statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            sneakyThrow(e);
        }
        return updatedRows;
    }

    /**
     * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
     * returning the value in the first column of the first row in the results
     * @param query The SQL query to execute
     * @param fields A vararg of the fields to set in the prepared statement
     * @param <T> The type to cast the return value to
     * @return The value in the first column of the first row of the returned results, or null if none is present
     */
    public <T> T querySingleResult(String query, Object... fields) {
        try {
            PreparedStatement statement = prepareStatement(query, fields);
            ResultSet results = statement.executeQuery();
            if (!results.next()) {
                return null;
            }
            T obj = (T) results.getObject(1);
            results.close();
            statement.close();
            return obj;
        } catch (SQLException e) {
            sneakyThrow(e);
            return null;
        }
    }

    /**
     * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
     * returning the value in the first column of the first row in the results as a String.
     * @param query The SQL query to execute
     * @param fields A vararg of the fields to set in the prepared statement
     * @return The String in the first column of the first row of the returned results, or null if none is present
     * Note: This method exists because {@link ResultSet#getObject(int)} can return an Integer if the String in the
     * column can be parsed into one.
     */
    public String querySingleResultString(String query, Object... fields) {
        try {
            PreparedStatement statement = prepareStatement(query, fields);
            ResultSet results = statement.executeQuery();
            if (!results.next()) {
                return null;
            }
            String val = results.getString(1);
            results.close();
            statement.close();
            return val;
        } catch (SQLException e) {
            sneakyThrow(e);
            return null;
        }
    }

    /**
     * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
     * returning the value in the first column of the first row in the results as a Bytes.
     * @param query The SQL query to execute
     * @param fields A vararg of the fields to set in the prepared statement
     * @return The bytes in the first column of the first row of the returned results, or null if none is present
     */
    public byte[] querySingleResultBytes(String query, Object... fields) {
        try {
            PreparedStatement statement = prepareStatement(query, fields);
            ResultSet results = statement.executeQuery();
            if (!results.next()) {
                return null;
            }
            byte[] val = results.getBytes(1);
            results.close();
            statement.close();
            return val;
        } catch (SQLException e) {
            sneakyThrow(e);
            return null;
        }
    }

    /**
     * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
     * returning the value in the first column of the first row in the results as a Long.
     * @param query The SQL query to execute
     * @param fields A vararg of the fields to set in the prepared statement
     * @return The String in the first column of the first row of the returned results, or null if none is present
     * Note: This method exists because {@link ResultSet#getObject(int)} can return an Integer if the Long in the
     * column can be parsed into one.
     */
    public Long querySingleResultLong(String query, Object... fields) {
        try {
            PreparedStatement statement = prepareStatement(query, fields);
            ResultSet results = statement.executeQuery();
            if (!results.next()) {
                return null;
            }
            long val = results.getLong(1);
            results.close();
            statement.close();
            return val;
        } catch (SQLException e) {
            sneakyThrow(e);
            return null;
        }
    }

    /**
     * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
     * returning a list of values in the first column of each row in the results
     * @param query The SQL query to execute
     * @param fields A vararg of the fields to set in the prepared statement
     * @param <T> The type to populate the list with and return
     * @return A list of the value in the first column of each row returned by the query
     */
    public <T> List<T> queryResultList(String query, Object... fields) {
        List<T> list = new ArrayList<>();
        try {
            PreparedStatement statement = prepareStatement(query, fields);
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                list.add((T) results.getObject(1));
            }
            results.close();
            statement.close();
        } catch (SQLException e) {
            sneakyThrow(e);
        }
        return list;
    }

    /**
     * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
     * returning a String list of values in the first column of each row in the results
     * @param query The SQL query to execute
     * @param fields A vararg of the fields to set in the prepared statement
     * @return A String list of the value in the first column of each row returned by the query
     * Note: This method exists because {@link ResultSet#getObject(int)} can return an Integer if the String in the
     * column can be parsed into one.
     */
    public List<String> queryResultStringList(String query, Object... fields) {
        List<String> list = new ArrayList<>();
        try {
            PreparedStatement statement = prepareStatement(query, fields);
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                list.add(results.getString(1));
            }
            results.close();
            statement.close();
        } catch (SQLException e) {
            sneakyThrow(e);
        }
        return list;
    }

    /**
     * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed.
     * Returns a {@link Results}, which wraps a {@link ResultSet} for easier use
     * @param query The SQL query to execute
     * @param fields A vararg of the fields to set in the prepared statement
     * @return The results of the query
     */
    public Results queryResults(String query, Object... fields) {
        try {
            PreparedStatement statement = prepareStatement(query, fields);
            ResultSet results = statement.executeQuery();
            return new Results(results, statement);
        } catch (SQLException e) {
            sneakyThrow(e);
            return null;
        }
    }

    /**
     * @return The Connection this SQLHelper wraps
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Sets the wrapped connection's auto-commit property. Calling this method will automatically disable
     * the task started by {@link SQLHelper#setCommitInterval(int)}.
     * @param autoCommit The auto-commit property - whether it will commit with every command
     */
    public void setAutoCommit(boolean autoCommit) {
        try {
            setCommitInterval(-1);
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            sneakyThrow(e);

        }
    }

    /**
     * @return The auto-commit property of the wrapped connection
     */
    public boolean isAutoCommit() {
        try {
            return connection.getAutoCommit();
        } catch (SQLException e) {
            sneakyThrow(e);
            return false;
        }
    }

    /**
     * Starts a task to call commit() on this SQLHelper every n seconds. Pass -1 to disable.
     * Automatically sets autoCommit to false.
     * @param seconds The number of seconds between commits, or -1 to disable
     */
    public void setCommitInterval(int seconds) {
        if (commitTask != null) {
            commitTask.dispose();
            commitTask = null;
        }
        if (seconds == -1) {
            return;
        }
        setAutoCommit(false);
        commitTask = Flux.interval(Duration.ofSeconds(seconds)).subscribe(ignored -> this.commit());
    }

    /**
     * Flushes all caches and commits the transaction
     */
    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            sneakyThrow(e);
        }
    }

    /**
     * Prepares a statement, setting its fields to the elements of the vararg passed
     * @param query The SQL query to prepare
     * @param fields A vararg of the fields to set in the prepared statement
     * @return The PreparedStatement with its fields set
     */
    public PreparedStatement prepareStatement(String query, Object... fields) {
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            int i = 1;
            for (Object object : fields) {
                statement.setObject(i, object);
                i++;
            }
            return statement;
        } catch (SQLException e) {
            sneakyThrow(e);
            return null;
        }
    }

    /**
     * Closes the underlying connection this SQLHelper wraps
     */
    @Override
    public void close() {
        try {
            setCommitInterval(-1);
            connection.close();
            connection = null;
            System.gc();
        } catch (SQLException e) {
            sneakyThrow(e);
        }
    }

    /**
     * Wraps a {@link ResultSet} with easier use
     * @author Redempt
     */
    public static class Results implements AutoCloseable {

        private ResultSet results;
        private boolean empty;
        private PreparedStatement statement;

        private Results(ResultSet results, PreparedStatement statement) {
            this.results = results;
            this.statement = statement;
            try {
                empty = !results.next();
            } catch (SQLException e) {
                sneakyThrow(e);
            }
        }

        /**
         * @return False if the first call of {@link ResultSet#next()} on the wrapped ResultSet returned false,
         * true otherwise
         */
        public boolean isEmpty() {
            return empty;
        }

        /**
         * Moves to the next row in the wrapped ResultSet. Note that this method is called immediately when the
         * Results object is constructed, and does not need to be called to retrieve the items in the first row.
         * @return True if there is another row available in the wrapped ResultSet
         */
        public boolean next() {
            try {
                return results.next();
            } catch (SQLException e) {
                sneakyThrow(e);
                return false;
            }
        }

        /**
         * Performs an operation on every row in these Results, passing itself each time it iterates to a new row
         * @param lambda The callback to be run on every row in these Results
         */
        public void forEach(Consumer<Results> lambda) {
            if (isEmpty()) {
                return;
            }
            lambda.accept(this);
            while (next()) {
                lambda.accept(this);
            }
            close();
        }

        /**
         * Gets an Object in the given column in the current row
         * @param column The index of the column to get, starting at 1
         * @param <T> The type to cast the return value to
         * @return The value in the column
         */
        public <T> T get(int column) {
            try {
                return (T) results.getObject(column);
            } catch (SQLException e) {
                sneakyThrow(e);
                return null;
            }
        }

        /**
         * Gets the bytes in the given column in the current row
         * @param column The index of the column to get, starting at 1
         * @return The bytes in the column
         */
        public byte[] getBytes(int column) {
            try {
                return results.getBytes(column);
            } catch (SQLException e) {
                sneakyThrow(e);
                return null;
            }
        }

        /**
         * Gets a String in the given column in the current row
         * @param column The index of the column to get, starting at 1
         * @return The String in the column
         * Note: This method exists because {@link ResultSet#getObject(int)} can return an Integer if the String in the
         * column can be parsed into one.
         */
        public String getString(int column) {
            try {
                return results.getString(column);
            } catch (SQLException e) {
                sneakyThrow(e);
                return null;
            }
        }

        /**
         * Gets a Long in the given column in the current row
         * @param column The index of the column to get, starting at 1
         * @return The String in the column
         * Note: This method exists because {@link ResultSet#getObject(int)} can return an Integer if the Long in the
         * column can be parsed into one.
         */
        public Long getLong(int column) {
            try {
                return results.getLong(column);
            } catch (SQLException e) {
                sneakyThrow(e);
                return null;
            }
        }

        /**
         * Gets the column count from the returned data
         * @return The column count
         */
        public int getColumnCount() {
            try {
                return results.getMetaData().getColumnCount();
            } catch (SQLException e) {
                sneakyThrow(e);
                return 0;
            }
        }

        /**
         * Closes the wrapped ResultSet. Call this when you are done using these Results.
         */
        @Override
        public void close() {
            try {
                results.close();
                statement.close();
            } catch (SQLException e) {
                sneakyThrow(e);
            }
        }

    }

}
