/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;

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
		super(ConnectionsModelSingleton.getInstance().getRecentConnection(), null, null, null, true, true,
				"Import OpenShift Application");
	}

	/**
	 * Constructor invoked via Server adapter wizard "Import application"
	 */
	public ImportOpenShiftApplicationWizard(Connection connection, IApplication application) {
		super(connection, application.getDomain(), application, null, true, false, "Import OpenShift Application");
	}

	/**
	 * Constructor invoked via OpenShift Explorer context menu
	 */
	public ImportOpenShiftApplicationWizard(IApplication application, boolean showCredentialsPage) {
		super(ConnectionsModelSingleton.getInstance().getConnectionByResource(application),
				application.getDomain(), application, null, true, showCredentialsPage, "Import OpenShift Application");
	}
}
