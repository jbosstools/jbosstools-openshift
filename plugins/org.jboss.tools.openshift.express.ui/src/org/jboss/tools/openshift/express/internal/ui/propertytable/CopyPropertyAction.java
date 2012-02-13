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
package org.jboss.tools.openshift.express.internal.ui.propertytable;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * @author Andre Dietisheim
 */
public class CopyPropertyAction extends SelectionProviderAction {

	private ISelectionProvider selectionProvider;

	public CopyPropertyAction(ISelectionProvider provider) {
		super(provider, "Copy");
		initAction();
		this.selectionProvider = provider;
	}

	protected void initAction() {
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
//		setActionDefinitionId(ActionFactory.COPY.getCommandId());
//		setAccelerator(SWT.CTRL | 'C');
	}

	@Override
	public void run() {
		ISelection selection = selectionProvider.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.getFirstElement() instanceof IProperty) {
				IProperty property = (IProperty) structuredSelection.getFirstElement();
				Clipboard clipboard = new Clipboard(Display.getDefault());
				clipboard.setContents(
						new Object[] { property.getValue() },
						new Transfer[] { TextTransfer.getInstance() });
			}
		}
	}
}