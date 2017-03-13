package org.jboss.tools.openshift.cdk.server.ui.internal.detection;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.cdk.server.core.internal.detection.CDK3RuntimeDetector;
import org.jboss.tools.runtime.core.model.IRuntimeDetectionResolution;
import org.jboss.tools.runtime.core.model.IRuntimeDetectionResolutionProvider;
import org.jboss.tools.runtime.core.model.RuntimeDefinition;
import org.jboss.tools.runtime.core.model.RuntimeDetectionProblem;

public class MissingMinishiftResolutionProvider implements IRuntimeDetectionResolutionProvider {

	public static final int MISSING_MINISHIFT_PROBLEM_ID = 2008;
	
	public MissingMinishiftResolutionProvider() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IRuntimeDetectionResolution[] getResolutions(RuntimeDetectionProblem problem, RuntimeDefinition def) {
		if( problem.getCode() == MISSING_MINISHIFT_PROBLEM_ID) {
			return new IRuntimeDetectionResolution[] { new MissingMinishiftResolution() };
		}
		return null;
	}
	public static class MissingMinishiftResolution implements IRuntimeDetectionResolution {

		@Override
		public String getLabel() {
			return "Set minishift binary location.";
		}

		@Override
		public void run(RuntimeDetectionProblem problem, RuntimeDefinition definition) {
			File found = getFile(null, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			if( found != null ) {
				definition.setProperty(CDK3RuntimeDetector.OVERRIDE_MINISHIFT_LOCATION, found.getAbsolutePath());
			}
		}
		
		protected static File getFile(File startingDirectory, Shell shell) {
			FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
			if (startingDirectory != null) {
				fileDialog.setFilterPath(startingDirectory.getPath());
			}

			String dir = fileDialog.open();
			if (dir != null) {
				dir = dir.trim();
				if (dir.length() > 0) {
					return new File(dir);
				}
			}
			return null;
		}
	}
}
