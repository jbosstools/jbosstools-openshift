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
import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.HOME_FOLDER;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
    
    private static class RedirectedStream extends FilterInputStream {
      private boolean emitLF = false;
      private int previous = -1;

      private RedirectedStream(InputStream delegate) {
          super(delegate);
      }

      @Override
      public synchronized int read() throws IOException {
          if (emitLF) {
              emitLF = false;
              return '\n';
          } else {
              int c = super.read();
              if (c == '\n' && previous != '\r') {
                  emitLF = true;
                  c = '\r';
              }
              previous = c;
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


	private static void executeWithTerminalInternal(File workingDirectory, boolean waitForProcessToExit,
	        String... command) throws IOException {
		Process p = ProcessFactory.getFactory().exec(command, null, workingDirectory, new PTY(PTY.Mode.TERMINAL));
		InputStream in = new RedirectedStream(p.getInputStream());
		InputStream err = new RedirectedStream(p.getErrorStream());
		OutputStream out = p.getOutputStream();
		Map<String, Object> properties = new HashMap<>();
		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID,
		 "org.eclipse.tm.terminal.connector.streams.launcher.streams");
		properties.put(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID,
		 "org.eclipse.tm.terminal.connector.streams.StreamsConnector");
		properties.put(ITerminalsConnectorConstants.PROP_TITLE, String.join(" ", command));
		properties.put(ITerminalsConnectorConstants.PROP_LOCAL_ECHO, false);
		properties.put(ITerminalsConnectorConstants.PROP_FORCE_NEW, true);
		properties.put(ITerminalsConnectorConstants.PROP_STREAMS_STDIN, out);
		properties.put(ITerminalsConnectorConstants.PROP_STREAMS_STDOUT, in);
		properties.put(ITerminalsConnectorConstants.PROP_STREAMS_STDERR, err);
		properties.put(ITerminalsConnectorConstants.PROP_ENCODING, StandardCharsets.UTF_8.name());
		ITerminalService service = TerminalServiceFactory.getService();
		service.openConsole(properties, null);
		if (waitForProcessToExit) {
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
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
