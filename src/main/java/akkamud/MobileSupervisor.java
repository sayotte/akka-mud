// Use Allman style bracing or I'll stab you.

package akkamud;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.actor.Props;
import akka.actor.Kill;
import akka.japi.Function; // new Function<T, R>() { func_declaration_here }
import akka.actor.SupervisorStrategy;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.stop;
import static akka.actor.SupervisorStrategy.escalate;
import akka.actor.SupervisorStrategy.Directive;
//import akka.actor.OneForOneStrategy;

//import java.sql.SQLException;

//import java.io.StringWriter;

// import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.concurrent.duration.Duration;
import scala.concurrent.Await;
import akka.pattern.Patterns;
import scala.concurrent.Future;

import static akkamud.EntityCommand.*;
import static akkamud.ReportCommands.*;

class MobileSupervisor extends UntypedActor
{
    private final ActorRef reportLogger;
    private final SupervisorStrategy strategy;
    private ActorRef defaultRoom;
    
    public MobileSupervisor(ActorRef newReportLogger)
    {
    	System.out.println(self().path().name() + ", running");
        reportLogger = newReportLogger;
        System.out.println(self().path().name() + " created with logger: "+reportLogger.path().name());
        strategy = new ReportingOneForOneStrategy(10,                           // max retries
								                  Duration.create(1, "minute"), // within this time period
								                  decider,                      // with this "decider" for handling
								                  this.reportLogger);
    }

    private final Function<Throwable, Directive> decider = 
        new Function<Throwable, Directive>()
        {
            @Override
            public Directive apply(Throwable t)
            {
                if(t instanceof java.lang.Exception)
                    return restart();
                else
                {
                	System.out.println("Mobile supervisor, I caught an exception of type '" + t.getClass().getSimpleName() + "'");                	
                    return escalate();
                }
            }
        };

    @Override
    public SupervisorStrategy supervisorStrategy()
    {
        return strategy;
    }

    public void onReceive(Object message)
    throws Exception
    {
//    	System.out.println(self().path().name()+".onReceive(): received message: "+message);
        if(message instanceof StartChildren)
            launchChildren();
        else if(message instanceof ReportChildren)
            announceChildren();
        else if(message instanceof RestartChildren)
            restartChildren();
        else if(message instanceof MoveAllChildrenToRoom)
        	moveAllChildrenToRoom((MoveAllChildrenToRoom)message);
        else if(message instanceof SetDefaultRoom)
        	setDefaultRoom((SetDefaultRoom)message);
        else
            unhandled(message);
    }

    private void launchChildren()
    {
        System.out.println(self().path().name() + ": launching children!");
        int i;
        try
        {
		    for(i = 0; i < 300; i++)
		    {
		    	Props p = Props.create(Human.class, this.reportLogger);
		    	ActorRef child = this.getContext().actorOf(p, "mobile" + Integer.toString(i));
		    	
		    	ProgressReport report =
	    			new ProgressReport(self().path().name(), child.path().name(), "child_starting");
		    	this.reportLogger.tell(report, getSelf());
		    }
		    for(ActorRef child: JavaConversions.asJavaIterable(getContext().children()))
		    {
		    	Future<Object> f = Patterns.ask(child, new MoveToRoom(defaultRoom), 5000);
		    	Await.ready(f, Duration.create(5000, "millis"));
		    }
		    getSender().tell(new Object(), self());
        }
        catch(Exception e)
        {
            System.out.println(self().path().name()+".launchChildren(): caught Exception, TERMINATING SYSTEM IMMEDIATELY. Exception follows: " + e);
            System.out.println(self().path().name()+".launchChildren(): stack trace: ");
            e.printStackTrace(System.out);
            this.getContext().system().shutdown();
        }
    }
    private void announceChildren()
    {
        System.out.println(self().path().name() + ": ordering children to report!");
        for(ActorRef child: JavaConversions.asJavaIterable(this.getContext().children()))
        {
            child.tell(new AnnounceYourself(), this.self());
        }
    }
    private void restartChildren()
    {
        System.out.println(self().path().name() + ": restarting children!");
        for(ActorRef child: JavaConversions.asJavaIterable(this.getContext().children()))
        {
        	System.out.println(self().path().name()+".restartChildren(): restarting "+child.path().name());
            child.tell(new RestartYourself(), this.self());
        	//child.tell(akka.actor.Kill.getInstance(), this.self());
        }
    }
    private void setDefaultRoom(SetDefaultRoom msg)
    {
    	System.out.println(self().path().name() + ": setting default room to: " + 
    			            msg.room.path().name());
    	defaultRoom = msg.room;    	
    }
    private void moveAllChildrenToRoom(MoveAllChildrenToRoom msg)
    throws Exception
    {
        System.out.println(self().path().name() + ": moving all children to some room");
        for(ActorRef child: JavaConversions.asJavaIterable(this.getContext().children()))
        {
            child.tell(new MoveToRoom(msg.room), this.self());
        }
    }
}
