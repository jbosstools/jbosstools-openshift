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
package org.jboss.tools.openshift.test.ui.wizard.deployimage;

import static org.junit.Assert.*;

import org.jboss.tools.openshift.internal.ui.wizard.deployimage.PortSpecAdapter;
import org.junit.Test;

import com.openshift.restclient.model.IPort;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class PortSpecAdapterTest {

	@Test
	public void testFullPortSpec() {
		String spec = "8080/tcp";
		IPort port = new PortSpecAdapter(spec);
		assertEquals(8080, port.getContainerPort());
		assertEquals("TCP", port.getProtocol());
		assertEquals("8080-tcp", port.getName());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testEmptyString() {
		new PortSpecAdapter("");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullString() {
		new PortSpecAdapter(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingProtocol() {
		new PortSpecAdapter("8080");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNonNumericPort() {
		new PortSpecAdapter("foo/tcp");
	}

}
