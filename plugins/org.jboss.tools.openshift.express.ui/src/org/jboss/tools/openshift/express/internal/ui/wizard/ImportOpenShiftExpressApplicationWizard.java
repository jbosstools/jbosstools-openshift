/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.eclipse.core.resources.IProject;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;

/**
 * @author Xavier Coulon
 *
 */
public class ImportOpenShiftExpressApplicationWizard extends OpenShiftExpressApplicationWizard {

	/**
	 * Constructor
	 */
	public ImportOpenShiftExpressApplicationWizard() {
		super("Import OpenShift Application");
	}

	/**
	 * Constructor
	 * @param user
	 * @param project
	 * @param application
	 */
	public ImportOpenShiftExpressApplicationWizard(IUser user, IProject project, IApplication application) {
		super(user, project, application, "Import OpenShift Application");
	}
}
