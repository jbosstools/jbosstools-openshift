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


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public static final String PROPERTY_PORT_FORWARDING_ALLOWED = "portForwardingAllowed";
	public static final String PROPERTY_FREE_PORT_SEARCH_ALLOWED = "freePortSearchAllowed";
	public static final String PROPERTY_USE_FREE_PORTS = "useFreePorts";
	private static final Map<IPod, IPortForwardable> REGISTRY = new HashMap<IPod, IPortForwardable>();

	private Boolean useFreePorts = Boolean.FALSE;
	private final IPod pod;
	private final ConsoleListener consoleListener = new ConsoleListener();
	private Set<IPortForwardable.PortPair> ports = new HashSet<IPortForwardable.PortPair>();
	
	private boolean isPortForwardingAllowed = false;

	public PortForwardingWizardModel(final IPod pod) {
		this.pod = pod;
		IPortForwardable forwardable = REGISTRY.get(pod);
		if(forwardable != null && forwardable.getPortPairs() != null && forwardable.getPortPairs().length > 0) {
			for(IPortForwardable.PortPair p : forwardable.getPortPairs()) {
				ports.add(p);
			}
		}else {
			for (IPort port : pod.getContainerPorts()) {
				ports.add(new IPortForwardable.PortPair(port));
			}
		}
		ports = Collections.unmodifiableSet(ports);
		useFreePorts = computeUsingFreePorts();
		updatePortForwardingAllowed();
	}

	public final String getPodName() {
		return pod.getNamespace() + "\\" + pod.getName();
	}

	
	public boolean getPortForwarding() {
		return isPortForwarding(pod);
	}

	/**
	 * Since we do not listen to port changes and do not poll them regularly,
	 * on start port forwarding which assumes that it is allowed, 
	 * actual state should be checked.
	 * 
	 * @return
	 */
	public boolean checkPortForwardingAllowed() {
		updatePortForwardingAllowed();
		return isPortForwardingAllowed();
	}

	public boolean isPortForwardingAllowed() {
		return isPortForwardingAllowed;
	}

	void updatePortForwardingAllowed() {
		boolean newValue = !getForwardablePorts().isEmpty() && !isPortForwarding(pod) && !hasPortInUse();
		firePropertyChange(PROPERTY_PORT_FORWARDING_ALLOWED, isPortForwardingAllowed, isPortForwardingAllowed = newValue);
	}

	public boolean isFreePortSearchAllowed() {
		return !getForwardablePorts().isEmpty() && !isPortForwarding(pod);
	}

	private boolean hasPortInUse() {
		for (IPortForwardable.PortPair port : ports) {
			if(isPortInUse(port.getLocalPort())) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPortInUse(int port) {
		if(port < 0 || port >= 65536) {
			return false;
		}
		try {
			new ServerSocket(port).close();
			return false;
		} catch (IOException e) {
			//success
		}
		try (Socket socket = new Socket("localhost", port)) {
			return true; //success
		} catch (IOException e) {
			return false;
		}
	}

	public Collection<IPortForwardable.PortPair> getForwardablePorts(){
		return ports;
	}

	public void startPortForwarding() {
		if(isPortForwarding(pod)) return;
		IPortForwardable portForwardable = pod.accept(new CapabilityVisitor<IPortForwardable, IPortForwardable>() {

			@Override
			public IPortForwardable visit(final IPortForwardable cap) {
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
				return cap;
			}
		}, null);
		if(portForwardable != null) {
			REGISTRY.put(pod, portForwardable);
		}
		firePropertyChange(PROPERTY_PORT_FORWARDING, false, getPortForwarding());
		updatePortForwardingAllowed();
	}
	
	private boolean isPortForwarding(IPod pod) {
		IPortForwardable capability = REGISTRY.get(pod);
		return capability != null && capability.isForwarding();
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

	private String getMessageConsoleName() {
		return NLS.bind("Port forwarding to pod {0} ({1})", pod.getName(), pod.getNamespace());
	}

	public void stopPortForwarding() {
		if(!isPortForwarding(pod)) return;
		IPortForwardable cap = REGISTRY.remove(pod);
		MessageConsoleStream stream = null;
		try {
			final MessageConsole console = ConsoleUtils.findMessageConsole(getMessageConsoleName());
			stream = console.newMessageStream();
			stream.println("Stopping port-forwarding...");
			cap.stop();
			waitForStoppingProcessComplete(stream);

			for (IPortForwardable.PortPair port : ports) {
				stream.println(NLS.bind("{0} {1} -> {2}", new Object [] {port.getName(), port.getLocalPort(), port.getRemotePort()}));
			}
			if(!hasPortInUse()) {
				stream.println("done.");
			} else {
				stream.println("Ports remain in use yet. Stopping ports is requested and eventually will be completed.");
			}
			ConsoleUtils.displayConsoleView(console);
			firePropertyChange(PROPERTY_PORT_FORWARDING, true, getPortForwarding());
			updatePortForwardingAllowed();
		}finally {
			if(stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
			ConsoleUtils.deregisterConsoleListener(consoleListener);					
		}
	}

	/**
	 * Wait up to 5 seconds until the forcibly destroyed process really dies.
	 * 
	 * @param stream
	 */
	private void waitForStoppingProcessComplete(MessageConsoleStream stream) {
		for (int i = 0; i < 50 && hasPortInUse(); i++) {
			if(i % 10 == 0) {
				//report once a second;
				stream.println("Waiting for port-forwarding to stop...");
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		}
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

	private void updateLocalPortBindings(final boolean useFreePorts) {
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
		updatePortForwardingAllowed();
	}

	/*
	 * Called on dialog opening for initialization of 'useFreePorts' flag.
	 * Returns true only if all of the following is true:
	 * - there is at least one port,
	 * - port forwarding is on,
	 * - each PortPair is set to a port different from its remote port.
	 * A slim chance that some free port coincided with the default one is neglected.
	 */
	private boolean computeUsingFreePorts() {
		if(ports.isEmpty() || !isPortForwarding(pod)) {
			return false;
		}
		for (IPortForwardable.PortPair port : ports) {
			if(port.getLocalPort() == port.getRemotePort()) {
				return false;
			}
		}
		return true;
	}

	private String computeKey(IPortForwardable.PortPair port) {
		return "127.0.0.1:" + port.getLocalPort();
	}

}
