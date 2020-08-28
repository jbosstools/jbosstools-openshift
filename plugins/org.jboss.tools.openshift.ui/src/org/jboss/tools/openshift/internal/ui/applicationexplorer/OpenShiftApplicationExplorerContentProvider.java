/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.applicationexplorer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.openshift.internal.ui.models.IElementListener;
import org.jboss.tools.openshift.internal.ui.models.IOpenshiftUIElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.MessageElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ProjectElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ServiceElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.StorageElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.URLElement;

import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * @author Red Hat Developers
 *
 */
public class OpenShiftApplicationExplorerContentProvider implements ITreeContentProvider, IElementListener {

	private ApplicationExplorerUIModel model;
	private StructuredViewer viewer;
	
	public OpenShiftApplicationExplorerContentProvider() {
		this(ApplicationExplorerUIModel.getInstance());
	}
	
	protected OpenShiftApplicationExplorerContentProvider(ApplicationExplorerUIModel model) {
		this.model = model;
		model.addListener(this);
	}
	
	@Override
	public void dispose() {
		model.removeListener(this);
	}
	
	protected void refreshViewer(Object element) {
		if (viewer != null) {
			viewer.refresh(element);
		}
	}

	@Override
	public void elementChanged(IOpenshiftUIElement<?, ?, ?> element) {
			refreshViewer(element);
	}
	
	

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (StructuredViewer) viewer;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot) {
			return new Object[] { model };
		} else {
			return new Object[0];
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ApplicationExplorerUIModel) {
			return getChildren((ApplicationExplorerUIModel) parentElement);
		} else if (parentElement instanceof ProjectElement) {
			return getChildren((ProjectElement) parentElement);
		} else if (parentElement instanceof ApplicationElement) {
			return getChildren((ApplicationElement) parentElement);
		} else if (parentElement instanceof ComponentElement) {
			return getChildren((ComponentElement) parentElement);
		}
		return null;
	}

	private Object[] getChildren(ApplicationExplorerUIModel parentElement) {
		try {
			List<ProjectElement> childs = new ArrayList<>();
			parentElement.getOdo().getProjects(parentElement.getClient()).forEach(project -> childs.add(new ProjectElement(project, parentElement)));
			return childs.toArray();
		} catch (Exception e) {
			return new Object[] { new MessageElement("Can't connect to cluster. Click to login.", parentElement) };
		}
	}

	private Object[] getChildren(ProjectElement parentElement) {
		try {
			List<ApplicationElement> childs = new ArrayList<>();
			parentElement.getParent().getOdo().getApplications(parentElement.getWrapped().getMetadata().getName()).forEach(application -> childs.add(new ApplicationElement(application, parentElement)));
			return childs.toArray();
		} catch (IOException e) {
			return new Object[] { "Can't list applications" };
		}
	}

	private Object[] getChildren(ApplicationElement parentElement) {
		List<Object> childs = new ArrayList<>();
		try {
			ProjectElement project = parentElement.getParent();
			ApplicationExplorerUIModel cluster = project.getParent();
			cluster.getOdo().getComponents(cluster.getClient(), project.getWrapped().getMetadata().getName(), parentElement.getWrapped().getName()).forEach(comp -> childs.add(new ComponentElement(comp,  parentElement)));
			cluster.getOdo().getServices(cluster.getClient(), project.getWrapped().getMetadata().getName(), parentElement.getWrapped().getName()).forEach(service -> childs.add(new ServiceElement(service, parentElement)));
		} catch (IOException|KubernetesClientException e) {
			if (childs.isEmpty()) {
				return new Object[] { "Can't list components" };
			}
		}
		return childs.toArray();
	}

	private Object[] getChildren(ComponentElement parentElement) {
		try {
			List<Object> childs = new ArrayList<>();
			ApplicationElement application = parentElement.getParent();
			ProjectElement project = application.getParent();
			ApplicationExplorerUIModel cluster = project.getParent();
			cluster.getOdo().getStorages(cluster.getClient(), project.getWrapped().getMetadata().getName(), application.getWrapped().getName(), parentElement.getWrapped().getPath(), parentElement.getWrapped().getName()).forEach(storage -> childs.add(new StorageElement(storage, parentElement)));
			cluster.getOdo().listURLs(project.getWrapped().getMetadata().getName(), application.getWrapped().getName(), parentElement.getWrapped().getPath(),  parentElement.getWrapped().getName()).forEach(url -> childs.add(new URLElement(url, parentElement)));
			return childs.toArray();
		} catch (IOException e) {
			return new Object[] { "Can't list storages or urls" };
		}
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IOpenshiftUIElement<?, ?, ?>) {
			return ((IOpenshiftUIElement<?, ?, ?>) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof ApplicationExplorerUIModel || element instanceof ProjectElement
		        || element instanceof ApplicationElement || element instanceof ComponentElement;
	}
}
