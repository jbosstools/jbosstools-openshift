/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.job;

import java.io.BufferedInputStream;
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
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.console.ConsoleUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IPodLogRetrieval;
import com.openshift.restclient.model.IPod;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class PodLogsJob extends AbstractDelegatingMonitorJob {
	private static final String DOCUMENT_IS_CLOSED = "Document is closed";

	private static final Map<IPod, ConsoleStreamPipe> REGISTRY = new HashMap<IPod, ConsoleStreamPipe>();


	private IPod pod;

	public PodLogsJob(IPod pod) {
		super("FollowPodLogsJob");
		this.pod = pod;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			monitor.worked(IProgressMonitor.UNKNOWN);
			if(REGISTRY.containsKey(pod)) {
				showConsole();
			}else {
				ConsoleStreamPipe pipe = pod.accept(new CapabilityVisitor<IPodLogRetrieval, ConsoleStreamPipe>() {

					@Override
					public ConsoleStreamPipe visit(final IPodLogRetrieval capability) {
						ConsoleStreamPipe consoleStream = new ConsoleStreamPipe(capability);
						new Thread(consoleStream).start();
						return consoleStream;
					}

				}, null);
				if(pipe != null) {
					REGISTRY.put(pod, pipe);
				}
			}
		}finally {
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
		return NLS.bind("{0}\\{1} log", pod.getNamespace(), pod.getName());
	}	
	
	private class ConsoleStreamPipe implements Runnable {
		
		private IPodLogRetrieval capability;
		private boolean running = true;
		
		ConsoleStreamPipe(IPodLogRetrieval capability){
			this.capability = capability;
		}
		
		public void stop() {
			this.running = false;
			capability.stop();
		}
		
		@Override
		public void run() {
			ConsoleUtils.registerConsoleListener(new ConsoleListener(this));
			final MessageConsole console = showConsole();
			final MessageConsoleStream stream = console.newMessageStream();
			try {
				final InputStream logs = new BufferedInputStream(capability.getLogs(true));
				int c;
				while(running && (c = logs.read()) != -1){
					if(!stream.isClosed()) {
						stream.write(c);
					}
				}
			}catch (OpenShiftException e) {
				stream.println(e.getMessage());
			} catch (IOException e) {
				if(!DOCUMENT_IS_CLOSED.equals(e.getMessage())) {
					OpenShiftCommonUIActivator.log("Exception reading pod log inputstream", e);
				}
			}
		}
	}

	
	private class ConsoleListener implements IConsoleListener{
		
		private ConsoleStreamPipe pipe;
		
		protected ConsoleListener(ConsoleStreamPipe pipe) {
			this.pipe = pipe;
		}
		
		@Override
		public void consolesRemoved(IConsole[] consoles) {
			final String messageConsoleName = getMessageConsoleName();
			for (IConsole console : consoles) {
				if(console.getName().equals(messageConsoleName)) {
					try {
						pipe.stop();
						REGISTRY.remove(pod);
						return;
					}finally {
						ConsoleUtils.deregisterConsoleListener(this);
					}
				}
			}
		}
		
		@Override
		public void consolesAdded(IConsole[] consoles) {
		}
	}
}
