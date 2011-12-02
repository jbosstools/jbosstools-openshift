package org.jboss.tools.openshift.express.internal.ui.console;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.eclipse.ui.console.MessageConsole;
import org.eclipse.wst.server.core.IServer;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;

public class TailServerLogWorker implements Runnable {

	private final IServer server;

	private final Process process;

	private final MessageConsole console;

	public TailServerLogWorker(final IServer server, final MessageConsole console, final Process process) throws UnsupportedEncodingException,
			JSchException {
		this.server = server;
		this.console = console;
		this.process = process;
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

	

	static class MyLogger implements Logger {

		static java.util.Hashtable<Integer, String> name = new java.util.Hashtable<Integer, String>();
		static {
			name.put(new Integer(DEBUG), "DEBUG: ");
			name.put(new Integer(INFO), "INFO: ");
			name.put(new Integer(WARN), "WARN: ");
			name.put(new Integer(ERROR), "ERROR: ");
			name.put(new Integer(FATAL), "FATAL: ");
		}

		public boolean isEnabled(int level) {
			return true;
		}

		public void log(int level, String message) {
			System.err.print(name.get(new Integer(level)));
			System.err.println(message);
		}

	}

}
