/**
 * 
 */
package akkamud;

import akka.actor.UntypedActor;
import akka.io.Tcp.ConnectionClosed;
import akka.io.Tcp.Received;
import akka.io.TcpMessage;
import akka.util.ByteString;

/**
 * @author stephen.ayotte
 *
 */
class TelnetHandler extends UntypedActor
{
	@Override
	public void onReceive(Object message) throws Exception {
	    if(message instanceof Received)
	    {
	        final ByteString data = ((Received)message).data();
	        System.out.println(self().path().name()+": echoing data: "+data.utf8String());
	        getSender().tell(TcpMessage.write(data), getSelf());
	    }
	    else if(message instanceof ConnectionClosed)
	    {
	    	System.out.println(self().path().name()+": connection closed, shutting down");
	        getContext().stop(getSelf());
	    }
	}
}
