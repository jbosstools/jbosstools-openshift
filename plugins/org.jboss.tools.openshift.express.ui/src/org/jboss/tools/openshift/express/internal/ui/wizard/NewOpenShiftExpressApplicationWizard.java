/**
 * 
 */
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

/**
 * @author Xavier Coulon
 *
 */
public class NewOpenShiftExpressApplicationWizard extends OpenShiftExpressApplicationWizard {

	/**
	 * Constructor
	 */
	public NewOpenShiftExpressApplicationWizard() {
		super(false, "New OpenShift Application");
	}

	public NewOpenShiftExpressApplicationWizard(UserDelegate user) {
		super(user, null, null, false, "New OpenShift Application");
	}
}
