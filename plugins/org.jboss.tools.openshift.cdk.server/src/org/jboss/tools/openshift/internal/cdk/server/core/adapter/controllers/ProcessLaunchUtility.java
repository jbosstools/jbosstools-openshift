/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.runtime.Path;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationLookupStrategy;
import org.jboss.tools.openshift.internal.common.core.util.ThreadUtils;

public class ProcessLaunchUtility {

	public static Process callProcess(String rootCommand, String[] args, File vagrantDir, Map<String, String> env,
			boolean interactive) throws IOException {
		ensureCommandOnPath(rootCommand, env);
		String[] envp = (env == null ? null : convertEnvironment(env));

		List<String> cmd = new ArrayList<>();
		cmd.add(rootCommand);
		cmd.addAll(Arrays.asList(args));
		Process p = null;
		if (interactive) {
			p = ProcessFactory.getFactory().exec(cmd.toArray(new String[0]), envp, vagrantDir,
					new PTY(PTY.Mode.TERMINAL));
		} else {
			p = Runtime.getRuntime().exec(cmd.toArray(new String[0]), envp, vagrantDir);
		}
		return p;
	}

	/*
	 * The following methods are for dealing with raw calls to vagrant, not using
	 * launch configurations at all.
	 */

	/*
	 * Convert a string/string hashmap into an array of string environment variables
	 * as required by java.lang.Runtime This will super-impose the provided
	 * environment variables ON TOP OF the existing environment in eclipse, as users
	 * may not know *all* environment variables that need to be set, or to do so may
	 * be tedious.
	 */
	public static String[] convertEnvironment(Map<String, String> env) {
		if (env == null || env.size() == 0)
			return null;

		// Create a new map based on pre-existing environment of Eclipse
		Map<String, String> original = new HashMap<>(System.getenv());

		// Add additions or changes to environment on top of existing
		original.putAll(env);

		// Convert the combined map into a form that can be used to launch process
		ArrayList<String> ret = new ArrayList<>();
		Iterator<String> it = original.keySet().iterator();
		String working = null;
		while (it.hasNext()) {
			working = it.next();
			ret.add(working + "=" + original.get(working)); //$NON-NLS-1$
		}
		return ret.toArray(new String[ret.size()]);
	}

	private static void ensureCommandOnPath(String rootCommand, Map<String, String> env) {
		CommandLocationLookupStrategy.get().ensureOnPath(env, new Path(rootCommand).removeLastSegments(1).toOSString());
	}

	public static String[] call(String rootCommand, String[] args, File workingDir, Map<String, String> env,
			int timeout, boolean interactive) throws IOException, CommandTimeoutException {
		final Process p = callProcess(rootCommand, args, workingDir, env, interactive);

		InputStream errStream = p.getErrorStream();
		InputStream inStream = p.getInputStream();

		StreamGobbler inGob = new StreamGobbler(inStream);
		StreamGobbler errGob = new StreamGobbler(errStream);

		inGob.start();
		errGob.start();

		Integer exitCode = null;
		if (p.isAlive()) {

			exitCode = ThreadUtils.runWithTimeout(timeout, new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					return p.waitFor();
				}
			});
		} else {
			exitCode = p.exitValue();
		}

		List<String> inLines = null;
		if (exitCode == null) {
			inGob.cancel();
			errGob.cancel();

			// Timeout reached
			p.destroyForcibly();
			inLines = inGob.getOutput();
			List<String> errLines = errGob.getOutput();
			throw new CommandTimeoutException(inLines, errLines);
		} else {
			inLines = inGob.getOutput();
		}

		return (String[]) inLines.toArray(new String[inLines.size()]);
	}
	

	public static void linkTerminal(IServer server, Process p) {
		InputStream in = p.getInputStream();
		InputStream err = p.getErrorStream();
		OutputStream out = p.getOutputStream();
		Map<String, Object> properties = new HashMap<>();
		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID,
				"org.eclipse.tm.terminal.connector.streams.launcher.streams");
		properties.put(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID,
				"org.eclipse.tm.terminal.connector.streams.StreamsConnector");
		properties.put(ITerminalsConnectorConstants.PROP_TITLE, server.getName());
		properties.put(ITerminalsConnectorConstants.PROP_LOCAL_ECHO, false);
		properties.put(ITerminalsConnectorConstants.PROP_FORCE_NEW, true);
		properties.put(ITerminalsConnectorConstants.PROP_STREAMS_STDIN, out);
		properties.put(ITerminalsConnectorConstants.PROP_STREAMS_STDOUT, in);
		properties.put(ITerminalsConnectorConstants.PROP_STREAMS_STDERR, err);
		ITerminalService service = TerminalServiceFactory.getService();
		service.openConsole(properties, null);
	}
}
