package akkamud;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.actor.ActorPath;
import akka.actor.ActorSystem;

import akka.util.Timeout;

import scala.concurrent.duration.Duration;
import scala.concurrent.Future;
import scala.concurrent.Await;

public class Util
{
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
		Timeout t = new Timeout(10000, TimeUnit.MILLISECONDS);
//		try
//		{
			Future<ActorRef> fut = system.actorSelection(path).resolveOne(t);
			ref = Await.result(fut, t.duration());
			return ref;
//		}
//		catch(TimeoutException e){ throw(new ActorPathResolutionException(e)); }
//		finally{ return ref; }
	}
}

class ObjectRingBuffer
{
	private int size;
	private int index;
	private Object[] buf;
	public ObjectRingBuffer(int newSize)
	{
		size = newSize;
		index = 0;
		buf = new Object[size];
	}
	public void add(Object obj)
	{
		buf[index] = obj;
		index++;
		if(index >= size)
			index = 0;
	}
	public ArrayList<Object> getContents()
	{
		ArrayList<Object> ret = new ArrayList<Object>();
		int i, accum;
		for(i = index, accum = 0; accum < size; i++, accum++)
		{
			if(i >= size)
				i = 0;
			ret.add(buf[i]);
		}
		return ret;
	}
	
}
