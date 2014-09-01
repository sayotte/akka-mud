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

        final ActorRef purgatory = system.actorOf(Props.create(Purgatory.class), "purgatory");
        final ActorRef roomSup = system.actorOf(Props.create(RoomSupervisor.class),
        										"room-supervisor");
        long startTime = System.nanoTime();
        Future<Object> f = Patterns.ask(roomSup,  new LoadRooms(), 2000);
        Await.ready(f, Duration.create(2000, "millis"));
        long endTime = System.nanoTime();
        long durationMS = (endTime - startTime) / 1000000;
        System.out.println("AkkaMUD: completed room load in " + durationMS + "ms");

        final ActorRef mobileSup = system.actorOf(Props.create(MobileSupervisor.class),
                                                  "mobile-supervisor");

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
    	
    	ActorRef room2 = null;
    	while(room2 == null)
    	{
    		try{ room2 = Util.resolvePathToRefSync("akka://mud-actorsystem/user/room-supervisor/room2", system); }
    		catch(Exception e)
    		{
    			System.out.println("Caught exception resolving room1, trying again forever...: " + e);
    		}
    	}
    	mobileSup.tell(new SetDefaultRoom(room1), null);
        f = Patterns.ask(mobileSup, new StartChildren(),  1000);
        Await.ready(f, Duration.create(1000, "millis"));
//        room1.tell(new Announce(null), room2);
//        f = Patterns.ask(room1, new Announce(null), 100);
//        Await.ready(f,  Duration.create(100, "millis"));
//        mobileSup.tell(new RestartChildren(), null);
//        mobileSup.tell(new MoveAllChildrenToRoom(room2), null);

        return;
    }

}
