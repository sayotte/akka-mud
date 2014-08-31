package akkamud;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorPath;
import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import akka.actor.Terminated;
import akka.pattern.Patterns;
import akka.persistence.UntypedPersistentActor;
import akka.persistence.SnapshotOffer;
import akka.japi.Procedure;
import scala.concurrent.duration.Duration;
//import scala.concurrent.duration.Duration.Zero;
import scala.concurrent.Future;
import scala.concurrent.Await;
import static akkamud.EntityCommand.*;
import static akkamud.Util.*;

//// State and event definitions for MobileEntity
class MobileEntityState implements Serializable
{
    public Integer hitpoints = 0;
    public ActorPath roomPath = null;

    public Integer getHitpoints(){ return hitpoints; }

    public void setHitpoints(Integer points){ hitpoints = points; }
}
class SetHitpointsEvent implements Serializable
{
    public final int points;
    SetHitpointsEvent(int points){ this.points = points; }
}
class SetRoomEvent implements Serializable
{
	public final ActorPath roomPath;
	SetRoomEvent(ActorPath newRoom){ roomPath = newRoom; }
}	

class MobileEntity extends UntypedPersistentActor
{
    //// Boilerplate for Akka's persistence
    @Override
    public String persistenceId() { return this.self().path().name(); }

    private MobileEntityState state = new MobileEntityState();
    
    private final Cancellable tick = getContext().system().scheduler().schedule(
		Duration.create(0,  TimeUnit.MILLISECONDS), //Duration.Zero, // initial delay
		Duration.create(1000, TimeUnit.MILLISECONDS), // frequency
		getSelf(), "tick", getContext().dispatcher(), null);

    @Override
    public void postStop(){ tick.cancel(); }
    
    //// The reactive model!
    // First the definition for "normal" operations
    public void onReceiveCommand(Object command) throws Exception
    {
//    	System.out.println(self().path().name() + ": received message: "+command);
        if(command instanceof AnnounceYourself)
            System.out.println(self().path().name() + " here!");
        else if(command instanceof RestartYourself)
            throw new Exception();
        else if(command instanceof AnnounceHitpoints)
            System.out.println(self().path().name() + ": hitpoints: " + state.getHitpoints());
        else if(command instanceof AddHitpoints)
            addHitpoints((AddHitpoints)command);
        else if(command instanceof SubHitpoints)
            subHitpoints((SubHitpoints)command);
        else if(command instanceof MoveToRoom)
        	moveToRoom((MoveToRoom)command);
        else if(command instanceof Entry)
        	handleRoomEntry(((Entry)command).who);
        else if(command instanceof Terminated)
        	handleTerminated(((Terminated)command).getActor());
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
        System.out.println(self().path().name() + ": recovering...");
        if (msg instanceof SetHitpointsEvent)
            recoverSetHitpoints((SetHitpointsEvent)msg);
        else if(msg instanceof SetRoomEvent)
        	recoverSetRoom((SetRoomEvent)msg);
        else if(msg instanceof SnapshotOffer)
            state = (MobileEntityState)((SnapshotOffer)msg).snapshot();
        else
        {
          System.out.println(self().path().name() + ": unhandled recovery message: " + msg);
          unhandled(msg);
        }
    }


    //// Stuff to achieve persistent updates of state.hitpoints
    // This one is used for async application of events during normal execution
    private Procedure<SetHitpointsEvent> setHitpointsProc =
        new Procedure<SetHitpointsEvent>()
        {
          public void apply(SetHitpointsEvent evt)
          {
            state.setHitpoints(evt.points);
          }
        };
    // This one is used for sync application of events during recovery
    private void recoverSetHitpoints(SetHitpointsEvent evt)
    {
        System.out.println(self().path().name() + ": recovering by setting hitpoints to " + evt.points);
        state.setHitpoints(evt.points);
    }
    private void addHitpoints(AddHitpoints cmd)
    {
        System.out.println(self().path().name() + ": adding " + cmd.points + " hitpoints as commanded");
        int extraHP = cmd.points;
        SetHitpointsEvent evt = new SetHitpointsEvent(state.getHitpoints() + extraHP);
        persist(evt, setHitpointsProc);
    }
    private void subHitpoints(SubHitpoints cmd)
    {
        int lessHP = cmd.points;
        SetHitpointsEvent evt = new SetHitpointsEvent(state.getHitpoints() - lessHP);
        persist(evt, setHitpointsProc);
    }

    private ActorRef currentRoom = null; 
	private Procedure<SetRoomEvent> setRoomProc =
		new Procedure<SetRoomEvent>()
		{
			public void apply(SetRoomEvent evt)
			{
				state.roomPath = evt.roomPath;
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
		finally
		{
			state.roomPath = joinedRoom.path();
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
    private void moveToRoom(MoveToRoom cmd)
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
    	getSender().tell(new Object(), self());
    }
    private void leaveRoom(ActorRef room)
    {
    	room.tell(new RemoveRoomEntity(getSelf()), getSelf());
    }
    private void handleRoomEntry(ActorRef who)
    {
    	if(who.equals(self()))
    		System.out.println(self().path().name()+": I SEE MYSELF ENTERING THE ROOM!");
    	else
    		System.out.println(self().path().name()+": I see "+who.path().name()+" entering the room.");
    }
    private void handleTerminated(ActorRef who)
    {
    	return;
    }
}
