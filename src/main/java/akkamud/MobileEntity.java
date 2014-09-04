package akkamud;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorPath;
import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import akka.actor.Terminated;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.pattern.Patterns;
import akka.persistence.UntypedPersistentActor;
import akka.persistence.SnapshotOffer;
import akka.japi.Function;
import akka.japi.Procedure;
import akkamud.reporting.ReportingOneForOneStrategy;
import scala.concurrent.duration.Duration;
//import scala.concurrent.duration.Duration.Zero;
import scala.concurrent.Future;
import scala.concurrent.Await;
import static akka.actor.SupervisorStrategy.restart;
import static akkamud.CommonCommands.*;
import static akkamud.EntityCommand.*;
import static akkamud.Util.*;

//// State and event definitions for MobileEntity
class MobileEntityState implements Serializable
{
    public ActorPath roomPath = null;
}
class SetRoomEvent implements Serializable
{
	public final ActorPath roomPath;
	SetRoomEvent(ActorPath newRoom){ roomPath = newRoom; }
}

abstract class MobileEntity extends UntypedPersistentActor
{
    //// Boilerplate for Akka's persistence
    @Override
    public String persistenceId() { return this.self().path().name(); }

    // Concrete member variables
    private ActorRef currentRoom = null;
    private final Cancellable tick;
    private ObjectRingBuffer journal;
    
    // Constructor
    public MobileEntity()
    {
    	System.out.println(self().path().name()+": MobileEntity constructor called");
    	tick = getContext().system().scheduler().schedule(
    			Duration.create(1000,  TimeUnit.MILLISECONDS), //Duration.Zero, // initial delay
    			Duration.create(100, TimeUnit.MILLISECONDS), // frequency
    			getSelf(), "tick", getContext().dispatcher(), null);
    	journal = new ObjectRingBuffer(20);
    }

    // Accessors instead of abstract member variables, stupid Java.
    abstract protected <T extends MobileEntityState> T getState();
    abstract protected <T extends MobileEntityState> void setState(T newState) throws Exception;
    protected ObjectRingBuffer getJournal(){ return journal; }
    
    // Supervision bits
    private final Function<Throwable, Directive> decider = 
        new Function<Throwable, Directive>()
        {
            @Override
            public Directive apply(Throwable t)
            {
            	System.out.println(self().path().name()+": decider(): I caught an exception of type '" + t.getClass().getSimpleName() + "', returning restart()");                	
                return restart();
            }
        };
	private final SupervisorStrategy strategy =
	    new ReportingOneForOneStrategy(10,                           // max retries
	                                   Duration.create(1, "minute"), // within this time period
	                                   decider);                     // with this "decider" for handling
	@Override
	public SupervisorStrategy supervisorStrategy(){ return strategy; }
    
    //// The reactive model!
    // First the definition for "normal" operations
	public void onReceiveCommand(Object command) throws Exception
	{
		//System.out.println(self().path().name()+".Human.onReceiveCommand(): "+command);
		this.journal.add(command);
		try{ handleCommand(command); }
		catch(Exception e)
		{
			System.out.println(self().path().name()+".MobileEntity.onReceiveCommand(): caught an exception, dumping recent messages before re-throwing:");
			int i = 0;
			for(Object obj: this.journal.getContents())
			{
				System.out.println(i+": "+obj);
				i++;
			}
		}
	}
    protected void handleCommand(Object command) throws Exception
    {
//    	System.out.println(self().path().name() + ": received message: "+command);
        if(command instanceof AnnounceYourself)
            System.out.println(self().path().name() + " here!");
        else if(command instanceof RestartYourself)
            throw new Exception();
        else if(command instanceof MoveToRoom)
        	moveToRoom((MoveToRoom)command);
        else if(command instanceof Entry)
        	handleRoomEntry(((Entry)command).who);
        else if(command instanceof Terminated)
        	handleTerminated(((Terminated)command).getActor());
        else if(command instanceof WhatAreYourExits)
        	reportExits((WhatAreYourExits)command);
        else
        {
            System.out.println(self().path().name() + ": unhandled command: "+command+" from "+getSender().path().name());
            unhandled(command);
        }
    }
    // Now the definition for "recovery" operations (we've been restarted)
    @Override
    public void onReceiveRecover(Object msg)
    {
        System.out.println(self().path().name() + ": recovering, msg is of type "+ msg.getClass().getName());
        if(msg instanceof SetRoomEvent)
        	recoverSetRoom((SetRoomEvent)msg);
        else if(msg instanceof SnapshotOffer)
        {
        	try
        	{
		    	MobileEntityState state;
		        state = (MobileEntityState)((SnapshotOffer)msg).snapshot();
		        setState(state);
        	}
        	catch(Exception e)
        	{
        		System.out.println(self().path().name()+": caught an exception while trying to recover a SnapshotOffer: "+e);
        	}
        }
        else
        {
    	  System.out.println(self().path().name() + ": unhandled recovery message: " + msg);
          unhandled(msg);
        }
    }
    @Override
    public void postStop(){ tick.cancel(); }
 
