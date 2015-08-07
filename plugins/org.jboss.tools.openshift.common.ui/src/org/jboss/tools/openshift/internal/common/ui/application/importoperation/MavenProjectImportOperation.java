/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.application.importoperation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.osgi.util.NLS;

/**
 * @author Andre Dietisheim <adietish@redhat.com>
 * 
 */
public class MavenProjectImportOperation extends AbstractProjectImportOperation {

	private static final String POM_FILE = "pom.xml";
	private Collection<String> filters;

	public MavenProjectImportOperation(File projectFolder) {
		super(projectFolder);
	}

	public void setFilters(Collection<String> filters) {
		this.filters = filters;
	}
	
	public List<IProject> importToWorkspace(IProgressMonitor monitor)
			throws CoreException, InterruptedException {
		MavenPluginActivator mavenPlugin = MavenPluginActivator.getDefault();
		IProjectConfigurationManager configurationManager = mavenPlugin.getProjectConfigurationManager();
		MavenModelManager modelManager = mavenPlugin.getMavenModelManager();
		Set<MavenProjectInfo> projectInfos = getMavenProjects(getProjectDirectory(), filters, modelManager, monitor);
		ProjectImportConfiguration projectImportConfiguration = new ProjectImportConfiguration();
		List<IMavenProjectImportResult> importResults =
				configurationManager.importProjects(projectInfos, projectImportConfiguration, monitor);
		return validate(toProjects(importResults));
	}

	private List<IProject> toProjects(List<IMavenProjectImportResult> importResults) {
		List<IProject> projects = new ArrayList<IProject>();
		for (IMavenProjectImportResult importResult : importResults) {
			IProject project = importResult.getProject();
			if (project != null) {
				projects.add(importResult.getProject());
			}
		}

		return projects;
	}

	private List<IProject> validate(List<IProject> projects) {
		if (projects.size() == 0) {
			throw new ImportFailedException(
					NLS.bind("There was a maven related error that prevented us from importing the project. "
							+ "We encourage you to look into the pom in the cloned repository at {0}.\n "
							+ "One of the possible reasons is that there is already a project in your workspace "
							+ "that matches the maven name of the OpenShift application. "
							+ "You can then rename your workspace project and start over again.\n"
							, getProjectDirectory()));
		}
		return projects;
	}

	private Set<MavenProjectInfo> getMavenProjects(File directory, Collection<String> filters, MavenModelManager modelManager,
			IProgressMonitor monitor) throws InterruptedException {
		if (filters == null || filters.isEmpty()) {
			return scan(directory, modelManager, monitor);
		}
		Set<MavenProjectInfo> projectInfos = new LinkedHashSet<>();
		for (String path : filters) {
			File dir = new File(directory, path);
			projectInfos.addAll(scan(dir, modelManager, monitor));
		}
		return projectInfos;
	}

	private Set<MavenProjectInfo> scan(File directory, MavenModelManager modelManager,
			IProgressMonitor monitor) throws InterruptedException {
		LocalProjectScanner scanner = new LocalProjectScanner(directory.getParentFile(), directory.toString(), false,
				modelManager);
		scanner.run(monitor);
		return collectProjects(scanner.getProjects());
	}
	
	public boolean isMavenProject() {
		File root = getProjectDirectory();
		if (filters == null || filters.isEmpty()) {
			return  isMavenProject(root);
		}
		for (String path : filters) {
			File dir = new File(root, path);
			if (isMavenProject(dir)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isMavenProject(File dir) {
		if (!isReadable(dir)
				|| !dir.isDirectory()) {
			return false;
		}

		return isReadable(new File(dir, POM_FILE));
	}
	
	public Set<MavenProjectInfo> collectProjects(
			Collection<MavenProjectInfo> projects) {
		return new LinkedHashSet<MavenProjectInfo>() {
			private static final long serialVersionUID = 1L;

			public Set<MavenProjectInfo> collectProjects(
					Collection<MavenProjectInfo> projects) {
				for (MavenProjectInfo projectInfo : projects) {
					add(projectInfo);
					collectProjects(projectInfo.getProjects());
				}
				return this;
			}
		}.collectProjects(projects);
	}

}
