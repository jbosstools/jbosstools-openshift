/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.console;

import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIMessages;

/**
 * Console helper that allows contributing actions to the console view when the
 * Tail console is visible. Added to the console via an extension point from
 * org.eclipse.ui.console.
 * 
 * @author Xavier Coulon
 * 
 */
@SuppressWarnings("restriction")
public class TailConsolePageParticipant implements IConsolePageParticipant {

	/** The standard Eclipse UI CloseConsoleAction.*/
	private CloseConsoleAction closeConsoleAction;

	public void init(IPageBookViewPage page, IConsole console) {
		this.closeConsoleAction = new CloseConsoleAction(console);
		this.closeConsoleAction.setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_REMOVE));
		this.closeConsoleAction.setToolTipText(ExpressUIMessages.CloseConsole);
		IActionBars bars = page.getSite().getActionBars();
		bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeConsoleAction);
	}

	public void dispose() {
		this.closeConsoleAction = null;
	}

	public void activated() {
	}

	public void deactivated() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

}
