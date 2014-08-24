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

    public static final class AddHitpoints implements Serializable
    {
        public final int points;
        AddHitpoints(int points) { this.points = points; }
    }

    public static final class SubHitpoints implements Serializable
    {
        public final int points;
        SubHitpoints(int points) { this.points = points; }
    }
    public static final class AnnounceHitpoints implements Serializable {}

    public static final class AnnounceHitpointsForChildren implements Serializable {}
    public static final class PlusTenHitpointsForChildren implements Serializable {}
    
    public static final class SetDefaultRoom implements Serializable
    {
    	public ActorRef room;
    	public SetDefaultRoom(ActorRef setRoom){ room = setRoom; }
    }
    public static final class MoveToRoom implements Serializable
    {
    	public ActorRef room;
    	public MoveToRoom(ActorRef destRoom){ room = destRoom; }
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
}