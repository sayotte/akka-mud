package akkamud.reporting;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.sql.PreparedStatement;
import java.util.Date;

public class ReportLogger
{
    public static final String PROGRESS_T = "progress";
    public static final String SUPERVISOR_T = "supervisor";
    public static final String CRASH_T = "crash";
    // timestamp, supervisor(null), child(name), context(null?), reason(trace if available), EXT: neighbors?? message queue??

    private static ReportLogger logger = new ReportLogger();
    private static Connection connection;

    private ReportLogger()
    {
        this.connection = null;
    }

    public static ReportLogger getLogger()
    {
        return logger;
    }

    private void initDB() 
    throws ClassNotFoundException, SQLException
    {
        if(this.connection != null)
        {
            return;
        }

        // load the sqlite-JDBC driver using the current class loader
        // no longer needed as of JDBC-4.0
//         Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:reports.db");

        Statement statement = this.connection.createStatement();

        statement.executeUpdate("create table if not exists reports " +
                                                    "(type string, " +
                                                    "timestamp integer, " +
                                                    "supervisor string, " +
                                                    "child string, " +
                                                    "context string, " +
                                                    "reason blob, " +
                                                    "ext blob)");
    }

    public synchronized void logProgress(String supervisor, String child, String context)
    throws ClassNotFoundException, SQLException
    {
        // timestamp, supervisor(name), child(name), context(stage of starting if applicable), reason(null), EXT:null
        this.initDB();
        long timestamp = new Date().getTime();
        String statement_string =
          "INSERT INTO REPORTS (type, timestamp, supervisor, child, context)" +
          " VALUES (?, ?, ?, ?, ?)";
        PreparedStatement statement = this.connection.prepareStatement(statement_string);
        statement.setString(1, PROGRESS_T);
        statement.setLong(2, timestamp);
        statement.setString(3, supervisor);
        statement.setString(4, child);
        statement.setString(5, context);

        statement.executeUpdate();
    }

    public synchronized void logSupervisor(String supervisor, String child, String context, Throwable reason, String disposition)
    throws ClassNotFoundException, SQLException
    {
        // timestamp, supervisor(name), child(name), context(was it all the way started?), reason(trace), EXT:disposition(restart, resume, stop, escalate)
        this.initDB();
        long timestamp = new Date().getTime();
        String statement_string =
          "INSERT INTO REPORTS (type, timestamp, supervisor, child, context, reason, ext)" +
          " VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement statement = this.connection.prepareStatement(statement_string);
        statement.setString(1, SUPERVISOR_T);
        statement.setLong(2, timestamp);
        statement.setString(3, supervisor);
        statement.setString(4, child);
        statement.setString(5, context);
        statement.setObject(6, reason, java.sql.Types.BLOB);
        statement.setString(7, disposition);

        statement.executeUpdate();
    }


}