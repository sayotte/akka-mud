/**
 * 
 */
package akkamud;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.actor.SupervisorStrategy.Directive;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.io.Tcp.Bound;
import akka.io.Tcp.Command;
import akka.io.Tcp.Connected;
import akka.japi.Function;
import akka.pattern.Patterns;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;

import static akkamud.ReportCommands.*;

/**
 * @author stephen.ayotte
 *
 */
class TelnetListener extends UntypedActor
{
	// concrete member variables
	private final ActorRef reportLogger;
	private final SupervisorStrategy strategy;
	private final ActorRef tcpManager;
	private final int listenPort;
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
                    	System.out.println(self().path().toStringWithoutAddress()+": I caught an exception of type '" + t.getClass().getSimpleName() + "'");                	
                        return escalate();
                    }
                }
            };
    private int connectionCounter;
            
	// constructor
	public TelnetListener(ActorRef manager, int port, ActorRef newLogger)
	{
		this.tcpManager = manager;
		this.listenPort = port;
		this.reportLogger = newLogger;
		this.connectionCounter = 0;
		this.strategy = new ReportingOneForOneStrategy(10,                           // max retries
                									   Duration.create(1, "minute"), // within this time period
										               decider,                      // with this "decider" for handling
										               this.reportLogger);
	}

	// accessors
	public SupervisorStrategy supervisorStrategy(){ return this.strategy; }
	
	// Akka reactive model
	@Override
	public void preStart() throws Exception
	{
		InetSocketAddress addr =
			new InetSocketAddress("localhost", this.listenPort);
		Command bindCmd =
			TcpMessage.bind(getSelf(), addr, 100);
	    Future<Object> f = Patterns.ask(tcpManager, bindCmd, 1000);
	    Object response = Await.result(f, Duration.create(100, TimeUnit.MILLISECONDS));
	    if(!(response instanceof Bound))
	    {
	    	throw new Exception(self().path().toStringWithoutAddress()+": Failed to bind to port?!");
	    }
	}
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Connected)
			handleNewConnection((Connected)message);
		else
			System.out.println(self().path().name()+": unhandled message: "+message);
			unhandled(message);
	}

	// implementation methods
	private void handleNewConnection(Connected msg)
	{
		Props p = Props.create(TelnetHandler.class);
		ActorRef handler = getContext().actorOf(p, "telnet-handler"+this.connectionCounter);
		ProgressReport report =
			new ProgressReport(self().path().name(), handler.path().name(), "child_starting");
		
		getSender().tell(TcpMessage.register(handler), getSelf());
		this.connectionCounter++;
	}
}
