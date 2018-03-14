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

	public MinishiftDownloadRuntimesProvider() {
		// Zero-arg constructor for loading via extension pt
	}
	private static final String YAML_PATH = "resources/minishift.yaml";
	private static class StacksManager2 extends StacksManager {
		@Override
		public Stacks getStacksFromFile(File f) throws IOException {
			return super.getStacksFromFile(f);
		}
	}
	
	@Override
	protected Stacks[] getStacks(IProgressMonitor monitor) {
		Bundle bundle = CDKCoreActivator.getDefault().getBundle();
		URL url = null;
		try {
			URL inBundle = bundle.getEntry(YAML_PATH);
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
	protected String getLegacyId(String id) {
		// Intentionally left blank
		return null;
	}

}
