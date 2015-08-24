/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.job;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromTemplateJob;
import org.jboss.tools.openshift.internal.ui.wizard.common.IResourceLabelsPageModel.Label;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.openshift.restclient.IClient;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.capability.resources.IProjectTemplateProcessing;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;
import com.openshift.restclient.model.template.ITemplate;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateApplicationFromTemplateJobTest {
	
	@Mock private IProjectTemplateProcessing capability;
	@Mock private IClientCapability clientCapability;
	@Mock private IClient client;
	@Mock private ITemplate template;
	@Mock private IProject project;
	
	private Collection<Label> labels = new ArrayList<Label>();
	private Collection<IParameter> parameters = new ArrayList<IParameter>();
	
	private CreateApplicationFromTemplateJobRunner job;
	private Collection<IResource> resources = new ArrayList<IResource>();
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		labels.add(new Label("foo", "bar"));
		job = spy(new CreateApplicationFromTemplateJobRunner(project, template, parameters, labels));
		when(client.get(anyString(), anyString(), anyString())).thenThrow(OpenShiftException.class);
		when(clientCapability.getClient()).thenReturn(client);
		
		
		when(project.accept(isA(CapabilityVisitor.class), any()))
		.thenAnswer(new Answer() {
				@Override
				public Object answer(InvocationOnMock args) throws Throwable {
					if( args.getArguments()[1] instanceof IStatus) {
						CapabilityVisitor<IProjectTemplateProcessing, IStatus> visitor = (CapabilityVisitor<IProjectTemplateProcessing, IStatus>) args.getArguments()[0];
						return visitor.visit(capability);
						
					} else if (args.getArguments()[1] instanceof Collection) {
						CapabilityVisitor<IClientCapability, Collection> visitor = (CapabilityVisitor<IClientCapability, Collection>) args.getArguments()[0];
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
		resources.add(resource);
		resources.add(status);
		
		when(capability.process(template)).thenReturn(template);
		when(capability.apply(template)).thenReturn(resources);

		IStatus result = job.runMe();

		assertEquals(IStatus.WARNING, result.getSeverity());
	}
	
	/*
	 * End wizard
	 */
	@Test
	public void shouldReturnInfoStatusWhenAllResourcesCreatedWithoutErrors() {
		when(capability.process(template)).thenReturn(template);
		when(capability.apply(template)).thenReturn(resources);
		
		IStatus result = job.runMe();
		
		assertEquals(IStatus.OK, result.getSeverity());
		assertEquals(resources, job.getResources());
		verify(template).updateParameterValues(anyCollectionOf(IParameter.class));
		verify(template).addObjectLabel(anyString(), anyString());
	}
	
	/*
	 * Display failed resources and end wizard
	 */
	@Test
	public void shouldReturnErrorWhenThereIsAnException() {
		final String message = "Test with exception";
		when(capability.process(template)).thenThrow(new OpenShiftException(message));
		IStatus result = job.runMe();
		assertEquals(IStatus.ERROR, result.getSeverity());
		assertEquals(message, result.getMessage());
	}
	
	public static class CreateApplicationFromTemplateJobRunner extends CreateApplicationFromTemplateJob{

		public CreateApplicationFromTemplateJobRunner(IProject project,
				ITemplate template, Collection<IParameter> parameters,
				Collection<Label> labels) {
			super(project, template, parameters, labels);
		}
		
		public IStatus runMe() {
			return doRun(null);
		}
		
	}
}
