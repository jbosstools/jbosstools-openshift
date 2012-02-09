package org.jboss.tools.openshift.express.internal.ui.viewer;

import java.util.ArrayList;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

public class OpenShiftExpressConsoleContentProvider implements ITreeContentProvider {

	private StructuredViewer viewer;
	
	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (StructuredViewer)viewer;
	}
	
	public static class LoadingStub {
		public LoadingStub() {
		}
	}

	// Keep track of what's loading and what's finished
	private ArrayList<IUser> loadedUsers = new ArrayList<IUser>();
	private ArrayList<IUser> loadingUsers = new ArrayList<IUser>();
	
	@Override
	public Object[] getElements(final Object parentElement) {
		if(parentElement instanceof IWorkspaceRoot) {
			return UserModel.getDefault().getUsers();
		}
		if( parentElement instanceof UserModel ) {
			IUser[] users = ((UserModel)parentElement).getUsers();
			return users;
		}
		
		if( parentElement instanceof IUser ) {
			if( !loadedUsers.contains(parentElement)) {
				if( !loadingUsers.contains(parentElement)) {
					// Load the data
					launchLoadingUserJob((IUser)parentElement);
				}
				// return a stub object that says loading...
				return new Object[]{new LoadingStub()};
			}
		}
		return getChildrenFor(parentElement, false);
	}

	// Force the children to load completely
	private void getChildrenFor(Object[] parentElements) {
		for( int i = 0; i < parentElements.length; i++ ) {
			getChildrenFor(parentElements[i], true);
		}
	}
	
	// Get the children without the protection of a "loading..." situation
	private Object[] getChildrenFor(Object parentElement, boolean recurse) {
		// .... the actual work is done here...
		Object[] children = new Object[0];
		try {
			if (parentElement instanceof OpenShiftExpressConsoleContentCategory) {
				IUser user = ((OpenShiftExpressConsoleContentCategory) parentElement).getUser();
				children = new Object[] { user };
			}
			if (parentElement instanceof IUser) {
				children = ((IUser) parentElement).getApplications().toArray();
			}
			if (parentElement instanceof IApplication) {
				children = ((IApplication) parentElement).getEmbeddedCartridges().toArray();
			}
			
			if( recurse ) {
				getChildrenFor(children);
			}
		} catch (OpenShiftException e) {
			Logger.error("Unable to retrieve OpenShift Express information", e);
		}
		return children;
	}
	
	private void launchLoadingUserJob(final IUser user) {
		Job job = new Job("Loading OpenShift Express User information...") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading OpenShift Express information...", IProgressMonitor.UNKNOWN);
				monitor.worked(1);
				// Get the actual children, with the delay
				loadingUsers.add(user);
				getChildrenFor(user, true);
				
				loadedUsers.add(user);
				loadingUsers.remove(user);
				
				// refresh the parent object in the viewer when finished
				try {
					Thread.sleep(10000);
				} catch(InterruptedException ie) {
					
				}
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
	public Object[] getChildren(Object parentElement) {
		return getElements(parentElement);
	}

	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IUser) {
			return true;
		}
		if (element instanceof IApplication) {
			return true;
		}
		return false;
	}

}
