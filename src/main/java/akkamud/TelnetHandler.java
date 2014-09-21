/**
 * 
 */
package akkamud;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.regex.Matcher;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.io.Tcp.ConnectionClosed;
import akka.io.Tcp.Received;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
//import scala.Byte;
//import scala.Array;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.text.WordUtils;

//import static akkamud.TelnetHandlerCommands.*;

/**
 * @author stephen.ayotte
 *
 */
class TelnetHandler extends UntypedActor
{	
	private ActorRef connectionRef;
	private ByteString bufferedInput;
	private TelnetHandlerState stateHandler;
	
    // Constructor and initialization routines
	public TelnetHandler(ActorRef connRef)
	{		
		System.out.println(self().path().name()+": constructor...");
		connectionRef = connRef;
		
		bufferedInput = new ByteStringBuilder().result();
		
	    stateHandler = new TelnetLoginMenuState(connectionRef, getSelf(), null);
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
		stateHandler = stateHandler.handleLine(bufferedInput.slice(0, lineEnd));
		bufferedInput = bufferedInput.slice(lineEnd + 1, bufferedInput.size());
		return true;
	}

}
