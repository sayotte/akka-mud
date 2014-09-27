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
import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotOffer;
import akka.persistence.UntypedPersistentActor;
import akka.japi.Function;
import akka.japi.Procedure;
import scala.concurrent.duration.Duration;
//import scala.concurrent.duration.Duration.Zero;
import scala.concurrent.Future;
import scala.concurrent.Await;
import static akka.actor.SupervisorStrategy.restart;
import static akkamud.CommonCommands.*;
import static akkamud.EntityCommand.*;
import static akkamud.Util.*;
import static akkamud.ReportCommands.*;

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
    private ActorRef reportLogger;
    private ActorRef currentRoom = null;
    private final Cancellable tick;
    private final SupervisorStrategy strategy;
    private ObjectRingBuffer journal;
    
    // Constructor
    public MobileEntity(ActorRef newReportLogger)
    {
    	//System.out.println(self().path().name()+": MobileEntity constructor called");
    	
    	reportLogger = newReportLogger; 
    	
    	tick = getContext().system().scheduler().schedule(
    			Duration.create(1000,  TimeUnit.MILLISECONDS), //Duration.Zero, // initial delay
    			Duration.create(100, TimeUnit.MILLISECONDS), // frequency
    			getSelf(), "tick", getContext().dispatcher(), null);
    	
    	journal = new ObjectRingBuffer(20);
    	
    	strategy = 
    			new ReportingOneForOneStrategy(10,                           // max retries
						                       Duration.create(1, "minute"), // within this time period
						                       decider,                      // with this "decider" for handling
						                       reportLogger);
    }

    // Accessors instead of abstract member variables, stupid Java.
    abstract protected <T extends MobileEntityState> T getState();
    abstract protected <T extends MobileEntityState> void setState(T newState) throws IllegalArgumentException;
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
			String ext = this.journal.toString();
			ExceptionReportEx reportable = 
				new ExceptionReportEx(self().path().name(), e, this.journal.toString());
			//System.out.println(self().path().name()+".MobileEntity.onReceiveCommand(): caught an exception, dumping recent messages before re-throwing:");
			
			throw reportable;
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
	throws IllegalArgumentException, ActorPathResolutionException, Exception
    {
        //System.out.println(self().path().name() + ": recovering, msg is of type "+ msg.getClass().getName());
        if(msg instanceof SetRoomEvent)
        	recoverSetRoom((SetRoomEvent)msg);
        else if(msg instanceof RecoveryCompleted)
        	completeRecovery();
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
        		throw(e);
        	}
        }
        else
        {
    	  System.out.println(self().path().name() + ": unhandled recovery message: " + msg);
          unhandled(msg);
        }
    }
    private void completeRecovery() throws ActorPathResolutionException, Exception
    {
    	ActorRef roomWeWereInBeforeRestart;
		MobileEntityState state = this.getState();

		// if this is a brand-new entity, it will have no roomPath set by
		// its initial no-op recovery
		if(state.roomPath != null)
		{
//			try
//			{
				roomWeWereInBeforeRestart = Util.resolvePathToRefSync(state.roomPath, getContext().system());
				if(! roomWeWereInBeforeRestart.equals(this.currentRoom))
				{
					System.out.println(self().path().name()+".MobileEntity.completeRecovery(): entering room: "+state.roomPath);
					enterRoom(roomWeWereInBeforeRestart);
				}
//			}
//			catch(ActorPathResolutionException e)
//			{
//				System.out.println(self().path().name() + ": failed to resolve room?: " + e);
//				enterPurgatory(); // may throw an exception
//			}
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
	private void recoverSetRoom(SetRoomEvent evt) throws IllegalArgumentException
	{
		System.out.println(self().path().name() + ".MobileEntity.recoverSetRoom(): ...");
		MobileEntityState state = this.getState();
		state.roomPath = evt.roomPath;
		this.setState(state);
	}
	private void setRoom(ActorPath roomPath)
	{
		SetRoomEvent evt = new SetRoomEvent(roomPath);
		persist(evt, setRoomProc);
	}
	private void enterRoom(ActorRef room)
	throws Exception
    {
//    	try
//    	{
    		final Future<Object> f = Patterns.ask(room, new AddRoomEntity(self()), 5000);
    		Await.ready(f, Duration.create(5000, "millis"));
        	getContext().watch(room);
        	this.currentRoom = room;
//    	}
    	// FIXME XXX
    	// We should really just allow this exception to propagate-- throwing ourselves into
    	// Purgatory makes the situation essentially unrecoverable.
    	// Not that Purgatory is a bad idea by itself, but we shouldn't warp to it at the
    	// first sign of trouble... a few restarts later, we might succeed at whatever we were
    	// trying to do. If we end up dying an unrecoverable death, well, the MUD should
    	// probably go down with us.
//    	catch(Exception e)
//    	{
//    		System.out.println(self().path().name() + ": caught an exception trying to enter a room: " + e);
//    		System.out.println(self().path().name() + ": moving to Purgatory!");
//    		try{ enterPurgatory(); } // sets this.currentRoom
//    		// FIXME XXX
//    		catch(ActorPathResolutionException e2){ ; } 
//    	}
    }
    private void enterPurgatory() throws ActorPathResolutionException
    {
    	try
    	{
	    	ActorRef purgatory = 
	    		Util.resolvePathToRefSync(Purgatory.purgatoryPathString, getContext().system()); 
	    	purgatory.tell(new AddRoomEntity(getSelf()), getSelf());
	    	this.currentRoom = purgatory;
    	}
    	catch(ActorPathResolutionException e)
    	{
        	// if we died entering Purgatory, it's probably time for everything to go up in smoke..
        	// but just in case it was a timeout, let's re-throw the exception and allow the
    		// restart functionality to re-try a few times
    		System.out.println(self().path().name() + ": HOLY CRAP, exception entering Purgatory??: " +e);
    		throw(e);
    	}
    	// FIXME XXX
    	catch(Exception e){ ; }
    }
    protected void moveToRoom(MoveToRoom cmd)
    throws Exception
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
    	enterRoom(cmd.room);
		setRoom(this.currentRoom.path());
    	if(oldRoom != null)
    		leaveRoom(oldRoom);
    	if(cmd.synchronous == true)
    		getSender().tell(new PassFail(true), getSelf());
    }
    private void leaveRoom(ActorRef room)
    {
    	room.tell(new RemoveRoomEntity(getSelf()), getSelf());
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
    	final Future<Object> f = Patterns.ask(currentRoom, cmd, 5000);
    	response = (TheseAreMyExits)Await.result(f, Duration.create(5000, TimeUnit.MILLISECONDS));
//    	response = (TheseAreMyExits)Await.result(f, Duration.create("Inf"));
    	getSender().tell(response, getSelf());
    }
}
