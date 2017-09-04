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
package org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.marker.IOpenShiftMarker;
import org.jboss.tools.openshift.internal.common.ui.application.importoperation.GeneralProjectImportOperation;
import org.jboss.tools.openshift.internal.common.ui.application.importoperation.MavenProjectImportOperation;
import org.jboss.tools.openshift.internal.common.ui.application.importoperation.WontOverwriteException;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
public class ImportNewProject extends AbstractImportApplicationOperation {

    private File cloneDestination;

    public ImportNewProject(String projectName, IApplication application, String remoteName, File cloneDestination,
            List<IOpenShiftMarker> markers, ExpressConnection connection) {
        super(projectName, application, remoteName, markers, connection);
        this.cloneDestination = cloneDestination;
    }

    /**
     * Imports the (new) project that the user has chosen into the workspace.
     * 
     * @param monitor
     *            the monitor to report progress to
     * @throws OpenShiftException
     * @throws CoreException
     * @throws InterruptedException
     * @throws URISyntaxException
     * @throws InvocationTargetException
     * @throws IOException 
     * @throws GitAPIException 
     * @throws NoWorkTreeException 
     */
    @Override
    public IProject execute(IProgressMonitor monitor) throws OpenShiftException, CoreException, InterruptedException, URISyntaxException,
            InvocationTargetException, IOException, NoWorkTreeException, GitAPIException {
        if (cloneDestinationExists()) {
            throw new WontOverwriteException(NLS.bind("There's already a folder at {0}. The new OpenShift project would overwrite it. "
                    + "Please choose another destination to clone to.", getCloneDestination().getAbsolutePath()));
        }

        File repositoryFolder = cloneRepository(getApplication(), getRemoteName(), cloneDestination, true, monitor);
        List<IProject> importedProjects = importProjectsFrom(repositoryFolder, monitor);
        connectToGitRepo(importedProjects, repositoryFolder, monitor);
        // TODO: handle multiple projects (is this really possible?)
        IProject project = getSettingsProject(importedProjects);
        addToModified(setupGitIgnore(project, monitor));
        addSettingsFile(project, monitor);
        addToModified(setupMarkers(project, monitor));
        //server adapter will commit when publishing
        //addAndCommitModifiedResource(project, monitor);
        return getSettingsProject(importedProjects);
    }

    /**
     * Imports the projects that are within the given folder. Supports maven and
     * general projects
     * 
     * @param folder
     *            the folder the projects are located in
     * @param monitor
     *            the monitor to report progress to
     * @return
     * @throws CoreException
     * @throws InterruptedException
     */
    private List<IProject> importProjectsFrom(final File folder, IProgressMonitor monitor) throws CoreException, InterruptedException {
        MavenProjectImportOperation mavenImport = new MavenProjectImportOperation(folder);
        List<IProject> importedProjects = Collections.emptyList();
        if (mavenImport.isMavenProject()) {
            importedProjects = mavenImport.importToWorkspace(monitor);
        } else {
            importedProjects = new GeneralProjectImportOperation(folder).importToWorkspace(monitor);
        }
        return importedProjects;
    }

    private void connectToGitRepo(List<IProject> projects, File projectFolder, IProgressMonitor monitor) throws CoreException {
        for (IProject project : projects) {
            if (project != null) {
                EGitUtils.connect(project, monitor);
            }
        }
    }

    protected File getCloneDestination() {
        return cloneDestination;
    }

    protected boolean cloneDestinationExists() {
        return cloneDestination != null && cloneDestination.exists();
    }
}
