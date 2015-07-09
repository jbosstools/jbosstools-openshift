/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.portforwading;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.internal.common.ui.console.ConsoleUtils;

import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IPortForwardable;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IPort;

/**
 * @author jeff.cantrill
 */
public class PortForwardingWizardModel extends ObservablePojo {

	public static final String PROPERTY_FORWARDABLE_PORTS = "forwardablePorts";
	public static final String PROPERTY_PORT_FORWARDING = "portForwarding";
	public static final String PROPERTY_USE_FREE_PORTS = "useFreePorts";


	private Boolean useFreePorts = Boolean.FALSE;
	private final IPod pod;
	private final ConsoleListener consoleListener = new ConsoleListener();
	private List<IPortForwardable.PortPair> ports = new ArrayList<IPortForwardable.PortPair>();

	public PortForwardingWizardModel(final IPod pod) {
		this.pod = pod;
		for (IPort port : pod.getContainerPorts()) {
			ports.add(new IPortForwardable.PortPair(port));
		}
		
	}

	public final String getPodName() {
		return pod.getName();
	}

	
	public boolean getPortForwarding() {
		return pod.accept(new CapabilityVisitor<IPortForwardable, Boolean>() {

			@Override
			public Boolean visit(IPortForwardable capability) {
				return capability.isForwarding();
			}
		}, Boolean.FALSE);
	}

	public List<IPortForwardable.PortPair> getForwardablePorts(){
		return Collections.unmodifiableList(ports);
	}

	public void startPortForwarding() {
		pod.accept(new CapabilityVisitor<IPortForwardable, Object>() {

			@Override
			public Object visit(final IPortForwardable cap) {;
				if(cap.isForwarding()) return null;
				final MessageConsole console = ConsoleUtils.findMessageConsole(getMessageConsoleName());
				consoleListener.setCapability(cap);
				ConsoleUtils.registerConsoleListener(consoleListener);
				MessageConsoleStream stream = console.newMessageStream();
				stream.println("Starting port-forwarding...");
				for (IPortForwardable.PortPair port : ports) {
					stream.println(NLS.bind("{0} {1} -> {2}", new Object [] {port.getName(), port.getLocalPort(), port.getRemotePort()}));
				}
				cap.forwardPorts(ports.toArray(new IPortForwardable.PortPair []{}));
				stream.println("done.");
				ConsoleUtils.displayConsoleView(console);
				firePropertyChange(PROPERTY_PORT_FORWARDING, false, getPortForwarding());
				return null;
			}
		}, null);
	}
	
	private class ConsoleListener implements IConsoleListener{
		private IPortForwardable cap;
		
		public void setCapability(IPortForwardable cap) {
			this.cap = cap;
		}
		@Override
		public void consolesRemoved(IConsole[] consoles) {
			final String messageConsoleName = getMessageConsoleName();
			for (IConsole console : consoles) {
				if(console.getName().equals(messageConsoleName)) {
					try {
						cap.stop();
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

	private String getMessageConsoleName() {
		return NLS.bind("Port forwarding to pod {0} ({1})", pod.getName(), pod.getNamespace());
	}

	public void stopPortForwarding() {
		pod.accept(new CapabilityVisitor<IPortForwardable, Object>() {

			@Override
			public Object visit(IPortForwardable cap) {
				if(!cap.isForwarding()) return null;
				try {
					final MessageConsole console = ConsoleUtils.findMessageConsole(getMessageConsoleName());
					MessageConsoleStream stream = console.newMessageStream();
					stream.println("Stopping port-forwarding...");
					cap.stop();
					for (IPortForwardable.PortPair port : ports) {
						stream.println(NLS.bind("{0} {1} -> {2}", new Object [] {port.getName(), port.getLocalPort(), port.getRemotePort()}));
					}
					stream.println("done.");
					ConsoleUtils.displayConsoleView(console);
					firePropertyChange(PROPERTY_PORT_FORWARDING, true, getPortForwarding());
					return null;
				}finally {
					ConsoleUtils.deregisterConsoleListener(consoleListener);					
				}
			}
		}, null);
			
	}

	public Boolean getUseFreePorts() {
		return useFreePorts;
	}

	public void setUseFreePorts(Boolean useFreePorts) {
		// do not change the current bindings if port forwarding is already started.
		if (!getPortForwarding()) { 
			updateLocalPortBindings(useFreePorts);
		}
		firePropertyChange(PROPERTY_USE_FREE_PORTS, this.useFreePorts, this.useFreePorts = useFreePorts);
	}

	private void updateLocalPortBindings(final boolean useFreePorts){
		final List<String> bindings = new ArrayList<String>();
		// update local bindings while avoiding duplicates
		for (IPortForwardable.PortPair port : ports) {
			if (useFreePorts) {
				// find free port for every port
				port.setLocalPort(SocketUtil.findFreePort());
			} else {
				// find duplicates and if match we find free port for those
				// until stops.
				port.setLocalPort(port.getRemotePort());
				String key = computeKey(port);
				while (bindings.contains(key)) {
					port.setLocalPort(SocketUtil.findFreePort());
					key = computeKey(port);
				}
				bindings.add(key);
			}
		}
	}

	private String computeKey(IPortForwardable.PortPair port) {
		return "127.0.0.1:" + port.getLocalPort();
	}

}
