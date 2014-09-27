/**
 * 
 */
package akkamud;

import akka.actor.ActorRef;
import akka.actor.ActorContext;
import akka.util.ByteString;

/**
 * @author stephen.ayotte
 *
 */
final class TelnetLoginMenuState extends TelnetHandlerState
{
	private InputLineHandler lineHandler;
	private String accountName; 
	@Override
	protected InputLineHandler getLineHandler()
	{ return lineHandler; }
	
	/**
	 * @param newConnRef
	 * @param newHandlerRef
	 * @param newAIRef
	 */
	public TelnetLoginMenuState(ActorRef newConnRef, ActorRef newHandlerRef, ActorContext ctx)
	{
		super(newConnRef, newHandlerRef, ctx);
		lineHandler = this::loginMenuLineHandler;
		sendBanner();
		sendLoginMenu();
	}
	
	// Login-menu state implementation
	private TelnetHandlerState loginMenuLineHandler(ByteString line)
	{
		String lineStr = line.utf8String().trim();
		String r;
		switch(lineStr)
		{
			case "0":
				r = "Enter new username: ";
				sendOutput(r);
				lineHandler = this::newUsernameEntryLineHandler;
				break;
			case "1":
				r = "Username: ";
				sendOutput(r);
				lineHandler = this::usernameEntryLineHandler;
				break;
			case "2":
				r = "Enter email address: ";
				sendOutput(r);
				lineHandler = this::emailRecoveryLineHandler;
				break;
			default:
				r = "Unrecognized input '"+lineStr+"'\r\n";
				sendOutput(r);
				sendLoginMenu();
		}
		return this;
	}
	private void sendBanner()
	{
		String banner =
		  "Welcome to AkkaMUD!\r\n";
		sendOutput(banner);
	}
	private void sendLoginMenu()
	{
		String menu =
			"\r\n"+
			"[0] Create new account\r\n"+
			"[1] Login using existing account\r\n"+
		    "[2] Recover lost username/password\r\n"+
			"Enter selection: ";
		sendOutput(menu, false);
	}
	private TelnetHandlerState newUsernameEntryLineHandler(ByteString line)
	{
		String lineStr = line.utf8String().trim();
		String r = "You said '"+lineStr+"', did I get that right?\r\nY/N: ";
		sendOutput(r);
		return this;
	}
	private TelnetHandlerState newUsernameConfirmLineHandler(ByteString line)
	{
		return this;
	}
	private TelnetHandlerState newPasswordEntryLineHandler(ByteString line)
	{
		return this;
	}
	private TelnetHandlerState newPasswordReEntryLineHandler(ByteString line)
	{
		return this;
	}
	private TelnetHandlerState usernameEntryLineHandler(ByteString line)
	{
		String lineStr = line.utf8String().trim();
		accountName = lineStr;
		String r = "Password: ";
		sendOutput(r);
		lineHandler = this::passwordEntryLineHandler;
		return this;
	}
	private TelnetHandlerState passwordEntryLineHandler(ByteString line)
	{
		//String lineStr = line.utf8String().trim();
		System.out.println(getHandlerRef().path().name()+": logged in as '"+accountName+"'");
		String r = "Sounds good, whatever. Let's go to the main menu!\r\n";
		sendOutput(r);
		return new TelnetMainMenuState(getConnectionRef(),
									   getHandlerRef(),
									   getContext(),
									   accountName);
	}
	private TelnetHandlerState emailRecoveryLineHandler(ByteString line)
	{
		return this;
	}
	private TelnetHandlerState accountRecoveryLineHandler(ByteString line)
	{
		return this;
	}

	public boolean handleMessage(Object message, ActorRef from)
	{
		return false;
	}
}
