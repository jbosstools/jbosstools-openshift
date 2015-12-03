/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Platform;

public class CommandLocationLookupStrategy {
	private static final String LINUX_WHICH = "which ";
	private static final String WINDOWS_WHERE = "where ";
	private static final String LINUX_PATHVAR = "PATH";
	private static final String WINDOWS_PATHVAR = "Path";

	public static final CommandLocationLookupStrategy WINDOWS_STRATEGY = 
			new CommandLocationLookupStrategy(WINDOWS_WHERE, ";", WINDOWS_PATHVAR, new String[]{".exe", ".com"});
	public static final CommandLocationLookupStrategy LINUX_STRATEGY = 
			new CommandLocationLookupStrategy(LINUX_WHICH, ":", LINUX_PATHVAR, new String[]{});
	
	public static CommandLocationLookupStrategy get() {
		String os = Platform.getOS();
		if( Platform.OS_WIN32.equals(os)) {
			return WINDOWS_STRATEGY;
		}
		return LINUX_STRATEGY;
	}
	
	private String which, delim, pathvar;
	private String[] suffixes;
	public CommandLocationLookupStrategy(String which, String delim, String pathvar, String[] suffixes) {
		this.which = which;
		this.delim = delim;
		this.pathvar = pathvar;
		this.suffixes = suffixes;
	}
	
	public String search(CommandLocationBinary binary) {
		return search(binary, 2000);
	}
	
	public String search(CommandLocationBinary binary, int timeout) {
		String cmd = binary.getCommand(Platform.getOS());
		String defaultLoc = binary.getDefaultLoc(Platform.getOS());
		return findLocation(defaultLoc, cmd, which, delim, pathvar, timeout);
	}
	
	/**
	 * This method will try to find the given command. 
	 * 
	 * If the default location exists, it will use that. 
	 * 
	 * It will then attempt to search for the command name (with all possible suffixes)
	 * somewhere in the system path. 
	 * 
	 * If that still fails, it will run one where / which command to locate the command. 
	 * This will be called without the suffix. 
	 * 
	 * @param defaultLoc
	 * @param cmd
	 * @param which
	 * @param delim
	 * @param pathvar
	 * @param timeout
	 * @return
	 */
	private  String findLocation(String defaultLoc, String cmd, String which, String delim, String pathvar, int timeout) {
		if( defaultLoc != null && new File(defaultLoc).exists()) {
			return defaultLoc;
		}
		String ret = searchPath(System.getenv(pathvar), delim, cmd);
		if( ret == null ) {
			ret = runCommand(which + cmd, timeout);
		}
		return ret;
	}
	
	/**
	 * Get all possible command names by appending the various suffixes to the command name
	 * @param commandName
	 * @return
	 */
	private String[] getPossibleCommandNames(String commandName) {
		ArrayList<String> ret = new ArrayList<String>(5);
		ret.add(commandName);
		for( int i = 0; i < suffixes.length; i++ ) {
			ret.add(commandName + suffixes[i]);
		}
		return (String[]) ret.toArray(new String[ret.size()]);
	}
	
	private String searchPath(String path, String delim, String commandName) {
		String[] roots = path.split(delim);
		String[] withSuffixes = getPossibleCommandNames(commandName);
		for( int i = 0; i < roots.length; i++ ) {
			for( int j = 0; j < withSuffixes.length; j++ ) {
				File test = new File(roots[i], withSuffixes[j]);
				if( test.exists()) {
					return test.getAbsolutePath();
				}
			}
		}
		return null;
	}
	
	private  String runCommand(final String cmd, int timeout) {
		if( timeout == -1 ) {
			return runCommand(cmd);
		} else {
			String path = ThreadUtils.runWithTimeout(timeout, new Callable<String>() {
				@Override
				public String call() throws Exception {
					return runCommand(cmd);
				}
			});
			return path;
		}
	}
	
	private  String runCommand(String cmd) {

		Process p = null;
		try {
			p = Runtime.getRuntime().exec(cmd);
			try {
				p.waitFor();
			} catch(InterruptedException ie) {
				// Ignore, expected
			}
			if(p.exitValue() == 0) {
				InputStream is = p.getInputStream();
				java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
				String cmdOutput = s.hasNext() ? s.next() : "";
				if( !cmdOutput.isEmpty()) {
					cmdOutput = StringUtils.trim(cmdOutput);
					if( new File(cmdOutput).exists())
						return cmdOutput;
				}
			}

		} catch(IOException ioe) {
			// Ignore this
		} finally {
			if( p != null ) {
				p.destroy();
			}
		}
		return null;
	}
	
	
}
