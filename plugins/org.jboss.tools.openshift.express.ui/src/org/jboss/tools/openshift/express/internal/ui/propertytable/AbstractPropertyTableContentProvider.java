/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.propertytable;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 * 
 */
public abstract class AbstractPropertyTableContentProvider implements ITreeContentProvider {

	@Override
	public abstract Object[] getElements(Object inputElement);

	@Override
	public Object[] getChildren(Object parentElement) {
		if (!hasChildren(parentElement)) {
			return new Object[0];
		}
		return ((IProperty) parentElement).getChildren();
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IProperty) {
			return ((IProperty) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof IProperty
				&& ((IProperty) element).hasChildren();
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
