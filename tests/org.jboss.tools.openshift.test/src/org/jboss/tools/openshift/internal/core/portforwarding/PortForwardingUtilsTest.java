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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.launching.SocketUtil;
import org.junit.Test;
import org.mockito.Mockito;

import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IPortForwardable;
import com.openshift.restclient.capability.resources.IPortForwardable.PortPair;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IPort;

/**
 * Testing the {@link PortForwardingUtils} class.
 */
public class PortForwardingUtilsTest {

	@Test
	public void shouldCheckThatPortIsInUse() throws IOException {
		// given
		try (final ServerSocket serverSocket = new ServerSocket(SocketUtil.findFreePort())) {
			// when
			final int port = serverSocket.getLocalPort();
			final boolean portInUse = PortForwardingUtils.isPortInUse(port);
			// then
			assertThat(portInUse).isTrue();
		}
	}

	@Test
	public void shouldCheckThatPortIsNotInUse() throws IOException {
		// given
		final int port = SocketUtil.findFreePort();
		// when
		final boolean portInUse = PortForwardingUtils.isPortInUse(port);
		// then
		assertThat(portInUse).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldStartPortForwardingOnSinglePort() {
		// given
		final IPod pod = Mockito.mock(IPod.class);
		final PortPair port = Mockito.mock(PortPair.class);
		final IPortForwardable portForwardable = Mockito.mock(IPortForwardable.class);
		Mockito.when(pod.accept(Mockito.any(CapabilityVisitor.class), Mockito.any(IPortForwardable.class)))
				.thenReturn(portForwardable);
		Mockito.when(portForwardable.isForwarding()).thenReturn(true);
		// when
		final IPortForwardable startPortForwarding = PortForwardingUtils.startPortForwarding(pod, port);
		// then
		assertThat(startPortForwarding).isNotNull().isEqualTo(portForwardable);
		assertThat(PortForwardingUtils.isPortForwardingStarted(pod)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldStopPortForwarding() throws IOException {
		// given
		final IPod pod = Mockito.mock(IPod.class);
		final PortPair port = Mockito.mock(PortPair.class);
		final IPortForwardable portForwardable = Mockito.mock(IPortForwardable.class);
		Mockito.when(pod.accept(Mockito.any(CapabilityVisitor.class), Mockito.any(IPortForwardable.class)))
				.thenReturn(portForwardable);
		Mockito.when(portForwardable.isForwarding()).thenReturn(true);
		Mockito.when(portForwardable.getPortPairs()).thenReturn(new PortPair[]{port});
		PortForwardingUtils.startPortForwarding(pod, port);
		// when
		final IPortForwardable stopPortForwarding = PortForwardingUtils.stopPortForwarding(pod, null);
		// then
		assertThat(stopPortForwarding).isNotNull().isEqualTo(portForwardable);
		assertThat(PortForwardingUtils.isPortForwardingStarted(pod)).isFalse();
		Mockito.verify(portForwardable, Mockito.times(1)).stop();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldGetForwardablePortsOnStartedState() {
		// given
		final IPod pod = Mockito.mock(IPod.class);
		final PortPair port = Mockito.mock(PortPair.class);
		final IPortForwardable portForwardable = Mockito.mock(IPortForwardable.class);
		Mockito.when(portForwardable.getPortPairs()).thenReturn(new PortPair[]{port});
		Mockito.when(pod.accept(Mockito.any(CapabilityVisitor.class), Mockito.any(IPortForwardable.class)))
				.thenReturn(portForwardable);
		PortForwardingUtils.startPortForwarding(pod, port);
		// when
		final Set<PortPair> forwardablePorts = PortForwardingUtils.getForwardablePorts(pod);
		// then
		assertThat(forwardablePorts).isNotNull().containsExactly(port);
	}

	@Test
	public void shouldGetForwardablePortsOnStartedStopped() {
		// given
		final IPod pod = Mockito.mock(IPod.class);
		final IPort port = Mockito.mock(IPort.class);
		final IPortForwardable portForwardable = Mockito.mock(IPortForwardable.class);
		Mockito.when(pod.getContainerPorts()).thenReturn(toSet(port));
		// when
		final Set<PortPair> forwardablePorts = PortForwardingUtils.getForwardablePorts(pod);
		// then
		assertThat(forwardablePorts).isNotNull().hasSize(1);
	}
	
	/**
	 * Adds the given {@code element} in a new {@link Set}
	 * @param element the element to add
	 * @return the new {@link Set} containing the given {@code element}.
	 */
	static <T> Set<T> toSet(final T element) {
		return Stream.of(element).collect(Collectors.toSet());
	}
}
