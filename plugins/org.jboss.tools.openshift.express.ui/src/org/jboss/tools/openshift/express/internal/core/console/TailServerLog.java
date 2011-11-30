package org.jboss.tools.openshift.express.internal.core.console;

/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.util.Base64;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class TailServerLog {

	public static void main(String[] args) {
		TailServerLog tailer = new TailServerLog();
		if(args.length != 3) {
			System.out.println("Usage: TailServerLog <appId> <host> <file>");
		}
		String user = args[0]; // eg: "c883f7e5f7824c49bff4682731ec6e56"
		String host = args[1]; // eg: "jee-xcoulon.rhcloud.com";
		String filePath = args[2]; // eg: "jee/logs/server.log";
		String options = "-f -n 100"; // well, you change it here ;-)
		try {
			tailer.tail(user, host, filePath, options, System.out);
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void tail(final String user, final String host, final String filePath, final String options,
			final OutputStream out) throws JSchException, IOException {
		
		JSch.setLogger(new MyLogger());
		final JSch jsch = new JSch();
		final String libraRSAPrivateKeyPath = System.getProperty("user.home") + "/.ssh/libra_id_rsa";
		jsch.addIdentity(libraRSAPrivateKeyPath);
		final Session session = jsch.getSession(user, host, 22);

		// username and password will be given via UserInfo interface.
		session.setUserInfo(new MyUserInfo());
		session.connect();

		// the rhc-tail-files command template
		// ssh_cmd =
		// "ssh -t #{app_uuid}@#{app}-#{namespace}.#{rhc_domain} 'tail#{opt['opts'] ? ' --opts ' + Base64::encode64(opt['opts']).chomp : ''} #{file_glob}'"
		final String command = buildCommand(filePath, options);
		final Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);

		// get I/O streams for remote tail
		final InputStream in = channel.getInputStream();
		try {channel.connect();
		out.write("...\n".getBytes());
		IOUtils.copy(in, out);
		} catch(Throwable e) {
			e.printStackTrace();
		}
		finally {
		session.disconnect();
		}

	}

	private String buildCommand(final String filePath, final String options) {
		StringBuilder commandBuilder = new StringBuilder("tail ");
		if (options != null && !options.isEmpty()) {
			final String opts = Base64.encodeBytes(options.getBytes());
			commandBuilder.append("--opts ").append(opts).append(" ");
		}
		commandBuilder.append(filePath);
		final String command = commandBuilder.toString();
		System.out.println("cmd= '" + command + "'");
		return command;
	}

	
	public static class MyUserInfo implements UserInfo {

		private String passphrase = null;

		public String getPassword() {
			return null;
		}

		public boolean promptYesNo(String str) {
			// always accept
			System.out.println("Answering yes to: " + str);
			return true;
		}

		public String getPassphrase() {
			return passphrase;
		}

		public boolean promptPassphrase(String message) {
			// prompt the user to enter their name
			System.out.print("Enter your passphrase for : ");
			// open up standard input
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			// read the username from the command-line; need to use try/catch
			// with the
			// readLine() method
			try {
				passphrase = br.readLine();
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your passphrase!");
				System.exit(1);
			}
			return true;
		}

		public boolean promptPassword(String message) {
			return true;
		}

		public void showMessage(String message) {
			System.out.println(message);
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
