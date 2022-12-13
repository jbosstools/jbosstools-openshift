/*******************************************************************************
 * Copyright (c) 2015-2021 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.applicationexplorer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.ui.applicationexplorer.OpenShiftApplicationExplorerContentProvider;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel.ClusterClient;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.CreateComponentMessageElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.CreateProjectMessageElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.NamespaceElement;
import org.jboss.tools.openshift.internal.ui.odo.OdoCliFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class OpenShiftApplicationExplorerContentProviderTest {

	private OpenShiftApplicationExplorerContentProvider provider;
	private ApplicationExplorerUIModel model;
	private Odo odo;

	@Before
	public void setup() throws Exception {
	  odo = mock(Odo.class);
	  ClusterClient info = mock(ClusterClient.class);
	  doReturn(odo).when(info).getOdo();
	  OdoCliFactory factory = mock(OdoCliFactory.class);
	  doReturn(CompletableFuture.completedFuture(odo)).when(factory).getOdo();
		this.model = new ApplicationExplorerUIModel(info) {

			@Override
			protected OdoCliFactory getFactory() {
				return factory;
			}
		};
		this.provider = new OpenShiftApplicationExplorerContentProvider(model) {
		};
	}

  protected void mockProject(String name) throws IOException {
    doReturn(name).when(odo).getNamespace();
  }

  @Test
	public void checkEmptyClusterReturnsLinkToCreateProject() {
	  Object[] childs = provider.getChildren(model);
	  assertEquals(1, childs.length);
	  Object element = childs[0];
	  assertTrue(element instanceof CreateProjectMessageElement);
	}
	
	 @Test
   public void checkClusterWithSingleProjectReturnsLinkToCreateComponent()
       throws IOException {
	   mockProject("myproject");
     Object[] childs = provider.getChildren(model);
     assertEquals(1, childs.length);
     Object element = childs[0];
     assertTrue(element instanceof NamespaceElement);
     childs = provider.getChildren(element);
     assertEquals(1, childs.length);
     element = childs[0];
     assertTrue(element instanceof CreateComponentMessageElement<?>);
   }
	 
   @Test
   public void checkClusterWithSingleProjectAndSingleAppReturnsLinkToCreateComponent()
       throws IOException {
     mockProject("myproject");
     Object[] childs = provider.getChildren(model);
     assertEquals(1, childs.length);
     Object element = childs[0];
     assertTrue(element instanceof NamespaceElement);
     childs = provider.getChildren(element);
     assertEquals(1, childs.length);
     element = childs[0];
     assertTrue(element instanceof CreateComponentMessageElement<?>);
   }

 }
