package akkamud;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;

import static akkamud.EntityCommand.*;

class RequestMovementInstructions implements Serializable {}
class RequestActionInstructions implements Serializable {}

class PartialAI extends UntypedActor
{
	public void onReceive(Object message)
	throws Exception
	{
		if(message instanceof RequestMovementInstructions)
			sendMovementInstructions();
		else if(message instanceof RequestActionInstructions)
			sendActionInstructions();
		else
			unhandled(message);
	}
	
	private void sendMovementInstructions()
	throws Exception
	{
		TheseAreMyExits exits;
		Object response;
		final Future<Object> f = Patterns.ask(getSender(), new WhatAreYourExits(), 10);
		response = Await.result(f, Duration.create(10, TimeUnit.MILLISECONDS));
		if(! (response instanceof TheseAreMyExits))
		{
			System.out.println(self().path().name()+": got an empty answer to WhatAreYourExits, doing nothing");
			return;
		}
		exits = (TheseAreMyExits)response;
		
		ActorRef dest = null;
		if(exits.exitRefs.get(Directions.EAST) != null && exits.exitRefs.get(Directions.SOUTH) != null)
			dest = exits.exitRefs.get(Directions.SOUTH);
		else if(exits.exitRefs.get(Directions.EAST) != null && exits.exitRefs.get(Directions.NORTH) != null)
			dest = exits.exitRefs.get(Directions.EAST);
		else if(exits.exitRefs.get(Directions.NORTH) != null && exits.exitRefs.get(Directions.WEST) != null)
			dest = exits.exitRefs.get(Directions.NORTH);
		else if(exits.exitRefs.get(Directions.SOUTH) != null && exits.exitRefs.get(Directions.WEST) != null)
			dest = exits.exitRefs.get(Directions.WEST);
		
		if(dest != null)
			getSender().tell(new MoveToRoom(dest), getSelf());
	}
	private void sendActionInstructions()
	{
		return;
	}
}
