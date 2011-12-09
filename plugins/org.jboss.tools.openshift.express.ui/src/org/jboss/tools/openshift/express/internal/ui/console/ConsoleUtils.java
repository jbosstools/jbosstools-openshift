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

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

/**
 * A utility class to manager the message consoles creations and retrivals
 * 
 * @author Xavier Coulon
 * 
 */
public class ConsoleUtils {

	/**
	 * Constant key set into the created message console attributes to mark the
	 * given console as an 'openshift' one.
	 */
	public static final String CONSOLE_TYPE_KEY = "ConsoleType";

	/**
	 * Constant value set into the created message console attributes to mark
	 * the given console as an 'openshift' one.
	 */
	public static final String CONSOLE_TYPE_VALUE = "OpenShiftTailConsole";

	/**
	 * Registers the given listener as a console listener.
	 * 
	 * @param consoleListener
	 */
	public static void registerConsoleListener(IConsoleListener consoleListener) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager consoleManager = plugin.getConsoleManager();
		consoleManager.addConsoleListener(consoleListener);
	}

	/**
	 * Retrieve the message console given its name. If no console exists yet, a
	 * new one is created with a specifi attribute to mark it as an 'openshift'
	 * console. This attribute (or marker) is use later on by the
	 * ConsoleTypePropertyTester to add a 'remove' button on the console in the
	 * consoles view.
	 * 
	 * @param name
	 *            the name of the console to find
	 * @return the message console (found or created)
	 */
	public static MessageConsole findMessageConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager consoleManager = plugin.getConsoleManager();
		IConsole[] existing = consoleManager.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		// no console found, so create a new one
		MessageConsole console = new MessageConsole(name, null);
		console.setAttribute(CONSOLE_TYPE_KEY, CONSOLE_TYPE_VALUE);
		consoleManager.addConsoles(new IConsole[] { console });
		return console;
	}

	/**
	 * Displays the given console in the consoles view which becomes visible if
	 * it was not the case before.
	 * 
	 * @param console the console to display
	 */
	public static void displayConsoleView(IConsole console) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IWorkbenchPart part = page.findView(IConsoleConstants.ID_CONSOLE_VIEW);
				if (part == null) {
					try {
						part = page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
					} catch (PartInitException e) {
						Logger.warn("Could not open console view", e);
					}
				}
				if (part != null) {
					page.activate(part);
					IConsoleView view = (IConsoleView) part.getAdapter(IConsoleView.class);
					if (view != null) {
						view.setFocus();
						view.display(console);
					}
				}
			}
		}
	}

}
