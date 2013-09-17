package org.jboss.tools.openshift.express.internal.core.marker;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IOpenShiftMarker {

	public static final IOpenShiftMarker DISABLE_AUTO_SCALING = new BaseOpenShiftMarker("disable_auto_scaling",
			"Disable Auto Scaling",
			"Will prevent scalable applications from scaling up or down according to application load");

	public static final IOpenShiftMarker ENABLE_JPA = new BaseOpenShiftMarker("enable_jpda", "Enable JPDA",
			"Will enable the JPDA socket based transport on the java virtual "
					+ "machine running the JBoss AS 7 application server. This enables "
					+ "you to remotely debug code running inside the JBoss AS 7 "
					+ "application server.");

	public static final IOpenShiftMarker SKIP_MAVEN_BUILD = new BaseOpenShiftMarker("skip_maven_build", "Skip Maven Build",
			"Maven build step will be skipped");

	public static final IOpenShiftMarker FORCE_CLEAN_BUILD = new BaseOpenShiftMarker("force_clean_build",
			"Force Clean Build",
			"Will start the build process by removing all non "
					+ "essential Maven dependencies.  Any current dependencies specified in "
					+ "your pom.xml file will then be re-downloaded");

	public static final IOpenShiftMarker HOT_DEPLOY = new BaseOpenShiftMarker("hot_deploy", "Hot Deploy",
			"Will prevent a JBoss container restart during build/deployment. "
					+ "Newly build archives will be re-deployed automatically by the "
					+ "JBoss HDScanner component");

	public static final IOpenShiftMarker JAVA_7 = new BaseOpenShiftMarker("java7", "Java 7",
			"Will run JBoss AS7 with Java7 if present. If no marker is present then the "
					+ "baseline Java version will be used (currently Java6)");
			
	/**
	 * Adds this marker to the given project. Returns the new marker if it was
	 * created, <code>null</code> otherwise.
	 * 
	 * @param project
	 *            the project to add the marker to
	 * @param monitor
	 *            the monitor to report progress to
	 * @return the marker file that was created, null otherwise
	 * @throws CoreException
	 */
	public abstract IFile addTo(IProject project, IProgressMonitor monitor) throws CoreException;

	/**
	 * Removes this marker from the given project. Returns the marker that was removed if it existed 
	 * and was removed or <code>null</code> otherwise.
	 * 
	 * @param project
	 *            the project to remove the marker from
	 * @param monitor
	 *            the monitor to report progress to
	 * @return the marker file that was removed, null otherwise
	 * @throws CoreException
	 */
	public abstract IFile removeFrom(IProject project, IProgressMonitor monitor) throws CoreException;

	/**
	 * Returns <code>true</code> if the given project has this marker. Returns
	 * <code>false</code> otherwise.
	 * 
	 * @param project
	 *            the project to check for this marker
	 * @param monitor
	 *            the monitor to report progress to
	 * @return true if the project has this marker
	 * @throws CoreException 
	 */
	public abstract boolean existsIn(IProject project, IProgressMonitor monitor) throws CoreException;

	public abstract boolean matchesFilename(String fileName);

	public abstract String getName();

	public abstract String getFileName();

	public abstract String getDescription();

}