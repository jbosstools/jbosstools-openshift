/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 * 
 */
public class ImportOpenShiftApplicationWizard extends OpenShiftApplicationWizard {

	/**
	 * Constructor invoked via File->Import
	 */
	public ImportOpenShiftApplicationWizard() {
		super(ConnectionsRegistrySingleton.getInstance().getRecentConnection(ExpressConnection.class), null, null, null, true, 
				"Import OpenShift Application");
	}

	/**
	 * Constructor invoked via Server adapter wizard "Import application"
	 */
	public ImportOpenShiftApplicationWizard(ExpressConnection connection, IApplication application) {
		super(connection, application.getDomain(), application, null, true, "Import OpenShift Application");
	}

	/**
	 * Constructor invoked via OpenShift Explorer context menu
	 */
	public ImportOpenShiftApplicationWizard(IApplication application) {
		super(ExpressConnectionUtils.getByResource(application, ConnectionsRegistrySingleton.getInstance()),
				application.getDomain(), application, null, true, "Import OpenShift Application");
	}

	@Override
	public Object getContext() {
		return null;
	}
}
