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
package org.jboss.tools.openshift.express.internal.ui.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * @author Jeff Cantrill
 */
public class ExpressExplorerContentProvider implements ITreeContentProvider {

	private StructuredViewer viewer;

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (StructuredViewer) viewer;
	}

	// Keep track of what's loading and what's finished
	private List<Object> loadedElements = new ArrayList<Object>();
	private List<Object> loadingElements = new ArrayList<Object>();

	private Map<Object, Exception> errors = new HashMap<Object, Exception>();

	/**
	 * Called to obtain the root elements of the tree viewer, the connections
	 */
	@Override
	public Object[] getElements(final Object parentElement) {
		// A refresh on the whole model... clear our cache
		loadedElements.clear();
		loadingElements.clear();
		errors.clear();
		if (parentElement instanceof ConnectionsRegistry) {
			ConnectionsRegistry registry = (ConnectionsRegistry) parentElement;
			return registry.getAll(ExpressConnection.class).toArray();
		} else if (parentElement instanceof ExpressConnection) {
			List<IDomain> domains = ((ExpressConnection) parentElement).getDomains();
			return domains.toArray(new IDomain[domains.size()]);
		} else {
			return new Object[0];
		}
	}

	/**
	 * Called to obtain the children of any element in the tree viewer, ie, from
	 * a connection or an application
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ExpressConnection) {
			ExpressConnection connection = (ExpressConnection) parentElement;
			if (!connection.isConnected()
					&& !connection.canPromptForPassword()) {
				return new Object[] { new NotConnectedUserStub() };
			}
			return loadChildren(parentElement);
		} else if (parentElement instanceof IDomain) {
			return loadChildren(parentElement);
		} else if (parentElement instanceof IApplication) {
			return loadChildren(parentElement);
		} else {
			return getChildrenFor(parentElement);
		}
	}

	/**
	 * @param parentElement
	 * @return
	 */
	private Object[] loadChildren(Object parentElement) {
		if (!loadedElements.contains(parentElement)) {
			if (!loadingElements.contains(parentElement)) {
				// Load the data
				launchLoadingJob(parentElement);
			}
			// return a stub object that says loading...
			return new Object[] { new LoadingStub() };
		}
		Exception ose = errors.get(parentElement);
		if (ose != null) {
			return new Object[] { ose };
		}
		return getChildrenFor(parentElement);
	}

	private Object[] getChildrenFor(Object parentElement) {
		Object[] children = new Object[0];
		try {
			if (parentElement instanceof OpenShiftExplorerContentCategory) {
				ExpressConnection user = ((OpenShiftExplorerContentCategory) parentElement).getUser();
				children = new Object[] { user };
			} else if (parentElement instanceof ExpressConnection) {
				final ExpressConnection connection = (ExpressConnection) parentElement;
				children = connection.getDomains().toArray();
			} else if (parentElement instanceof IDomain) {
				final IDomain domain = (IDomain) parentElement;
				children = domain.getApplications().toArray();
			} else if (parentElement instanceof IApplication) {
				children = ((IApplication) parentElement).getEmbeddedCartridges().toArray();
			}
		} catch (OpenShiftException e) {
			errors.put(parentElement, e);
		}

		return children;
	}

	private void launchLoadingJob(final Object element) {
		Job job = new Job("Loading OpenShift resources...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading OpenShift resources...", IProgressMonitor.UNKNOWN);
				monitor.worked(1);
				// Get the actual children, with the delay
				loadingElements.add(element);
				getChildrenFor(element);
				loadedElements.add(element);
				loadingElements.remove(element);
				refreshViewerObject(element);
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.LONG);
		job.schedule();
	}

	private void refreshViewerObject(final Object object) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				viewer.refresh(object);
			}
		});
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof ConnectionsRegistry
				|| element instanceof IConnection
				|| element instanceof IDomain
				|| element instanceof IApplication;
	}

	public static class LoadingStub {
	}

	public static class NotConnectedUserStub {
	}
}
