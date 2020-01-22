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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private static class RedirectedStream extends FilterInputStream {
        private final boolean redirect;
        private final boolean delay;
        private boolean emitLF = false;

        private RedirectedStream(InputStream delegate, boolean redirect, boolean delay) {
            super(delegate);
            this.redirect = redirect;
            this.delay = delay;
        }

        @Override
        public synchronized int read() throws IOException {
            if (emitLF) {
                emitLF = false;
                return '\n';
            } else {
                int c = super.read();
                if (redirect && c == '\n') {
                    emitLF = true;
                    c = '\r';
                }
                return c;
            }
        }

        @Override
        public synchronized int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int c = read();
            if (c == -1) {
                if (delay) {
                    try {
                        Thread.sleep(60000L);
                    } catch (InterruptedException e) {
                    }
                }
                return -1;
            }
            b[off] = (byte) c;

            int i = 1;
            try {
                for (; i < len && available() > 0; i++) {
                    c = read();
                    if (c == -1) {
                        break;
                    }
                    b[off + i] = (byte) c;
                }
            } catch (IOException ee) {
            }
            return i;
        }
    }

    private static class RedirectedProcess extends Process {
        private final Process delegate;
        private final InputStream inputStream;

        private RedirectedProcess(Process delegate, boolean redirect, boolean delay) {
            this.delegate = delegate;
            inputStream = new RedirectedStream(delegate.getInputStream(), redirect, delay) {};
        }

        @Override
        public OutputStream getOutputStream() {
            return delegate.getOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public InputStream getErrorStream() {
            return delegate.getErrorStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            return delegate.waitFor();
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.waitFor(timeout, unit);
        }

        @Override
        public int exitValue() {
            return delegate.exitValue();
        }

        @Override
        public void destroy() {
            delegate.destroy();
        }

        @Override
        public Process destroyForcibly() {
            return delegate.destroyForcibly();
        }

        @Override
        public boolean isAlive() {
            return delegate.isAlive();
        }
    }

}
