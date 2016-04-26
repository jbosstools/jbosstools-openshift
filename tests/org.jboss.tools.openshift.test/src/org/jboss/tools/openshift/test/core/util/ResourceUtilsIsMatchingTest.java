/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
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
public class ResourceUtilsIsMatchingTest {

	@Mock private IResource resource;
	@Mock private ITags capability;
	
	@Before
	public void setUp() throws Exception {
		when(resource.getName()).thenReturn("the-resource-name-mongo");
		whenResourceDoesNotSupportITagCapability();
	}
	
	@Test
	public void elementsThatMatchTheNameShouldReturnTrue() {
		whenResourceSupportsITagCapability();
		assertTrue(ResourceUtils.isMatching("resource", resource));
	}
	
	@Test
	public void resourcesThatAreNotAnnotatedShouldReturnFalseWhenTheFilterIsNotEmpty() {
		whenResourceDoesNotSupportITagCapability();
		assertFalse(ResourceUtils.isMatching("foobar", resource));
	}

	@Test
	public void resourcesThatAreNotAnnotatedShouldReturnTrueWhenTheFilterIsEmpty() {
		whenResourceDoesNotSupportITagCapability();
		assertTrue(ResourceUtils.isMatching(" ", resource));
	}

	@Test
	public void resourcesThatAreAnnotatedWithTheIncludedTagShouldReturnTrue() {
		when(capability.getTags()).thenReturn(Arrays.asList(new String [] {"foo","foobar","bar"}));
		whenResourceSupportsITagCapability();
		
		assertTrue(ResourceUtils.isMatching("foobar", resource));
	}
	
	@Test
	public void resourcesThatAreAnnotatedWithTheIncludedTagShouldReturnFalseWhenNotMatched() {
		when(capability.getTags()).thenReturn(Arrays.asList(new String [] {"foo","foobar","bar"}));
		whenResourceSupportsITagCapability();
		assertFalse(ResourceUtils.isMatching("abcxyz", resource));
	}

	@Test
	public void nameThatPartiallyMatchesElementsShouldReturnFalse() {
		assertFalse(ResourceUtils.isMatching("resource mysql", resource));
	}

	@Test
	public void nameThatMatchesAllElementsShouldReturnTrue() {
		whenResourceSupportsITagCapability();
		assertTrue(ResourceUtils.isMatching("mongo resource", resource));
	}

	@Test
	public void tagsThatMatchAllElementsShouldReturnTrue() {
		when(capability.getTags()).thenReturn(Arrays.asList(new String [] {"foo","foobar","bar"}));
		whenResourceSupportsITagCapability();

		assertTrue(ResourceUtils.isMatching("foobar foo", resource));
	}

	@Test
	public void tagsThatPartiallyMatchAllElementsShouldReturnFalse() {
		when(capability.getTags()).thenReturn(Arrays.asList(new String [] {"foo","foobar","bar"}));
		whenResourceSupportsITagCapability();

		assertFalse(ResourceUtils.isMatching("foobar baz", resource));
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
