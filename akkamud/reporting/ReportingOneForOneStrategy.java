package akkamud.reporting;

import scala.collection.Iterable;
import scala.concurrent.duration.Duration;

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

        logger.logEvent("", "", "", null);
        return super.handleFailure(context, child, cause, stats, children);
    }
}