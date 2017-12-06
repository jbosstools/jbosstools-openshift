package org.jboss.tools.openshift.cdk.server.test.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.ide.eclipse.as.core.util.FileUtil;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.runtime.core.model.IRuntimeDetector;
import org.jboss.tools.runtime.core.model.RuntimeDefinition;
import org.jboss.tools.runtime.core.model.RuntimePath;
import org.jboss.tools.runtime.core.util.RuntimeInitializerUtil;
import org.junit.Test;

import junit.framework.TestCase;

public class CDKRuntimeDetectorTest extends TestCase {

	@Test
	public void testCDK30() {
		testCDK("3.0.1", "org.jboss.tools.openshift.cdk.server.core.internal.detection.CDK3RuntimeDetector");
	}

	@Test
	public void testCDK31() {
		testCDK("3.1.1", "org.jboss.tools.openshift.cdk.server.core.internal.detection.CDK3RuntimeDetector");
	}

	@Test
	public void testCDK32() {
		testCDK("3.2.0-alpha.1-1", "org.jboss.tools.openshift.cdk.server.core.internal.detection.CDK32RuntimeDetector");
	}

	public void testCDK(String version, String detectorId) {
		try {
			// Create a mock cdk30
			createCDK(version);
			RuntimePath runtimePath = new RuntimePath(getDotMinishift().getAbsolutePath());
			List<RuntimeDefinition> runtimeDefinitions = RuntimeInitializerUtil.createRuntimeDefinitions(runtimePath,
					new NullProgressMonitor());
			assertNotNull(runtimeDefinitions);
			assertEquals(runtimeDefinitions.size(), 1);
			RuntimeDefinition def = runtimeDefinitions.get(0);
			assertNotNull(def);
			IRuntimeDetector detector = def.getDetector();
			assertNotNull(detector);
			assertEquals(detector.getId(), detectorId);
		} catch (IOException ioe) {
			cleanupCDK();
			fail();
		}
	}

	private File getDotMinishift() {
		File home = getHomeDirectory();
		File minishift = new File(home, CDKConstants.CDK_RESOURCE_DOTMINISHIFT);
		return minishift;
	}

	private void cleanupCDK() {
		File home = getHomeDirectory();
		home.mkdirs();
		File minishift = new File(home, CDKConstants.CDK_RESOURCE_DOTMINISHIFT);
		FileUtil.completeDelete(minishift);
	}

	private void createCDK(String version) throws IOException {
		cleanupCDK();
		File home = getHomeDirectory();
		home.mkdirs();
		File minishift = new File(home, CDKConstants.CDK_RESOURCE_DOTMINISHIFT);
		minishift.mkdir();
		File config = new File(minishift, "config");
		config.mkdir();
		new File(config, "config.json").createNewFile();
		String cdkFileContents = "openshift.auth.scheme=basic\nopenshift.auth.username=developer\nopenshift.auth.password=developer\ncdk.version="
				+ version;
		FileUtil.writeTo(new ByteArrayInputStream(cdkFileContents.getBytes()), new File(minishift, "cdk"));
	}

	protected File getHomeDirectory() {
		String home = System.getProperty("user.home");
		return new File(home);
	}
}
