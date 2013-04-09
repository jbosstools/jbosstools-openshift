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
public abstract class AbstractOpenShiftAction extends Action implements ISelectionChangedListener {

	/** The current selection in the view. */
	private ISelection selection;
	
	protected StructuredViewer viewer;
	
	private boolean enableForSingleElement = false;

	public AbstractOpenShiftAction(String text) {
		super(text);
	}
	
	public AbstractOpenShiftAction(String text, boolean enableForSingleElement) {
		super(text);
		this.enableForSingleElement = enableForSingleElement;
	}

	public AbstractOpenShiftAction(String text, ImageDescriptor image) {
		super(text, image);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Object source = event.getSource();
		if (source instanceof CommonViewer) {
			this.viewer = (CommonViewer) source;
			this.selection = ((CommonViewer) source).getSelection();
			setEnablement(selection);
		}
	}

	protected void setEnablement(ISelection selection) {
		if (enableForSingleElement) {
			if (selection instanceof ITreeSelection
					&& ((ITreeSelection) selection).size() == 1) {
				setEnabled(true);
			} else {
				setEnabled(false);
			}
		}
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;
	}
	
	protected ISelection getSelection() {
		return selection;
	}
		
	public void setViewer(StructuredViewer viewer) {
		this.viewer = viewer;
	}
	
	public void validate() {
	}
}