package org.jboss.tools.openshift.internal.ui.models2;

import com.openshift.restclient.OpenShiftException;

public interface IExceptionHandler {
	void handleException(OpenShiftException e);
}
