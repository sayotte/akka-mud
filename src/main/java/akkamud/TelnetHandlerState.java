/**
 * 
 */
package akkamud;

import org.apache.commons.lang3.text.WordUtils;

import akka.actor.ActorRef;
import akka.io.TcpMessage;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;

/**
 * @author stephen.ayotte
 *
 */
abstract class TelnetHandlerState
{
	protected interface InputLineHandler
	{
		TelnetHandlerState invoke(ByteString line);
	}
	
	private ActorRef connectionRef;
	private ActorRef handlerRef;
	private ActorRef AIRef;
	
	protected ActorRef getConnectionRef(){ return this.connectionRef; }
	protected ActorRef getHandlerRef(){ return this.handlerRef; }
	protected ActorRef getAIRef(){ return this.AIRef; }
	abstract protected InputLineHandler getLineHandler();
	
	public TelnetHandlerState(ActorRef newConnRef, ActorRef newHandlerRef, ActorRef newAIRef)
	{
		this.connectionRef = newConnRef;
		this.handlerRef = newHandlerRef;
		this.AIRef = newAIRef;
	}
	
	public TelnetHandlerState handleLine(ByteString line)
	{
		return getLineHandler().invoke(line);
	}
	
	protected void sendOutput(String output)
	{
		sendOutput(output, true);
	}
	protected void sendOutput(String output, boolean wrap)
	{
		String r;
		if(wrap)
			r = WordUtils.wrap(output, 100);
		else
			r = output;
		ByteString out = new ByteStringBuilder().putBytes(r.getBytes()).result();
		getConnectionRef().tell(TcpMessage.write(out), getHandlerRef());
	}
}
