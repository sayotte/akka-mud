package akkamud;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.lang.Exception;

import com.codahale.metrics.*;

import scala.collection.JavaConversions;
import scala.collection.mutable.Buffer;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;
import scala.concurrent.Await;

import akka.actor.UntypedActor;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
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

import static akkamud.EntityCommand.*;


enum Directions
{
	NORTH, EAST, SOUTH, WEST
}

class WhatAreYourExits implements Serializable { private static final long serialVersionUID = 1; }
class TheseAreMyExits implements Serializable
{
	private static final long serialVersionUID = 1;
	Map<Directions, ActorRef> exitRefs;
	Map<Directions, String> exitPaths;
	public TheseAreMyExits()
	{
		exitRefs = new HashMap<Directions, ActorRef>();
		exitPaths = new HashMap<Directions, String>();
	}
}

class RoomState implements Serializable
{
    public String name = "nowhere";
    public String grossDescription = "This is a room. Right now, it's full of blackness.";
    //public List<ActorPath> entities = new ArrayList<ActorPath>();
    public ActorRef northExit = null;
    public ActorRef eastExit = null;
    public ActorRef southExit = null;
    public ActorRef westExit = null;
    public String northExitPath = null;
    public String eastExitPath = null;
    public String southExitPath = null;
    public String westExitPath = null;
}

class Room extends UntypedActor 
{
    private RoomState state = new RoomState();
    private Router router = new Router(new BroadcastRoutingLogic());
    private final Timer reportExitsTimer;
    private final Timer onReceiveTimer;
    
    public Room()
    {
    	String metricName = self().path().toStringWithoutAddress()
    						.replace('/',  '.')
    						.replaceFirst("\\.", "");
    	String reportExitsName = metricName + ".reportExits.timer";
    	this.reportExitsTimer = AkkaMud.registry.timer(reportExitsName);
    	String onReceiverName = metricName + ".onReceive.timer";
    	this.onReceiveTimer = AkkaMud.registry.timer(onReceiverName);
    }
    
    public void onReceive(Object command)
    throws Exception
    {
    	Timer.Context context = onReceiveTimer.time();
//    	System.out.println(self().path().name()+": received command: "+command);
        if(command instanceof AddRoomEntity)
            addEntity(((AddRoomEntity)command).entity);
        else if(command instanceof RemoveRoomEntity)
            remEntity(((RemoveRoomEntity)command).entity);
        else if(command instanceof Terminated)
            handleTerminatedEntity(((Terminated)command).getActor());
        else if(command instanceof RoomState)
        {
        	state = (RoomState)command;
        	getSender().tell(new Object(), self());
        }
        else if(command instanceof ResolveExitPaths)
        	resolveExitPaths();
        else if(command instanceof Announce)
        {
//        	router.route(command, getSender());
        	router.route(new Object(), null);
        	getSender().tell(new Object(), self());
        }
        else if(command instanceof WhatAreYourExits)
        	reportExits();
        else
            unhandled(command);
        
        context.stop();
    }
    
    private void addEntity(ActorRef who)
    {
    	//System.out.println(self().path().name() + ": adding " + who.path().name());
        router = router.addRoutee(who);
        getContext().watch(who);
        getSender().tell(new Object(), getSelf());
        router.route(new Entry(who), getSelf());
    }
    private void remEntity(ActorRef who)
    {
    	//System.out.println(self().path().name() + ": removing " + who.path().name());
        router = router.removeRoutee(who);
        getContext().unwatch(who);
        router.route(new Exit(who), getSelf());
//        router.route(new AnnounceRoomExit(who), who);
        //router.route(new Exit(who), getSelf());
    }
    private void handleTerminatedEntity(ActorRef who)
    {
        System.out.println(self().path().name() + ": handling terminated room member");
        remEntity(who);
    }
    private void resolveExitPaths()
    throws Exception
    {
//    	System.out.println(self().path().name() + ": resolving exit paths");
    	ActorSystem sys = getContext().system();
    	
    	if(state.northExitPath != null)
    	{
//    		System.out.println(self().path().name() + ": resolving my north exit, whose path is: " + state.northExitPath);
    		try{ state.northExit = Util.resolvePathToRefSync(state.northExitPath, sys); }
    		catch(Exception e){ throw(new Exception(self().path().name() + ": caught exception resolving north exit (path: " + state.northExitPath + "): " + e)); }     				                                
    	}	
    	if(state.eastExitPath != null)
    	{
//    		System.out.println(self().path().name() + ": resolving my east exit, whose path is: " + state.eastExitPath);
    		try{ state.eastExit = Util.resolvePathToRefSync(state.eastExitPath, sys); }
    		catch(Exception e){ throw(new Exception(self().path().name() + ": caught exception resolving east exit (path: " + state.eastExitPath + "): " + e)); } 
    	}
    	if(state.southExitPath != null)
    	{
//        	System.out.println(self().path().name() + ": resolving my south exit, whose path is: " + state.southExitPath);
    		try{ state.southExit = Util.resolvePathToRefSync(state.southExitPath, sys); }
    		catch(Exception e){ throw(new Exception(self().path().name() + ": caught exception resolving south exit (path: " + state.southExitPath + "): " + e)); } 
    	}
    	if(state.westExitPath != null)
    	{
//        	System.out.println(self().path().name() + ": resolving my west exit, whose path is: " + state.westExitPath);
    		try{ state.westExit = Util.resolvePathToRefSync(state.westExitPath, sys); }
    		catch(Exception e){ throw(new Exception(self().path().name() + ": caught exception resolving west exit (path: " + state.westExitPath + "): " + e)); } 
    	}
    	getSender().tell(new Object(), self());
    }
    private void reportExits()
    {
    	Timer.Context context = reportExitsTimer.time();
    	TheseAreMyExits response = new TheseAreMyExits();
		response.exitRefs.put(Directions.NORTH, state.northExit);
		response.exitRefs.put(Directions.EAST,  state.eastExit);
		response.exitRefs.put(Directions.SOUTH, state.southExit);
		response.exitRefs.put(Directions.WEST,  state.westExit);
		response.exitPaths.put(Directions.NORTH, state.northExitPath);
		response.exitPaths.put(Directions.EAST,  state.eastExitPath);
		response.exitPaths.put(Directions.SOUTH, state.southExitPath);
		response.exitPaths.put(Directions.WEST,  state.westExitPath);
    	getSender().tell(response, getSelf());
    	context.stop();
    }

}
