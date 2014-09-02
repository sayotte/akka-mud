package akkamud;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import akka.japi.Procedure;
import akkamud.reporting.ReportingOneForOneStrategy;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import java.lang.reflect.Field;

/**
 * This class represents a wound that causes blood loss with each heartbeat.
 * 
 * <p>It must be associated with the list of wounds for a given
 *  <tt>Creature</tt> to affect that creature. Most <tt>Creatures</tt> will
 *  keep a list of these for each body region.
 * @author stephen.ayotte
 * @see Creature
 */
class BleedingWound implements Serializable
{
	// the percentage of blood in the area which is lost
	// amputation would be 1.0, a minor cut might be 0.01
    public double flow;
    // the timing 
    public long inflictedTime;
    public BleedingWound(double newFlow)
    {
		flow = newFlow;
		                                    // ms      sec
		inflictedTime = System.nanoTime() / 1000000 / 1000;
	} 
}

enum CreatureVitalSelector
{
    HEARTRATE, RESTINGHEARTRATE, BLOODVOLUME, STAMINA, CARDIOEFFICIENCY
}
class SetCreatureVitalEvent implements Serializable
{
    CreatureVitalSelector which;
    public long longval;
    public double doubleval;
}

/**
 * This class represents a generic living creature. It should be subclassed to
 * create a specific creature type.
 * 
 * <p><tt>Creatures</tt> have stamina which is consumed by physical activities.
 * As they exhaust their stamina, they weaken and are unable to apply as much
 * force and speed to their activities. Their stamina is restored over time
 * by bloodflow, which is the product of overall vascular capacity (fixed) and
 * heartrate (variable based on remaining stamina). Each heartbeat will restore
 * a variable amount of stamina based on the cardiovascular efficiency of 
 * the entity. Bloodflow and stamina restoration are also affected by blood
 * volume, which represents the total amount of blood in the body.
 * 
 * <p>Starting from a fixed maximum, blood volume can be lost due to
 * <tt>BleedingWounds</tt>. As blood volume drops, the overall ability to 
 * restore stamina drops. Below a certain threshold of blood volume, blood loss
 * itself will cause stamina loss, and below a further threshold death will
 * occur.
 * 
 * <p>Blood loss from <tt>BleedingWounds</tt> varies on the severity of the
 * wound, the bloodflow in the area of the wound, and the heartrate of the
 * <tt>Creature</tt>.
 * 
 * @author stephen.ayotte
 * @see MobileEntity
 * @see BleedingWound
 */
class CreatureState extends MobileEntityState
{    
	/* All of the heartrate variables should be hidden or set by
	 * subclasses.
	 */
    // bloodflow is units * heartbeats, beats are measured in bpm
    public long heartRate = 1; 
    // HR returns to this when there's no activity
    public long restingHeartRate = 1;
    // HR should be 
    public long minHeartRate = 1;
    public long maxHeartRate = 2;

    public long bloodVolume = 1000;
    final public long maxBloodVolume = 1000;
    public long maxTotalBloodFlow = -1; // must be set in constructor
    
    // The rate at which bloodflow is converted into stamina
    // This is the overall measure of physical fitness; at 1.0, the
    // creature will regain stamina and return to a lower heartrate
    // much faster than at 0.5, leaving it less vulnerable to bloodloss
    // from wounds and generally better able to cope with continuous
    // exertion.
    // This is the stat most closely aligned with what other games
    // call "stamina".
    public double cardioEfficiency = 0.5; 
    
    // stamina is a modifier on strength; the lower it goes, the less
    // strength is put into every action
    // To keep things simple, this should be a simple percentage:
    //         appliedStrength = strength * (stamina / maxStamina)
    public long stamina = 1000;
    final public long maxStamina = 10000;
}

abstract class Creature extends MobileEntity
{
    protected ActorRef partialAI;
    protected long lastTickTime = System.nanoTime();
    
    public void onReceiveCommand(Object command) throws Exception
    {
        if(command.equals("tick"))
        	handleTick();
        else
            super.onReceiveCommand(command);
    }
    public void onReceiveRecover(Object msg)
    {
        if(msg instanceof SetCreatureVitalEvent)
        	try
        	{
        		recoverSetCreatureVital((SetCreatureVitalEvent)msg);
        	}
        	catch(Exception e)
        	{
        		System.out.println(self().path().name()+": caught an exception while trying to recover a SetCreatureVitalEvent: "+e);
        	}
        else
            System.out.println(self().path().name() + ": unhandled recovery message: " + msg);
            unhandled(msg);
    }
    
