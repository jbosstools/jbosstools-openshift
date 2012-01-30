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
package org.jboss.tools.openshift.express.internal.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.ui.propertytable.IProperty;

/**
 * @author Andre Dietisheim
 */
public class CopyPropertyCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		if (selection instanceof StructuredSelection) {
			Object selectedElement = ((IStructuredSelection) selection).getFirstElement();
			if (selectedElement instanceof IProperty) {
				IProperty selectedProperty = (IProperty) selectedElement;
				copyToClipboard(selectedProperty, event);
			}

		}
		return null;
	}

	private void copyToClipboard(IProperty selectedProperty, ExecutionEvent event) {
		TextTransfer textTransfer = TextTransfer.getInstance();
		Clipboard clipboard = new Clipboard(HandlerUtil.getActiveShell(event).getDisplay());
		clipboard.setContents(
				new Object[] { selectedProperty.getValue() },
				new Transfer[] { textTransfer });
	}

}
