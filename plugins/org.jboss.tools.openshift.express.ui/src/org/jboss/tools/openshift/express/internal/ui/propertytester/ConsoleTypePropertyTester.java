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
package org.jboss.tools.openshift.express.internal.ui.propertytester;

import static org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils.CONSOLE_TYPE_KEY;
import static org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils.CONSOLE_TYPE_VALUE;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.console.MessageConsole;

/**
 * Property tester used to verify that the given instance of
 * <code>org.eclipse.ui.console.MessageConsole</code> is an OpenShift Message
 * Console (that is, it should contain a specific attribute set a its creation).
 * 
 * @author Xavier Coulon
 * 
 */
public class ConsoleTypePropertyTester extends PropertyTester {

	/**
	 * Verifies that the given receiver, a <code>MessageConsole</code> contains
	 * an attribute name <code>ConsoleUtils.CONSOLE_TYPE_KEY</code> with a value
	 * set to <code>ConsoleUtils.CONSOLE_TYPE_VALUE</code>. Using the console
	 * attributes avoids the need to create a subtype of
	 * <code>MessageCode</code>.
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		MessageConsole console = (MessageConsole) receiver;
		return (CONSOLE_TYPE_VALUE.equals(console.getAttribute(CONSOLE_TYPE_KEY)));
	}

}
