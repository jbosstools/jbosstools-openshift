/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.wizard.newapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createBuildConfig;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createSourceBuildStrategy;

import java.util.Arrays;

import org.jboss.tools.openshift.internal.ui.wizard.newapp.ResourceDetailsContentProvider;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.ResourceDetailsContentProvider.ResourceProperty;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.model.IBuildConfig;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceDetailsContentProviderTest {

	private ResourceDetailsContentProvider contentProvider;

	@Before
	public void before() {
		this.contentProvider = new ResourceDetailsContentProvider();
	}

	@Test
	public void shouldReturnUnknownResourcePropertyGivenBuildConfigBuildStrategyIsNull() {
		// given
		IBuildConfig bc = ResourceMocks.createBuildConfig("42", null, null, null, null, null, null, null);

		// when
		Object[] children = contentProvider.getChildren(bc);

		// then
		ResourceProperty property = getResourceProperty(ResourceDetailsContentProvider.LABEL_STRATEGY, children);
		assertThat(property).isNotNull();
		assertThat(property.isUnknownValue()).isTrue();
	}

	@Test
	public void shouldReturnResourcePropertyGivenBuildConfigBuildStrategyExists() {
		// given
		IBuildConfig bc = createBuildConfig("42", null, null, null, null, null, null, createSourceBuildStrategy("42"));

		// when
		Object[] children = contentProvider.getChildren(bc);

		// then
		ResourceProperty property = getResourceProperty(ResourceDetailsContentProvider.LABEL_STRATEGY, children);
		assertThat(property).isNotNull();
		assertThat(property.isUnknownValue()).isFalse();
	}

	private ResourceProperty getResourceProperty(String label, Object[] children) {
		assertThat(label).isNotNull();
		
		if (children == null) {
			return null;
		}
		return 
				Arrays.stream(children)
					.map(object -> { 
						if (object instanceof ResourceProperty) { 
							return (ResourceProperty) object;
						} else { 
							return null; 
						}
					})
					.filter(property -> property != null)
					.filter(property -> { return label.equals(property.getProperty()); } )
					.findFirst()
					.orElse(null);
	}
	
}

