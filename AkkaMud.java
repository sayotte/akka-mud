// Use Allman style bracing or I'll stab you.

import akka.actor.UntypedActor;
import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.io.Serializable;
// import java.util.List;
// import java.util.ArrayList;

import scala.collection.Iterator;
import scala.collection.JavaConversions;

public class AkkaMud
{
    public static class StartChildren implements Serializable{}
    public static class AnnounceYourself implements Serializable{}

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
                throw new Exception();
            }
            else
                unhandled(message);
        }
    }

    public static class MobileSupervisor extends UntypedActor
    {
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

                for(ActorRef child: JavaConversions.asJavaIterable(this.getContext().children()))
                {
                    child.tell(new AnnounceYourself(), this.self());
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

        mobileSup.tell(new StartChildren(), null);

        return;
    }
}
