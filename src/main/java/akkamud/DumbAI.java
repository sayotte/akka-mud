package akkamud;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.*;
import akkamud.AkkaMud;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.Duration.Infinite;
import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;

import static akkamud.EntityCommand.*;
import static akkamud.CreatureCommands.*;
import static akkamud.CommonCommands.*;

class RequestMovementInstructions implements Serializable {}
class RequestActionInstructions implements Serializable {}

class DumbAI extends UntypedActor
{
	private ObjectRingBuffer history;
	private final Timer sendMovementTimer;
	private ActorRef AI;
	
	public DumbAI()
	{
		this.history = new ObjectRingBuffer(20);
		String metricName = self().path().toStringWithoutAddress();
		metricName = metricName.replace('/', '.');
		metricName = metricName.replaceFirst("\\.", "");
		metricName += ".sendMovementInstructions.timer";
		this.sendMovementTimer = AkkaMud.registry.timer(metricName);
	}
	public void onReceive(Object message)
	throws Exception
	{
		this.history.add(message);
		try
		{
			//System.out.println(self().path().name()+".PartialAI: received message @ "+nowMS+"ms: "+message);
			if(message instanceof RequestMovementInstructions)
				sendMovementInstructions();
			else if(message instanceof RequestActionInstructions)
				sendActionInstructions();
			else if(message instanceof NewAI)
				acceptNewAI((NewAI)message);
			else
				unhandled(message);
		}
		catch(Exception e)
		{
			System.out.println(self().path().name()+".partialAI.onReceiveCommand(): caught an exception, dumping recent messages before re-throwing:");
			int i = 0;
			for(Object obj: this.history.getContents())
			{
				System.out.println(i+": "+obj);
				i++;
			}
			throw e;
		}
	}
	
	private void sendMovementInstructions()
	throws Exception
	{
		Timer.Context context = sendMovementTimer.time();
		final Object exitsResponse;
		final Future<Object> exitReq = Patterns.ask(getSender(), new WhatAreYourExits(), 5000);
		exitsResponse = Await.result(exitReq, Duration.create(5000, TimeUnit.MILLISECONDS));
		//exitsResponse = Await.result(exitReq, Duration.create("Inf"));
		if(! (exitsResponse instanceof TheseAreMyExits))
		{
			System.out.println(self().path().name()+": got an empty answer to WhatAreYourExits, doing nothing");
			System.out.println(self().path().name()+": class of actual answer: " + exitsResponse.getClass().getName());
			return;
		}
		final TheseAreMyExits exits = (TheseAreMyExits)exitsResponse;
		
		// bullshit go-in-a-circle direction logic
		final ActorRef dest;
		if(exits.exitRefs.get(Directions.EAST) != null && exits.exitRefs.get(Directions.SOUTH) != null)
			dest = exits.exitRefs.get(Directions.SOUTH);
		else if(exits.exitRefs.get(Directions.EAST) != null && exits.exitRefs.get(Directions.NORTH) != null)
			dest = exits.exitRefs.get(Directions.EAST);
		else if(exits.exitRefs.get(Directions.NORTH) != null && exits.exitRefs.get(Directions.WEST) != null)
			dest = exits.exitRefs.get(Directions.NORTH);
		else if(exits.exitRefs.get(Directions.SOUTH) != null && exits.exitRefs.get(Directions.WEST) != null)
			dest = exits.exitRefs.get(Directions.WEST);
		else
			dest = null;
		
		if(dest == null)
		{
			System.out.println(self().path().toStringWithoutAddress()+
							   ": can't figure out where to go based on exits available, not moving");
			return;
		}
//		final Future<Object> moveReq = Patterns.ask(getSender(), new WalkToRoom(dest), 10);
		final Future<Object> moveReq = Patterns.ask(getSender(), new JogToRoom(dest), 5000);
		final PassFail response;
		response = (PassFail)Await.result(moveReq, Duration.create(5000, TimeUnit.MILLISECONDS));
		//response = (PassFail)Await.result(moveReq, Duration.create("Inf"));
		//System.out.println(self().path().name()+": response status of request to move: "+response.status);
		context.stop();
	}
 	private void sendActionInstructions()
	{
		return;
	}
 	private void acceptNewAI(NewAI msg)
 	{
 		
 	}
}
