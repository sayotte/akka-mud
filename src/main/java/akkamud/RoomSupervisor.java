/**
 * 
 */
package akkamud;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import akkamud.reporting.ReportLogger;
import akkamud.reporting.ReportingOneForOneStrategy;

import static akkamud.EntityCommand.*;
/**
 * @author stephen.ayotte
 *
 */
class RoomSupervisor extends UntypedActor {
	private ReportLogger logger;
	
	public RoomSupervisor()
	{
		System.out.println(self().path().name() + ": running");
		logger = ReportLogger.getLogger();
	}
	
    private final Function<Throwable, Directive> decider = 
        new Function<Throwable, Directive>()
        {
            public Directive apply(Throwable t)
            {
            	System.out.println("Room supervisor, I caught an exception of type '" + t.getClass().getSimpleName() + "':\n" + t);                	
                return escalate();
            }
        };
    private final SupervisorStrategy strategy = 
            new ReportingOneForOneStrategy(10,                           // max retries
                                           Duration.create(1, "minute"), // within this time period
                                           decider);                     // with this "decider" for handling
    @Override
    public SupervisorStrategy supervisorStrategy()
    {
        return strategy;
    }

    public void onReceive(Object message)
    {
    	if(message instanceof LoadRooms)
    		loadRooms();
    	else
    		unhandled(message);
    }
    
    private void loadRooms()
    {
    	try
    	{
	    	Connection connection = DriverManager.getConnection("jdbc:sqlite:rooms.db");
	    	Statement stmt = connection.createStatement();
	    	ResultSet rs = stmt.executeQuery("SELECT * FROM ROOMS");
	    	while(rs.next())
	    	{
	    		int id = rs.getInt("ID");
	    		ActorRef room = getContext().actorOf(Props.create(Room.class), 
	    											 "room" + id);
	    	}
    	}
    	catch(Exception e)
    	{
    		System.out.println(self().path().name() + ": caught an exception in loadRooms(): " + e);
    		getContext().system().shutdown();
    	}
    }
}
