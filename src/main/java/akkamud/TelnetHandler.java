/**
 * 
 */
package akkamud;

import java.util.SortedMap;

import akka.actor.UntypedActor;
import akka.io.Tcp.ConnectionClosed;
import akka.io.Tcp.Received;
import akka.io.TcpMessage;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;

import scala.Byte;
import scala.Array;

import org.apache.commons.collections4.trie.PatriciaTrie;

/**
 * @author stephen.ayotte
 *
 */
class TelnetHandler extends UntypedActor
{
	private PatriciaTrie lookupTrie;
	private ByteString bufferedInput;
	
	public TelnetHandler()
	{
		lookupTrie = new PatriciaTrie();
//		lookupTrie.put("keyzzzz", "value");
//		SortedMap<String, String> set = lookupTrie.prefixMap("k");
//		String key = set.firstKey();
//		System.out.println(self().path().name()+": first key in trie: "+key);
		
		System.out.println(self().path().name()+": constructor...");
		bufferedInput = new ByteStringBuilder().result();
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
	    if(message instanceof Received)
	    {
	        final ByteString data = ((Received)message).data();
	        bufferReceived(data);
	    }
	    else if(message instanceof ConnectionClosed)
	    {
	    	System.out.println(self().path().name()+": connection closed, shutting down");
	        getContext().stop(getSelf());
	    }
	}
	
	private void bufferReceived(ByteString input)
	{
		bufferedInput = bufferedInput.$plus$plus(input); // str += input
		while(handleBufferedInput() == true){ ; }
	}
	private boolean handleBufferedInput()
	{
		int lineEnd = bufferedInput.indexOf('\n');
		if(lineEnd == -1)
			return false;
		handleInputLine(bufferedInput.slice(0,  lineEnd));
		bufferedInput = bufferedInput.slice(lineEnd + 1, bufferedInput.size());
		return true;
	}
	private void handleInputLine(ByteString line)
	{
		String str = line.utf8String().trim() + "\n";
		ByteString out = new ByteStringBuilder().putBytes(str.getBytes()).result();
		getSender().tell(TcpMessage.write(out), getSelf());
	}
}
