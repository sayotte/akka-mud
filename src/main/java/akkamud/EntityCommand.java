package akkamud;

import java.io.Serializable;
import akka.actor.ActorRef;

class EntityCommand
{
    public static final class StartChildren implements Serializable {}
    public static final class ReportChildren implements Serializable {}
    public static final class RestartChildren implements Serializable {}
    public static final class AnnounceYourself implements Serializable {}
    public static final class RestartYourself implements Serializable {}
    
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
    public static final class MoveToRoom implements Serializable
    {
    	public ActorRef room;
    	public MoveToRoom(ActorRef destRoom){ room = destRoom; }
    }
    public static final class MoveAllChildrenToRoom implements Serializable
    {
    	public final ActorRef room;
    	public MoveAllChildrenToRoom(ActorRef newRoom){ room = newRoom; }
    }
    
    public static final class AddRoomEntity implements Serializable
    {
        public final ActorRef entity;
        public AddRoomEntity(ActorRef ent){ entity = ent; }
    }
    public static final class RemoveRoomEntity implements Serializable
    {
        public final ActorRef entity;
        public RemoveRoomEntity(ActorRef ent){ entity = ent; }
    }
    public static final class ResolveExitPaths implements Serializable {}
    public static final class AnnounceRoomEntry implements Serializable
    {
    	public ActorRef who;
    	public AnnounceRoomEntry(ActorRef newWho){ who = newWho; }
    }
    public static final class AnnounceRoomExit implements Serializable
    {
    	public ActorRef who;
    	public AnnounceRoomExit(ActorRef newWho){ who = newWho; }
    }

    public static final class LoadRooms implements Serializable {}
    public static final class LoadRoomsComplete implements Serializable {}
}