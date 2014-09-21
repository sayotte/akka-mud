/**
 * 
 */
package akkamud;

import java.util.concurrent.TimeUnit;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.ByteString;

import static akkamud.MobileSupervisorCommands.*;

/**
 * @author stephen.ayotte
 *
 */
final class TelnetMainMenuState extends TelnetHandlerState
{
	private InputLineHandler lineHandler;
	private String accountName;

	/**
	 * @param newConnRef
	 * @param newHandlerRef
	 * @param newAIRef
	 */
	public TelnetMainMenuState(ActorRef newConnRef,
							   ActorRef newHandlerRef,
						   	   String authenticatedAccount)
	{
		super(newConnRef, newHandlerRef);
		accountName = authenticatedAccount;
		lineHandler = this::mainMenuLineHandler;
		sendMainMenu();
	}
	
	@Override
	protected InputLineHandler getLineHandler()
	{
		return this.lineHandler;
	}
	
	// Main-menu state implementation
	private TelnetHandlerState mainMenuLineHandler(ByteString line)
	throws Exception
	{
		String lineStr = line.utf8String().trim();
		String r;
		switch(lineStr)
		{
			case "0":
				sendAllEntitiesAvailable();
				break;
			default:
				r = "The main menu isn't implemented. Let's pretend you're "+
				    "bound to an entity and transition to that state!\r\n";
				sendOutput(r);
		}

		return new TelnetBoundToEntityState(getConnectionRef(),
										    getHandlerRef(),
										    accountName,
										    null);
	}
	private void sendMainMenu()
	{
		String menu =
			"\r\n"+
			"[0] List entities available for binding\r\n"+
		    "[1] Search for a specific entity\r\n"+
			"[2] Bind to an entity\r\n"+
		    "Enter selection: ";
		sendOutput(menu, false);
	}
	private void sendAllEntitiesAvailable()
	throws Exception
	{
		final Future<Object> allMobileRefsReq =
			Patterns.ask(AkkaMud.mobileSup, new ListChildren(), 5000);
		final Object response = Await.result(allMobileRefsReq,
									Duration.create(5000, TimeUnit.MILLISECONDS));
		final Iterable<ActorRef> allMobileRefs = (Iterable)response;
		
		String r = "All entity names:\r\n";
		for(ActorRef mobile: allMobileRefs)
		{
			r += "\t"+mobile.path().name()+"\r\n";
		}
		sendOutput(r, false);
		sendMainMenu();
	}

}
