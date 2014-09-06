package akkamud;

import scala.collection.Iterable;
import scala.concurrent.duration.Duration;

import java.sql.SQLException;

import akka.actor.OneForOneStrategy;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ChildRestartStats;
import akka.japi.Function;

import static akkamud.ReportCommands.*;

public class ReportingOneForOneStrategy extends OneForOneStrategy
{
	private ActorRef logger;
	
    public ReportingOneForOneStrategy(int maxNrOfRetries, Duration withinTimeRange, Function<Throwable,Directive> decider, ActorRef newLogger)
    {
        super(maxNrOfRetries, withinTimeRange, decider);
        this.logger = newLogger;
        //System.out.println("ReportingOneForOneStrategy: instance created with logger "+newLogger.path().name());
    }

    @Override
    public boolean handleFailure(ActorContext context, ActorRef child, Throwable cause, ChildRestartStats stats, scala.collection.Iterable<ChildRestartStats> children)
    {
    	Throwable innerCause;
    	SupervisorReport report;
    	String supervisorName = context.self().path().name();
    	String childName;
    	String reportContext = "child_running"; // is this always true?
    	String ext;
    	
    	if(cause instanceof ExceptionReportEx)
    	{
    		ExceptionReportEx reportable = (ExceptionReportEx)cause;
    		
    		childName = reportable.who;
    	    innerCause = reportable.reason;
    	    ext = reportable.ext;
    	}
    	else
    	{
    		childName = child.path().name();
    		innerCause = cause;
    		ext = "";
    	}
    	report = new SupervisorReport(supervisorName, childName, reportContext, innerCause, ext);
    	this.logger.tell(report, context.self());
    	
    	return super.handleFailure(context, child, innerCause, stats, children);
    }
}