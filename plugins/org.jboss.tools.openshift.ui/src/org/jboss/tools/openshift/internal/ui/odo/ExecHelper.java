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
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ExecHelper {
    private static final ScheduledExecutorService SERVICE = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public static void submit(Runnable runnable) {
        SERVICE.submit(runnable);
    }

    /**
     * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
     *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
     *
     * @param executable the executable
     * @param checkExitCode if exit code should be checked
     * @param workingDirectory the working directory for the process
     * @param envs the map for the environment variables
     * @param arguments the arguments
     * @return the combined output and error stream as a String
     * @throws IOException if error during process execution
     */
    public static ExecResult execute(String executable, boolean checkExitCode, File workingDirectory, Map<String,String> envs,
                                 String... arguments) throws IOException {
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
      StringWriter outWriter = new StringWriter();
      StringWriter errWriter = new StringWriter();
      PumpStreamHandler handler = new PumpStreamHandler(new WriterOutputStream(outWriter), new WriterOutputStream(errWriter));
      executor.setStreamHandler(handler);
      executor.setWorkingDirectory(workingDirectory);
      CommandLine command = new CommandLine(executable).addArguments(arguments, false);
      Map<String, String> env = new HashMap<>(System.getenv());
      env.putAll(envs);
      try {
        int exitCode = executor.execute(command, env);
        return new ExecResult(outWriter.toString(), errWriter.toString(), exitCode);
      } catch (IOException e) {
        throw new IOException(e.getLocalizedMessage() + " " + errWriter.toString(), e);
      }
    }


    /**
     * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
     *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
     * @param executable the executable
     * @param workingDirectory the working directory for the process
     * @param envs the map for the environment variables
     * @param arguments the arguments
     * @return the combined output and error stream as a String
     * @throws IOException if error during process execution
     */
    public static ExecResult execute(String executable, File workingDirectory, Map<String, String> envs, String... arguments) throws IOException {
      return execute(executable, true, workingDirectory, envs, arguments);
    }

    /**
     * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
     *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
     * @param executable the executable
     * @param envs the map for the environment variables
     * @param arguments the arguments
     * @return the combined output and error stream as a String
     * @throws IOException if error during process execution
     */
    public static ExecResult execute(String executable, Map<String, String> envs, String... arguments) throws IOException {
      return execute(executable, true, new File(HOME_FOLDER), envs, arguments);
    }
    /**
     * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
     *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
     * @param executable the executable
     * @param arguments the arguments
     * @return the combined output and error stream as a String
     * @throws IOException if error during process execution
     */
    public static ExecResult execute(String executable, String... arguments) throws IOException {
      return execute(executable, Collections.emptyMap(), arguments);
    }

    /**
     * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
     *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
     * @param executable the executable
     * @param workingDirectory the working directory for the process
     * @param arguments the arguments
     * @return the combined output and error stream as a String
     * @throws IOException if error during process execution
     */
    public static ExecResult execute(String executable, File workingDirectory, String... arguments) throws IOException {
      return execute(executable, true, workingDirectory, Collections.emptyMap(), arguments);
    }

    /**
     * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
     *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
     * @param executable the executable
     * @param checkExitCode if exit code should be checked
     * @param envs the map for the environment variables
     * @param arguments the arguments
     * @return the combined output and error stream as a String
     * @throws IOException if error during process execution
     */
    public static ExecResult execute(String executable, boolean checkExitCode, Map<String, String> envs,
                                 String... arguments) throws IOException {
      return execute(executable, checkExitCode, new File(HOME_FOLDER), envs, arguments);
    }

    /**
     * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
     *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
     * @param executable the executable
     * @param checkExitCode if exit code should be checked
     * @param arguments the arguments
     * @return the combined output and error stream as a String
     * @throws IOException if error during process execution
     */
    public static ExecResult execute(String executable, boolean checkExitCode, String... arguments) throws IOException {
      return execute(executable, checkExitCode, new File(HOME_FOLDER), Collections.emptyMap(), arguments);
    }
    
    public static class ExecResult {
      private final String stdOut;
      private final String stdErr;
      private final int exitCode;

      public ExecResult(String stdOut, String stdErr, int exitCode) {
        this.stdOut = stdOut;
        this.stdErr = stdErr;
        this.exitCode = exitCode;
      }

      public String getStdOut() {
        return stdOut;
      }

      public String getStdErr() {
        return stdErr;
      }

      public int getExitCode() {
        return exitCode;
      }
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

  private static Map<String, String> appendNativeEnv(Map<String, String> env) {
    Map<String, String> nativeEnv = new HashMap(System.getenv());
    nativeEnv.putAll(env);
    return nativeEnv;
  }

	private static void executeWithTerminalInternal(File workingDirectory, boolean waitForProcessToExit,
	        Map<String, String> envs, String... command) throws IOException {
	  String[] env = envs==null?null:EnvironmentUtils.toStrings(appendNativeEnv(envs));
		Process p = Platform.OS_WIN32.equals(Platform.getOS())?ProcessFactory.getFactory().exec(command, env, workingDirectory):ProcessFactory.getFactory().exec(command, env, workingDirectory, new PTY(PTY.Mode.TERMINAL));
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
		if (Display.getDefault().getThread() == Thread.currentThread()) {
		  waitForProcessToExit = false;
		}
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

    public static void executeWithTerminal(File workingDirectory, Map<String, String> envs, String... command) throws IOException {
        executeWithTerminal(workingDirectory, true, envs, command);
    }

    public static void executeWithTerminal(File workingDirectory, boolean waitForProcessToExit, String... command)
	        throws IOException {
       executeWithTerminal(workingDirectory, waitForProcessToExit, Collections.emptyMap(), command);
    }

    public static void executeWithTerminal(File workingDirectory, boolean waitForProcessToExit, Map<String, String> envs, String... command)
        throws IOException {
       executeWithTerminalInternal(workingDirectory, waitForProcessToExit, envs, command);
    }

    public static void executeWithTerminal(String... command) throws IOException {
        executeWithTerminal(new File(HOME_FOLDER), command);
    }

    public static void executeWithTerminal(Map<String, String> envs, String... command) throws IOException {
        executeWithTerminal(new File(HOME_FOLDER), envs, command);
    }
}
