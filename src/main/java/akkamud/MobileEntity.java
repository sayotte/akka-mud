package akkamud;

import akka.actor.UntypedActor;
import akka.persistence.UntypedPersistentActor;
import akka.japi.Procedure;

import static akkamud.EntityCommand.*;

// class MobileEntity extends UntypedPersistentActor
class MobileEntity extends UntypedActor
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
            System.out.println("Mobile entity '" + this.self().path().name() + "' here!");
        else if(message instanceof RestartYourself)
            throw new Exception();
        else
            unhandled(message);
    }
}