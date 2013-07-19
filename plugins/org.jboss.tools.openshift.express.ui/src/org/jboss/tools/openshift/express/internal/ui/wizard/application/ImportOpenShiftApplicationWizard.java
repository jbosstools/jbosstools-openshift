/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 *
 */
public class ImportOpenShiftApplicationWizard extends OpenShiftApplicationWizard {

	/**
	 * Constructor
	 */
	public ImportOpenShiftApplicationWizard() {
		super(true, "Import OpenShift Application");
	}

	/**
	 * Constructor
	 * @param connection
	 * @param project
	 * @param application
	 */
	public ImportOpenShiftApplicationWizard(Connection connection, IProject project, IApplication application) {
		super(connection, project, application, true, false, "Import OpenShift Application");
	}
}
