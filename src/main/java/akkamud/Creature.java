package akkamud;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import scala.concurrent.duration.Duration;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import akka.japi.Procedure;
import akkamud.reporting.ReportingOneForOneStrategy;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import java.lang.reflect.Field;

class BleedingWound
{
    public long flow;
    public BleedingWound(long newFlow){ flow = newFlow; } 
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

class CreatureState extends MobileEntityState
{    
    // bloodflow restores stamina, or drives bloodloss when there are wounds
    // bloodflow is units * heartbeats, beats are measured in bpm
    public long heartRate = 60; 
    // HR returns to this when there's no activity;
    // a lower resting HR gives a bigger buffer before stamina starts sapping
    public long restingHeartRate = 60;
    final public long minHeartRate = 40;
    final public long maxHeartRate = 200;

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
    // To keep things simple, this is a simple percentage:
    //         appliedStrength = strength * (stamina / maxStamina)
    public long stamina = 1000;
    final public long maxStamina = 10000;
}

class Creature extends MobileEntity
{
    // abstract? this should be implemented by children
    protected CreatureState state;
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
            recoverSetCreatureVital((SetCreatureVitalEvent)msg);
        else
            System.out.println(self().path().name() + ": unhandled recovery message: " + msg);
            unhandled(msg);
    }
    
    private void handleTick() throws Exception
    {
    	updateStamina();
    	updateHeartrate();
    }
	private void updateStamina()
	{
        // Stamina is restored by bloodflow; a standard human flows 43 units
        // per heartbeat, but we step it down if they've lost a lot of blood:
    	long newTime = System.nanoTime();
    	double msElapsed = (newTime - lastTickTime) / 1000000;
    	lastTickTime = newTime;
        double heartbeatsPerMs = (double)state.heartRate / 60000;
        double heartbeats = heartbeatsPerMs * msElapsed;
        double bloodFlow = (state.bloodVolume / state.maxBloodVolume) * 
        					state.maxTotalBloodFlow * heartbeats;
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
        // Heartrate climbs and falls based on stamina remaining:
        double remainingStaminaPct = 1.0 - ((double)state.stamina / (double)state.maxStamina);
        //System.out.println(self().path().name()+": handleTick(): remainingStaminaPct: "+remainingStaminaPct);
        double heartRateSpread = state.maxHeartRate - state.restingHeartRate;
        //System.out.println(self().path().name()+": handleTick(): heartRateSpread: "+heartRateSpread);
        long newHeartRate = state.restingHeartRate + 
        					(long)((heartRateSpread * remainingStaminaPct) * 2);
        if(state.heartRate != newHeartRate && 
           newHeartRate >= state.minHeartRate)
        {
        	if(newHeartRate > state.maxHeartRate)
        		newHeartRate = state.maxHeartRate;
        	setCreatureVital(CreatureVitalSelector.HEARTRATE, newHeartRate);
        }
        //System.out.println(self().path().name()+": handleTick(): newHeartRate: "+newHeartRate);
        System.out.println("");
    }
    
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
            public void apply(SetCreatureVitalEvent evt)
            {
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
            }
        };
    private void recoverSetCreatureVital(SetCreatureVitalEvent evt)
    {
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