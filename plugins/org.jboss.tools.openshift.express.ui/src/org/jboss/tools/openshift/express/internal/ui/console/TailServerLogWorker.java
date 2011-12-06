package org.jboss.tools.openshift.express.internal.ui.console;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.ui.console.MessageConsole;

import com.jcraft.jsch.Logger;

/**
 * The underlying 'Tail' worker, that uses an established RemoteSession (with
 * the help of JGit), runs in a dedicated process and displays the outputstream
 * into a specific console. This worker is a <code>java.lang.Runnable</code> in
 * order to run in a separate thread
 * 
 * @author Xavier Coulon
 * 
 */
public class TailServerLogWorker implements Runnable {

	/** the remote 'tail' process. */
	private final Process process;

	/** the output message console. */
	private final MessageConsole console;

	/** the SSH session. */
	private final RemoteSession remoteSession;

	/**
	 * Constructor.
	 * 
	 * @param console
	 * @param process
	 * @param remoteSession
	 */
	public TailServerLogWorker(final MessageConsole console, final Process process, final RemoteSession remoteSession) {
		this.console = console;
		this.process = process;
		this.remoteSession = remoteSession;
	}

	@Override
	public void run() {
		try {
			// get I/O streams for remote tail
			final InputStream in = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			// Read File Line By Line
			while ((line = reader.readLine()) != null) {
				console.newMessageStream().println(line);
			}
		} catch (Throwable e) {
			org.jboss.tools.openshift.express.internal.utils.Logger.error(
					"Error while receiving the remote server log", e);
		} finally {

		}
	}

	/**
	 * Method called when the overall 'tail' process should be stopped: the
	 * underlying ssh remote session must be disconnected and the running
	 * process must be destroyed.
	 */
	public void stop() {
		this.remoteSession.disconnect();
		this.process.destroy();
	}

	/**
	 * Bridge between the JSch logger and the Eclipse logger (to ouput results
	 * in the .log files and/or into the 'Error log' view.
	 * 
	 * @author Xavier Coulon
	 * 
	 */
	static class JschToEclipseLogger implements Logger {

		static java.util.Hashtable<Integer, String> name = new java.util.Hashtable<Integer, String>();
		static {
			name.put(new Integer(DEBUG), "DEBUG: ");
			name.put(new Integer(INFO), "INFO: ");
			name.put(new Integer(WARN), "WARN: ");
			name.put(new Integer(ERROR), "ERROR: ");
			name.put(new Integer(FATAL), "FATAL: ");
		}

		@Override
		public boolean isEnabled(int level) {
			return true;
		}

		@Override
		public void log(int level, String message) {
			switch (level) {
			case DEBUG:
			case INFO:
				org.jboss.tools.openshift.express.internal.utils.Logger.debug(message);
				break;
			case WARN:
				org.jboss.tools.openshift.express.internal.utils.Logger.warn(message);
				break;
			case ERROR:
			case FATAL:
				org.jboss.tools.openshift.express.internal.utils.Logger.error(message);
				break;
			}
		}

	}

}
