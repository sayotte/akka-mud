package akkamud;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import scala.concurrent.duration.Duration;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import akkamud.reporting.ReportingOneForOneStrategy;

import java.util.List;
import java.util.ArrayList;

class BleedingWound
{
	public long flow;
	public BleedingWound(long newFlow){ flow = newFlow; } 
}

class CreatureState extends MobileEntityState
{	
	// bloodflow restores stamina, or drives bloodloss when there are wounds
	public long heartRate = 60; // how many times/sec to send a heartbeat message, which triggers bloodflow, stamina calculation, 
	public long restingHeartRate = 60; // HR returns to this when there's no activity; a lower resting HR gives a bigger buffer before stamina starts sapping
	public long sustainableHeartRate = 120; // anything above this saps stamina; 120 is a slow jog for most humans
	final public long minHeartRate = 40;
	final public long maxHeartRate = 200;
}

class Creature extends MobileEntity
{
	private CreatureState state = new CreatureState();
	
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
	
	public void onReceiveCommand(Object command) throws Exception
	{
		if(command.equals("tick"))
			System.out.println(self().path().name()+": I am a human!");
		else
			super.onReceiveCommand(command);
	}
}


//human or objective-based AI
//??? actor, object, or static callable?
//actor, so that the API b/w this and the entity can be the same for HCI as for AI

//entity subclass (implements rules about what a given entity is capable of)
//should be instantiated; is an actor
//behavior examples:
//grab an entity
//use an entity
//be grabbed by another entity
//be used by another entity
//??? does this codify things specific to a horse versus a human? I think yes...
//		but wouldn't a horse and a human have many things in common? yes, so move those into the base class

//entity base-class (implements rules that are common to all entities)
//should not be instantiated directly