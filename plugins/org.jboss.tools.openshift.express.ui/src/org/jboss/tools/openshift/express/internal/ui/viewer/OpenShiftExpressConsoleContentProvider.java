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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

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
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

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
	private ArrayList<UserDelegate> loadedUsers = new ArrayList<UserDelegate>();
	private ArrayList<UserDelegate> loadingUsers = new ArrayList<UserDelegate>();
	private HashMap<UserDelegate, Exception> errors = new HashMap<UserDelegate, Exception>();

	@Override
	public Object[] getElements(final Object parentElement) {
		// A refresh on the whole model... clear our cache
		loadedUsers.clear();
		loadingUsers.clear();
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
	
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof UserDelegate) {
			UserDelegate user = (UserDelegate) parentElement;
			if(!user.isConnected() && !user.canPromptForPassword()) {
				return new Object[]{new NotConnectedUserStub()};
			}
			if (!loadedUsers.contains(parentElement)) {
				if (!loadingUsers.contains(parentElement)) {
					// Load the data
					launchLoadingUserJob((UserDelegate) parentElement);
				}
				// return a stub object that says loading...
				return new Object[] { new LoadingStub() };
			}
			Exception ose = errors.get((UserDelegate)parentElement);
			if( ose != null ) {
				return new Object[]{ose};
			}
		}
		return getChildrenForElement_LogException(parentElement, false);
	}

	// Force the children to load completely
	private void getChildrenFor(Object[] parentElements) {
		for (int i = 0; i < parentElements.length; i++) {
			getChildrenForElement_LogException(parentElements[i], true);
		}
	}

	// Get the children without the protection of a "loading..." situation
	private Object[] getChildrenForElement_LogException(Object parentElement, boolean recurse) {
		try {
			return getChildrenForElement(parentElement, recurse);
		} catch (OpenShiftException e) {
			Logger.error("Unable to retrieve OpenShift information", e);
		} catch (SocketTimeoutException e) {
			Logger.error("Unable to retrieve OpenShift information", e);
		}
		return new Object[0];
	}
	
	private Object[] getChildrenForElement(Object parentElement, boolean recurse) throws OpenShiftException, SocketTimeoutException {
		// .... the actual work is done here...
		Object[] children = new Object[0];
//		try {
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

			if (recurse) {
				getChildrenFor(children);
			}
//		} catch (OpenShiftException e) {
//			Logger.error("Unable to retrieve OpenShift information", e);
//		}
		return children;
	}

	private void launchLoadingUserJob(final UserDelegate user) {
		Job job = new Job("Loading OpenShift User information...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading OpenShift information...", IProgressMonitor.UNKNOWN);
				monitor.worked(1);
				// Get the actual children, with the delay
				loadingUsers.add(user);
				try {
					getChildrenForElement(user, Boolean.valueOf(System.getProperty("org.jboss.tools.openshift.express.ui.eagerloading", "true"))); // JBIDE-11680 false = fast, but blocks ui while loading cartridges, true = slow, but no blocking since cartridges is forced loaded.

				} catch(OpenShiftException e) {
					errors.put(user, e);
				} catch(SocketTimeoutException e) {
					errors.put(user, e);
				}
				loadedUsers.add(user);
				loadingUsers.remove(user);
				refreshViewerObject(user); 
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
