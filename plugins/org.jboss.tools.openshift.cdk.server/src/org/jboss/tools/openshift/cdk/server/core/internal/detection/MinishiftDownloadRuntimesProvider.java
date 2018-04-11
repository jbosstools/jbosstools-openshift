package org.jboss.tools.openshift.cdk.server.core.internal.detection;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.jdf.stacks.model.Stacks;
import org.jboss.tools.as.runtimes.integration.util.AbstractStacksDownloadRuntimesProvider;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.runtime.core.model.DownloadRuntime;
import org.jboss.tools.stacks.core.model.StacksManager;
import org.osgi.framework.Bundle;

public class MinishiftDownloadRuntimesProvider extends AbstractStacksDownloadRuntimesProvider {
	private static final String MINISHIFT_YAML_URL = "https://raw.githubusercontent.com/jboss-developer/jboss-stacks/1.0.0.Final/minishift.yaml";
	private static final String BUNDLED_YAML = "resources/minishift.yaml";
	
	public MinishiftDownloadRuntimesProvider() {
		// Zero-arg constructor for loading via extension pt
	}
	private static class StacksManager2 extends StacksManager {
		@Override
		public Stacks getStacksFromFile(File f) throws IOException {
			return super.getStacksFromFile(f);
		}
	}
	
	@Override
	protected Stacks[] getStacks(IProgressMonitor monitor) {
		Stacks[] ret = getWebStacks(monitor);
		if( ret == null ) {
			ret = getBundledStacks(monitor);
		}
		if( ret == null ) {
			return new Stacks[0];
		}
		return ret;
	}
	
	private Stacks[] getWebStacks(IProgressMonitor monitor) {
		StacksManager mgr = new StacksManager();
		Stacks s = mgr.getStacks(MINISHIFT_YAML_URL, monitor);
		if( s != null ) {
			return new Stacks[] {s};
		}
		CDKCoreActivator.pluginLog().logError("Can't access or parse  " + MINISHIFT_YAML_URL); //$NON-NLS-1$
		return null;
	}
	private Stacks[] getBundledStacks(IProgressMonitor monitor) {
		Bundle bundle = CDKCoreActivator.getDefault().getBundle();
		URL url = null;
		try {
			URL inBundle = bundle.getEntry(BUNDLED_YAML);
			url = FileLocator.toFileURL(inBundle);
			File f = new File(url.getPath());
			StacksManager2 mgr = new StacksManager2();
			return new Stacks[] {mgr.getStacksFromFile(f)};
		} catch (IOException e) {
			if( url == null )
				CDKCoreActivator.pluginLog().logError("Cannot find minishift.yaml.  " + url, e ); //$NON-NLS-1$
			else 
				CDKCoreActivator.pluginLog().logError("Can't access or parse  " + url.getPath(), e ); //$NON-NLS-1$
		}
		return new Stacks[0];
	}

	@Override
	protected void traverseStacks(Stacks stacks, ArrayList<DownloadRuntime> list, IProgressMonitor monitor) {
		traverseStacks(stacks, list, "MINISHIFT", monitor);
	}

	@Override
	protected void setDisclaimerData(DownloadRuntime dlrt, org.jboss.jdf.stacks.model.Runtime workingRT, String wtpRT, String category) {
		dlrt.setDisclaimer(false);
	}

	
	@Override
	protected String getLegacyId(String id) {
		// Intentionally left blank
		return null;
	}

}
