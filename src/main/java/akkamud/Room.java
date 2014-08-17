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

import akka.actor.UntypedActor;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActorContext;
//import akka.persistence.UntypedPersistentActor;
//import akka.persistence.SnapshotOffer;
//import akka.japi.Procedure;
//import akka.routing.ActorRefRoutee;
import akka.routing.Routee;
import akka.routing.Router;
import akka.routing.BroadcastRoutingLogic;
import akka.util.Timeout;
//import akka.pattern.AskableActorSelection;

class AddEntity implements Serializable
{
	public final ActorRef entity;
	AddEntity(ActorRef ent){ entity = ent; }
}
class RemoveEntity implements Serializable
{
	public final ActorRef entity;
	RemoveEntity(ActorRef ent){ entity = ent; }
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

class Room extends UntypedActor 
{
	private RoomState state = new RoomState();
	private Router router = new Router(new BroadcastRoutingLogic());
	
	public void onReceive(Object command)
	{
		if(command instanceof AddEntity)
			addEntity(((AddEntity)command).entity);
		else if(command instanceof RemoveEntity)
			remEntity(((RemoveEntity)command).entity);
		else if(command instanceof Terminated)
			handleTerminatedEntity(((Terminated)command).getActor());
		else
			unhandled(command);
	}
	
	private void addEntity(ActorRef who)
	{
		router.addRoutee(who);
		getContext().watch(who);
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
