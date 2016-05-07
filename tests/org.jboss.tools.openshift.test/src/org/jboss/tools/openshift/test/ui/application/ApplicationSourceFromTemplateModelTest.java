/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.application;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.tools.openshift.internal.ui.wizard.common.IResourceLabelsPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSourceListPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.ApplicationSourceFromTemplateModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.TemplateApplicationSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.template.IParameter;
import com.openshift.restclient.model.template.ITemplate;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationSourceFromTemplateModelTest {
	
	@Mock
	private ITemplate template;
	private ApplicationSourceFromTemplateModel model; 
	
	@Before
	public void setUp() throws Exception {

		when(template.getKind()).thenReturn(ResourceKind.TEMPLATE);
		model = new ApplicationSourceFromTemplateModel();

	}

	@Test
	public void setTemplateShouldCopyParametersAndLabels() {
		Map<String, IParameter> parameters = givenTheTemplateHasParameters();
		HashMap<String, String> labels = givenTheTemplateHasObjectLabels();
		
		Collection<IResourceLabelsPageModel.Label> modelLabels = new ArrayList<>();
		for (Entry<String, String> label : labels.entrySet()) {
			modelLabels.add(new IResourceLabelsPageModel.Label(label.getKey(), label.getValue()));
		}
		
		whenTemplateIsSelected();

		assertArrayEquals(parameters.values().toArray(), model.getParameters().toArray());
		assertArrayEquals(modelLabels.toArray(), model.getLabels().toArray());
	}
	
	@Test
	public void resetParameterShouldSetTheOriginalValue() {
		IParameter param = mock(IParameter.class);
		when(param.getName()).thenReturn("foo");
		when(param.getValue()).thenReturn("abc");
		when(param.clone()).thenReturn(param);

		IParameter other = mock(IParameter.class);
		when(other.getName()).thenReturn("foo");
		when(other.getValue()).thenReturn("xyz");
		
		model.setParameters(Arrays.asList(new IParameter[] {param}));
		
		
		model.resetParameter(other);
		
		verify(other).setValue("abc");
	}
	
	@Test
	public void updateParameterValueShouldUpdateTheParameterValue() {
		IParameter param = mock(IParameter.class);
		model.updateParameterValue(param, "abc123");
		
		verify(param).setValue(eq("abc123"));
	}
	
	@Test
	public void getParametersShouldReturnAParameterMapWhenTemplateIsNotNull() {
		Map<String, IParameter> parameters = givenTheTemplateHasParameters();
		whenTemplateIsSelected();
		assertArrayEquals(parameters.values().toArray(), model.getParameters().toArray());
	}
	
	private void whenTemplateIsSelected() {
		whenTemplateIsSelected(template);
	}
	
	private void whenTemplateIsSelected(ITemplate template) {
		model.propertyChange(
				new PropertyChangeEvent(this, 
						IApplicationSourceListPageModel.PROPERTY_SELECTED_APP_SOURCE,
						null, 
						new TemplateApplicationSource(template)));
	}

	private HashMap<String, String> givenTheTemplateHasObjectLabels() {
		HashMap<String, String> labels = new HashMap<>();
		labels.put("abc", "xyz");
		when(template.getObjectLabels()).thenReturn(labels);
		return labels;
	}
	
	private Map<String, IParameter> givenTheTemplateHasParameters() {
		IParameter param = mock(IParameter.class);
		when(param.getName()).thenReturn("foo");
		when(param.clone()).thenReturn(param);
		Map<String, IParameter> parameters = new HashMap<>();
		parameters.put(param.getName(), param );
		when(template.getParameters()).thenReturn(parameters);
		return parameters;
	}

}
