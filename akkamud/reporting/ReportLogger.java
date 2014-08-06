package akkamud.reporting;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        Class.forName("org.sqlite.JDBC");

        this.connection = null;
        // create a database connection
        connection = DriverManager.getConnection("jdbc:sqlite:reports.db");
        Statement statement = this.connection.createStatement();
        statement.setQueryTimeout(30);  // set timeout to 30 sec.

//             statement.executeUpdate("drop table if exists reports");
        statement.executeUpdate("create table if not exists reports " +
                                                    "(type string, " +
                                                    "timestamp integer, " +
                                                    "supervisor string, " +
                                                    "child string, " +
                                                    "context string, " +
                                                    "reason blob, " +
                                                    "ext blob)");
        //statement.executeUpdate("insert into person values(1, 'leo')");
        //statement.executeUpdate("insert into person values(2, 'yui')");
//             ResultSet rs = statement.executeQuery("select * from reports");
//             while(rs.next())
//             {
//                 // read the result set
//                 System.out.println("name = " + rs.getString("logger"));
//                 System.out.println("id = " + rs.getInt("timestamp"));
//             }
    }

    public void logProgress(String supervisor, String child, String context)
    throws ClassNotFoundException, SQLException
    {
        // timestamp, supervisor(name), child(name), context(stage of starting if applicable), reason(null), EXT:null
        this.initDB();
        Statement statement = this.connection.createStatement();
        statement.setQueryTimeout(1);  // set timeout to 30 sec.

        long timestamp = new Date().getTime();
        statement.executeUpdate("INSERT INTO REPORTS (type, timestamp, supervisor, child, context) " +
                                "VALUES ('" + PROGRESS_T + "', '" + timestamp + "', '" + 
                                              supervisor + "', '" + child + "', '" + context + "')");
    }

    public void logSupervisor(String supervisor, String child, String context, Throwable reason, String disposition)
    throws ClassNotFoundException, SQLException
    {
        // timestamp, supervisor(name), child(name), context(was it all the way started?), reason(trace), EXT:disposition(restart, resume, stop, escalate)
        this.initDB();
        Statement statement = this.connection.createStatement();
        statement.setQueryTimeout(1);  // set timeout to 30 sec.

        long timestamp = new Date().getTime();
        statement.executeUpdate("INSERT INTO REPORTS (type, timestamp, supervisor, child, context) " +
                                "VALUES ('" + PROGRESS_T + "', '" + timestamp + "', '" + 
                                              supervisor + "', '" + child + "', '" + context + "')");
    }

    public void logEvent(String logger, String type, String message, Object payload)
    {
        return;
    }

}