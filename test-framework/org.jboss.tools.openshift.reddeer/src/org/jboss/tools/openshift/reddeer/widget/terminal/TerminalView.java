/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.widget.terminal;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.handler.LabelHandler;
import org.eclipse.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.eclipse.reddeer.workbench.impl.view.WorkbenchView;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tm.internal.terminal.textcanvas.TextCanvas;

/**
 * Represents Console view in Eclipse
 * 
 * @author jjankovi, mlabuda@redhat.com
 *
 */
public class TerminalView extends WorkbenchView {
	/**
	 * Constructs the view with "Console".
	 */
	public TerminalView() {
		super("Terminal");
	}

	/**
	 * Gets text from terminal.
	 * 
	 * @return terminal text, if there is a terminal, null otherwise
	 */
	public String getTerminalText() {
		activate();
		WidgetIsFound widgetIsFound = new WidgetIsFound(TextCanvas.class, cTabItem.getControl());
		new WaitUntil(widgetIsFound, TimePeriod.DEFAULT, false);
		// Check whether there is a console to display or not
		if (widgetIsFound.getResult() == null) {
			return null;
		}
		// wait for text to appear
		new WaitWhile(new TerminalHasText(""), TimePeriod.SHORT, false);
		return new DefaultTextCanvas(cTabItem).getText();
	}

	/**
	 * Toggles the button indicating if the view should be activated on standard
	 * output change.
	 *
	 * @param toggle the toggle
	 */
	public void toggleShowConsoleOnStandardOutChange(boolean toggle) {
		activate();
		new DefaultToolItem(cTabItem.getFolder(), "Show Console When Standard Out Changes").toggle(toggle);
	}

	/**
	 * Returns true if console has launch.
	 *
	 * @return true, if successful
	 */
	public boolean terminalHasLaunch() {
		activate();
		return new WidgetIsFound(TextCanvas.class, cTabItem.getControl()).test();
	}

	/**
	 * Returns true when terminal is terminated.
	 *
	 * @return true, if successful
	 */
	public boolean terminalIsTerminated() {
		String consoleLabel = getTerminalLabel();
		return consoleLabel != null && consoleLabel.contains("<Closed>");
	}

	/**
	 * 
	 * This is not exactly a condition for checking if the console contains text.
	 * For this purpose use org.eclipse.reddeer.eclipse.condition.ConsoleHasText
	 *
	 */
	private class TerminalHasText extends AbstractWaitCondition {
		private String terminalText;

		public TerminalHasText(String terminalText) {
			this.terminalText = terminalText;
		}

		@Override
		public boolean test() {
			WidgetIsFound widgetIsFound = new WidgetIsFound(org.eclipse.swt.custom.StyledText.class,
					cTabItem.getControl());
			widgetIsFound.test();
			org.eclipse.swt.widgets.Widget swtWidget = widgetIsFound.getResult();
			return swtWidget != null
					&& terminalText.equals(TextCanvasHandler.getInstance().getText((TextCanvas) swtWidget));
		}

		@Override
		public String description() {
			return "terminal text is \"" + this.terminalText + "\"";
		}

	}

	/**
	 * Returns console label title or null when console has no label.
	 *
	 * @return the console label
	 */
	public String getTerminalLabel() {
		activate();
		WidgetIsFound widgetIsFound = new WidgetIsFound(org.eclipse.swt.widgets.Label.class, cTabItem.getControl());
		widgetIsFound.test();
		org.eclipse.swt.widgets.Widget swtWidget = widgetIsFound.getResult();
		return (swtWidget == null) ? null
				: LabelHandler.getInstance().getText((org.eclipse.swt.widgets.Label) swtWidget);
	}

	/**
	 * Returns a control registered via adapters. This is usually StyledText or
	 * Canvas.
	 * 
	 * @return registered control
	 */
	@Override
	protected Control getRegisteredControl() {
		activate();
		WidgetIsFound widgetIsFound = new WidgetIsFound(TextCanvas.class, cTabItem.getControl());
		new WaitUntil(widgetIsFound, TimePeriod.SHORT, false);
		// Check whether there is a console to display or not
		if (widgetIsFound.getResult() == null) {
			log.debug("There is no terminal in terminal view.");
			return null;
		}
		return (TextCanvas) widgetIsFound.getResult();
	}

}
