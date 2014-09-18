// Use Allman style bracing or I'll stab you.

package akkamud;

import java.util.concurrent.TimeoutException;
//import java.lang.InterruptedException;

import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.io.Tcp;
import akka.pattern.Patterns;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;

import static akkamud.EntityCommand.*;
import static akkamud.Util.*;

public class AkkaMud
{
	private static ActorRef startReportLogger(ActorSystem system)
	{
		System.out.println("AkkaMUD: starting report logger");
        ActorRef reportLogger = 
    		system.actorOf(Props.create(ReportLogger.class, "reports.db"), "report-logger");
        
        return reportLogger;
	}
	private static ActorRef startRoomSup(ActorSystem system, ActorRef reportLogger)
	{
		System.out.println("AkkaMUD: starting purgatory and the room supervisor");
        ActorRef purgatory = system.actorOf(Props.create(Purgatory.class), "purgatory");
        ActorRef roomSup = 
    		system.actorOf(Props.create(RoomSupervisor.class, reportLogger), "room-supervisor");
        
        return roomSup;
	}
	private static void loadRooms(ActorSystem system, ActorRef roomSup)
	throws TimeoutException, InterruptedException
	{
		System.out.println("AkkaMUD: loading rooms");
        long startTime = System.nanoTime();
        Future<Object> f = Patterns.ask(roomSup,  new LoadRooms(), 1000);
        Await.ready(f, Duration.create(5, "minutes"));
        long endTime = System.nanoTime();
        long durationMS = (endTime - startTime) / 1000000;
        System.out.println("AkkaMUD: completed room load in " + durationMS + "ms");
	}
	private static ActorRef getDefaultRoom(ActorSystem system)
	{
		System.out.println("AkkaMUD: resolving room1 to use as default room");
        long startTime = System.nanoTime();
    	ActorRef room1 = null; 
    	while(room1 == null)
    	{
    		try{ room1 = Util.resolvePathToRefSync("/user/room-supervisor/room1", system); }
    		catch(Exception e)
    		{
    			System.out.println("Caught exception resolving room1, trying again forever...: " + e);
    		}
    	}
    	long endTime = System.nanoTime();
    	long durationMS = (endTime - startTime) / 1000000;
    	System.out.println("AkkaMUD: resolved room1 in " + durationMS + "ms");
    	
    	return room1;
	}
	private static ActorRef startMobileSup(ActorSystem system, ActorRef reportLogger, ActorRef defaultRoom)
	throws TimeoutException, Exception
	{
		System.out.println("AkkaMUD: starting mobile supervisor");
    	long startTime = System.nanoTime();
        ActorRef mobileSup = 
    		system.actorOf(Props.create(MobileSupervisor.class, reportLogger), "mobile-supervisor");
    	mobileSup.tell(new SetDefaultRoom(defaultRoom), null);
        Future<Object> f = Patterns.ask(mobileSup, new StartChildren(),  100);
        Await.ready(f, Duration.create(5, "minutes"));
        long endTime = System.nanoTime();
    	long durationMS = (endTime - startTime) / 1000000;
    	System.out.println("AkkaMUD: presuming mobiles started after " + durationMS + "ms");
    	
    	return mobileSup;
	}
	public static void main(String[] args)
    throws Exception
    {
        final ActorSystem system = ActorSystem.create("mud-actorsystem");
        final ActorRef reportLogger = startReportLogger(system);
    	final ActorRef roomSup = startRoomSup(system, reportLogger);
    	loadRooms(system, roomSup);        
    	final ActorRef defaultRoom = getDefaultRoom(system);
        final ActorRef mobileSup = startMobileSup(system, reportLogger, defaultRoom);
        
        // stuff related to TCP
        final ActorRef tcpManager = Tcp.get(system).manager();
        final Props listenerProps =
    		Props.create(TelnetListener.class, tcpManager, 4000, reportLogger);
        final ActorRef telnetListener =
    		system.actorOf(listenerProps, "telnet-listener");


//        while(true)
//        {
//        	Thread.sleep(4000);
//        	mobileSup.tell(new RestartChildren(), null);
//        }
//        mobileSup.tell(new MoveAllChildrenToRoom(room2), null);

        //return;
    }

}
