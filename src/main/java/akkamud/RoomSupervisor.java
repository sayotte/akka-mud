/**
 * 
 */
package akkamud;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import scala.collection.JavaConversions;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import akka.pattern.Patterns;
import static akkamud.EntityCommand.*;
import static akkamud.ReportCommands.*;
/**
 * @author stephen.ayotte
 *
 */
class RoomSupervisor extends UntypedActor {
	private final ActorRef reportLogger;
	private final SupervisorStrategy strategy;
	
	public RoomSupervisor(ActorRef newReportLogger)
	{
		System.out.println(self().path().name() + ": running");
		reportLogger = newReportLogger;
		strategy = new ReportingOneForOneStrategy(10,                           // max retries
                								  Duration.create(1, "minute"), // within this time period
									              decider,                      // with this "decider" for handling
									              this.reportLogger);
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
        	long startTime = System.nanoTime();
        	
	    	Connection connection = DriverManager.getConnection("jdbc:sqlite:rooms.db");
	    	Statement stmt = connection.createStatement();
	    	ResultSet rs = stmt.executeQuery("SELECT * FROM ROOMS");
	    	long endTime = System.nanoTime();
	    	long durationMS = (endTime - startTime) / 1000000;
	    	System.out.println(self().path().name() + ": completed room query in " +durationMS+ "ms");
	    	
	    	while(rs.next())
	    	{
	    		startTime = System.nanoTime();
	    		int id = rs.getInt("ID");
//	    		System.out.println(self().path().name() + ": creating child room" + id);
	    		ActorRef room = getContext().actorOf(Props.create(Room.class), 
	    											 "room" + id);
	    		RoomState state = new RoomState();
	    		state.name = rs.getString("name");
	    		state.grossDescription = rs.getString("grossDescription");

	    		String basePath = "/user/room-supervisor/room";
	    		int exitId = rs.getInt("northexit");
	    		if(exitId != 0)
	    			state.northExitPath = basePath + exitId;
	    		exitId = rs.getInt("eastexit");
	    		if(exitId != 0)
	    			state.eastExitPath = basePath + exitId;
	    		exitId = rs.getInt("southexit");
	    		if(exitId != 0)
	    			state.southExitPath = basePath + rs.getInt("southexit");
	    		exitId = rs.getInt("westexit");
	    		if(exitId != 0)
	    			state.westExitPath = basePath + rs.getInt("westexit");
	    		
	    		Future<Object> f = Patterns.ask(room,  state, 5000);
	    		Await.ready(f,  Duration.create(5000, TimeUnit.MILLISECONDS));
	    		endTime = System.nanoTime();
	    		durationMS = (endTime - startTime) / 1000000;
	    		System.out.println(self().path().name() + ": loaded room instance for " +room.path().name()+ " in " +durationMS+ "ms");
	    	}
	    	
	    	/* We couldn't ask the rooms to resolve their exit paths until they were all
	    	 * loaded (they'd just fail). Now that they're all loaded, ask them to resolve
	    	 * those paths.
	    	 */
	    	for(ActorRef room: JavaConversions.asJavaIterable(getContext().children()))
	    	{
    			startTime = System.nanoTime();
	    		Future<Object> f = Patterns.ask(room, new ResolveExitPaths(), 5000);
	    		Await.ready(f, Duration.create(5000, TimeUnit.MILLISECONDS));
	    		endTime = System.nanoTime();
	    		durationMS = (endTime - startTime) / 1000000;
	    		System.out.println(self().path().name() + ": completed ResolveExitPaths for " + room.path().name() + " in " +durationMS+ "ms");
	    	}
	    	
	    	getSender().tell(new Object(), self());
    	}
    	catch(Exception e)
    	{
    		System.out.println(self().path().name() + ": caught an exception in loadRooms(): " + e);
    		getContext().system().shutdown();
    	}
    }
}
