package akkamud;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import akka.actor.ActorRef;
import akka.actor.ActorPath;
import akka.actor.ActorSystem;

import akka.util.Timeout;

import scala.concurrent.duration.Duration;
import scala.concurrent.Future;
import scala.concurrent.Await;

public class Util {
	public static final class ActorPathResolutionException extends Exception 
	{
		public ActorPathResolutionException(Throwable t){ super(t); }
	}
	
	public static ActorRef resolvePathToRefSync(ActorPath path, ActorSystem system)
	throws ActorPathResolutionException, Exception
	{
		ActorRef ref = null;
		Timeout t = new Timeout(100, TimeUnit.MILLISECONDS);
		try
		{
			Future<ActorRef> fut = system.actorSelection(path).resolveOne(t);
			ref = Await.result(fut, t.duration());
		}
		catch(TimeoutException e){ throw(new ActorPathResolutionException(e)); }
		finally{ return ref; }
	}
	public static ActorRef resolvePathToRefSync(String path, ActorSystem system)
	throws ActorPathResolutionException, Exception
	{
		ActorRef ref = null;
		Timeout t = new Timeout(100, TimeUnit.MILLISECONDS);
		try
		{
			Future<ActorRef> fut = system.actorSelection(path).resolveOne(t);
			ref = Await.result(fut, t.duration());
		}
		catch(TimeoutException e){ throw(new ActorPathResolutionException(e)); }
		finally{ return ref; }
	}
}