    protected void handleTick() throws Exception
    {
    	System.out.println(self().path().name()+": handleTick()...");
    	updateStamina();
    	updateHeartrate();
    }
	private void updateStamina()
	{
		CreatureState state = getState();
        // Stamina is restored by bloodflow; a standard human flows 43 units
        // per heartbeat, but we step it down if they've lost a lot of blood:
    	long newTime = System.nanoTime();
    	double msElapsed = (newTime - lastTickTime) / 1000000;
    	lastTickTime = newTime;
        double heartbeatsPerMs = (double)state.heartRate / 60000;
        double heartbeats = heartbeatsPerMs * msElapsed;
        double bloodFlow = ((double)state.bloodVolume / (double)state.maxBloodVolume) * 
        					(double)state.maxTotalBloodFlow * (double)heartbeats;
        //System.out.println(self().path().name()+": handleTick(): bloodFlow = ("+state.bloodVolume+" / "+state.maxBloodVolume+") * "+state.maxTotalBloodFlow);
        //System.out.println(self().path().name()+": handleTick(): bloodFlow: "+bloodFlow);
        long newStamina = state.stamina + (long)(bloodFlow * heartbeats * state.cardioEfficiency);
        //System.out.println(self().path().name()+": handleTick(): newStamina: "+newStamina);
        if(state.stamina != newStamina &&
		   newStamina <= state.maxStamina &&
		   newStamina >= 0)
        {
        	setCreatureVital(CreatureVitalSelector.STAMINA, newStamina);
        }
	}
    private void updateHeartrate()
    {
    	CreatureState state = getState();
        // Heartrate climbs and falls based on stamina remaining:
        double remainingStaminaPct = 1.0 - ((double)state.stamina / (double)state.maxStamina);
        //System.out.println(self().path().name()+": handleTick(): remainingStaminaPct: "+remainingStaminaPct);
        double heartRateSpread = state.maxHeartRate - state.restingHeartRate;
        //System.out.println(self().path().name()+": handleTick(): heartRateSpread: "+heartRateSpread);
        long newHeartRate = state.restingHeartRate + 
        					(long)((heartRateSpread * remainingStaminaPct) * 2);
        if(newHeartRate > state.maxHeartRate)
    		newHeartRate = state.maxHeartRate;
        else if(newHeartRate < state.minHeartRate)
        	newHeartRate = state.minHeartRate;
        if(state.heartRate != newHeartRate)
        	setCreatureVital(CreatureVitalSelector.HEARTRATE, newHeartRate);
        //System.out.println(self().path().name()+": handleTick(): newHeartRate: "+newHeartRate);
        //System.out.println("");
    }
    abstract List<BleedingWound> getWounds(); 

    private void setCreatureVital(CreatureVitalSelector which, long newVal)
    {
    	SetCreatureVitalEvent evt = new SetCreatureVitalEvent();
    	evt.which = which;
    	evt.longval = newVal;
    	persist(evt, setVitalProc);
    }
    private Procedure<SetCreatureVitalEvent> setVitalProc =
        new Procedure<SetCreatureVitalEvent>()
        {
            public void apply(SetCreatureVitalEvent evt) throws Exception
            {
            	CreatureState state = getState(); 
                // HEARTRATE, RESTINGHEARTRATE, BLOODVOLUME, STAMINA
                switch(evt.which)
                {
                    case HEARTRATE:
                    	System.out.println(self().path().name()+": persisting heartrate as "+evt.longval);
                        state.heartRate = evt.longval;
                        break;
                    case RESTINGHEARTRATE:
                        state.restingHeartRate = evt.longval;
                        break;
                    case BLOODVOLUME:
                        state.bloodVolume = evt.longval;
                        break;
                    case STAMINA:
                    	System.out.println(self().path().name()+": persisting stamina as "+evt.longval);
                        state.stamina = evt.longval;
                        break;
                }
                setState(state);
            }
        };
    private void recoverSetCreatureVital(SetCreatureVitalEvent evt)
    throws Exception
    {
    	CreatureState state = this.getState();
        switch(evt.which)
        {
            case HEARTRATE:
                state.heartRate = evt.longval;
                break;
            case RESTINGHEARTRATE:
                state.restingHeartRate = evt.longval;
                break;
            case BLOODVOLUME:
                state.bloodVolume = evt.longval;
                break;
            case STAMINA:
                state.stamina = evt.longval;
                break;
        }
        this.setState(state);
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
//        but wouldn't a horse and a human have many things in common? yes, so move those into the base class

//entity base-class (implements rules that are common to all entities)
//should not be instantiated directly