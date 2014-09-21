/**
 * 
 */
package akkamud;

import akka.actor.ActorRef;
import akka.util.ByteString;

/**
 * @author stephen.ayotte
 *
 */
final class TelnetMainMenuState extends TelnetHandlerState
{
	private InputLineHandler lineHandler;

	/**
	 * @param newConnRef
	 * @param newHandlerRef
	 * @param newAIRef
	 */
	public TelnetMainMenuState(ActorRef newConnRef, ActorRef newHandlerRef,
			ActorRef newAIRef)
	{
		super(newConnRef, newHandlerRef, newAIRef);
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
	{
		String lineStr = line.utf8String().trim();
		String r = "The main menu isn't implemented. Let's pretend you're "+
		           "bound to an entity and transition to that state!\r\n";
		sendOutput(r);
		//enterBoundToEntityState();
		return new TelnetBoundToEntityState(getConnectionRef(),
										    getHandlerRef(),
										    getAIRef());
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
	{
		;
	}

}
