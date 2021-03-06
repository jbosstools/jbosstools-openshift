/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.applicationexplorer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import org.jboss.tools.openshift.core.odo.Application;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.ui.applicationexplorer.OpenShiftApplicationExplorerContentProvider;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel.ClusterClient;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.CreateComponentMessageElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.CreateProjectMessageElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ProjectElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenShiftApplicationExplorerContentProviderTest {

	private static final String OPENSHIFT_SERVER_URL = "https://localhost:8442";

	private OpenShiftApplicationExplorerContentProvider provider;
	private ApplicationExplorerUIModel model;
	private Odo odo;

	@Before
	public void setup() throws Exception {
	  odo = mock(Odo.class);
	  ClusterClient info = mock(ClusterClient.class);
	  doReturn(odo).when(info).getOdo();
		this.model = new ApplicationExplorerUIModel(info) {
		};
		this.provider = new OpenShiftApplicationExplorerContentProvider(model) {
		};
	}

  protected Project mockProject(String name) {
    Project project = mock(Project.class);
    ObjectMeta meta = mock(ObjectMeta.class);
    doReturn(name).when(meta).getName();
    doReturn(meta).when(project).getMetadata();
    doReturn(Collections.singletonList(project)).when(odo).getProjects(any(OpenShiftClient.class));
    return project;
  }

  @Test
	public void checkEmptyClusterReturnsLinkToCreateProject() throws InterruptedException, TimeoutException {
	  Object[] childs = provider.getChildren(model);
	  assertEquals(1, childs.length);
	  Object element = childs[0];
	  assertTrue(element instanceof CreateProjectMessageElement);
	}
	
	 @Test
   public void checkClusterWithSingleProjectReturnsLinkToCreateComponent()
       throws InterruptedException, TimeoutException {
	   mockProject("myproject");
     Object[] childs = provider.getChildren(model);
     assertEquals(1, childs.length);
     Object element = childs[0];
     assertTrue(element instanceof ProjectElement);
     childs = provider.getChildren(element);
     assertEquals(1, childs.length);
     element = childs[0];
     assertTrue(element instanceof CreateComponentMessageElement<?>);
   }
	 
   @Test
   public void checkClusterWithSingleProjectAndSingleAppReturnsLinkToCreateComponent()
       throws InterruptedException, TimeoutException, IOException {
     mockProject("myproject");
     Application app = mock(Application.class);
     doReturn(Collections.singletonList(app)).when(odo).getApplications(eq("myproject"));
     Object[] childs = provider.getChildren(model);
     assertEquals(1, childs.length);
     Object element = childs[0];
     assertTrue(element instanceof ProjectElement);
     childs = provider.getChildren(element);
     assertEquals(1, childs.length);
     element = childs[0];
     assertTrue(element instanceof ApplicationElement);
     childs = provider.getChildren(element);
     assertEquals(1, childs.length);
     element = childs[0];
     assertTrue(element instanceof CreateComponentMessageElement<?>);
   }

 }
