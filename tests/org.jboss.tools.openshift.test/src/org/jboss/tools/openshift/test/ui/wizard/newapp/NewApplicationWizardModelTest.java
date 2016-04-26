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
package org.jboss.tools.openshift.test.ui.wizard.newapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createObservableTreeItems;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createResources;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSource;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.NewApplicationWizardModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.NotATemplateException;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.TemplateApplicationSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.route.IRoute;
import com.openshift.restclient.model.template.ITemplate;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class NewApplicationWizardModelTest {

	private TestableNewApplicationWizardModel model;
	@Mock
	private ITemplate template;
	@Mock
	private IProject project;
	@Mock
	private IResourceFactory factory;
	private List<ObservableTreeItem> projectItems;
	
	@Before
	public void setup() throws Exception {
		when(template.getKind()).thenReturn(ResourceKind.TEMPLATE);
		when(project.getName()).thenReturn(String.valueOf(System.currentTimeMillis()));
		createProjectTemplateItems();
		TestableNewApplicationWizardModel model = new TestableNewApplicationWizardModel();
		this.projectItems = createProjectTemplateItems();
		model.setProjectItems(projectItems);
		model.setProject(project);
		model.setResourceFactory(factory);

		this.model = spy(model);
		doReturn(mock(InputStream.class)).when(this.model).createInputStream(anyString());
	}

	/**
	 * Creates a tree of items:
	 * 
	 *  - project1
	 *     - template1
	 *  - project2
	 *     - template2
	 *     - template3
	 *  - project3
	 *     - template4
	 *     - template5
	 *     - template6
	 * @return 
	 */
	private List<ObservableTreeItem> createProjectTemplateItems() {
		List<ObservableTreeItem> projectItems = createObservableTreeItems(createResources(3, IProject.class,
				resource -> {
					when(resource.getName()).thenReturn(String.valueOf(System.currentTimeMillis()));
					}));
		for (int i = 0; i < 3; i++) {
			projectItems.get(i).setChildren(createObservableTreeItems(createResources(i + 1, ITemplate.class)));;
		}
		return projectItems;
	}

	@Test
	public void getProjectItemsShouldReturnAllItemsSet() {
		// pre-conditions
		model.setProjectItems(Collections.emptyList());

		// operations
		model.setProjectItems(projectItems);

		// verification
		assertThat(model.getProjectItems()).containsExactlyElementsOf(projectItems);
	}

	@Test
	public void setProjectShouldReturnSameProject() {
		// pre-conditions
		assertThat(model.getProjectItems().size()).isGreaterThan(2); 
		IProject project2 = (IProject) model.getProjectItems().get(1).getModel();

		// operations
		model.setProject(project2);

		// verification
		assertThat(model.getProject()).isEqualTo(project2);
	}

	@Test
	public void setProjectItemsShouldPreserveSelectedProjectIfContained() {
		// pre-conditions
		this.projectItems.add(new ObservableTreeItem(project));
		model.setProject(project);
		IProject selectedProject = model.getProject(); 
		assertThat(selectedProject).isNotNull();
		
		// operations
		model.setProjectItems(projectItems);

		// verification
		assertThat(model.getProject()).isEqualTo(selectedProject);
	}
	
	@Test
	public void setProjectItemsShouldSelect1stProjectIfCurrentNotContained() {
		// pre-conditions
		model.setProject(project);

		// operations
		model.setProjectItems(projectItems);

		// verification
		ObservableTreeItem projectItem = projectItems.get(0);
		assertThat(projectItem).isNotNull();
		assertThat(model.getProject()).isEqualTo(projectItem.getModel());
	}

	@Test
	public void setNullProjectShouldSet1stProject() {
		// pre-conditions

		// operations
		model.setProject(null);

		// verification
		assertThat(model.getProject()).isEqualTo(getProject(0));
	}
	
	@Test
	public void setNullProjectShouldSetNullIfNoProjectsAvailable() {
		// pre-conditions
		model.setProjectItems(Collections.emptyList());
		
		// operations
		model.setProject(null);

		// verification
		assertThat(model.getProject()).isNull();
	}

	@Test
	public void setProjectToProject2ShouldHaveGetTemplatesReturnTemplatesForProject2() {
		// pre-conditions
		IProject project2 = getProject(1); 

		// operations
		model.setProject(project2);
		List<ObservableTreeItem> templates = model.getAppSources();
		
		// verification
		assertThat(templates).containsAll(getTemplateItemsForProject(1));
	}

	@Test
	public void setServerTemplateShouldSetUseLocalTemplateToFalse() {
		// pre-conditions
		IApplicationSource template = mock(IApplicationSource.class);

		// operations
		model.setServerAppSource(template );

		// verification
		assertThat(model.isUseLocalAppSource()).isFalse();
	}
	
	@Test
	public void setLocalTemplateFilenameShouldSetUseLocalTemplateToTrue() {
		// pre-conditions

		// operations
		model.setLocalAppSourceFileName("test.json");

		// verification
		assertThat(model.isUseLocalAppSource()).isTrue();
	}

	@Test
	public void setTemplateFileNameShouldLoadAndParseTheTemplate() {
		when(factory.create(any(InputStream.class))).thenReturn(template);
		model.setUseLocalAppSource(true);
		model.setLocalAppSourceFileName("resources/eap6-basic-sti.json");
		
		verify(factory).create(any(InputStream.class));
		assertEquals(TemplateApplicationSource.class, model.getSelectedAppSource().getClass());
	}
	
	@Test
	public void setWrongJsonAsTemplateFile() throws Exception {
		IRoute route = Mockito.mock(IRoute.class);
		when(route.getKind()).thenReturn(ResourceKind.ROUTE);
		when(factory.create(any(InputStream.class))).thenReturn(route);
		try {
			model.setLocalAppSourceFileName("resources/jboss_infinispan-server_ImageStreamImport.json");
			fail("No NotATemplateException occurred");
		} catch (NotATemplateException e) {
			assertEquals(ResourceKind.ROUTE, e.getResourceKind());
		}
	}
	
	private IProject getProject(int i) {
		assertThat(projectItems.size()).isGreaterThan(i + 1);

		return (IProject) projectItems.get(i).getModel();
	}

	private List<ObservableTreeItem> getTemplateItemsForProject(int i) {
		assertThat(projectItems.size()).isGreaterThan(i + 1);

		return projectItems.get(i).getChildren().stream()
				.collect(Collectors.<ObservableTreeItem>toList());
	}

	public static class TestableNewApplicationWizardModel extends NewApplicationWizardModel {
		@Override
		public void setProjectItems(List<ObservableTreeItem> projects) {
			super.setProjectItems(projects);
		}
	}
}
