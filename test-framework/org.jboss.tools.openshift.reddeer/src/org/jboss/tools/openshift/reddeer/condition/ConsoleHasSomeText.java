/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.eclipse.ui.console.ConsoleView;

/**
 * Condition notifies about not empty console.
 * 
 * TODO replace this condition by RedDeer implementation of ConsoleHasText, once
 * RedDeer implementation is updated
 * 
 * @author mlabuda@redhat.com
 *
 */
public class ConsoleHasSomeText extends AbstractWaitCondition{

	private ConsoleView consoleView;
	
	public ConsoleHasSomeText() {
		consoleView = new ConsoleView();
		consoleView.open();
	}
	
	@Override
	public boolean test() {
		try {
			return !consoleView.getConsoleText().isEmpty();
		} catch (RedDeerException ex) {
			return false;
		}
	}

	@Override
	public String description() {
		return "console contains text";
	}

	
}
