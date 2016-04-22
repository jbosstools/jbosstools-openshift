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
package org.jboss.tools.openshift.internal.ui.comparators;

import java.util.Comparator;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

public class ProjectViewerComparator extends ViewerComparator {
	
	private static final int LAST = 1;
	private static final String DEFAULT_PROJECT = "default";
	private static final String OPENSHIFT_PROJECT = "openshift";

	private ILabelProvider labelProvider;

	public ProjectViewerComparator() {}

	public ProjectViewerComparator(ILabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}

	/**
	 * Returns comparator that compares only tree items representing projects.
	 * It should not be applied to items other than projects. 
	 * @return
	 */
	public Comparator<ObservableTreeItem> asItemComparator() {
		return new Comparator<ObservableTreeItem>() {
			@Override
			public int compare(ObservableTreeItem o1, ObservableTreeItem o2) {
				Object m1 = ((ObservableTreeItem) o1).getModel();
				Object m2 = ((ObservableTreeItem) o2).getModel();
				return ProjectViewerComparator.this.compare(null, m1, m2);
			}
		};
	}

	/**
	 * Compares only projects. Other objects always go after projects and equal to each other.
	 * When applied to sorting a mixed array, projects go to the beginning of the array and are sorted,
	 * other objects go to the end without change in order.
	 * Project named 'default' is always the first.
	 * Projects with name prefixed 'openshift' go after 'default' and before all other projects.
	 * Finally, projects are compared by label provider, or if it is not available then by name.
	 */
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		boolean isProject1 = (e1 instanceof IProject);
		boolean isProject2 = (e2 instanceof IProject);
		if(!isProject1 || !isProject2) {
			return (isProject1) ? -1 : (isProject2) ? LAST : 0;
		}
		IProject projectOne = (IProject) e1;
		IProject projectTwo = (IProject) e2;
		
		final String name1 = projectOne.getName();
		final String name2 = projectTwo.getName();
		
		if(DEFAULT_PROJECT.equals(name1)) {
			return -1;
		}
		if(DEFAULT_PROJECT.equals(name2)) {
			return 1;
		}
		if(name1.startsWith(OPENSHIFT_PROJECT) && !name2.startsWith(OPENSHIFT_PROJECT)) {
			return -1;
		}
		if(!name1.startsWith(OPENSHIFT_PROJECT) && name2.startsWith(OPENSHIFT_PROJECT)) {
			return 1;
		}

		if(labelProvider != null) {
			return labelProvider.getText(e1).compareTo(labelProvider.getText(e2));
		}
		
		return name1.compareTo(name2);
	}

	public static ViewerComparator createProjectTreeSorter() {
		return createProjectTreeSorter(null);
	}

	/**
	 * Label provider is helpful for cases when viewer's label provider extends
	 * StyledCellLabelProvider but does not implement ILabelProvider.
	 * @param labelProvider
	 * @return
	 */
	public static ViewerComparator createProjectTreeSorter(final ILabelProvider labelProvider) {
		final ProjectViewerComparator projectComparator = new ProjectViewerComparator(labelProvider);
		return new ViewerComparator() {
			@SuppressWarnings("unchecked")
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if(e1 instanceof ObservableTreeItem && e2 instanceof ObservableTreeItem) {
					Object o1 = ((ObservableTreeItem) e1).getModel();
					Object o2 = ((ObservableTreeItem) e2).getModel();
					if(o1 instanceof IProject && o2 instanceof IProject) {
						return projectComparator.compare(viewer, o1, o2);
					} else if(labelProvider != null) {
						return labelProvider.getText(o1).compareTo(labelProvider.getText(o2));
					} else if(o1 instanceof IResource && o2 instanceof IResource) {
						String name1 = ((IResource) o1).getName();
						String name2 = ((IResource) o2).getName();
						return getComparator().compare(name1, name2);
					}
				}
				return super.compare(viewer, e1, e2);
			}
		};
	}
	
}
