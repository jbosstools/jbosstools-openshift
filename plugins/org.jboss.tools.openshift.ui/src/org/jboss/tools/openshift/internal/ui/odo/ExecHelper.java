/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.odo;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.WriterOutputStream;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.HOME_FOLDER;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ExecHelper {
    private static final ScheduledExecutorService SERVICE = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public static void submit(Runnable runnable) {
        SERVICE.submit(runnable);
    }

    public static String execute(String executable, boolean checkExitCode, File workingDirectory, String... arguments) throws IOException {
        DefaultExecutor executor = new DefaultExecutor() {
            @Override
            public boolean isFailure(int exitValue) {
                if (checkExitCode) {
                    return super.isFailure(exitValue);
                } else {
                    return false;
                }
            }
        };
        StringWriter writer = new StringWriter();
        PumpStreamHandler handler = new PumpStreamHandler(new WriterOutputStream(writer));
        executor.setStreamHandler(handler);
        executor.setWorkingDirectory(workingDirectory);
        CommandLine command = new CommandLine(executable).addArguments(arguments);
        try {
            executor.execute(command);
            return writer.toString();
        } catch (IOException e) {
            throw new IOException(e.getLocalizedMessage() + " " + writer.toString(), e);
        }
    }

    public static String execute(String executable, File workingDirectory, String... arguments) throws IOException {
        return execute(executable, true, workingDirectory, arguments);
    }

    public static String execute(String executable, boolean checkExitCode, String... arguments) throws IOException {
        return execute(executable, checkExitCode, new File(HOME_FOLDER), arguments);
    }

	private static void executeWithTerminalInternal(File workingDirectory, boolean waitForProcessToExit,
	        String... command) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(command).directory(workingDirectory).redirectErrorStream(true);
		Process p = builder.start();
		IConsole odoConsole = new OdoConsole(p, command);
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { odoConsole });
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(odoConsole);
		if (waitForProcessToExit) {
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}
	}

    public static void executeWithTerminal(File workingDirectory, String... command) throws IOException {
        executeWithTerminal(workingDirectory, true, command);
    }

	public static void executeWithTerminal(File workingDirectory, boolean waitForProcessToExit, String... command)
	        throws IOException {
		executeWithTerminalInternal(workingDirectory, waitForProcessToExit, command);
	}

    public static void executeWithTerminal(String... command) throws IOException {
        executeWithTerminal(new File(HOME_FOLDER), command);
    }

}
