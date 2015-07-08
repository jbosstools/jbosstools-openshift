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
package org.jboss.tools.openshift.test.ui.portforwarding;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.tools.openshift.internal.ui.portforwading.PortForwardingWizardModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.capability.resources.IPortForwardable;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IPort;

@RunWith(MockitoJUnitRunner.class)
public class PortForwardingWizardModelTest {

	@Mock
	private IPod pod;
	@Mock
	private IPort port;
	private PortForwardingWizardModel model;

	@Before
	public void setUp() throws Exception {
		when(port.getName()).thenReturn("http");
		when(port.getContainerPort()).thenReturn(3436);
		when(port.getProtocol()).thenReturn("TCP");
		when(pod.getName()).thenReturn("apodname");
		Set<IPort> ports = new HashSet<IPort>();
		ports.add(port);
		
		when(pod.getContainerPorts()).thenReturn(ports);
		this.model = new PortForwardingWizardModel(pod);
	}
	
	@Test
	public void testGetPodName() {
		assertEquals(pod.getName(), model.getPodName());
	}
	
	@Test
	public void getForwardablePorts() {
		List<IPortForwardable.PortPair> ports = new ArrayList<IPortForwardable.PortPair>();
		ports.add(new IPortForwardable.PortPair(port));
		assertArrayEquals(ports.toArray(), model.getForwardablePorts().toArray());
	}
}
