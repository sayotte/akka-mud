package akkamud;

import java.io.Serializable;

class EntityCommand
{
    public static final class StartChildren implements Serializable {}
    public static final class ReportChildren implements Serializable {}
    public static final class RestartChildren implements Serializable {}
    public static final class AnnounceYourself implements Serializable {}
    public static final class RestartYourself implements Serializable {}
}