package akkamud;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.lang.Exception;

import scala.collection.JavaConversions;
import scala.collection.mutable.Buffer;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;
import scala.concurrent.Await;

//import akka.actor.UntypedActor;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.UntypedActorContext;
import akka.persistence.UntypedPersistentActor;
import akka.persistence.SnapshotOffer;
import akka.japi.Procedure;
import akka.routing.ActorRefRoutee;
import akka.routing.Routee;
import akka.routing.Router;
import akka.routing.BroadcastRoutingLogic;
import akka.util.Timeout;
import akka.pattern.AskableActorSelection;

class AddEntity implements Serializable
{
	public final ActorPath entity;
	AddEntity(ActorPath ent){ entity = ent; }
}
class RemoveEntity implements Serializable
{
	public final ActorPath entity;
	RemoveEntity(ActorPath ent){ entity = ent; }
}
class SetEntitiesEvent implements Serializable
{
	public final List<ActorPath> entities;
	SetEntitiesEvent(List<ActorPath> ents){ entities = ents; }
}

class RoomState implements Serializable
{
	public String name = "nowhere";
	public String grossDescription = "This is a room. Right now, it's full of blackness.";
	public List<ActorPath> entities = new ArrayList<ActorPath>();
	public ActorPath northExit = null;
	public ActorPath eastExit = null;
	public ActorPath southExit = null;
	public ActorPath westExit = null;
}

class Room extends UntypedPersistentActor 
{
	private RoomState state = new RoomState();
	private Router router = new Router(new BroadcastRoutingLogic());
	@Override
	public String persistenceId(){ return this.self().path().name(); }
	
	public void onReceiveCommand(Object command)
	{
		if(command instanceof AddEntity)
			addEntity((AddEntity)command);
		else
			unhandled(command);
	}
	@Override
	public void onReceiveRecover(Object msg)
	{
		unhandled(msg);
	}
	
	private void addEntity(AddEntity cmd)
	{
		ActorPath newpath = cmd.entity;
		if(state.entities.contains(newpath))
			return;
		
		
		return;
	}
	private void remEntity(RemoveEntity cmd)
	{
		return;
	}
	private Procedure<SetEntitiesEvent> setEntityListProc =
		new Procedure<SetEntitiesEvent>()
		{
		  public void apply(SetEntitiesEvent evt) throws Exception
		  {
			  setEntityList(evt);
		  }
		};
	private void setEntityList(SetEntitiesEvent evt)
	throws Exception
	{
	    // Convert the list of paths to a list of Routees
		List<Routee> routees =
			Room.getRouteesFromActorPaths(evt.entities, getContext());
		  
		// Replace our router's Routees
		scala.collection.mutable.Buffer<Routee> buf = JavaConversions.asScalaBuffer(routees);
		router = router.withRoutees(buf.toIndexedSeq());

		// Replace our persisted state so we can 
		state.entities = evt.entities;
	}
	static private List<Routee> getRouteesFromActorPaths(List<ActorPath> paths, UntypedActorContext context)
	throws Exception
	{
		Timeout t = new Timeout(100, TimeUnit.MILLISECONDS);
		
		// Asynchronously ask for a list of ActorRefs using the paths given
		List<Future<ActorRef>> futures = new ArrayList<Future<ActorRef>>();
		for(ActorPath path: paths)
		{
			Future<ActorRef> future = context.actorSelection(path).resolveOne(t);
			futures.add(future);
		}

		// Resolve the Futures returned into actual ActorRefs,
		// wrap those into a list of ActorRefRoutees
		List<Routee> routees = new ArrayList<Routee>();
		for(Future<ActorRef> future: futures)
		{
			ActorRef ref = Await.result(future, t.duration());
			routees.add(new ActorRefRoutee(ref));
		}
		
		return routees;
	}
}
