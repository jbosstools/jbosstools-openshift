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
package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * @author Xavier Coulon
 */
public abstract class AbstractAction extends Action implements ISelectionChangedListener {

	/** The current selection in the view. */
	protected ISelection selection = null;
	
	protected StructuredViewer viewer;
	
	private boolean enableForSingleElement = false;

	public AbstractAction(String text) {
		super(text);
	}
	
	public AbstractAction(String text, boolean enableForSingleElement) {
		super(text);
		this.enableForSingleElement = enableForSingleElement;
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
			if(enableForSingleElement){
				if(selection instanceof ITreeSelection && ((ITreeSelection) selection).size() == 1){
					setEnabled(true);
				}else{
					setEnabled(false);
				}
			}
		}
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;
	
	}

	public void setViewer(StructuredViewer viewer) {
		this.viewer = viewer;
	}

}