/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.console;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.jboss.ide.eclipse.as.ui.UIUtil;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class ConsoleUtils {

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
	 * Deregister the given listener as a console listener.
	 * 
	 * @param consoleListener
	 */
	public static void deregisterConsoleListener(IConsoleListener consoleListener) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager consoleManager = plugin.getConsoleManager();
		consoleManager.removeConsoleListener(consoleListener);
	}
	
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
	public static void displayConsoleView(final IConsole console) {
		UIUtils.ensureDisplayExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchPart part = null;
				try {
					part = UIUtil.bringViewToFront(IConsoleConstants.ID_CONSOLE_VIEW);
					if (part == null) {
						OpenShiftCommonUIActivator.getDefault().getLogger().logWarning("Could not open console, " + IConsoleConstants.ID_CONSOLE_VIEW + " was not found");
						return;
					}
					final IConsoleView view = (IConsoleView) part.getAdapter(IConsoleView.class);
					if (view == null) {
						return;
					}
					view.display(console);
				} catch (PartInitException e) {
					OpenShiftCommonUIActivator.getDefault().getLogger().logWarning("Could not open console view", e);
				}

				
			}
			
		});
	}
}
