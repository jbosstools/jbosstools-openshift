package org.jboss.tools.openshift.internal.ui.explorer;

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
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
//import org.osgi.resource.Resource;

import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * 
 */
public class OpenShiftExplorerContentProvider implements ITreeContentProvider {

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
	 * Called to obtain the root elements of the tree viewer, ie, the Users
	 */
	@Override
	//FIXME leaving here for idea on impl
	public Object[] getElements(final Object parentElement) {
		// A refresh on the whole model... clear our cache
		loadedElements.clear();
		loadingElements.clear();
		errors.clear();
		if (parentElement instanceof IWorkspaceRoot) {
			return ConnectionsRegistrySingleton.getInstance().getAll();
		} else if (parentElement instanceof ConnectionsRegistry) {
			return ((ConnectionsRegistry) parentElement).getAll();
//		} else if (parentElement instanceof ExpressConnection) {
//			List<IDomain> domains = ((ExpressConnection) parentElement).getDomains();
//			return domains.toArray(new IDomain[domains.size()]);
//		} else if (parentElement instanceof com.openshift.kube.Client) {
//			List<Project> openshiftProjects = ((com.openshift.kube.Client) parentElement).list(ResourceKind.Project);
//			openshiftProjects.toArray(new Project[openshiftProjects.size()]);
//		} else if (parentElement instanceof Project) {
//			Project p = (Project) parentElement;
//			List<Resource> resources = new ArrayList<Resource>(p.getResources(ResourceKind.DeploymentConfig));
//			return resources.toArray();
		}
		return new Object[0];
	}

	/**
	 * Called to obtain the children of any element in the tree viewer, ie, from
	 * a user or an application
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
//		if (parentElement instanceof com.openshift.kube.Client) {
//			return loadChildren(parentElement);
//		}
//		if (parentElement instanceof ExpressConnection) {
//			ExpressConnection connection = (ExpressConnection) parentElement;
//			if (!connection.isConnected()
//					&& !connection.canPromptForPassword()) {
//				return new Object[] { new NotConnectedUserStub() };
//			}
//			return loadChildren(parentElement);
//		} else if (parentElement instanceof IDomain) {
//			return loadChildren(parentElement);
//		} else if (parentElement instanceof IApplication) {
//			return loadChildren(parentElement);
//		}
		return getChildrenFor(parentElement);
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
//			if (parentElement instanceof OpenShiftExplorerContentCategory) {
//				ExpressConnection user = ((OpenShiftExplorerContentCategory) parentElement).getUser();
//				children = new Object[] { user };
//			} else if (parentElement instanceof com.openshift.kube.Client) {
//				children = ((com.openshift.kube.Client) parentElement).list(ResourceKind.Project).toArray();
//			} else if (parentElement instanceof Project) {
//				Project p = (Project) parentElement;
//				children = p.getResources(ResourceKind.DeploymentConfig).toArray();
//			} else if (parentElement instanceof ResourceGrouping) {
//				children = ((ResourceGrouping) parentElement).getResources();
//			} else if (parentElement instanceof ExpressConnection) {
//				final ExpressConnection connection = (ExpressConnection) parentElement;
//				children = connection.getDomains().toArray();
//			} else if (parentElement instanceof IDomain) {
//				final IDomain domain = (IDomain) parentElement;
//				children = domain.getApplications().toArray();
//			} else if (parentElement instanceof IApplication) {
//				children = ((IApplication) parentElement).getEmbeddedCartridges().toArray();
//			}
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
		return false;
//		return element instanceof IConnection
//				|| element instanceof Project
//				|| element instanceof ResourceGrouping
//				|| element instanceof IDomain
//				|| element instanceof IApplication;
	}

	public static class LoadingStub {
	}

	public static class NotConnectedUserStub {
	}

//	static class ResourceGrouping {
//		private List<Resource> resources;
//		private String title;
//		private ResourceKind kind;
//
//		ResourceGrouping(String title, List<Resource> resources, ResourceKind kind) {
//			this.title = title;
//			this.resources = resources;
//			this.kind = kind;
//		}
//
//		public ResourceKind getKind() {
//			return kind;
//		}
//
//		public Object[] getResources() {
//			return resources.toArray();
//		}
//
//		@Override
//		public String toString() {
//			return title;
//		}
//	}

}
