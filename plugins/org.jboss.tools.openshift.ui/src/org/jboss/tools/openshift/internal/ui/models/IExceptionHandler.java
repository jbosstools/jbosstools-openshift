package org.jboss.tools.openshift.internal.ui.models;

import com.openshift.restclient.OpenShiftException;

public interface IExceptionHandler {
	public static IExceptionHandler NULL_HANDLER= new IExceptionHandler() {

		@Override
		public void handleException(OpenShiftException e) {
			// do nothing
		}
		
	};
	
	void handleException(OpenShiftException e);
}
