/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

import com.openshift.express.client.IApplication;

/**
 * @author Xavier Coulon
 *
 */
public class ImportOpenShiftExpressApplicationWizard extends OpenShiftExpressApplicationWizard {

	/**
	 * Constructor
	 */
	public ImportOpenShiftExpressApplicationWizard() {
		super(true, "Import OpenShift Express Application");
	}

	/**
	 * Constructor
	 * @param user
	 * @param project
	 * @param application
	 */
	public ImportOpenShiftExpressApplicationWizard(UserDelegate user, IProject project, IApplication application) {
		super(user, project, application, true, "Import OpenShift Express Application");
	}
}
