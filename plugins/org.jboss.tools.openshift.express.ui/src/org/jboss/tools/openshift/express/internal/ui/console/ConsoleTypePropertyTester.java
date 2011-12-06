/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.console;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.console.MessageConsole;
import static org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils.*;

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
