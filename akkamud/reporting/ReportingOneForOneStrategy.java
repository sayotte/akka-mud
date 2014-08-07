package akkamud.reporting;

import scala.collection.Iterable;
import scala.concurrent.duration.Duration;

import java.sql.SQLException;

import akka.actor.OneForOneStrategy;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ChildRestartStats;
import akka.japi.Function;

public class ReportingOneForOneStrategy extends OneForOneStrategy
{
    public ReportingOneForOneStrategy(int maxNrOfRetries, Duration withinTimeRange, Function<Throwable,Directive> decider)
    {
        super(maxNrOfRetries, withinTimeRange, decider);
    }

    @Override
    public boolean handleFailure(ActorContext context, ActorRef child, Throwable cause, ChildRestartStats stats, scala.collection.Iterable<ChildRestartStats> children)
    {
        ReportLogger logger = ReportLogger.getLogger();

//         logSupervisor(String supervisor, String child, String context, Throwable reason, String disposition)
        try
        {
            logger.logSupervisor(context.self().path().name(),
                                child.path().name(),
                                "child_running",
                                cause,
                                "unknown disposition");
        }
        catch(ClassNotFoundException e)
        {
            System.out.println("FATAL: ReportingOneForOneStrategy.handleFailure() caught ClassNotFoundException, TERMINATING SYSTEM IMMEDIATELY. Stacktrace follows: " + e.getMessage());
            context.system().shutdown();
        }
        catch(SQLException e)
        {
            System.out.println("FATAL: ReportingOneForOneStrategy.handleFailure() caught SQLException, TERMINATING SYSTEM IMMEDIATELY. Stacktrace follows: " + e.getMessage());
            context.system().shutdown();
        }
        return super.handleFailure(context, child, cause, stats, children);
    }
}