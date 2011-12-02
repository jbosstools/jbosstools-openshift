package org.jboss.tools.openshift.express.internal.ui.console;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.eclipse.ui.console.MessageConsole;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.client.utils.Base64Encoder;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class TailServerLogWorker implements Runnable {

	private final IServer server;

	private final Session session;

	private final Channel channel;

	private final MessageConsole console;

	public TailServerLogWorker(final IServer server, final MessageConsole console, final Channel channel) throws UnsupportedEncodingException,
			JSchException {
		this.server = server;
		this.console = console;
		this.channel = channel;
		this.session = channel.getSession();
	}

	@Override
	public void run() {
		try {
			// get I/O streams for remote tail
			final InputStream in = channel.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			// Read File Line By Line
			while ((line = reader.readLine()) != null) {
				console.newMessageStream().println(line);
			}
			if (!session.isConnected()) {
				org.jboss.tools.openshift.express.internal.utils.Logger.warn("Session closed");

			}
			if (channel.isClosed()) {
				org.jboss.tools.openshift.express.internal.utils.Logger.warn("Channel closed with exit status "
						+ channel.getExitStatus());
			}
		} catch (Throwable e) {
			org.jboss.tools.openshift.express.internal.utils.Logger.error(
					"Error while receiving the remote server log", e);
		} finally {
			org.jboss.tools.openshift.express.internal.utils.Logger.warn("Disconnecting from the remote server log");
			session.disconnect();

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
