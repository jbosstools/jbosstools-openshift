/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.jboss.tools.openshift.internal.core.portforwarding;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IPortForwardable;
import com.openshift.restclient.capability.resources.IPortForwardable.PortPair;
import com.openshift.restclient.model.IPod;

/**
 * Utility class about port forwarding.
 */
public class PortForwardingUtils {

	/** Internal registry of {@link IPod}'s port-forwarding. */
	private static final Map<IPod, IPortForwardable> REGISTRY = new HashMap<IPod, IPortForwardable>();

	/**
	 * Checks if the given port is already used
	 * 
	 * @param port
	 *            the port to check
	 * @return <code>true</code> if the port is used, <code>false</code> if it
	 *         is available.
	 */
	public static boolean isPortInUse(int port) {
		if (port < 0 || port >= 65536) {
			return false;
		}
		try {
			new ServerSocket(port).close();
			return false;
		} catch (IOException e) {
			// success
		}
		try (Socket socket = new Socket("localhost", port)) {
			return true; // success
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Checks if any of the given ports is already used.
	 * @param ports the ports to check
	 * @return <code>true</code> if at least one port is used, <code>false</code> otherwise
	 */
	public static boolean hasPortInUse(final Collection<IPortForwardable.PortPair> ports) {
		return ports.stream().filter(port -> PortForwardingUtils.isPortInUse(port.getLocalPort())).findAny()
				.isPresent();
	}

	/**
	 * Checks if the given {@link IPod} is already forwarding
	 * <strong>all</strong> its ports.
	 * 
	 * @param pod
	 *            the pod to check
	 * @return <code>true</code> if port-forwarding is started,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isPortForwardingStarted(final IPod pod) {
		final IPortForwardable capability = REGISTRY.get(pod);
		return capability != null && capability.isForwarding();
	}
	
	/**
	 * Returns the {@link PortPair} for the given {@code pod}
	 * @param pod the pod to analyze
	 * @return an <strong>immutable</strong> {@link Set} of {@link PortPair} 
	 */
	public static Set<IPortForwardable.PortPair> getForwardablePorts(final IPod pod) {
		final Set<IPortForwardable.PortPair> ports = new HashSet<>();
		final IPortForwardable forwardable = REGISTRY.get(pod);
		if (forwardable != null && forwardable.getPortPairs() != null) {
			Stream.of(forwardable.getPortPairs()).forEach(portPair -> ports.add(portPair));
		} else if (pod.getContainerPorts() != null) {
			pod.getContainerPorts().stream().map(containerPort -> new IPortForwardable.PortPair(containerPort))
					.forEach(portPair -> ports.add(portPair));
		}
		return Collections.unmodifiableSet(ports);
	}

	/**
	 * Starts port-forwarding for the given {@code pod}
	 * 
	 * @param pod
	 *            the pod on which port-forwarding is to be started
	 * @param ports
	 *            the ports to forward
	 * @return the {@link IPortForwardable} referencing all ports that were
	 *         forwarded, or <code>null</code> if port-forwarding was already
	 *         started on the given pod.
	 */
	public static IPortForwardable startPortForwarding(final IPod pod, final Set<IPortForwardable.PortPair> ports) {
		// skip if port-forwarding is already started
		if (isPortForwardingStarted(pod)) {
			return null;
		}
		final IPortForwardable portForwarding = pod
				.accept(new CapabilityVisitor<IPortForwardable, IPortForwardable>() {

					@Override
					public IPortForwardable visit(final IPortForwardable portForwarding) {
						portForwarding.forwardPorts(ports.toArray(new IPortForwardable.PortPair[] {}));
						return portForwarding;
					}
				}, null);
		if (portForwarding != null) {
			REGISTRY.put(pod, portForwarding);
		}
		return portForwarding;
	}

	/**
	 * Starts port-forwarding for the given {@code pod} for a <strong>single</strong> port.
	 * 
	 * @param pod
	 *            the pod on which port-forwarding is to be started
	 * @param port
	 *            the port to forward
	 * @return the {@link IPortForwardable} referencing all ports that were
	 *         forwarded, or <code>null</code> if port-forwarding was already
	 *         started on the given pod.
	 */
	public static IPortForwardable startPortForwarding(final IPod pod, final PortPair port) {
		final HashSet<PortPair> ports = new HashSet<>();
		ports.add(port);
		return startPortForwarding(pod, ports);
	}

	/**
	 * Stops all port-forwarding for the given {@code pod}
	 * 
	 * @param pod
	 *            the pod on which port-forwarding is to be stopped
	 * @param stream the MessageConsoleStream to use to print messages 
	 * @return the {@link IPortForwardable} referencing all ports that were
	 *         forwarded, or <code>null</code> if port-forwarding was not already
	 *         started on the given pod.
	 * @throws IOException when writing into the given {@code stream} fails
	 */
	public static IPortForwardable stopPortForwarding(final IPod pod, final OutputStream stream) throws IOException {
		if (!PortForwardingUtils.isPortForwardingStarted(pod)) {
			return null;
		}
		final IPortForwardable portForwarding = REGISTRY.remove(pod);
		if (portForwarding != null) {
			portForwarding.stop();
		}
		final List<PortPair> ports = Arrays.asList(portForwarding.getPortPairs());
		waitForPortsToGetFree(ports, 5, stream);
		return portForwarding;
	}

	/**
	 * Polls the given ports for given time.
	 * Returns true if all ports get free, returns false otherwise.
	 * @param ports
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	public static boolean waitForPortsToGetFree(Collection<PortPair> ports, int pollingTimeSeconds, OutputStream stream) throws IOException {
		int pollCount = pollingTimeSeconds * 10; //One poll per 100 ms.
		for (int i = 0; i < pollCount; i++) {
			if(!PortForwardingUtils.hasPortInUse(ports)) {
				return true;
			}
			if (i % 10 == 0) {
				// report once a second;
				if(stream != null) {
					stream.write("Waiting for port-forwarding to stop...\n".getBytes());
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		}
		return false;
	}
}
