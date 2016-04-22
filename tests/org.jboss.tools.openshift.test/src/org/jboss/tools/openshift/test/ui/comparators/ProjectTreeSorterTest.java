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
package org.jboss.tools.openshift.test.ui.comparators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.viewers.ViewerComparator;
import org.jboss.tools.openshift.internal.ui.comparators.ProjectViewerComparator;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;

@RunWith(MockitoJUnitRunner.class)
public class ProjectTreeSorterTest {
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testProjectsInTree() {
		final ViewerComparator comparator = ProjectViewerComparator.createProjectTreeSorter();
		ObservableTreeItem i1 = new ObservableTreeItem(mockProject("default", null));
		ObservableTreeItem i2 = new ObservableTreeItem(mockProject("openshift", null));
		ObservableTreeItem i3 = new ObservableTreeItem(mockProject("a", null));
		ObservableTreeItem i4 = new ObservableTreeItem(mockProject("d", null));
		ObservableTreeItem[] items = new ObservableTreeItem[]{i2, i4, i3, i1};
		Arrays.sort(items, new Comparator<ObservableTreeItem>() {

			@Override
			public int compare(ObservableTreeItem o1, ObservableTreeItem o2) {
				return comparator.compare(null, o1, o2);
			}

		});
		assertEquals(i1, items[0]);
		assertEquals(i2, items[1]);
		assertEquals(i3, items[2]);
		assertEquals(i4, items[3]);
	}

	IProject mockProject(String name, String displayName) {
		IProject p = Mockito.mock(IProject.class);
		when(p.getName()).thenReturn(name);
		if(displayName != null) {
			when(p.getDisplayName()).thenReturn(displayName);
		}
		return p;
	}

	@Test
	public void testProjectsWithLabelProvider() {
		final ViewerComparator comparator = new ProjectViewerComparator(new OpenShiftExplorerLabelProvider());
		IProject p1 = mockProject("default", "z");
		IProject p2 = mockProject("openshift", "y");
		IProject p3 = mockProject("a", "c");
		IProject p4 = mockProject("b2", null);
		IProject p5 = mockProject("d", "b");
		IProject[] projects = new IProject[]{p3,p5,p2,p1,p4};
		Arrays.sort(projects, new Comparator<IProject>() {

			@Override
			public int compare(IProject o1, IProject o2) {
				return comparator.compare(null, o1, o2);
			}

		});
		assertEquals(p1, projects[0]);
		assertEquals(p2, projects[1]);
		assertEquals(p5, projects[2]);
		assertEquals(p4, projects[3]);
		assertEquals(p3, projects[4]);
	}

	@Test
	public void testProjectItemsWithLabelProvider() {
		final Comparator<ObservableTreeItem> comparator = new ProjectViewerComparator(new OpenShiftExplorerLabelProvider()).asItemComparator();
		ObservableTreeItem p1 = new ObservableTreeItem(mockProject("default", "z"));
		ObservableTreeItem p2 = new ObservableTreeItem(mockProject("openshift", "y"));
		ObservableTreeItem p3 = new ObservableTreeItem(mockProject("a", "c"));
		ObservableTreeItem p4 = new ObservableTreeItem(mockProject("b2", null));
		ObservableTreeItem p5 = new ObservableTreeItem(mockProject("d", "b"));
		ObservableTreeItem[] projects = new ObservableTreeItem[]{p3,p5,p2,p1,p4};
		Arrays.sort(projects, comparator);
		assertEquals(p1, projects[0]);
		assertEquals(p2, projects[1]);
		assertEquals(p5, projects[2]);
		assertEquals(p4, projects[3]);
		assertEquals(p3, projects[4]);
	}

	@Test
	public void testServices() {
		ViewerComparator comparator = ProjectViewerComparator.createProjectTreeSorter();
		ObservableTreeItem i1 = new ObservableTreeItem(mockService("s1", "z"));
		ObservableTreeItem i2 = new ObservableTreeItem(mockService("s2", "a"));
		
		assertTrue(comparator.compare(null, i1, i2) < 0);
		assertTrue(comparator.compare(null, i2, i1) > 0);
		assertEquals(0, comparator.compare(null, i1, i1));
	}

	private IService mockService(String name, String toString) {
		IService service = Mockito.mock(IService.class);
		when(service.getKind()).thenReturn(ResourceKind.SERVICE);
		when(service.getName()).thenReturn(name);
		when(service.toString()).thenReturn(toString);
		return service;
	}

	@Test
	public void testBuildConfigs() {
		ViewerComparator comparator = ProjectViewerComparator.createProjectTreeSorter(new OpenShiftExplorerLabelProvider());
		ObservableTreeItem i1 = new ObservableTreeItem(mockBuildConfig("n", "c1", "z"));
		ObservableTreeItem i2 = new ObservableTreeItem(mockBuildConfig("n", "c2", "x"));
		assertTrue(comparator.compare(null, i1, i2) < 0); //compared by source uri!
		assertTrue(comparator.compare(null, i2, i1) > 0);
		assertEquals(0, comparator.compare(null, i1, i1));
	}

	private IBuildConfig mockBuildConfig(String name, String uri, String toString) {
		IBuildConfig config = Mockito.mock(IBuildConfig.class);
		when(config.getKind()).thenReturn(ResourceKind.BUILD_CONFIG);
		when(config.getName()).thenReturn(name);
		when(config.getSourceURI()).thenReturn(uri);
		when(config.toString()).thenReturn(toString);
		return config;
	}

}
