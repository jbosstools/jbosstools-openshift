package org.jboss.tools.openshift.internal.ui.property.tabbed;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.openshift.internal.ui.models2.IElementListener;
import org.jboss.tools.openshift.internal.ui.models2.IOpenshiftUIElement;
import org.jboss.tools.openshift.internal.ui.models2.ResourceContainer;

public class ResourceContainerContentProvider implements IStructuredContentProvider {
	private String resourceKind;

	public ResourceContainerContentProvider(String kind) {
		this.resourceKind= kind;
	}
	
	private StructuredViewer viewer;
	private IElementListener listener= new IElementListener() {
		
		@Override
		public void elementChanged(IOpenshiftUIElement<?> element) {
			viewer.refresh();
		}
	};

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer= (StructuredViewer) viewer;
		if (oldInput instanceof IOpenshiftUIElement<?>) {
			((IOpenshiftUIElement<?>) oldInput).getRoot().removeListener(listener);
		}
		if (newInput instanceof IOpenshiftUIElement<?>) {
			((IOpenshiftUIElement<?>) newInput).getRoot().addListener(listener);
		}
	}
	
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ResourceContainer<?, ?>) {
			return ((ResourceContainer<?, ?>) inputElement).getResourcesOfKind(resourceKind).toArray();
		}
		return null;
	}

}
