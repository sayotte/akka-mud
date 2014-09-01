package akkamud;

import akka.actor.ActorRef;
import akka.actor.Props;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

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

class Human extends Creature
{
	private boolean moving;
	private boolean busy;
	public Human()
	{
		this.state = new HumanState();
		this.partialAI = getContext().actorOf(Props.create(PartialAI.class), "partialAI");
		moving = false;
		busy = false;
	}
	
	public void onReceiveCommand(Object command)
	throws Exception
	{
		if(command.equals("tick"))
			handleTick();
		super.onReceiveCommand(command);
	}
	
	protected void handleTick()
	throws Exception
	{
		if(moving != true)
			partialAI.tell(new RequestMovementInstructions(), getSelf());
		if(busy != true)
			partialAI.tell(new RequestActionInstructions(), getSelf());
		super.handleTick();
	}
	
	protected List<BleedingWound> getWounds()
	{
		return null;
	}
}
