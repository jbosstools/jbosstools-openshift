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

import org.jboss.tools.openshift.internal.ui.wizard.application.AnnotationTagViewerFilter;
import org.jboss.tools.openshift.internal.ui.wizard.application.TemplateListPage.ITextControl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.model.IResource;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class AnnotationTagViewerFilterTest {

	@Mock
	private IResource resource;
	@Mock
	private ITextControl control;
	private AnnotationTagViewerFilter filter;
	
	@Before
	public void setUp() throws Exception {
		filter = new AnnotationTagViewerFilter(control);
	}

	@Test
	public void elementsThatAreNotResourcesShouldReturnFalse() {
		assertFalse(filter.select(null, null, new Object()));
	}
	
	@Test
	public void resourcesThatAreNotAnnotatedShouldReturnFalse() {
		when(resource.isAnnotatedWith(anyString())).thenReturn(false);
		assertFalse(filter.select(null, null, resource));
	}
	
	@Test
	public void resourcesThatAreAnnotatedWithTheIncludedTagShouldReturnTrue() {
		when(control.getText()).thenReturn("foobar");
		when(resource.isAnnotatedWith(anyString())).thenReturn(true);
		when(resource.getAnnotation(anyString())).thenReturn("foo,foobar,bar");
		assertTrue(filter.select(null, null, resource));
	}
	@Test
	public void resourcesThatAreAnnotatedWithTheIncludedTagShouldReturnFalseWhenNotMatched() {
		when(control.getText()).thenReturn("abcxyz");
		when(resource.isAnnotatedWith(anyString())).thenReturn(true);
		when(resource.getAnnotation(anyString())).thenReturn("foo,foobar,bar");
		assertFalse(filter.select(null, null, resource));
	}
}
