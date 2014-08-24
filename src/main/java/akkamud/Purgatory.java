/**
 * 
 */
package akkamud;

import akka.actor.UntypedActor;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.Router;
import akka.routing.BroadcastRoutingLogic;

import static akkamud.EntityCommand.*;

/**
 * @author stephen.ayotte
 *
 */
class Purgatory extends UntypedActor
{
	public static final String purgatoryPathString = "akka://mud-actorsystem/user/purgatory";
	private Router router = new Router(new BroadcastRoutingLogic());

	public void onReceive(Object message)
	{
		if(message instanceof AddRoomEntity)
			addEntity(((AddRoomEntity)message).entity);
		else if(message instanceof RemoveRoomEntity)
			remEntity(((RemoveRoomEntity)message).entity);
		else
			unhandled(message);
	}
	
    private void addEntity(ActorRef who)
    {
        router.addRoutee(who);
        getContext().watch(who);
        getSender().tell(new Object(), getSelf());
    }
    private void remEntity(ActorRef who)
    {
        router.removeRoutee(who);
        getContext().unwatch(who);
    }
    private void handleTerminatedEntity(ActorRef who)
    {
        System.out.println(self().path().name() + ": handling terminated room member");
        remEntity(who);
    }
}
