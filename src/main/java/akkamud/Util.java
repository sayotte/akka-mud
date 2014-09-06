package akkamud;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.ArrayList;

import akka.actor.ActorNotFound;
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
		public ActorPathResolutionException(String m){ super(m); }
	}
	
	public static ActorRef resolvePathToRefSync(ActorPath path, ActorSystem system)
	throws ActorPathResolutionException, Exception
	{
		ActorRef ref = null;
		int retries = 0;
		int maxRetries = 10;
		Timeout t = new Timeout(100, TimeUnit.MILLISECONDS);
		// use 10x100ms retries for a total of 1000ms because not all failures
		// are timeouts
		while(retries < maxRetries)
		{
			try
			{
				Future<ActorRef> fut = system.actorSelection(path).resolveOne(t);
				ref = Await.result(fut, t.duration());
				break;
			}
			catch(ActorNotFound e){ retries++; } // thrown by resolveOne()
			catch(TimeoutException e){ retries++; } // thrown by Await.result()
			catch(InterruptedException e){ retries++; } // thrown by Await.result()
		}
		if(retries >= maxRetries)
			throw(new ActorPathResolutionException("Max retries exceeded"));
		return ref;
	}
	public static ActorRef resolvePathToRefSync(String path, ActorSystem system)
	throws ActorPathResolutionException, Exception
	{
		ActorRef ref = null;
		int retries = 0;
		int maxRetries = 10;
		Timeout t = new Timeout(100, TimeUnit.MILLISECONDS);
		// use 10x100ms retries for a total of 1000ms because not all failures
		// are timeouts
		while(retries < maxRetries)
		{
			try
			{
				Future<ActorRef> fut = system.actorSelection(path).resolveOne(t);
				ref = Await.result(fut, t.duration());
				break;
			}
			catch(ActorNotFound e){ retries++; } // thrown by resolveOne()
			catch(TimeoutException e){ retries++; } // thrown by Await.result()
			catch(InterruptedException e){ retries++; } // thrown by Await.result()
		}
		if(retries >= maxRetries)
			throw(new ActorPathResolutionException("Max retries exceeded"));
		return ref;
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
	public String toString()
	{
		ArrayList<Object> contents = getContents();
		String str = ""; 
		int i = 0;
		for(Object obj: contents)
		{
			str += i+": "+obj+"\n";
			i++;
		}
		return str;
	}
	
}
