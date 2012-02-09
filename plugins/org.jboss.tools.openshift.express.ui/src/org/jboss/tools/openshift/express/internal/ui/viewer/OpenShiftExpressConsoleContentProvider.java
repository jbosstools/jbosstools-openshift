package org.jboss.tools.openshift.express.internal.ui.viewer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

public class OpenShiftExpressConsoleContentProvider implements ITreeContentProvider {

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(final Object parentElement) {
		if(parentElement instanceof IWorkspaceRoot) {
			return UserModel.getDefault().getUsers();
		}
		if( parentElement instanceof UserModel ) {
			return ((UserModel)parentElement).getUsers();
		}
		
		final ArrayBlockingQueue<Object[]> queue = new ArrayBlockingQueue<Object[]>(1);
		try {
			Job job = new Job("Loading OpenShift Express User information...") {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// TODO Auto-generated method stub
					monitor.beginTask("Loading OpenShift Express information...", IProgressMonitor.UNKNOWN);
					monitor.worked(1);
					try {
						if (parentElement instanceof OpenShiftExpressConsoleContentCategory) {
							queue.offer(new Object[] { ((OpenShiftExpressConsoleContentCategory) parentElement).getUser() });
						}
						if (parentElement instanceof IUser) {
							queue.offer(((IUser) parentElement).getApplications().toArray());
						}
						if (parentElement instanceof IApplication) {
							queue.offer(((IApplication) parentElement).getEmbeddedCartridges().toArray());
						}
						// .... the actual work is done here...
					} catch (OpenShiftException e) {
						Logger.error("Unable to retrieve OpenShift Express information", e);
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.LONG);
			job.schedule();
			//job.join();
			return queue.poll(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			Logger.error("Failed to load OpenShit Express account information", e);
		}

		return null;
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
