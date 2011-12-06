/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.console;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Console helper that allows contributing actions to the console view when the
 * Tail console is visible. Added to the console via an extension point from
 * org.eclipse.ui.console.
 * 
 * @author Xavier Coulon
 * 
 */
public class TailConsolePageParticipant implements IConsolePageParticipant {

	/** The standard Eclipse UI CloseConsoleAction.*/
	private CloseConsoleAction closeConsoleAction;

	public void init(IPageBookViewPage page, IConsole console) {
		this.closeConsoleAction = new CloseConsoleAction(console);
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
	public Object getAdapter(Class adapter) {
		return null;
	}

}
