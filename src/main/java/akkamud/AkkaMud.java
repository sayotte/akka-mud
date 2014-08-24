// Use Allman style bracing or I'll stab you.

package akkamud;

import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import akka.actor.Props;

import static akkamud.EntityCommand.*;

public class AkkaMud
{
    public static void main(String[] args)
    {
        final ActorSystem system = ActorSystem.create("mud-actorsystem");

        final ActorRef purgatory = system.actorOf(Props.create(Purgatory.class), "purgatory");
        System.out.println("TOP: created purgatory node at path: " + purgatory.path().elements());
//        final ActorRef roomSup = system.actorOf(Props.create(RoomSupervisor.class),
//        										"room-supervisor");
        final ActorRef mobileSup = system.actorOf(Props.create(MobileSupervisor.class),
                                                  "mobile-supervisor");

        mobileSup.tell(new StartChildren(), null);
        mobileSup.tell(new ReportChildren(), null);
        //mobileSup.tell(new AnnounceHitpointsForChildren(), null);
        mobileSup.tell(new PlusTenHitpointsForChildren(), null);
        mobileSup.tell(new PlusTenHitpointsForChildren(), null);
        //mobileSup.tell(new AnnounceHitpointsForChildren(), null);
        mobileSup.tell(new RestartChildren(), null);
        //mobileSup.tell(new AnnounceHitpointsForChildren(), null);
        //mobileSup.tell(new GetHitpointsFromChildren(), null);

        return;
    }

}
