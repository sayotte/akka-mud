package akkamud;

import java.io.Serializable;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.actor.Terminated;
import akka.persistence.UntypedPersistentActor;
import akka.persistence.SnapshotOffer;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;
import scala.concurrent.Future;
import scala.concurrent.Await;

import static akkamud.EntityCommand.*;

//// State and event definitions for MobileEntity
class MobileEntityState implements Serializable
{
    public Integer hitpoints = 0;
    public ActorRef room = null;

    public Integer getHitpoints() { return hitpoints; }

    public void setHitpoints(Integer points) { hitpoints = points; }
}
class SetHitpointsEvent implements Serializable
{
    public final int points;
//     SetHitpointsEvent() { this.points = 0; } // needed to make this serializable
    SetHitpointsEvent(int points) { this.points = points; }
}

class MobileEntity extends UntypedPersistentActor
{
//         /**
//         * Create Props for an actor of this type.
//         * @param magicNumber The magic number to be passed to this actor’s constructor.
//         * @return a Props for creating this actor, which can then be further configured
//         *         (e.g. calling `.withDispatcher()` on it)
//         */
//         public static Props props(final int magicNumber)
//         {
//             // I don't understand this syntax at ALL, but apparently it lets us accept
//             // a parameter when we're instantiating, like so:
//             // ActorRef blah = system.actorOf(MobileEntity.props(__arg__), "blah");
//             return Props.create(new Creator<DemoActor>()
//             {
//                 private static final long serialVersionUID = 1L;
// 
//                 @Override
//                 public DemoActor create() throws Exception
//                 {
//                     return new DemoActor(magicNumber);
//                 }
//             });
//         }
    //// Boilerplate for Akka's persistence
    @Override
    public String persistenceId() { return this.self().path().name(); }

    private MobileEntityState state = new MobileEntityState();


    //// The reactive model!
    // First the definition for "normal" operations
    public void onReceiveCommand(Object command) throws Exception
    {
    	System.out.println(self().path().name() + ": received message");
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
        else if(command instanceof Terminated)
        	handleTerminated(((Terminated)command).getActor());
        else
            unhandled(command);
    }
    // Now the definition for "recovery" operations (we've been restarted)
    @Override
    public void onReceiveRecover(Object msg)
    {
        System.out.println(self().path().name() + ": recovering...");
        if (msg instanceof SetHitpointsEvent)
            recoverSetHitpoints((SetHitpointsEvent)msg);
        else if (msg instanceof SnapshotOffer)
            state = (MobileEntityState)((SnapshotOffer)msg).snapshot();
        else 
          unhandled(msg);
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

    private void enterRoom(ActorRef room)
    {
    	final Future<Object> f = Patterns.ask(room, new AddEntity(self()), 10);
    	try
    	{
    		Await.ready(f, Duration.create(10, "millis"));
    	}
    	catch(Exception e)
    	{
    		System.out.println(self().path().name() + ": caught an exception trying to enter a room: " + e);
    	}
    	getContext().watch(room);
    }
    
    private void handlerTerminated(ActorRef who)
    {
    	
    	
    }
}
