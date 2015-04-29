/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.application;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.jboss.tools.openshift.internal.ui.wizard.application.AnnotationTagViewerFilter;
import org.jboss.tools.openshift.internal.ui.wizard.application.TemplateListPage.ITextControl;
import org.jboss.tools.openshift.internal.ui.wizard.application.TemplateListPage.TemplateNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.ITags;
import com.openshift.restclient.model.IResource;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class AnnotationTagViewerFilterTest {

	@Mock private IResource resource;
	@Mock private ITextControl control;
	@Mock private ITags capability;
	private AnnotationTagViewerFilter filter;
	
	@Before
	public void setUp() throws Exception {
		filter = new AnnotationTagViewerFilter(control);
	}

	@Test
	public void elementsThatAreNotResourcesOrNodesShouldReturnFalse() {
		assertFalse(filter.select(null, null, new Object()));
	}

	@Test
	public void elementsThatAreResourceNodesShouldReturnTrue() {
		assertTrue(filter.select(null, null, new TemplateNode("foo")));
	}
	
	@Test
	public void resourcesThatAreNotAnnotatedShouldReturnFalse() {
		whenResourceDoesNotSupportITagCapability();
		assertFalse(filter.select(null, null, resource));
	}
	
	@Test
	public void resourcesThatAreAnnotatedWithTheIncludedTagShouldReturnTrue() {
		when(control.getText()).thenReturn("foobar");
		
		when(capability.getTags()).thenReturn(Arrays.asList(new String [] {"foo","foobar","bar"}));
		whenResourceSupportsITagCapability();
		
		assertTrue(filter.select(null, null, resource));
	}
	@Test
	public void resourcesThatAreAnnotatedWithTheIncludedTagShouldReturnFalseWhenNotMatched() {
		when(control.getText()).thenReturn("abcxyz");
		when(capability.getTags()).thenReturn(Arrays.asList(new String [] {"foo","foobar","bar"}));
		whenResourceSupportsITagCapability();
		assertFalse(filter.select(null, null, resource));
	}
	
	private void whenResourceSupportsITagCapability() {
		@SuppressWarnings("unchecked")
		CapabilityVisitor<ITags, Boolean> visitor = any(CapabilityVisitor.class);
		when(resource.accept(visitor, any(Boolean.class))).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock arg0) throws Throwable {
				@SuppressWarnings("unchecked")
				CapabilityVisitor<ITags, Boolean> visitor = (CapabilityVisitor<ITags, Boolean>)arg0.getArguments()[0];
				return visitor.visit(capability);
			}
		});
	}
	private void whenResourceDoesNotSupportITagCapability() {
		@SuppressWarnings("unchecked")
		CapabilityVisitor<ITags, Boolean> visitor = any(CapabilityVisitor.class);
		when(resource.accept(visitor, any(Boolean.class))).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock arg0) throws Throwable {
				Boolean result = (Boolean)arg0.getArguments()[1];
				return result;
			}
		});
	}
}
