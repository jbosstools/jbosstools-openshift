/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.explorer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.explorer.ResourceGrouping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift3.client.IClient;
import com.openshift3.client.ResourceKind;
import com.openshift3.client.model.IProject;
import com.openshift3.client.model.IResource;
import com.openshift3.client.model.IService;

@RunWith(MockitoJUnitRunner.class)
/*
 * Skipping getImage tests as they can't be run headless
 */
public class OpenShiftExplorerLabelProviderTest {

	private OpenShiftExplorerLabelProvider provider;
	@Mock IClient client;
	
	@Before
	public void setup() throws MalformedURLException{
		when(client.getBaseURL()).thenReturn(new URL("https://localhost:8443"));
		provider = new OpenShiftExplorerLabelProvider();
	}
	
	@Test
	public void getTextForResourceGrouping(){
		ResourceGrouping grouping = new ResourceGrouping(ResourceKind.Service, new ArrayList<IResource>());
		assertEquals("Services", provider.getText(grouping));
	}
	@Test
	public void getTextForResourceReturnsName(){
		IService service = mock(IService.class);
		when(service.getName()).thenReturn("theServiceName");
		
		assertEquals(service.getName(), provider.getText(service));
	}
	@Test
	public void getTextForProjectReturnsDisplayName(){
		IProject project = mock(IProject.class);
		when(project.getDisplayName()).thenReturn("aDisplayName");
		
		assertEquals(project.getDisplayName(), provider.getText(project));
	}
	
	@Test
	public void getTextForConnection(){
		Connection connection = new Connection(client);
		assertEquals("Exp. a connection to display its base URL",client.getBaseURL().toString(), provider.getText(connection));
	}

}
