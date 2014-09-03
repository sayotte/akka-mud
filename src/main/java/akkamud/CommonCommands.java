/**
 * 
 */
package akkamud;

import java.io.Serializable;

/**
 * @author stephen.ayotte
 *
 */
class CommonCommands {
	public static final class PassFail implements Serializable
	{
		private static final long serialVersionUID = 1;
		public boolean status;
		public PassFail(boolean newStatus){ status = newStatus; } 
	}
}
