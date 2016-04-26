/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.job;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.internal.ui.job.CreateResourceJob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.restclient.IClient;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * @author Jeff Maury
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateResourceJobTest {
	
	@Mock private IClientCapability clientCapability;
	@Mock private IClient client;
	@Mock private IProject project;
	@Mock private InputStream input;
	@Mock private ResourceFactory resourceFactory;
	
	private CreateResourceJobRunner job;
	private Collection<IResource> resources = new ArrayList<>();
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		job = spy(new CreateResourceJobRunner(project, input));
		when(client.get(anyString(), anyString(), anyString())).thenThrow(OpenShiftException.class);
		when(client.getResourceFactory()).thenReturn(resourceFactory);
		when(clientCapability.getClient()).thenReturn(client);
		
		
		when(project.accept(isA(CapabilityVisitor.class), any()))
		.thenAnswer(new Answer() {
				@Override
				public Object answer(InvocationOnMock args) throws Throwable {
					if( args.getArguments()[1] instanceof IStatus) {
						CapabilityVisitor<IClientCapability, IStatus> visitor = (CapabilityVisitor<IClientCapability, IStatus>) args.getArguments()[0];
						return visitor.visit(clientCapability);
					}
					return null;
				}
			});

	}

	/*
	 * Display failed resources and end wizard
	 */
	@Test
	public void shouldNotifyAndReturnWarningStatusWhenResourcesAlreadyExist() {
		IResource resource = mock(IResource.class);
		when(resource.getKind()).thenReturn(ResourceKind.IMAGE_STREAM);
		IResource status = mock(com.openshift.restclient.model.IStatus.class);
		when(status.getKind()).thenReturn(ResourceKind.STATUS);
		resources.add(status);
		
		when(resourceFactory.create(input)).thenReturn(resource);
		when(client.create(resource, project.getNamespace())).thenReturn(status);

		IStatus result = job.runMe();

		assertEquals(IStatus.OK, result.getSeverity());
		assertEquals(resources, job.getResource());
	}
	
	/*
	 * End wizard
	 */
	@Test
	public void shouldReturnResourcesWhenAllResourcesCreatedWithoutErrors() {
        IResource resource = mock(IResource.class);
        when(resource.getKind()).thenReturn(ResourceKind.IMAGE_STREAM);
        resources.add(resource);
        
        when(resourceFactory.create(input)).thenReturn(resource);
        when(client.create(resource, project.getNamespace())).thenReturn(resource);

        IStatus result = job.runMe();

        assertEquals(IStatus.OK, result.getSeverity());
        assertEquals(resources, job.getResource());
	}
	
	/*
	 * Display failed resources and end wizard
	 */
	@Test
	public void shouldReturnErrorWhenThereIsAnException() {
		final String message = "Test with exception";
	    when(resourceFactory.create(input)).thenThrow(new OpenShiftException(message));
	    IStatus result = job.runMe();
		assertEquals(IStatus.ERROR, result.getSeverity());
		assertEquals(message, result.getMessage());
	}
	
	public static class CreateResourceJobRunner extends CreateResourceJob {

		public CreateResourceJobRunner(IProject project, InputStream input) {
			super(project, input);
		}
		
		public IStatus runMe() {
			return doRun(null);
		}
		
	}
}
