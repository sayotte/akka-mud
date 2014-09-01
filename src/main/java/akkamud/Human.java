package akkamud;

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
							neckBloodFlow +
							headBloodFlow; // 43
	}
}

class Human extends Creature {
	public Human()
	{
		this.state = new HumanState();
	}
}
