/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.ui.internal.dialogs;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.internal.ImageResource;

public class ChooseServerDialog extends Dialog {
	private ArrayList<IServer> valid;
	private IServer selected;
	
	public ChooseServerDialog(Shell parentShell, ArrayList<IServer> valid) {
		super(parentShell);
		this.valid = valid;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		final TreeViewer tv = new TreeViewer(container);
		tv.setContentProvider(new ITreeContentProvider() {
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			public void dispose() {
			}

			public boolean hasChildren(Object element) {
				return false;
			}

			public Object getParent(Object element) {
				return null;
			}

			public Object[] getElements(Object inputElement) {
				return (IServer[]) valid.toArray(new IServer[valid.size()]);
			}

			public Object[] getChildren(Object parentElement) {
				return null;
			}
		});
		tv.setLabelProvider(new LabelProvider(){
			public Image getImage(Object element) {
				if( element instanceof IServer ) 
					return ImageResource.getImage(((IServer)element).getServerType().getId());
				return super.getImage(element);
			}
			public String getText(Object element) {
				if( element instanceof IServer ) 
					return ((IServer)element).getName();
				return super.getText(element);
			}
		});
		
		
		
		tv.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selected = null;
				ISelection sel = tv.getSelection();
				if( sel instanceof IStructuredSelection) {
					Object first = ((IStructuredSelection)sel).getFirstElement();
					if( first instanceof IServer) {
						selected = (IServer)first;
					}
				}
				getButton( IDialogConstants.OK_ID).setEnabled(selected != null);
			}
		});
		tv.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		tv.setInput(PlatformUI.getWorkbench());
		return container;
	}

	protected Control createButtonBar(Composite parent) {
		Control c = super.createButtonBar(parent);
		getButton( IDialogConstants.OK_ID).setEnabled(false);
		return c;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select a CDK Server");
	}

	public IServer getServer() {
		return selected;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}
}