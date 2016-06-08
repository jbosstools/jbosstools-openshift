package org.jboss.tools.openshift.internal.ui.models;

public interface IExceptionHandler {
	public static IExceptionHandler NULL_HANDLER= new IExceptionHandler() {

		@Override
		public void handleException(Throwable e) {
			// do nothing
		}
		
	};
	
	void handleException(Throwable e);
}
