/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.test.ui.wizard.newapp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jboss.tools.openshift.internal.ui.wizard.newapp.ResourceDetailsContentProvider.ResourceProperty;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.ResourceDetailsLabelProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.template.IParameter;

public class ResourceDetailsLabelProviderTest {

	private static Map<String, IParameter> templateParameters;
	private static ResourceDetailsLabelProvider labelsProvider;

	@BeforeClass
	public static void init() {
		IParameter p1 = mock(IParameter.class);
		when(p1.getValue()).thenReturn("Dmitrii");

		IParameter p2 = mock(IParameter.class);
		when(p2.getValue()).thenReturn("Bocharov");

		templateParameters = new HashMap<>();
		templateParameters.put("name", p1);
		templateParameters.put("surname", p2);

		labelsProvider = new ResourceDetailsLabelProvider(templateParameters);
	}

	@Test
	public void getStyledTextForResourceWithoutParams() {
		IBuildConfig buildConfig = mock(IBuildConfig.class);
		when(buildConfig.getKind()).thenReturn(ResourceKind.BUILD_CONFIG);
		when(buildConfig.getName()).thenReturn("build-config-name");

		assertEquals(ResourceKind.BUILD_CONFIG + " build-config-name",
				labelsProvider.getStyledText(buildConfig).toString());
	}

	@Test
	public void getStyledTextForResourceWithParams() {
		IBuildConfig buildConfig = mock(IBuildConfig.class);
		when(buildConfig.getKind()).thenReturn(ResourceKind.BUILD_CONFIG);
		when(buildConfig.getName()).thenReturn("named by ${name} ${surname}");

		assertEquals(ResourceKind.BUILD_CONFIG + " named by Dmitrii Bocharov",
				labelsProvider.getStyledText(buildConfig).toString());
	}

	@Test
	public void getStyledTextForResourcePropertyWithoutParams() {
		ResourceProperty resourceProperty = mock(ResourceProperty.class);
		when(resourceProperty.getProperty()).thenReturn("prop1");
		when(resourceProperty.getValue()).thenReturn("property value");

		assertEquals("Prop1: property value", labelsProvider.getStyledText(resourceProperty).toString());
	}

	@Test
	public void getStyledTextForResourcePropertyWithParams() {
		ResourceProperty resourceProperty = mock(ResourceProperty.class);
		when(resourceProperty.getProperty()).thenReturn("Prop");
		when(resourceProperty.getValue()).thenReturn("surname ${surname}");

		assertEquals("Prop: surname Bocharov", labelsProvider.getStyledText(resourceProperty).toString());
	}

	@Test
	public void getStyledTextForResourcePropertyWithParamsInMap() {
		ResourceProperty resourceProperty = mock(ResourceProperty.class);
		when(resourceProperty.getProperty()).thenReturn("Prop");

		Map<String, String> params = new HashMap<>();
		params.put("${name}", "is name");
		params.put("surname", "${surname}");
		when(resourceProperty.getValue()).thenReturn(params);

		assertEquals("Prop: Dmitrii=is name,surname=Bocharov",
				labelsProvider.getStyledText(resourceProperty).toString());
	}

	@Test
	public void getStyledTextWithNotProvidedLabel() {
		ResourceProperty resourceProperty = mock(ResourceProperty.class);
		when(resourceProperty.getProperty()).thenReturn("prop1");
		when(resourceProperty.isUnknownValue()).thenReturn(false);
		when(resourceProperty.getValue()).thenReturn("");

		assertEquals("Prop1: (Not Provided)", labelsProvider.getStyledText(resourceProperty).toString());
	}

	@Test
	public void getStyledTextWithUnknownLabel() {
		ResourceProperty resourceProperty = mock(ResourceProperty.class);
		when(resourceProperty.getProperty()).thenReturn("prop1");
		when(resourceProperty.isUnknownValue()).thenReturn(true);
		when(resourceProperty.getValue()).thenReturn("");

		assertEquals("Prop1: (Unknown)", labelsProvider.getStyledText(resourceProperty).toString());
	}

	@Test
	public void getStyledTextWithUnkownParameterInMap() {
		ResourceProperty resourceProperty = mock(ResourceProperty.class);
		when(resourceProperty.getProperty()).thenReturn("Prop");

		Map<String, String> params = new TreeMap<>();
		params.put("${name}", "is name");
		params.put("surname", "${surname}");
		params.put("gender", "${gender}");
		when(resourceProperty.getValue()).thenReturn(params);

		assertEquals("Prop: Dmitrii=is name,gender=(Unknown parameter gender),surname=Bocharov",
				labelsProvider.getStyledText(resourceProperty).toString());
	}

}
