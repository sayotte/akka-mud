/**
 * 
 */
package akkamud;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.trie.PatriciaTrie;

import akka.actor.ActorRef;
import akka.util.ByteString;

/**
 * @author stephen.ayotte
 *
 */
final class TelnetBoundToEntityState extends TelnetHandlerState
{
	private PatriciaTrie<String> lookupTrie;
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
    private InputLineHandler lineHandler;
    @Override
    protected InputLineHandler getLineHandler()
    { return lineHandler; }
    
    // Constructor and init methods
	public TelnetBoundToEntityState(ActorRef newConnRef,
								    ActorRef newHandlerRef,
								    ActorRef newAIRef)
	{
		super(newConnRef, newHandlerRef, newAIRef);
		lookupTrie = buildCommandTrie();
		inputRE = Pattern.compile("\\w+");
		lineHandler = this::boundToEntityLineHandler;
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
	
	// Bound-to-entity state implementation
	private TelnetHandlerState boundToEntityLineHandler(ByteString line)
	{
		// Convert to a String, find the first token, and use it as a key
		// in the lookupTrie to find any matches.
		String lineStr = line.utf8String().trim();
		Matcher match = inputRE.matcher(lineStr);
		if(! match.find())
			return this; // no input, just an empty line
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
			return this;
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
		return this;
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
