/**
 * 
 */
package akkamud;

import java.io.Serializable;

/**
 * @author stephen.ayotte
 *
 */
class ReportCommands {
	public static final class ProgressReport implements Serializable
	{
		public String supervisor;
		public String child;
		public String context;
		public ProgressReport(String newSupervisor, String newChild, String newContext)
		{
			supervisor = newSupervisor;
			child = newChild;
			context = newContext;
		}
	}
	public static final class ExceptionReportEx extends Exception
	{
		public String who;
		public Throwable reason;
		public String ext;
		public ExceptionReportEx(String newWho, Throwable newReason, String newExt)
		{
			who = newWho;
			reason = newReason;
			ext = newExt;
		}
		public String toString(){ return "Wrapped exception: " + this.reason; }
	}
	public static final class SupervisorReport implements Serializable
	{
		public String supervisor;
		public String child;
		public String context;
		public Throwable reason;
		public String ext;
		
		public SupervisorReport(String newSupervisor, String newChild, 
								String newContext, Throwable newReason,
								String newExt)
		{
			supervisor = newSupervisor;
			child = newChild;
			context = newContext;
			reason = newReason;
			ext = newExt;
		}
	}
}
