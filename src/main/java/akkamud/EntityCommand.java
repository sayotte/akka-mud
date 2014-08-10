package akkamud;

import java.io.Serializable;

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
}