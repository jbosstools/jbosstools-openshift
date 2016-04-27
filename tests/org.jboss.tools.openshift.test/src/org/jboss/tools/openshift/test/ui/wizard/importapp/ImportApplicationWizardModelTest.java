/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.wizard.importapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.ImportApplicationWizardModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
/**
 * @author Fred Bricon
 */
@RunWith(MockitoJUnitRunner.class)
public class ImportApplicationWizardModelTest {

	private ImportApplicationWizardModel model;

	@Mock
	private Connection connection;

	@Before
	public void setUp() {
		model = new ImportApplicationWizardModel();
		model.setConnection(connection);
	}

	@Test
	public void extractProjectNameFromURI() {
		assertNull(ImportApplicationWizardModel.extractProjectNameFromURI(null));
		assertEquals("bar", ImportApplicationWizardModel.extractProjectNameFromURI("http://foo/bar"));
		assertEquals("bar", ImportApplicationWizardModel.extractProjectNameFromURI("http://foo/bar.git"));
		assertEquals("bar", ImportApplicationWizardModel.extractProjectNameFromURI("http://foo/bar/"));
		assertEquals("ba.r", ImportApplicationWizardModel.extractProjectNameFromURI("http://foo/ba.r"));

		assertEquals("quickstart", ImportApplicationWizardModel.extractProjectNameFromURI("https://github.com/akram/quickstart"));
		assertEquals("quickstart", ImportApplicationWizardModel.extractProjectNameFromURI("https://github.com/akram/quickstart.git"));
		assertEquals("quickstart", ImportApplicationWizardModel.extractProjectNameFromURI("https://github.com/akram/quickstart/"));
		assertEquals("quickstart", ImportApplicationWizardModel.extractProjectNameFromURI("https://github.com/akram/quickstart.git/"));
	}
	
	@Test
	public void testLoadBuildConfigs() {
		IProject p1 = mock(IProject.class);
		IBuildConfig bc1 = mock(IBuildConfig.class);
		IBuildConfig bc2 = mock(IBuildConfig.class);
		when(p1.getResources(ResourceKind.BUILD_CONFIG)).thenReturn(Arrays.asList(bc1, bc2));

		IProject p2 = mock(IProject.class);
		IBuildConfig bc3 = mock(IBuildConfig.class);
		when(p2.getResources(ResourceKind.BUILD_CONFIG)).thenReturn(Arrays.asList(bc3));

		when(connection.getResources(ResourceKind.PROJECT)).thenReturn(Arrays.asList(p1, p2));

		//No project selected -> 2 projects returned
		model.loadBuildConfigs();
		List<ObservableTreeItem> results = model.getBuildConfigs();
		assertEquals(2, results.size());
		assertEquals(p1, results.get(0).getModel());
		assertEquals(p2, results.get(1).getModel());

		//p1 selected -> 2 build configs returned
		model.setProject(p1);
		model.loadBuildConfigs();
		results = model.getBuildConfigs();
		assertEquals(2, results.size());
		assertEquals(bc1, results.get(0).getModel());
		assertEquals(bc2, results.get(1).getModel());

		//p2 selected -> 1 build config returned
		model.setProject(p2);
		model.loadBuildConfigs();
		results = model.getBuildConfigs();
		assertEquals(1, results.size());
		assertEquals(bc3, results.get(0).getModel());
	}

}