	private Procedure<SetRoomEvent> setRoomProc =
		new Procedure<SetRoomEvent>()
		{
			public void apply(SetRoomEvent evt) throws Exception
			{
				//System.out.println(self().path().name()+": setRoomProc(): evt: "+evt);
				//System.out.println(self().path().name()+": setRoomProc(): state: "+state);
				MobileEntityState state = getState();
				state.roomPath = evt.roomPath;
				setState(state);
			}
		};
	private void recoverSetRoom(SetRoomEvent evt)
	{
		System.out.println(self().path().name() + ": recovering by setting room");
		ActorRef joinedRoom = null;
		try
		{
			ActorRef recoveredRoom = Util.resolvePathToRefSync(evt.roomPath, getContext().system());
			joinedRoom = enterRoom(recoveredRoom);
		}
		catch(Exception e)
		{
			if(e instanceof ActorPathResolutionException)
				System.out.println(self().path().name() + ": failed to resolve room?: " + e);
			else
				System.out.println(self().path().name() + ": exception recovering room:" + e);
			joinedRoom = enterPurgatory();
		}
		try
		{
			MobileEntityState state = this.getState();
			state.roomPath = joinedRoom.path();
			this.setState(state);
		}
		catch(Exception e)
		{
			System.out.println(self().path().name()+": caught an exception while trying to recover a SetRoomEvent: "+e);
		}
	}
	private void setRoom(ActorPath roomPath)
	{
		SetRoomEvent evt = new SetRoomEvent(roomPath);
		persist(evt, setRoomProc);
	}
	private ActorRef enterRoom(ActorRef room)
    {
    	try
    	{
    		final Future<Object> f = Patterns.ask(room, new AddRoomEntity(self()), 10);
    		Await.ready(f, Duration.create(10, "millis"));
        	getContext().watch(room);
        	this.currentRoom = room;
    	}
    	catch(Exception e)
    	{
    		System.out.println(self().path().name() + ": caught an exception trying to enter a room: " + e);
    		System.out.println(self().path().name() + ": moving to Purgatory!");
    		this.currentRoom = enterPurgatory();
    	}
    	return this.currentRoom;
    }
    private ActorRef enterPurgatory()
    {
    	// Entering Purgatory *must* succeed since it's a fallback, so we catch all exceptions
    	try
    	{
	    	ActorRef purgatory = 
	    		Util.resolvePathToRefSync(Purgatory.purgatoryPathString, 
	    								  getContext().system()); 
	    	purgatory.tell(new AddRoomEntity(getSelf()), getSelf());
	    	return purgatory;
    	}
    	catch(Exception e)
    	{
    		System.out.println(self().path().name() + ": HOLY CRAP, exception entering Purgatory??: " +e);
    		return null;
    	}
    }
    protected void moveToRoom(MoveToRoom cmd)
    {
    	// Moving room-to-room is not atomic; we must have one foot in each
    	// room at some point. We model this by entering the new room before
    	// exiting the old one.
    	// 
    	// Also, if we fail to enter the new room for some mechanical reason,
    	// it may be useful to refuse to leave the previous room. It might be
    	// more useful at some point in the future to instead be warped to some
    	// sort of "purgatory" room, where no state-modifying messages are
    	// delivered, waiting on administrative intervention.
    	
    	ActorRef oldRoom = currentRoom;
    	ActorRef joinedRoom = enterRoom(cmd.room);
		setRoom(joinedRoom.path());
    	if(oldRoom != null)
    		leaveRoom(oldRoom);
    	if(cmd.synchronous == true)
    		getSender().tell(new PassFail(true), getSelf());
    }
    private void leaveRoom(ActorRef room)
    {
    	room.tell(new RemoveRoomEntity(getSelf()), getSelf());
    }
    private void handleRoomEntry(ActorRef who)
    {
    	if(! who.equals(self()))
    		System.out.println(self().path().name()+": I see "+who.path().name()+" entering the room.");
    }
    private void handleTerminated(ActorRef who)
    {
    	return;
    }
    private void reportExits(WhatAreYourExits cmd)
    throws Exception
    {
    	if(currentRoom == null)
    	{
    		getSender().tell(new Object(), getSelf());
    		return;
    	}
    	TheseAreMyExits response;
    	final Future<Object> f = Patterns.ask(currentRoom, cmd, 10);
    	response = (TheseAreMyExits)Await.result(f, Duration.create(10, TimeUnit.MILLISECONDS));
    	getSender().tell(response, getSelf());
    }
}
