package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.resource.ImageDescriptor;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftSshSessionFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftSSHOperationException;

public abstract class AbstractSSHAction extends AbstractAction {

	public AbstractSSHAction(String text) {
		super(text);
	}

	public AbstractSSHAction(String text, boolean enableForSingleElement) {
		super(text, enableForSingleElement);
	}

	public AbstractSSHAction(String text, ImageDescriptor image) {
		super(text, image);
	}

	/**
	 * @param monitor
	 * @throws OpenShiftSSHOperationException
	 * @throws JSchException
	 */
	protected boolean verifyApplicationSSHSession(final IApplication application) throws OpenShiftSSHOperationException {
		final boolean hasAlreadySSHSession = application.hasSSHSession();
		if (!hasAlreadySSHSession) {
			Logger.debug("Opening a new SSH Session for application '" + application.getName() + "'");
			final Session session = OpenShiftSshSessionFactory.getInstance().createSession(
					application);
			application.setSSHSession(session);
		}
		// now, check if the session is valid (ie, not null and still connected)
		return application.hasSSHSession();
	}

}