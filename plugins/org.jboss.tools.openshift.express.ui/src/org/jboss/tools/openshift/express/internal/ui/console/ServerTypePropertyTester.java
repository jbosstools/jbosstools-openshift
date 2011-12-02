package org.jboss.tools.openshift.express.internal.ui.console;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;

/**
 * Property tester used to compare the given Server's serverTypeId with a given value. 
 * This property tester is configured in the plugin.xml file and used to display extra menus item only when the server is on OpenShift one.
 * 
 * @author Xavier Coulon
 *
 */
public class ServerTypePropertyTester extends PropertyTester {

	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if(receiver instanceof IServer) {
			return ExpressServerUtils.isOpenShiftRuntime((IServer) receiver);
		}
		
		return false;
	}

}
