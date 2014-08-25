// Use Allman style bracing or I'll stab you.

package akkamud;

import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import akka.actor.Props;

import static akkamud.EntityCommand.*;

public class AkkaMud
{
    public static void main(String[] args)
    throws Exception
    {
        final ActorSystem system = ActorSystem.create("mud-actorsystem");

        final ActorRef purgatory = system.actorOf(Props.create(Purgatory.class), "purgatory");
        final ActorRef roomSup = system.actorOf(Props.create(RoomSupervisor.class),
        										"room-supervisor");
        roomSup.tell(new LoadRooms(), null);
        final ActorRef mobileSup = system.actorOf(Props.create(MobileSupervisor.class),
                                                  "mobile-supervisor");

    	ActorRef room1 = null; 
    	while(room1 == null)
    	{
    		try{ room1 = Util.resolvePathToRefSync("akka://mud-actorsystem/user/room-supervisor/room1", system); }
    		catch(Exception e)
    		{
    			System.out.println("Caught exception resolving room1, trying again forever...: " + e);
    		}
    	}
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
        mobileSup.tell(new StartChildren(), null);
        mobileSup.tell(new ReportChildren(), null);
        //mobileSup.tell(new AnnounceHitpointsForChildren(), null);
        mobileSup.tell(new PlusTenHitpointsForChildren(), null);
        mobileSup.tell(new PlusTenHitpointsForChildren(), null);
        //mobileSup.tell(new AnnounceHitpointsForChildren(), null);
//        mobileSup.tell(new RestartChildren(), null);
        //mobileSup.tell(new AnnounceHitpointsForChildren(), null);
        //mobileSup.tell(new GetHitpointsFromChildren(), null);
        mobileSup.tell(new MoveAllChildrenToRoom(room2), null);

        return;
    }

}
