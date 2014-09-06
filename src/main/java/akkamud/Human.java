package akkamud;

import akka.actor.ActorRef;
import akka.actor.Props;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import static akkamud.EntityCommand.*;
import static akkamud.CreatureCommands.*;
import static akkamud.CommonCommands.*;

class HumanState extends CreatureState
{
	public double leftArmCond = 100.0;
	public double rightArmCond = 100.0;
	final public long perArmBloodFlow = 4; // bleed out in 120s w/ 1 max arm wound @ 120bpm
	List<BleedingWound> leftArmWounds = new ArrayList();
	List<BleedingWound> rightArmWounds = new ArrayList();
	
	public double leftLegCond = 100.0;
	public double rightLegCond = 100.0;
	List<BleedingWound> leftLegWounds = new ArrayList();
	List<BleedingWound> rightLegWounds = new ArrayList();
	final public long perLegBloodFlow = 8; // bleed out in 60s w/ 1 max leg wound @ 120bpm
	
	public double bodyCond = 100.0;
	List<BleedingWound> bodyWounds = new ArrayList();
	final public long bodyBloodFlow = 25; // bleed out in 20s w/ max wound @ 120bpm
	
	public double neckCond = 100.0;
	final public long neckBloodFlow = 16; // bleed out in 30s w/ maximum neck wound @ 120bpm
	List<BleedingWound> neckWounds = new ArrayList();
	
	public double headCond = 100.0;
	final public long headBloodFlow = 3; // bleed out in 160s w/ maximum head wound @ 120bpm
	List<BleedingWound> headWounds = new ArrayList();
	
	public HumanState()
	{
		maxTotalBloodFlow = (perArmBloodFlow * 2) +
		        	        (perLegBloodFlow * 2) +
		        	        bodyBloodFlow + 
							neckBloodFlow +
							headBloodFlow;
		heartRate = 60;
		restingHeartRate = 60;
		minHeartRate = 60;
		maxHeartRate = 180;
	}
}

final class Human extends Creature
{
	// private member variables
	private long movingUntilMS;
	private long busyUntilMS;
	private HumanState state;
	
	// accessors, because we don't have abstract member variables in Java
	@Override
	protected <T extends MobileEntityState> T getState(){ return (T)state; }
	@Override
	protected <T extends MobileEntityState> void setState(T newState)
	throws IllegalArgumentException
	{
		if(!(newState instanceof HumanState))
			throw(new IllegalArgumentException("setState() called with object not an instance of HumanState; actually instance of "+state.getClass().getName()));
		state = (HumanState)newState;
	}
	
	// constructor
	public Human(ActorRef reportLogger)
	{
		super(reportLogger);
		//System.out.println(self().path().name()+".Human(): constructor called with logger: "+reportLogger.path().name());
		this.state = new HumanState();
		this.partialAI = getContext().actorOf(Props.create(PartialAI.class), "partialAI");
		this.movingUntilMS = 0;
		this.busyUntilMS = 0;
	}
	
	// Akka Actor bits
	protected void handleCommand(Object command)
	throws Exception
	{
		long nowMS = System.nanoTime() / 1000000;
		//System.out.println(self().path().name()+".Human: received message @ "+nowMS+"ms: "+command);
		if(command.equals("tick"))
			handleTick();
		else if(command instanceof AmbulateToRoom)
			handleAmbulation(command);
		else
			super.handleCommand(command);
	}
	
	// Implementation methods
	@Override
	protected void handleTick()
	throws Exception
	{
		long nowMS = System.nanoTime() / 1000000;
		//System.out.println(self().path().name()+".Human.handleTick(): @ "+nowMS+"ms");
		if(nowMS >= movingUntilMS)
			partialAI.tell(new RequestMovementInstructions(), getSelf());
		if(nowMS >= busyUntilMS)
			partialAI.tell(new RequestActionInstructions(), getSelf());
		super.handleTick();
	}
	protected List<BleedingWound> getWounds()
	{
		return null;
	}

	private void handleAmbulation(Object cmd)
	throws Exception
	{
		final long nowMS = System.nanoTime() / 1000000;
		if(nowMS <= this.movingUntilMS)
		{
			getSender().tell(new PassFail(false), getSelf());
			return;
		}

		long delayMS;
		long staminaTax;
		String movementDesc;
		if(cmd instanceof WalkToRoom)
		{
			movementDesc = "walk";
			delayMS = 3000;
			staminaTax = 90; 
		}
		else if(cmd instanceof JogToRoom)
		{
			movementDesc = "jogg";
			delayMS = 1500;
			staminaTax = 120;
		}
		else if(cmd instanceof RunToRoom)
		{
			movementDesc = "runn";
			delayMS = 750;
			staminaTax = 140;
		}
		else
			throw(new Exception("Unrecognized subclass of AmbulateToRoom:"+cmd));
		this.movingUntilMS = (System.nanoTime() / 1000000) + delayMS;
		
		if((state.stamina - staminaTax) <= 0)
		{
			System.out.println(self().path().name()+": panting with exhaustion instead of "+movementDesc+"ing to another room!");
			getSender().tell(new PassFail(false), getSelf());
			return;
		}
		System.out.println(self().path().name()+": "+movementDesc+"ing to another room");
		setCreatureVital(CreatureVitalSelector.STAMINA, state.stamina - staminaTax);
		
		ActorRef room = ((AmbulateToRoom)cmd).room;
		moveToRoom(new MoveToRoom(room, false));
		getSender().tell(new PassFail(true), getSelf());
	}
}
