/**
 * 
 */
package akkamud;

import java.util.concurrent.TimeUnit;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.ActorContext;
import akka.pattern.Patterns;
import akka.util.ByteString;

import static akkamud.EntityCommand.*;
import static akkamud.MobileSupervisorCommands.*;

/**
 * @author stephen.ayotte
 *
 */
final class TelnetMainMenuState extends TelnetHandlerState
{
	private InputLineHandler lineHandler;
	private String accountName;
	//private TelnetMenu lastMenu;

	/**
	 * @param newConnRef
	 * @param newHandlerRef
	 * @param newAIRef
	 */
	public TelnetMainMenuState(ActorRef newConnRef,
							   ActorRef newHandlerRef,
							   ActorContext ctx,
						   	   String authenticatedAccount)
	{
		super(newConnRef, newHandlerRef, ctx);
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
			case "2":
				sendBindToEntityPrompt();
				break;
			default:
				r = "The main menu isn't implemented. Let's pretend you're "+
				    "bound to an entity and transition to that state!\r\n";
				sendOutput(r);
		}

		return this;
	}
	private void sendMainMenu()
	{
//		lastMenu = new TelnetMenu();
//		lastMenu.addMenuItem(null, "List entities available for binding");
//		lastMenu.addMenuItem(null, "Search for a specific entity");
//		lastMenu.addMenuItem(null, "Bind to an entity");
		
		String menu =
			"\r\n"+
			"[0] List entities available for binding\r\n"+
			"[1] Search for a specific entity\r\n"+
			"[2] Bind to an entity\r\n"+
			//lastMenu.buildMenuString()+
		    "Enter selection: ";
		sendOutput(menu, false);
	}
	private void sendBindToEntityPrompt()
	{
		String prompt =
			"\r\n"+
			"Enter ID of entity: ";
		sendOutput(prompt);
		lineHandler = this::bindToEntityLineHandler;
	}
	private TelnetHandlerState bindToEntityLineHandler(ByteString line)
	throws Exception
	{
		String lineStr = line.utf8String().trim();
		ActorRef entityRef;
		String r;
		try
		{
			entityRef = 
				Util.resolvePathToRefSync(lineStr, getContext().system());
		}
		catch(Exception e)
		{
			r = "I couldn't find that entity, '"+lineStr+"', are you sure you"+
				" entered it correctly?\r\n";
			sendOutput(r);
			sendMainMenu();
			lineHandler = this::mainMenuLineHandler;
			return this;
		}
		
		sendOutput("Binding...");
		
		Future<Object> f = Patterns.ask(entityRef, new NewAI(getHandlerRef()), 5000);
		Await.ready(f, Duration.create(5000, TimeUnit.MILLISECONDS));
		TelnetHandlerState newState = 
			new TelnetBoundToEntityState(getConnectionRef(),
										 getHandlerRef(),
										 getContext(),
										 accountName,
										 entityRef);
		sendOutput(" bound!\r\n");
		return newState;
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
			r += "\t"+mobile.path().toStringWithoutAddress()+"\r\n";
		}
		sendOutput(r, false);
		sendMainMenu();
	}

	public boolean handleMessage(Object message, ActorRef from)
	{
		return false;
	}
}
