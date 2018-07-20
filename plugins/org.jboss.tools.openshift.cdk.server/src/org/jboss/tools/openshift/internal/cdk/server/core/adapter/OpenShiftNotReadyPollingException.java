package org.jboss.tools.openshift.internal.cdk.server.core.adapter;

import org.eclipse.core.runtime.IStatus;
import org.jboss.ide.eclipse.as.core.server.IServerStatePoller.PollingException;

public class OpenShiftNotReadyPollingException extends PollingException {
	public static final int OPENSHIFT_UNREACHABLE_CODE = 10001;
	private transient IStatus stat;

	public OpenShiftNotReadyPollingException(IStatus status) {
		super(status.getMessage());
		this.stat = status;
	}

	public IStatus getStatus() {
		return stat;
	}
}