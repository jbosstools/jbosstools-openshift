package org.jboss.tools.openshift.express.test.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.openshift.express.internal.core.marker.IOpenShiftMarker;
import org.jboss.tools.openshift.express.internal.core.marker.OpenShiftMarkers;
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftProjectUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OpenShiftMarkerTest {

	private IProject project;
	private OpenShiftMarkers markers;

	@Before
	public void setUp() throws CoreException {
		String projectName = String.valueOf(System.currentTimeMillis());
		this.project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		this.markers = new OpenShiftMarkers(project);
	}

	@After
	public void tearDown() throws CoreException {
		this.project.delete(true, new NullProgressMonitor());
	}

	@Test
	public void shouldReturnAllKnownMarkersAndNonePresent() throws CoreException {
		// operations
		List<IOpenShiftMarker> all = markers.getAll();
		List<IOpenShiftMarker> present = markers.getPresent();

		// verification
		assertEquals(6, all.size());
		assertTrue(present.isEmpty());
	}

	@Test
	public void shouldReturnAllKnownMarkersAnd1Present() throws CoreException {
		// prerequisites
		createMarker(IOpenShiftMarker.SKIP_MAVEN_BUILD.getFileName());
		
		// operations
		List<IOpenShiftMarker> all = markers.getAll();
		List<IOpenShiftMarker> present = markers.getPresent();

		// verification
		assertEquals(6, all.size());
		assertEquals(1, present.size());
	}

	@Test
	public void shouldReturnAllKnownMarkersAnd1Custom() throws CoreException {
		// prerequisites
		createMarker("adietish");
		
		// operations
		List<IOpenShiftMarker> all = markers.getAll();
		List<IOpenShiftMarker> present = markers.getPresent();

		assertEquals(6 + 1, all.size());
		assertEquals(1, present.size());
	}
	
	@Test
	public void shouldIgnoreDotFileInMarkers() throws CoreException {
		// prerequisites
		createMarker(".gitignore");
		
		// operations
		List<IOpenShiftMarker> all = markers.getAll();
		List<IOpenShiftMarker> present = markers.getPresent();

		// verification
		assertEquals(6, all.size());
		assertEquals(0, present.size());
	}

	@Test
	public void shouldCreateMarkerWhenMissingMarkersFolder() throws CoreException {
		// prerequisites
		
		// operations
		IOpenShiftMarker.SKIP_MAVEN_BUILD.addTo(project, new NullProgressMonitor());
		
		// verification
		assertEquals(1, markers.getPresent().size());
	}

	@Test
	public void shouldRemoveMarker() throws CoreException {
		// prerequisites
		IOpenShiftMarker.SKIP_MAVEN_BUILD.addTo(project, new NullProgressMonitor());
		
		// operations
		IOpenShiftMarker.SKIP_MAVEN_BUILD.removeFrom(project, new NullProgressMonitor());
		
		// verification
		assertEquals(0, markers.getPresent().size());
	}

	private void createMarker(String filename) throws CoreException {
		IFolder markersFolder = OpenShiftProjectUtils.ensureMarkersFolderExists(project, new NullProgressMonitor());
		markersFolder.getFile(filename)
				.create(new ByteArrayInputStream(new byte[] {}), false, new NullProgressMonitor());
	}
}
