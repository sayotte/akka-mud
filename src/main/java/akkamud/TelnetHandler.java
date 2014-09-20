/**
 * 
 */
package akkamud;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.io.Tcp.ConnectionClosed;
import akka.io.Tcp.Received;
import akka.io.TcpMessage;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import scala.Byte;
import scala.Array;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.text.WordUtils;

//import static akkamud.TelnetHandlerCommands.*;

/**
 * @author stephen.ayotte
 *
 */
class TelnetHandler extends UntypedActor
{
	private interface InputLineHandler
	{
		void invoke(ByteString line);
	}
	
	private ActorRef connectionRef;
	private InputLineHandler lineHandler;
	private PatriciaTrie<String> lookupTrie;
	private ByteString bufferedInput;
	private Pattern inputRE;
    private final String[] recognizedCommands =
	{
		// direction movement
		"east",
		"west",
		"north",
		"south",
		// speed of movement
		"jog",
		"run",
		"walk",
		// entity interaction
		"use",
		"open",
		"close",
		// miscellaneous
		"enable",
		"?",
		"help",
	};
    private final String[] adminCommands =
	{
		"halt",
		"freeze",
		"shutdown"
	};
	
    // Constructor and initialization routines
	public TelnetHandler(ActorRef connRef)
	{		
		System.out.println(self().path().name()+": constructor...");
		connectionRef = connRef;
		
		bufferedInput = new ByteStringBuilder().result();
		
		inputRE = Pattern.compile("\\w+");
		
	    lookupTrie = buildCommandTrie();
	    
	    enterLoginMenuState();
	}
	private PatriciaTrie<String> buildCommandTrie()
	{
		PatriciaTrie<String> trie = new PatriciaTrie<String>();
//        File commandsFile = new File(filename);
//        Element root = new SAXBuilder().build(commandsFile).getRootElement();
//        System.out.println(self().path().name()+": text-commands.xml root element: "+root.getName());
//        IteratorIterable<Element> commands = 
//        	root.getDescendants(Filters.element("command"));
//        while(commands.hasNext())
//        {
//        	Element command = commands.next();
//        	String inputText = command.getText();
		for(String command: this.recognizedCommands)
		{
        	trie.put(command, null);
        }
        return trie;
	}

	// Main Akka receive-loop implementation
	@Override
	public void onReceive(Object message) throws Exception {
	    if(message instanceof Received)
	        handleReceived(((Received)message).data());
	    else if(message instanceof ConnectionClosed)
	    {
	    	System.out.println(self().path().name()+": connection closed, shutting down");
	        getContext().stop(getSelf());
	    }
	}

	// Helper methods
	private void handleReceived(ByteString input)
	{
		bufferedInput = bufferedInput.$plus$plus(input); // str += input
		while(handleBufferedInput() == true){ ; }
	}
	private boolean handleBufferedInput()
	{
		int lineEnd = bufferedInput.indexOf('\n');
		if(lineEnd == -1)
			return false;
		lineHandler.invoke(bufferedInput.slice(0, lineEnd));;
		bufferedInput = bufferedInput.slice(lineEnd + 1, bufferedInput.size());
		return true;
	}
	private void sendOutput(String output)
	{
		sendOutput(output, true);
	}
	private void sendOutput(String output, boolean wrap)
	{
		String r;
		if(wrap)
			r = WordUtils.wrap(output, 100);
		else
			r = output;
		ByteString out = new ByteStringBuilder().putBytes(r.getBytes()).result();
		connectionRef.tell(TcpMessage.write(out), getSelf());
	}

