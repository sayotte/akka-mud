package akkamud;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class StructuredLogger
{
    public static final String PROGRESS_T = "progress";
    // timestamp, supervisor, child
    public static final String SUPERVISOR_T = "supervisor";
    // timestamp, supervisor, context(was it all the way started?), reason(trace), offender(child or spec)
    // disposition(restart, resume, stop, escalate)
    public static final String CRASH_T = "crash";
    // timestamp, crasher(name, reason, message queue), neighbors

    private static StructuredLogger logger = new StructuredLogger();
    private static Connection connection;

    private StructuredLogger()
    {
        this.connection = null;
    }

    public static StructuredLogger getLogger()
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
        connection = DriverManager.getConnection("jdbc:sqlite:logs.db");
        Statement statement = this.connection.createStatement();
        statement.setQueryTimeout(30);  // set timeout to 30 sec.

//             statement.executeUpdate("drop table if exists events");
        statement.executeUpdate("create table if not exists events " +
                                                    "(id integer, " +
                                                    "timestamp integer, " +
                                                    "logger string, " +
                                                    "type string, " +
                                                    "message string, " +
                                                    "payload blob)");
        //statement.executeUpdate("insert into person values(1, 'leo')");
        //statement.executeUpdate("insert into person values(2, 'yui')");
//             ResultSet rs = statement.executeQuery("select * from events");
//             while(rs.next())
//             {
//                 // read the result set
//                 System.out.println("name = " + rs.getString("logger"));
//                 System.out.println("id = " + rs.getInt("timestamp"));
//             }
    }

    public void logProgress(String supervisor, String child)
    throws ClassNotFoundException, SQLException
    {
        this.initDB();
        Statement statement = this.connection.createStatement();
        statement.setQueryTimeout(1);  // set timeout to 30 sec.
        statement.executeUpdate("insert into events values(timestamp, type, 

//         statement.executeUpdate("create table if not exists events " +
//                                                     "(id integer, " +
//                                                     "timestamp integer, " +
//                                                     "logger string, " +
//                                                     "type string, " +
//                                                     "message string, " +
//                                                     "payload blob)");
    }

    public void logEvent(String logger, String type, String message, Object payload)
    {
        return;
    }

}