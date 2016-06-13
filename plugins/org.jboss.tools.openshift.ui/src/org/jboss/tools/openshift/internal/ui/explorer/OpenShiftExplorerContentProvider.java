/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.explorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerContentProvider;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerContentProvider.LoadingStub;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.models.IConnectionWrapper;
import org.jboss.tools.openshift.internal.ui.models.IElementListener;
import org.jboss.tools.openshift.internal.ui.models.IOpenshiftUIElement;
import org.jboss.tools.openshift.internal.ui.models.IProjectWrapper;
import org.jboss.tools.openshift.internal.ui.models.IServiceWrapper;
import org.jboss.tools.openshift.internal.ui.models.LoadingState;
import org.jboss.tools.openshift.internal.ui.models.OpenshiftUIModel;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.route.IRoute;

/**
 * Contributes OpenShift 3 specific content to the OpenShift explorer view
 * 
 * @author jeff.cantrill
 */
public class OpenShiftExplorerContentProvider implements ITreeContentProvider {
	private static final List<String> TERMINATED_STATUS = Arrays.asList("Complete", "Failed", "Error", "Cancelled");

	private OpenshiftUIModel model;
	private IElementListener listener;
	private StructuredViewer viewer;
	private Map<Object, BaseExplorerContentProvider.LoadingStub> stubs = new HashMap<Object, BaseExplorerContentProvider.LoadingStub>();

	public OpenShiftExplorerContentProvider() {
		this(new OpenshiftUIModel());
	}

	public OpenShiftExplorerContentProvider(OpenshiftUIModel model) {
		this.model = model;
		listener = new IElementListener() {

			@Override
			public void elementChanged(IOpenshiftUIElement<?, ?> element) {
				if (element instanceof OpenshiftUIModel) {
					refreshViewer(ConnectionsRegistrySingleton.getInstance());
				} else {
					refreshViewer(element);
				}
				if (element.getWrapped() instanceof IRoute) {
					viewer.update(element.getParent(), null);
				}
			}
		};
		model.addListener(listener);
	}

	protected void refreshViewer(Object element) {
		if (viewer != null) {
			viewer.refresh(element);
		}
	}

	protected void asyncExec(Runnable r) {
		if (viewer != null) {
			Control control = viewer.getControl();
			if (control != null && !control.isDisposed()) {
				control.getDisplay().asyncExec(r);
			}
		}
	}

	@Override
	public void dispose() {
		model.removeListener(listener);
		model.dispose();
	}

	/**
	 * Called to obtain the root elements of the tree viewer, which should be
	 * Connections
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ConnectionsRegistry) {
			return model.getConnections().toArray();
		} else {
			return new Object[0];
		}
	}

	private void handleLoadingException(Object parentElement, Throwable e) {
		LoadingStub stub = makeStub(parentElement);
		stub.add(e);
		asyncExec(() -> refreshViewer(stub));
	}

	private LoadingStub makeStub(Object parentElement) {
		synchronized (stubs) {
			LoadingStub stub = stubs.get(parentElement);
			if (stub == null) {
				stub = new LoadingStub();
				stubs.put(parentElement, stub);
			}
			return stub;
		}
	}

	/**
	 * Called to obtain the children of any element in the tree viewer
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IConnectionWrapper) {
			IConnectionWrapper connection = (IConnectionWrapper) parentElement;
			if (connection.getState() == LoadingState.LOADED) {
				synchronized (stubs) {
					stubs.remove(parentElement);
				}
				Object[] result = connection.getResources().toArray();
				if (result == null || result.length == 0) {
					result = new Object[] { new NewProjectLinkNode((Connection) connection.getWrapped()) };
				}
				return result;
			} else {
				connection.load(e -> {
					handleLoadingException(parentElement, e);
				});
				return new Object[] { makeStub(parentElement) };
			}
		} else if (parentElement instanceof IProjectWrapper) {
			IProjectWrapper project = (IProjectWrapper) parentElement;

			if (project.getState() == LoadingState.LOADED) {
				synchronized (stubs) {
					stubs.remove(parentElement);
				}
				return project.getResourcesOfKind(ResourceKind.SERVICE).toArray();
			} else {
				project.load(e -> {
					handleLoadingException(parentElement, e);
				});
				return new Object[] { makeStub(parentElement) };
			}
		} else if (parentElement instanceof IServiceWrapper) {
			IServiceWrapper wrapper = (IServiceWrapper) parentElement;
			ArrayList<Object> result = new ArrayList<>();
			wrapper.getResourcesOfKind(ResourceKind.BUILD).stream()
					.filter(b -> !isTerminatedBuild((IBuild) b.getWrapped())).forEach(r -> result.add(r));
			wrapper.getResourcesOfKind(ResourceKind.POD).stream()
					.filter(p -> !ResourceUtils.isBuildPod((IPod) p.getWrapped())).forEach(r -> result.add(r));
			return result.toArray();
		} else if (parentElement instanceof LoadingStub) {
			return ((LoadingStub) parentElement).getChildren();
		}

		return new Object[0];
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// non-structured viewer would be a configuration problem. Crash!
		this.viewer = (StructuredViewer) viewer;
	}

	private boolean isTerminatedBuild(IBuild build) {
		String phase = build.getStatus();
		return TERMINATED_STATUS.contains(phase);
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IConnectionWrapper) {
			return ConnectionsRegistrySingleton.getInstance();
		}
		if (element instanceof IOpenshiftUIElement<?, ?>) {
			return ((IOpenshiftUIElement<?, ?>) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof LoadingStub) {
			return ((LoadingStub) element).hasChildren();
		}
		return element instanceof ConnectionsRegistry || element instanceof OpenshiftUIModel
				|| element instanceof IConnectionWrapper || element instanceof IProjectWrapper
				|| element instanceof IServiceWrapper;
	}

}
