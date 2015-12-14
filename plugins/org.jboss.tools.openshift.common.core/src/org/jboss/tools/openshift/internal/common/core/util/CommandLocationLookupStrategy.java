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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Platform;

public class CommandLocationLookupStrategy {
	private static final String LINUX_WHICH = "which ";
	private static final String WINDOWS_WHERE = "where ";
	private static final String LINUX_PATHVAR = "PATH";
	private static final String WINDOWS_PATHVAR = "Path";

	public static final CommandLocationLookupStrategy WINDOWS_STRATEGY = 
			new CommandLocationLookupStrategy(WINDOWS_WHERE, ";", WINDOWS_PATHVAR, new String[]{".exe", ".com"}, null);
	public static final CommandLocationLookupStrategy LINUX_STRATEGY = 
			new CommandLocationLookupStrategy(LINUX_WHICH, ":", LINUX_PATHVAR, new String[]{}, null);
	public static final CommandLocationLookupStrategy MAC_STRATEGY = 
			new CommandLocationLookupStrategy(LINUX_WHICH, ":", LINUX_PATHVAR, new String[]{}, new String[]{"bash", "-c", "echo $PATH"});
	
	public static CommandLocationLookupStrategy get() {
		String os = Platform.getOS();
		if( Platform.OS_WIN32.equals(os)) {
			return WINDOWS_STRATEGY;
		}
		if( Platform.OS_MACOSX.equals(os)) {
			return MAC_STRATEGY;
		}
		return LINUX_STRATEGY;
	}
	
	private String which, delim, pathvar;
	private String[] pathCommand;
	private String[] suffixes;
	public CommandLocationLookupStrategy(String which, String delim, String pathvar, String[] suffixes, String[] pathCommand) {
		this.which = which;
		this.delim = delim;
		this.pathvar = pathvar;
		this.suffixes = suffixes;
		this.pathCommand = pathCommand;
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
			ret = runCommand(which + cmd, timeout, true);
		}
		return ret;
	}
	
	public void ensureOnPath( Map<String, String> env, String folder) {
		String newPath = ensureFolderOnPath(env.get(pathvar), folder);
		env.put(pathvar, newPath);
	}
	
	public String ensureFolderOnPath(String existingPath, String folder) {
		existingPath = (existingPath == null ? "" : existingPath);
		String[] roots = null;
		if( existingPath.isEmpty() && pathCommand != null) {
			// Path should never be empty, we have to discover the path (OSX when eclipse launched via .app)
			String pathresult = runCommand(pathCommand, false);
			roots = pathresult.split(delim);
		} else
			roots = existingPath.split(delim);
		ArrayList<String> list = new ArrayList(Arrays.asList(roots));
		if( !list.contains(folder)) {
			list.add(folder);
		}
		return String.join(delim, list);
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
	private  String runCommand(final String cmd, int timeout, boolean verifyFileExists) {
		if( timeout == -1 ) {
			return runCommand(cmd, verifyFileExists);
		} else {
			String path = ThreadUtils.runWithTimeout(timeout, new Callable<String>() {
				@Override
				public String call() throws Exception {
					return runCommand(cmd, verifyFileExists);
				}
			});
			return path;
		}
	}
	
	private  String runCommand(String cmd, boolean verifyFileExists) {
		return runCommand(new String[]{cmd}, verifyFileExists);
	}
	
	private  String runCommand(String cmd[], boolean verifyFileExists) {
		Process p = null;
		try {
			if( cmd.length > 1 ) 
				p = Runtime.getRuntime().exec(cmd);
			else 
				p = Runtime.getRuntime().exec(cmd[0]);
			try {
				p.waitFor();
			} catch(InterruptedException ie) {
				// Ignore, expected
			}
			InputStream is = null;
			if(p.exitValue() == 0) {
				is = p.getInputStream();
			} else {
				// For debugging only
				//is = p.getErrorStream();
			}
			if( is != null ) {
				java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
				String cmdOutput = s.hasNext() ? s.next() : "";
				if( !cmdOutput.isEmpty()) {
					cmdOutput = StringUtils.trim(cmdOutput);
					if( !verifyFileExists || new File(cmdOutput).exists())
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
