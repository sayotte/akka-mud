// Use Allman style bracing or I'll stab you.

package akkamud;

import akka.actor.UntypedActor;
import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.io.Serializable;
// import java.util.List;
// import java.util.ArrayList;

import akka.japi.Function; // new Function<T, R>() { func_declaration_here }
import akka.actor.SupervisorStrategy;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.stop;
import static akka.actor.SupervisorStrategy.escalate;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.OneForOneStrategy;

import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.concurrent.duration.Duration;

public class AkkaMud
{
    public static class StartChildren implements Serializable{}
    public static class ReportChildren implements Serializable{}
    public static class RestartChildren implements Serializable{}
    public static class AnnounceYourself implements Serializable{}
    public static class RestartYourself implements Serializable{}

    public static class MobileEntity extends UntypedActor
    {
//         /**
//         * Create Props for an actor of this type.
//         * @param magicNumber The magic number to be passed to this actor’s constructor.
//         * @return a Props for creating this actor, which can then be further configured
//         *         (e.g. calling `.withDispatcher()` on it)
//         */
//         public static Props props(final int magicNumber)
//         {
//             // I don't understand this syntax at ALL, but apparently it lets us accept
//             // a parameter when we're instantiating, like so:
//             // ActorRef blah = system.actorOf(MobileEntity.props(__arg__), "blah");
//             return Props.create(new Creator<DemoActor>()
//             {
//                 private static final long serialVersionUID = 1L;
// 
//                 @Override
//                 public DemoActor create() throws Exception
//                 {
//                     return new DemoActor(magicNumber);
//                 }
//             });
//         }

        public void onReceive(Object message) throws Exception
        {
            if(message instanceof AnnounceYourself)
            {
                System.out.println("Mobile entity '" + this.self().path().name() + "' here!");
            }
            else if(message instanceof RestartYourself)
            {
                throw new Exception();
            }
            else
                unhandled(message);
        }
    }

    public static class MobileSupervisor extends UntypedActor
    {
        private static Function<Throwable, Directive> decider = 
            new Function<Throwable, Directive>()
            {
                @Override
                public Directive apply(Throwable t)
                {
                    if(t instanceof java.lang.Exception)
                    {
                        return resume();
                    }
                    return escalate();
                }
            };

        private static SupervisorStrategy strategy = 
            new OneForOneStrategy(10,                           // max retries
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
            {
                System.out.println("Mobile supervisor, launching children!");
                System.out.println("Mobile supervisor name: " + this.self().path().name());
                int i;
                for(i = 0; i < 10; i++)
                {
                    this.getContext().actorOf(Props.create(MobileEntity.class),
                                              "mobile" + Integer.toString(i));
                }
            }
            else if(message instanceof ReportChildren)
            {
                System.out.println("Mobile supervisor, ordering children to report!");
                for(ActorRef child: JavaConversions.asJavaIterable(this.getContext().children()))
                {
                    child.tell(new AnnounceYourself(), this.self());
                }
            }
            else if(message instanceof RestartChildren)
            {
                System.out.println("Mobile supervisor, resuming children!");
                for(ActorRef child: JavaConversions.asJavaIterable(this.getContext().children()))
                {
                    child.tell(new RestartYourself(), this.self());
                }
            }
            else
                unhandled(message);
        }
    }

    public static void main(String[] args)
    {
        final ActorSystem system = ActorSystem.create("mud-actorsystem");

        final ActorRef mobileSup = system.actorOf(Props.create(MobileSupervisor.class),
                                                  "mobile-supervisor");

        final StructuredLogger logger = StructuredLogger.getLogger();

        mobileSup.tell(new StartChildren(), null);
        mobileSup.tell(new ReportChildren(), null);
        mobileSup.tell(new RestartChildren(), null);
        mobileSup.tell(new ReportChildren(), null);

        return;
    }

}
