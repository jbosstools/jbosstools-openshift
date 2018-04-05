/*******************************************************************************
 * Copyright (c) 2017-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.internal.ui.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.jobs.JobGroup;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.ui.handler.DeleteResourceHandler;
import org.jboss.tools.openshift.internal.ui.models.IProjectWrapper;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

public class DeleteResourceHandlerTest {
    
    @Before
    public void init() {
        WatchManager.getInstance()._getWatches().clear();
    }

	@Test
	public void testStopWatchProject() throws Exception {
		IProject project = mock(IProject.class);
		when(project.getName()).thenReturn("ProjectName");
		when(project.getKind()).thenReturn(ResourceKind.PROJECT);

		IProjectWrapper projectWrapper = mock(IProjectWrapper.class);
		when(projectWrapper.getWrapped()).thenReturn(project);

		Connection connection = mock(Connection.class);
		ConnectionsRegistrySingleton.getInstance().add(connection);
		when(connection.ownsResource(project)).thenReturn(true);

		WatchManager.getInstance().startWatch(project, connection);
		
		assertEquals(WatchManager.KINDS.length, WatchManager.getInstance()._getWatches().size());
		
		DeleteResourceHandlerTestExtension handler = new DeleteResourceHandlerTestExtension();
		JobGroup deleteResourcesJobGroup = handler.deleteResources(new IResourceWrapper<?, ?>[] { projectWrapper });
		deleteResourcesJobGroup.join(500, null);

		assertEquals(0, WatchManager.getInstance()._getWatches().size());
	}

	class DeleteResourceHandlerTestExtension extends DeleteResourceHandler {

		public JobGroup deleteResources(final IResourceWrapper<?, ?>[] uiResources) {
			return super.deleteResources(uiResources);
		}
	}
}
