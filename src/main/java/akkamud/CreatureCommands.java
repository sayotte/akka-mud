/**
 * 
 */
package akkamud;

import java.io.Serializable;

import akka.actor.ActorRef;

/**
 * @author stephen.ayotte
 *
 */
class CreatureCommands {
	public static abstract class AmbulateToRoom implements Serializable
	{
		public ActorRef room;
		public AmbulateToRoom(ActorRef newRoom){ room = newRoom; }
	}
	public static final class WalkToRoom extends AmbulateToRoom
	{
		private static final long serialVersionUID = 1; // for Serializable
		public WalkToRoom(ActorRef newRoom){ super(newRoom); }
	}
	public static final class JogToRoom extends AmbulateToRoom
	{
		private static final long serialVersionUID = 1; // for Serializable
		public JogToRoom(ActorRef newRoom){ super(newRoom); }
	}
	public static final class RunToRoom extends AmbulateToRoom
	{
		private static final long serialVersionUID = 1; // for Serializable
		public RunToRoom(ActorRef newRoom){ super(newRoom); }
	}
}
