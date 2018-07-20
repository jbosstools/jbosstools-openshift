/*******************************************************************************
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.job;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.console.ConsoleUtils;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinaryOperation;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.IBinaryCapability;
import com.openshift.restclient.capability.resources.IPodLogRetrieval;
import com.openshift.restclient.model.IPod;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class PodLogsJob extends AbstractDelegatingMonitorJob {

	private static final String DOCUMENT_IS_CLOSED = "Document is closed";

	private static final Map<Key, ConsoleStreamPipe> REGISTRY = new HashMap<>();

	private final Key key;

	public PodLogsJob(IPod pod, String containerName) {
		super("Displaying pod logs...");
		this.key = new Key(pod, containerName);
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			monitor.worked(IProgressMonitor.UNKNOWN);
			if (REGISTRY.containsKey(key)) {
				showConsole();
			} else {
				ConsoleStreamPipe pipe = key.pod.accept(new CapabilityVisitor<IPodLogRetrieval, ConsoleStreamPipe>() {

					@Override
					public ConsoleStreamPipe visit(final IPodLogRetrieval capability) {
						Connection connection = ConnectionsRegistryUtil.getConnectionFor(key.pod);
						ConsoleStreamPipe consoleStream = new ConsoleStreamPipe(capability, connection);
						new Thread(consoleStream).start();
						return consoleStream;
					}

				}, null);
				if (pipe != null) {
					REGISTRY.put(key, pipe);
				}
			}
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private MessageConsole showConsole() {
		final MessageConsole console = ConsoleUtils.findMessageConsole(getMessageConsoleName());
		ConsoleUtils.displayConsoleView(console);
		return console;
	}

	private String getMessageConsoleName() {
		IPod pod = key.pod;
		return NLS.bind("{0}\\{1}\\{2} log", new Object[] { pod.getNamespaceName(), pod.getName(), key.container });
	}

	private static class Key {
		final IPod pod;
		final String container;

		Key(IPod pod, String containerName) {
			this.pod = pod;
			this.container = containerName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((container == null) ? 0 : container.hashCode());
			result = prime * result + ((pod == null) ? 0 : pod.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (container == null) {
				if (other.container != null)
					return false;
			} else if (!container.equals(other.container))
				return false;
			if (pod == null) {
				if (other.pod != null)
					return false;
			} else if (!pod.equals(other.pod))
				return false;
			return true;
		}

	}

	private class ConsoleStreamPipe extends OCBinaryOperation implements Runnable {

		private IPodLogRetrieval capability;
		private boolean running = true;
		private Connection connection;

		ConsoleStreamPipe(IPodLogRetrieval capability, Connection connection) {
			this.capability = capability;
			this.connection = connection;
		}

		public void stop() {
			this.running = false;
			capability.stop();
		}

		@Override
		public void run() {
			run(connection);
		}

		@Override
		protected void runOCBinary() {
			ConsoleUtils.registerConsoleListener(new ConsoleListener(this));
			final MessageConsole console = showConsole();
			final MessageConsoleStream os = console.newMessageStream();
			os.setEncoding("UTF-8");
			try {
				final InputStream logs = capability.getLogs(true, key.container, IBinaryCapability.SKIP_TLS_VERIFY);
				byte[] data = new byte[256];
				int read = 0;
				while (running && (read = readSafely(logs, data)) != -1 && !os.isClosed()) {
					os.write(data, 0, read);
				}
			} catch (OpenShiftException e) {
				OpenShiftUIActivator.getDefault().getLogger().logError(e);
				try {
					if (os != null)
						os.write(e.getMessage().getBytes());
				} catch (IOException e1) {
					OpenShiftUIActivator.getDefault().getLogger().logError(e1);
				}
			} catch (IOException e) {
				if (!DOCUMENT_IS_CLOSED.equals(e.getMessage())) {
					OpenShiftUIActivator.getDefault().getLogger().logError("Exception reading pod log inputstream", e);
				}
			} finally {
				try {
					if (os != null)
						os.close();
				} catch (IOException e) {
					OpenShiftUIActivator.getDefault().getLogger()
							.logError("Exception while closing pod log inputstream", e);
				}
			}
		}
	}

	/**
	 * This helper method try not to make a fuss out of closing the input stream internally.
	 * All other failures should not be hidden.
	 *  
	 * @param logs
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private int readSafely(InputStream logs, byte[] data) throws IOException {
		try {
			if (logs.available() < 0) {
				return -1;
			}
		} catch (IOException e) {
			//InputStream.available() may throw exception if the stream is closed externally.
			return -1;
		}
		try {
			return logs.read(data);
		} catch (IOException e) {
			if ("Stream closed".equals(e.getMessage())) {
				//Closed externally, nothing to read. Now we can only rely on chain of input streams 
				//having BufferedInputStream in it. Otherwise, we cannot say if stream is closed or failed. 
				return -1;
			}
			throw e;
		}
	}

	private class ConsoleListener implements IConsoleListener {

		private ConsoleStreamPipe pipe;

		protected ConsoleListener(ConsoleStreamPipe pipe) {
			this.pipe = pipe;
		}

		@Override
		public void consolesRemoved(IConsole[] consoles) {
			final String messageConsoleName = getMessageConsoleName();
			for (IConsole console : consoles) {
				if (console.getName().equals(messageConsoleName)) {
					try {
						pipe.stop();
						REGISTRY.remove(key);
						return;
					} finally {
						ConsoleUtils.deregisterConsoleListener(this);
					}
				}
			}
		}

		@Override
		public void consolesAdded(IConsole[] consoles) {
			// nothing to do
		}
	}
}
