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

/**
 * Base content provider to hold common logic for OpenShift Explorer contributions
 */
public abstract class BaseExplorerContentProvider implements ITreeContentProvider {

	private static final String MSG_LOADING_RESOURCES = "Loading OpenShift resources...";

	private StructuredViewer viewer;

	// Keep track of what's loading and what's finished
	private List<Object> loadedElements = new ArrayList<Object>();
	private List<Object> loadingElements = new ArrayList<Object>();
	private Map<Object, Exception> errors = new HashMap<Object, Exception>();
	
	/**
	 * Get the root elements for the explorer.  This should provide what
	 * getElements would normally provide but  this method will be called from
	 * {@link BaseExplorerContentProvider#getElements(Object)} 
	 * @param parentElement
	 * @return
	 */
	protected abstract Object[] getExplorerElements(final Object parentElement);
	
	/**
	 * Retrieves the child elements for a given parent.  This should provide what
	 * getChildren would normally provide but this method will be called from 
	 * {@link BaseExplorerContentProvider#getChildren(Object)} or a job for the
	 * defered loading
	 * @param parentElement
	 * @return
	 */
	protected abstract Object[] getChildrenFor(Object parentElement);

	
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (StructuredViewer) viewer;
	}

	/**
	 * Called to obtain the root elements of the tree viewer, the connections
	 */
	@Override
	public Object[] getElements(final Object parentElement) {
		// A refresh on the whole model... clear our cache
		loadedElements.clear();
		loadingElements.clear();
		errors.clear();
		return getExplorerElements(parentElement);
	}
	/**
	 * The default implementation will load the children in a
	 * deferred job
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		return loadChildren(parentElement);
	}
	
	/**
	 * Add the exception for the given element
	 * @param element
	 * @param e
	 */
	protected final void addException(Object element, Exception e){
		errors.put(element, e);
	}
	
	/**
	 * @param parentElement
	 * @return
	 */
	protected final Object[] loadChildren(Object parentElement) {
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

	private void launchLoadingJob(final Object element) {
		Job job = new Job(MSG_LOADING_RESOURCES) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(MSG_LOADING_RESOURCES, IProgressMonitor.UNKNOWN);
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
	public void dispose() {
	}

	public static class LoadingStub {
	}

	public static class NotConnectedUserStub {
	}
}
