package akkamud;

import java.io.Serializable;

import akka.actor.ActorRef;

class MobileSupervisorCommands {
    public static final class StartChildren implements Serializable {}
    public static final class ReportChildren implements Serializable {}
    public static final class RestartChildren implements Serializable {}
    public static final class SetDefaultRoom implements Serializable
    {
    	public ActorRef room;
    	public SetDefaultRoom(ActorRef setRoom)
    	throws Exception
    	{
    		if(setRoom == null)
    			throw(new Exception("Fuck you and your NULL ROOMS!"));
    		room = setRoom;
		}
    }
    public static final class MoveAllChildrenToRoom implements Serializable
    {
    	public final ActorRef room;
    	public MoveAllChildrenToRoom(ActorRef newRoom){ room = newRoom; }
    }
    public static final class ListChildren implements Serializable {}
}
