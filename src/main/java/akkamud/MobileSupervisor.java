// Use Allman style bracing or I'll stab you.

package akkamud;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.actor.Props;
import akka.japi.Function; // new Function<T, R>() { func_declaration_here }
import akka.actor.SupervisorStrategy;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.stop;
import static akka.actor.SupervisorStrategy.escalate;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.OneForOneStrategy;

import java.sql.SQLException;

// import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.concurrent.duration.Duration;

import akkamud.reporting.ReportLogger;
import akkamud.reporting.ReportingOneForOneStrategy;

import static akkamud.EntityCommand.*;

class MobileSupervisor extends UntypedActor
{
    private ReportLogger logger;
    public MobileSupervisor()
    {
        System.out.println("Mobile supervisor name: " + this.self().path().name());
        logger = ReportLogger.getLogger();
    }

    private final Function<Throwable, Directive> decider = 
        new Function<Throwable, Directive>()
        {
            @Override
            public Directive apply(Throwable t)
            {
                if(t instanceof java.lang.Exception)
                {
                    return restart();
                }
                else
                    return escalate();
            }
        };

    private final SupervisorStrategy strategy = 
        new ReportingOneForOneStrategy(10,                           // max retries
                                       Duration.create(1, "minute"), // within this time period
                                       decider);                     // with this "decider" for handling

    @Override
    public SupervisorStrategy supervisorStrategy()
    {
        return strategy;
    }

    public void onReceive(Object message)
    {
        if(message instanceof StartChildren)
            launchChildren();
        else if(message instanceof ReportChildren)
            announceChildren();
        else if(message instanceof RestartChildren)
            restartChildren();
        else if(message instanceof AnnounceHitpointsForChildren)
            announceHitpointsForChildren();
        else if(message instanceof PlusTenHitpointsForChildren)
            plusTenHitpointsForChildren();
        else
            unhandled(message);
    }

    private void launchChildren()
    {
        System.out.println("Mobile supervisor, launching children!");
        int i;
        for(i = 0; i < 1; i++)
        {
            ActorRef child = this.getContext().actorOf(Props.create(MobileEntity.class),
                                                        "mobile" + Integer.toString(i));
            try
            {
                logger.logProgress(this.getSelf().path().name(), child.path().name(), "child_starting");
            }
            catch(ClassNotFoundException e)
            {
                System.out.println("FATAL: ReportingOneForOneStrategy.handleFailure() caught ClassNotFoundException, TERMINATING SYSTEM IMMEDIATELY. Stacktrace follows: " + e.getMessage());
                this.getContext().system().shutdown();
            }
            catch(SQLException e)
            {
                System.out.println("FATAL: ReportingOneForOneStrategy.handleFailure() caught SQLException, TERMINATING SYSTEM IMMEDIATELY. Stacktrace follows: " + e.getMessage());
                this.getContext().system().shutdown();
            }
        }
    }

    private void announceChildren()
    {
        System.out.println("Mobile supervisor, ordering children to report!");
        for(ActorRef child: JavaConversions.asJavaIterable(this.getContext().children()))
        {
            child.tell(new AnnounceYourself(), this.self());
        }
    }

    private void restartChildren()
    {
        System.out.println("Mobile supervisor, restarting children!");
        for(ActorRef child: JavaConversions.asJavaIterable(this.getContext().children()))
        {
            child.tell(new RestartYourself(), this.self());
        }
    }
    private void announceHitpointsForChildren()
    {
        System.out.println("Mobile supervisor, ordering children to announce hitpoints!");
        for(ActorRef child: JavaConversions.asJavaIterable(this.getContext().children()))
        {
            child.tell(new AnnounceHitpoints(), this.self());
        }
    }
    private void plusTenHitpointsForChildren()
    {
        System.out.println("Mobile supervisor, adding ten hitpoints to all children!");
        for(ActorRef child: JavaConversions.asJavaIterable(this.getContext().children()))
        {
            child.tell(new AddHitpoints(10), this.self());
        }
    }
}
