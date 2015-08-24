/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
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
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.tools.openshift.internal.ui.wizard.common.IResourceLabelsPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.NewApplicationWizardModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.template.IParameter;
import com.openshift.restclient.model.template.ITemplate;
/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class NewApplicationWizardModelTest {

	private NewApplicationWizardModel model;
	@Mock
	private ITemplate template;
	@Mock
	private IProject project;
	@Mock
	private IResourceFactory factory;
	
	@Before
	public void setup() throws Exception {
		NewApplicationWizardModel model = new NewApplicationWizardModel();
		model.setProject(project);
		model.setResourceFactory(factory);

		this.model = spy(model);
		doReturn(mock(InputStream.class)).when(this.model).createInputStream(anyString());
	}
	
	@Test
	public void setTemplateFileNameShouldLoadAndParseTheTemplate() {
		when(factory.create(any(InputStream.class))).thenReturn(template);
		model.setTemplateFileName("theFileName");
		
		verify(factory).create(any(InputStream.class));
		assertEquals(template, model.getTemplate());
	}

	@Test
	public void setTemplateShouldCopyParametersAndLabels() {
		Map<String, IParameter> parameters = givenTheTemplateHasParameters();
		HashMap<String, String> labels = givenTheTemplateHasObjectLabels();
		Collection<IResourceLabelsPageModel.Label> modelLabels = new ArrayList<IResourceLabelsPageModel.Label>();
		for (Entry<String, String> label : labels.entrySet()) {
			modelLabels.add(new IResourceLabelsPageModel.Label(label.getKey(), label.getValue()));
		}
		model.setTemplate(template);

		assertArrayEquals(parameters.values().toArray(), model.getParameters().toArray());
		assertArrayEquals(modelLabels.toArray(), model.getLabels().toArray());
	}
	
	private HashMap<String, String> givenTheTemplateHasObjectLabels() {
		HashMap<String, String> labels = new HashMap<String, String>();
		labels.put("abc", "xyz");
		when(template.getObjectLabels()).thenReturn(labels);
		return labels;
	}

	@Test
	public void resetParameterShouldSetTheOriginalValue() {
		IParameter param = mock(IParameter.class);
		when(param.getName()).thenReturn("foo");
		when(param.getValue()).thenReturn("abc");
		
		model.setParameters(Arrays.asList(new IParameter[] {param}));
		model.resetParameter(param);
		
		verify(param).setValue("abc");
	}
	
	@Test
	public void updateParameterValueShouldUpdateTheParameterValue() {
		IParameter param = mock(IParameter.class);
		model.updateParameterValue(param, "abc123");
		
		verify(param).setValue(eq("abc123"));
	}
	
	@Test
	public void getParametersShouldReturnAnEmptyMapWhenTemplateIsNull() {
		model.setTemplate(null);
		assertNotNull("Exp. an empty map",model.getParameters());
	}
	
	@Test
	public void getParametersShouldReturnAParameterMapWhenTemplateIsNotNull() {
		Map<String, IParameter> parameters = givenTheTemplateHasParameters();
		model.setTemplate(template);
		
		assertArrayEquals(parameters.values().toArray(), model.getParameters().toArray());
	}
	
	private Map<String, IParameter> givenTheTemplateHasParameters() {
		IParameter param = mock(IParameter.class);
		when(param.getName()).thenReturn("foo");

		Map<String, IParameter> parameters = new HashMap<String, IParameter>();
		parameters.put(param.getName(), param );
		when(template.getParameters()).thenReturn(parameters);
		return parameters;
	}
}
