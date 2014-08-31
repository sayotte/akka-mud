package akkamud;

class HumanState extends MobileEntityState
{
	public long bloodVolume = 1000;
	final public long maxBloodVolume = 1000;
	public double leftArmCond = 100.0;
	public double rightArmCond = 100.0;
	final public long perArmBloodFlow = 4; // bleed out in 120s w/ 1 max arm wound
	public double leftLegCond = 100.0;
	public double rightLegCond = 100.0;
	final public long perLegBloodFlow = 8; // bleed out in 60s w/ 1 max leg wound
	public double neckCond = 100.0;
	final public long neckBloodFlow = 16; // bleed out in 30s w/ maximum neck wound
	public double headCond = 100.0;
	final public long headBloodFlow = 3; // bleed out in 160s w/ maximum head wound 
}

class Human extends MobileEntity
{
	private HumanState state = new HumanState();
	
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

//partial AI (implements things shared by HCI and objective-based AI)
//??? should this be part of the entity subclass? I think maybe it should...

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