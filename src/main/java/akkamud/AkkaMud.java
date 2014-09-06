// Use Allman style bracing or I'll stab you.

package akkamud;

import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;

import static akkamud.EntityCommand.*;
import static akkamud.Util.*;

public class AkkaMud
{
    public static void main(String[] args)
    throws Exception
    {
        final ActorSystem system = ActorSystem.create("mud-actorsystem");
        
        System.out.println("AkkaMUD: starting report logger");
        final ActorRef reportLogger = 
    		system.actorOf(Props.create(ReportLogger.class, "reports.db"), "report-logger");

        System.out.println("AkkaMUD: starting purgatory and the room supervisor");
        final ActorRef purgatory = system.actorOf(Props.create(Purgatory.class), "purgatory");
        final ActorRef roomSup = 
    		system.actorOf(Props.create(RoomSupervisor.class, reportLogger), "room-supervisor");

        System.out.println("AkkaMUD: loading rooms");
        long startTime = System.nanoTime();
        Future<Object> f = Patterns.ask(roomSup,  new LoadRooms(), 1000);
        Await.ready(f, Duration.create(5, "minutes"));
        long endTime = System.nanoTime();
        long durationMS = (endTime - startTime) / 1000000;
        System.out.println("AkkaMUD: completed room load in " + durationMS + "ms");

        System.out.println("AkkaMUD: resolving room1 to use as default room");
        startTime = System.nanoTime();
    	ActorRef room1 = null; 
    	while(room1 == null)
    	{
    		try{ room1 = Util.resolvePathToRefSync("/user/room-supervisor/room1", system); }
    		catch(Exception e)
    		{
    			System.out.println("Caught exception resolving room1, trying again forever...: " + e);
    		}
    	}
    	endTime = System.nanoTime();
    	durationMS = (endTime - startTime) / 1000000;
    	System.out.println("AkkaMUD: resolved room1 in " + durationMS + "ms");

    	System.out.println("AkkaMUD: starting mobile supervisor");
    	startTime = System.nanoTime();
        final ActorRef mobileSup = 
    		system.actorOf(Props.create(MobileSupervisor.class, reportLogger), "mobile-supervisor");
    	mobileSup.tell(new SetDefaultRoom(room1), null);
        f = Patterns.ask(mobileSup, new StartChildren(),  100);
        Await.ready(f, Duration.create(5, "minutes"));
        endTime = System.nanoTime();
    	durationMS = (endTime - startTime) / 1000000;
    	System.out.println("AkkaMUD: presuming mobiles started after " + durationMS + "ms");
       

//        while(true)
//        {
//        	Thread.sleep(4000);
//        	mobileSup.tell(new RestartChildren(), null);
//        }
//        mobileSup.tell(new MoveAllChildrenToRoom(room2), null);

        //return;
    }

}
