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

/**
 * @author Red Hat Developers
 *
 */

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;

/**
 * Returns true if a terminal contains text.
 * 
 * @author jkopriva@redhat.com
 * 
 */
public class TerminalContainsText extends AbstractWaitCondition {

	private String text;

	/**
	 * Construct the condition with expected text.
	 * 
	 * @param text
	 */
	public TerminalContainsText(String text) {
		this.text = text;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.reddeer.common.condition.WaitCondition#test()
	 */
	@Override
	public boolean test() {
		return getTerminalText().contains(this.text);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.reddeer.common.condition.AbstractWaitCondition#description()
	 */
	@Override
	public String description() {
		return "Terminal does not contain " + text;
	}

	private static String getTerminalText() {
		TerminalView consoleView = new TerminalView();
		consoleView.open();
		return consoleView.getTerminalText();
	}
}
