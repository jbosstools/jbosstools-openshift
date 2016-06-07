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
package org.jboss.tools.openshift.internal.common.ui.explorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;

/**
 * Base content provider to hold common logic for OpenShift Explorer
 * contributions
 */
public abstract class BaseExplorerContentProvider implements ITreeContentProvider {

	private static final String MSG_LOADING_RESOURCES = "Loading OpenShift resources...";

	private TreeViewer viewer;

	// Keep track of what's loading and what's finished
	private Map<Object, LoadingStub> loadedElements = new ConcurrentHashMap<>();
	private Map<Object, LoadingStub> loadingElements = new ConcurrentHashMap<>();

	/**
	 * Get the root elements for the explorer. This should provide what
	 * getElements would normally provide but this method will be called from
	 * {@link BaseExplorerContentProvider#getElements(Object)}
	 * 
	 * @param parentElement
	 * @return
	 */
	protected abstract Object[] getExplorerElements(final Object parentElement);

	/**
	 * Retrieves the child elements for a given parent. This should provide what
	 * getChildren would normally provide but this method will be called from
	 * {@link BaseExplorerContentProvider#getChildren(Object)} or a job for the
	 * defered loading
	 * 
	 * @param parentElement
	 * @return
	 */
	protected abstract Object[] getChildrenFor(Object parentElement);

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TreeViewer) viewer;
	}

	/**
	 * Allow subclasses the opportunity to handle the connection changed event
	 * 
	 * @param connection
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	protected void handleConnectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
		refreshViewer(connection);
	}

	/**
	 * Allow subclasses the opportunity to handle the connection removed event
	 * 
	 * @param connection
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	protected void handleConnectionRemoved(IConnection connection) {
		refreshViewer(null);
	}

	/**
	 * Called to obtain the root elements of the tree viewer, the connections
	 */
	@Override
	public Object[] getElements(final Object parentElement) {
		// A refresh on the whole model... clear our cache
		loadedElements.clear();
		loadingElements.clear();
		return getExplorerElements(parentElement);
	}

	/**
	 * The default implementation will load the children in a deferred job
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (loadedElements.containsKey(parentElement)) {
			return loadedElements.remove(parentElement).getChildren();
		}
		return loadChildren(parentElement);
	}

	/**
	 * Add the exception for the given element
	 * 
	 * @param element
	 * @param e
	 */
	protected final void addException(Object element, Exception e) {
		if (loadingElements.containsKey(element)) {
			loadingElements.get(element).add(e);
		}
	}

	/**
	 * @param parentElement
	 * @return
	 */
	protected final Object[] loadChildren(Object parentElement) {
		if (!loadedElements.containsKey(parentElement)) {
			if (!loadingElements.containsKey(parentElement)) {
				// Load the data
				return new Object[] { launchLoadingJob(parentElement) };
			} else {
				// Data is loading, after it is loaded refresh will be called.
				return new Object[] { loadingElements.get(parentElement) };
			}
		}
		return getChildrenFor(parentElement);
	}

	private LoadingStub launchLoadingJob(final Object element) {
		final LoadingStub stub = new LoadingStub();
		loadingElements.put(element, stub);
		Job job = new Job(MSG_LOADING_RESOURCES) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask(MSG_LOADING_RESOURCES, IProgressMonitor.UNKNOWN);
					monitor.worked(1);
					// Get the actual children, with the delay
					stub.addChildren(getChildrenFor(element));
					return Status.OK_STATUS;
				} catch (Exception e) {
					addException(element, e);
					return new Status(Status.ERROR, OpenShiftCommonUIActivator.PLUGIN_ID,
							"There was an error retrieving children in the OpenShift explorer", e);
				} finally {
					loadedElements.put(element, stub);
					loadingElements.remove(element);
					monitor.done();
					refreshViewer(element);
				}
			}
		};
		job.setPriority(Job.LONG);
		job.schedule();
		return stub;
	}

	protected void refreshViewer(final Object object) {
		Control control = viewer.getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!viewer.getControl().isDisposed()) {
						if (object != null) {
							viewer.refresh(object);
						} else {
							viewer.refresh();
						}
					}
				}
			});
		}
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	public static class LoadingStub {

		private List<Object> children = new ArrayList<>();

		public LoadingStub() {
		}

		public void add(Exception e) {
			children.add(e);
		}

		public Object[] getChildren() {
			return children.toArray();
		}

		public void addChildren(Object[] children) {
			this.children.addAll(Arrays.asList(children));
		}

	}

	public static class NotConnectedUserStub {
	}
}
