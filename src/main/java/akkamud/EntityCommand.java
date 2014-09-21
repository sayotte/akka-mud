package akkamud;

import java.io.Serializable;
import akka.actor.ActorRef;

class EntityCommand
{	    
    public static final class MoveToRoom implements Serializable
    {
    	public ActorRef room;
    	public boolean synchronous;
    	public MoveToRoom(ActorRef destRoom){ room = destRoom; synchronous = true; }
    	public MoveToRoom(ActorRef destRoom, boolean sync){ room = destRoom; synchronous = sync; }
    }
    public static final class AnnounceYourself implements Serializable {}
    public static final class RestartYourself implements Serializable {}
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
//    public static final class AnnounceRoomEntry implements Serializable
//    {
//    	public ActorRef who;
//    	public AnnounceRoomEntry(ActorRef newWho){ who = newWho; }
//    }
//	public static final class AnnounceRoomExit implements Serializable
//    {
//    	public ActorRef who;
//    	public AnnounceRoomExit(ActorRef newWho){ who = newWho; }
//    }

    public static final class LoadRooms implements Serializable {}
    public static final class LoadRoomsComplete implements Serializable {}
}