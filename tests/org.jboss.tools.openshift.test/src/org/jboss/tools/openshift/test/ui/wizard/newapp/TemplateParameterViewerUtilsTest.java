/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.wizard.newapp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.TemplateParameterViewerUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.model.template.IParameter;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class TemplateParameterViewerUtilsTest {
	
	@Mock
	private IParameter param;
	
	@Test
	public void parametersWithBlankValueAndNoGeneratorShouldReturnBlank() {
		assertEquals("", TemplateParameterViewerUtils.getValueLabel(param));
	}

	@Test
	public void parametersWithBlankValueAndAGeneratorShouldReturnGenerated() {
		when(param.getGeneratorName()).thenReturn("expression");
		assertEquals("(generated)", TemplateParameterViewerUtils.getValueLabel(param));
	}

	@Test
	public void parametersWithAValueAndNoGeneratorShouldReturnTheValue() {
		when(param.getValue()).thenReturn("abc123");
		assertEquals("abc123", TemplateParameterViewerUtils.getValueLabel(param));
	}
	
	@Test
	public void parametersWithAValueAndAGeneratorShouldReturnTheValue() {
		when(param.getGeneratorName()).thenReturn("expression");
		when(param.getValue()).thenReturn("abc123");
		assertEquals("abc123", TemplateParameterViewerUtils.getValueLabel(param));
	}
}
