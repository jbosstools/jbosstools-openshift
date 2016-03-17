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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.internal.common.ui.console.ConsoleUtils;
import org.jboss.tools.openshift.internal.core.portforwarding.PortForwardingUtils;

import com.openshift.restclient.capability.resources.IPortForwardable;
import com.openshift.restclient.model.IPod;

/**
 * @author jeff.cantrill
 */
public class PortForwardingWizardModel extends ObservablePojo {

	public static final String PROPERTY_FORWARDABLE_PORTS = "forwardablePorts";
	public static final String PROPERTY_PORT_FORWARDING = "portForwarding";
	public static final String PROPERTY_PORT_FORWARDING_ALLOWED = "portForwardingAllowed";
	public static final String PROPERTY_FREE_PORT_SEARCH_ALLOWED = "freePortSearchAllowed";
	public static final String PROPERTY_USE_FREE_PORTS = "useFreePorts";

	private Boolean useFreePorts = Boolean.FALSE;
	private final IPod pod;
	private final ConsoleListener consoleListener = new ConsoleListener();
	private final Set<IPortForwardable.PortPair> ports;
	
	private boolean isPortForwardingAllowed = false;

	public PortForwardingWizardModel(final IPod pod) {
		this.pod = pod;
		this.ports = PortForwardingUtils.getForwardablePorts(pod);
		this.useFreePorts = computeUsingFreePorts();
		updatePortForwardingAllowed();
	}

	public final String getPodName() {
		return pod.getNamespace() + "\\" + pod.getName();
	}

	
	public boolean getPortForwarding() {
		return PortForwardingUtils.isPortForwardingStarted(pod);
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

	/**
	 * @return boolean flag to indicate if port-forwarding is allowed.
	 * @see PROPERTY_PORT_FORWARDING_ALLOWED
	 */
	public boolean isPortForwardingAllowed() {
		return this.isPortForwardingAllowed;
	}

	private void updatePortForwardingAllowed() {
		boolean newValue = !getForwardablePorts().isEmpty() && !PortForwardingUtils.isPortForwardingStarted(pod) && !PortForwardingUtils.hasPortInUse(this.ports);
		firePropertyChange(PROPERTY_PORT_FORWARDING_ALLOWED, this.isPortForwardingAllowed, this.isPortForwardingAllowed = newValue);
	}

	public boolean isFreePortSearchAllowed() {
		return !getForwardablePorts().isEmpty() && !PortForwardingUtils.isPortForwardingStarted(pod);
	}

	public Collection<IPortForwardable.PortPair> getForwardablePorts(){
		return ports;
	}

	/**
	 * Starts the port-forwarding for the current pod.
	 */
	public void startPortForwarding() {
		final MessageConsole console = ConsoleUtils.findMessageConsole(getMessageConsoleName());
		MessageConsoleStream stream = console.newMessageStream();
		ConsoleUtils.registerConsoleListener(consoleListener);
		ConsoleUtils.displayConsoleView(console);
		stream.println("Starting port-forwarding...");
		final IPortForwardable portForwardable = PortForwardingUtils.startPortForwarding(this.pod, this.ports);
		Stream.of(portForwardable.getPortPairs()).forEach(port -> stream.println(NLS.bind("{0} {1} -> {2}",
				new Object[] { port.getName(), port.getLocalPort(), port.getRemotePort() })));
		stream.println("done.");
		firePropertyChange(PROPERTY_PORT_FORWARDING, false, getPortForwarding());
		updatePortForwardingAllowed();
	}
	
	
	private class ConsoleListener implements IConsoleListener{
		
		/**
		 * Stops the port-forwarding is the port-forwarding console was removed, does nothing otherwise.
		 *
		 * @param consoles the consoles that were removed.
		 */
		//TODO: shouldn't we replace this with a ProcessConsole to have a big-stop-red button ? #UX
		@Override
		public void consolesRemoved(IConsole[] consoles) {
			final String messageConsoleName = getMessageConsoleName();
			if (Stream.of(consoles).filter(console -> console.getName().equals(messageConsoleName)).findAny()
					.isPresent()) {
				try {
					PortForwardingUtils.stopPortForwarding(pod, null);
				} catch (IOException e) {
					// ignore here: the IOException could be thrown when writing
					// in the console but 'null' is passed here, so there's no
					// risk of such an Exception.
				} finally {
					ConsoleUtils.deregisterConsoleListener(this);
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

	/**
	 * Stops the port-forwarding for the current pod.
	 * @throws IOException 
	 */
	public void stopPortForwarding() throws IOException {
		if (!PortForwardingUtils.isPortForwardingStarted(pod)) {
			return;
		}
		final MessageConsole console = ConsoleUtils.findMessageConsole(getMessageConsoleName());
		try (final MessageConsoleStream stream = console.newMessageStream()) {
			stream.println("Stopping port-forwarding...");
			final IPortForwardable cap = PortForwardingUtils.stopPortForwarding(pod, stream);
			if (cap != null) {
				Stream.of(cap.getPortPairs()).forEach(port -> stream.println(NLS.bind("{0} {1} -> {2}",
						new Object[] { port.getName(), port.getLocalPort(), port.getRemotePort() })));
			}
			if (!PortForwardingUtils.hasPortInUse(this.ports)) {
				stream.println("done.");
			} else {
				stream.println(
						"Ports remain in use yet. Stopping ports is requested and eventually will be completed.");
			}
			ConsoleUtils.displayConsoleView(console);
			firePropertyChange(PROPERTY_PORT_FORWARDING, true, getPortForwarding());
			updatePortForwardingAllowed();
		} finally {
			ConsoleUtils.deregisterConsoleListener(consoleListener);
		}
	}

	boolean waitForPortsToGetFree(int timeSeconds) {
		try {
			boolean result = (timeSeconds == 0) 
				? !PortForwardingUtils.hasPortInUse(ports)
				: PortForwardingUtils.waitForPortsToGetFree(ports, timeSeconds, System.out);
			updatePortForwardingAllowed();
			return result;
		} catch (IOException e) {
			//Ignore, with System.out it cannot happen
			return false;
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
		if(ports.isEmpty() || !PortForwardingUtils.isPortForwardingStarted(pod)) {
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
