package akkamud;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.sql.PreparedStatement;
import java.util.Date;

import akka.actor.UntypedActor;

import static akkamud.ReportCommands.*;

class ReportLogger extends UntypedActor
{
	// Invariant strings
    private static final String PROGRESS_T = "progress";
    private static final String SUPERVISOR_T = "supervisor";
//    private static final String CRASH_T = "crash";
    private static final String TABLE_CREATE =
		"CREATE TABLE IF NOT EXISTS reports " +
				"(type STRING, " +
                "timestamp INTEGER, " +
                "supervisor STRING, " +
                "child STRING, " +
                "context STRING, " +
                "reason STRING, " +
                "ext STRING)";
    private static final String SUPERVISOR_INSERT = 
		"INSERT INTO REPORTS (type, timestamp, supervisor, child, context, reason, ext)" +
        " VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String PROGRESS_INSERT =
		"INSERT INTO REPORTS (type, timestamp, supervisor, child, context)" +
		" VALUES (?, ?, ?, ?, ?)";

    // Private member variables
    private Connection connection;
    private PreparedStatement supervisor_statement;
    private PreparedStatement progress_statement;

    // Constructor and initialization routines
    public ReportLogger(String dbFilename) throws ClassNotFoundException, SQLException
    { 
    	initDB(dbFilename);
    	supervisor_statement = this.connection.prepareStatement(SUPERVISOR_INSERT);
    	progress_statement = this.connection.prepareStatement(PROGRESS_INSERT);
	}
    private void initDB(String dbFilename) 
    throws ClassNotFoundException, SQLException
    {
        // load the sqlite-JDBC driver using the current class loader
        // no longer needed as of JDBC-4.0
//         Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:"+dbFilename);

        Statement statement = this.connection.createStatement();
        statement.executeUpdate(TABLE_CREATE);
    }

    // Akka reactive loop
    public void onReceive(Object message)
    throws ClassNotFoundException, SQLException
    {
    	if(message instanceof ProgressReport)
    		logProgress((ProgressReport)message);
    	if(message instanceof SupervisorReport)
    		logSupervisor((SupervisorReport)message);
    	unhandled(message);
    }
    
    // Implementation methods
    private void logProgress(ProgressReport m)
    throws ClassNotFoundException, SQLException
    {
        long timestamp = new Date().getTime();
        
        // timestamp, supervisor(name), child(name), context(stage of starting if applicable), reason(null), EXT:null
        progress_statement.setString(1, PROGRESS_T);
        progress_statement.setLong(2, timestamp);
        progress_statement.setString(3, m.supervisor);
        progress_statement.setString(4, m.child);
        progress_statement.setString(5, m.context);

        progress_statement.executeUpdate();
    }
    private void logSupervisor(SupervisorReport m)
    throws ClassNotFoundException, SQLException
    {
        long timestamp = new Date().getTime();
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        m.reason.printStackTrace(pw);
        String reasonString = sw.toString(); // stack trace as a string

        // timestamp, supervisor(name), child(name), context(was it all the way started?), reason(trace), EXT:disposition(restart, resume, stop, escalate)
        supervisor_statement.setString(1, SUPERVISOR_T);
        supervisor_statement.setLong(2, timestamp);
        supervisor_statement.setString(3, m.supervisor);
        supervisor_statement.setString(4, m.child);
        supervisor_statement.setString(5, m.context);
        supervisor_statement.setString(6, reasonString);
        supervisor_statement.setString(7, m.ext);

        supervisor_statement.executeUpdate();
    }
}
