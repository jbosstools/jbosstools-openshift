package org.jboss.tools.openshift.express.internal.ui.console;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.ui.console.MessageConsole;


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
			org.jboss.tools.openshift.express.internal.ui.utils.Logger.error(
					"Error while receiving the remote server log", e);
			console.newMessageStream().println("Error while receiving the remote server log: " + e.getMessage());
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

}
