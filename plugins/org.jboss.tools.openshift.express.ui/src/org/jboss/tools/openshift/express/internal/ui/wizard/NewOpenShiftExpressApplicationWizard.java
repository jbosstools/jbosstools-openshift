/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard;

import com.openshift.express.client.IUser;

/**
 * @author Xavier Coulon
 *
 */
public class NewOpenShiftExpressApplicationWizard extends OpenShiftExpressApplicationWizard {

	/**
	 * Constructor
	 */
	public NewOpenShiftExpressApplicationWizard() {
		super("New OpenShift Application");
	}

	public NewOpenShiftExpressApplicationWizard(IUser user) {
		super(user, null, null, "New OpenShift Application");
	}
}
