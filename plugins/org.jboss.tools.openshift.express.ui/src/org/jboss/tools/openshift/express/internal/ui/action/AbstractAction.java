package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.navigator.CommonViewer;

public abstract class AbstractAction extends Action implements ISelectionChangedListener {

	/** The current selection in the view. */
	protected ISelection selection = null;

	public AbstractAction(String text) {
		super(text);
	}

	public AbstractAction(String text, ImageDescriptor image) {
		super(text, image);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Object source = event.getSource();
		if (source instanceof CommonViewer) {
			this.selection = ((CommonViewer) source).getSelection();
		}
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;
	
	}

}