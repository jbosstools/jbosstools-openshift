package org.jboss.tools.openshift.internal.ui.models2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

public class ConnectionWrapper extends AbstractOpenshiftUIElement<OpenshiftUIModel> {
	private IOpenShiftConnection connection;
	private AtomicReference<LoadingState> state = new AtomicReference<LoadingState>(LoadingState.INIT);
	private Set<ProjectWrapper> projects = new HashSet<>();

	public ConnectionWrapper(OpenshiftUIModel parent, IOpenShiftConnection wrapped) {
		super(parent);
		this.connection = wrapped;
	}

	public IOpenShiftConnection getConnection() {
		return connection;
	}

	public Collection<ProjectWrapper> getProjects() {
		synchronized (projects) {
			return new ArrayList<ProjectWrapper>(projects);
		}
	}

	void initWith(List<IResource> resources) {
		synchronized (projects) {
			resources.forEach(project -> {
				projects.add(new ProjectWrapper(this, (IProject) project));
			});
			state.set(LoadingState.LOADED);
		}
	}

	public boolean load(IExceptionHandler handler) {
		if (state.compareAndSet(LoadingState.INIT, LoadingState.LOADING)) {
			getRoot().startLoadJob(this, handler);
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> T getAdapter(Class<T> adapter) {
		if (adapter.isInstance(connection)) {
			return (T) connection;
		}
		return super.getAdapter(adapter);
	}

}
