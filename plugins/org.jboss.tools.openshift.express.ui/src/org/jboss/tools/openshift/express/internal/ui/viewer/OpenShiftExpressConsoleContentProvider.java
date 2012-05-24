/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.viewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * 
 */
public class OpenShiftExpressConsoleContentProvider implements ITreeContentProvider {

	private StructuredViewer viewer;

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (StructuredViewer) viewer;
	}

	public static class LoadingStub {
		public LoadingStub() {
		}
	}

	public static class NotConnectedUserStub {
		public NotConnectedUserStub () {
		}
	}

	// Keep track of what's loading and what's finished
	private List<Object> loadedElements = new ArrayList<Object>();
	private List<Object> loadingElements = new ArrayList<Object>();
	
	private Map<Object, Exception> errors = new HashMap<Object, Exception>();

	/**
	 * Called to obtain the root elements of the tree viewer, ie, the Users
	 */
	@Override
	public Object[] getElements(final Object parentElement) {
		// A refresh on the whole model... clear our cache
		loadedElements.clear();
		loadingElements.clear();
		errors.clear();
		if (parentElement instanceof IWorkspaceRoot) {
			return UserModel.getDefault().getUsers();
		}
		if (parentElement instanceof UserModel) {
			UserDelegate[] users = ((UserModel) parentElement).getUsers();
			return users;
		}
		return new Object[0];
	}
	
	/**
	 * Called to obtain the children of any element in the tree viewer, ie, from a user or an application
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof UserDelegate) {
			UserDelegate user = (UserDelegate) parentElement;
			if(!user.isConnected() && !user.canPromptForPassword()) {
				return new Object[]{new NotConnectedUserStub()};
			}
			return loadChildren(parentElement);
		} else if(parentElement instanceof IApplication) {
			return loadChildren(parentElement);
		}
		return getChildrenForElement(parentElement);
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
		if( ose != null ) {
			return new Object[]{ose};
		}
		return getChildrenForElement(parentElement);
	}

	
	
	private Object[] getChildrenForElement(Object parentElement) {
		// .... the actual work is done here...
		Object[] children = new Object[0];
		try {
			if (parentElement instanceof OpenShiftExpressConsoleContentCategory) {
				UserDelegate user = ((OpenShiftExpressConsoleContentCategory) parentElement).getUser();
				children = new Object[] { user };
			} else if (parentElement instanceof UserDelegate) {
				final UserDelegate user = (UserDelegate) parentElement;
				if (user.hasDomain()) {
					children = user.getApplications().toArray();
				}
			} else if (parentElement instanceof IApplication) {
				children = ((IApplication) parentElement).getEmbeddedCartridges().toArray();
			}
		} catch(OpenShiftException e) {
			errors.put(parentElement, e);
		}

		return children;
	}

	private void launchLoadingJob(final Object element) {
		Job job = new Job("Loading OpenShift information...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading OpenShift information...", IProgressMonitor.UNKNOWN);
				monitor.worked(1);
				// Get the actual children, with the delay
				loadingElements.add(element);
				getChildrenForElement(element); //Boolean.valueOf(System.getProperty("org.jboss.tools.openshift.express.ui.eagerloading", "true"))); // JBIDE-11680 false = fast, but blocks ui while loading cartridges, true = slow, but no blocking since cartridges is forced loaded.
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof UserDelegate) {
			return true;
		}
		if (element instanceof IApplication) {
			return true;
		}
		return false;
	}

}