	// Login-menu state implementation
	private void enterLoginMenuState()
	{
		lineHandler = this::loginMenuLineHandler;
		sendBanner();
		sendLoginMenu();
	}
	private void loginMenuLineHandler(ByteString line)
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
	private void newUsernameEntryLineHandler(ByteString line)
	{
		String lineStr = line.utf8String().trim();
		String r = "You said '"+lineStr+"', did I get that right?\r\nY/N: ";
		sendOutput(r);
	}
	private void newUsernameConfirmLineHandler(ByteString line)
	{
		
	}
	private void newPasswordEntryLineHandler(ByteString line)
	{
		
	}
	private void newPasswordReEntryLineHandler(ByteString line)
	{
		
	}
	private void usernameEntryLineHandler(ByteString line)
	{
		String lineStr = line.utf8String().trim();
		String r = "Password: ";
		sendOutput(r);
		lineHandler = this::passwordEntryLineHandler;
	}
	private void passwordEntryLineHandler(ByteString line)
	{
		String lineStr = line.utf8String().trim();
		String r = "Sounds good, whatever. Let's go to the main menu!\r\n";
		sendOutput(r);
		enterMainMenuState();
	}
	private void emailRecoveryLineHandler(ByteString line)
	{
		
	}
	private void accountRecoveryLineHandler(ByteString line)
	{
		
	}
	
	// Main-menu state implementation
	private void enterMainMenuState()
	{
		lineHandler = this::mainMenuLineHandler;
		sendMainMenu();
	}
	private void mainMenuLineHandler(ByteString line)
	{
		String lineStr = line.utf8String().trim();
		String r = "The main menu isn't implemented. Let's pretend you're "+
		           "bound to an entity and transition to that state!\r\n";
		sendOutput(r);
		enterBoundToEntityState();
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
	
	// Bound-to-entity state implementation
	private void enterBoundToEntityState()
	{
		lineHandler = this::boundToEntityLineHandler;
	}
	private void boundToEntityLineHandler(ByteString line)
	{
		// Convert to a String, find the first token, and use it as a key
		// in the lookupTrie to find any matches.
		String lineStr = line.utf8String().trim();
		Matcher match = inputRE.matcher(lineStr);
		if(! match.find())
			return; // no input, just an empty line
		int start = match.start();
		int end = match.end();
		String commandStr = lineStr.substring(start,  end).toLowerCase();		
		SortedMap<String, String> commandMatches = lookupTrie.prefixMap(commandStr);
		
		// We expect exactly one match; anything else is unknown or ambiguous
		// Note that if we're munging the lookupTrie at runtime, it could be
		// a valid command that is handled below, but which is unknown in the
		// context of this telnet handler.
		if(commandMatches.size() != 1)
		{
			String r = "Command was: " + commandStr + "\r\n";
			if(commandMatches.size() <= 0)
				r += "Unrecognized command.\r\n";
			else if(commandMatches.size() > 1)
			{
				r += "Ambiguous command. Possible completions:";
				Iterator<String> i = commandMatches.keySet().iterator();
				while(i.hasNext())
				{
					String key = i.next();
					r += " " + key;
				}
				r += "\r\n";
			}
			sendOutput(r);
			return;
		}
		
		// We found a recognized command; take appropriate action
		commandStr = commandMatches.firstKey();
//		System.out.println(self().path().name()+": commandStr is '"+commandStr+"'");
		switch(commandStr)
		{
			case "enable":
				doEnable();
				break;
			case "help":
			case "?":
				doHelp();
				break;
			case "walk":
				doWalk(lineStr);
				break;
			default:
				doUnhandledCommand(commandStr);
				break;
		}
		return;
	}
	private void doUnhandledCommand(String commandStr)
	{
		String r =
			"Well, this is embarassing... '"+commandStr+"' is a recognized command, "+
			"but it's not linked to any code!\r\n";
		sendOutput(r);
		return;
	}
	private void doEnable()
	{
		for(String command: this.adminCommands)
		{
			this.lookupTrie.put(command, null);
		}
	}
	private void doHelp()
	{
		String r = "Recognized commands:";
		Iterator<String> i = lookupTrie.keySet().iterator();
		while(i.hasNext())
		{
			r += " "+i.next();
		}
		r += "\r\n";
		sendOutput(r);
		return;
	}
	private void doWalk(String lineStr)
	{
		return;
	}
}
