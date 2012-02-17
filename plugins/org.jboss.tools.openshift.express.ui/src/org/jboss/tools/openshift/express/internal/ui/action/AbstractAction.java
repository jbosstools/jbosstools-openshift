package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.navigator.CommonViewer;

public abstract class AbstractAction extends Action implements ISelectionChangedListener {

	/** The current selection in the view. */
	protected ISelection selection = null;
	
	protected StructuredViewer viewer;

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
			this.viewer = (CommonViewer) source;
			this.selection = ((CommonViewer) source).getSelection();
		}
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;
	
	}

	public void setViewer(StructuredViewer viewer) {
		this.viewer = viewer;
	}

}