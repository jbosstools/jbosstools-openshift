/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.util;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.profiles.core.internal.IProfileManager;
import org.eclipse.m2e.profiles.core.internal.ProfileData;
import org.eclipse.m2e.profiles.core.internal.ProfileState;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * A maven profile that can be activated or deactivated.
 * 
 * @author Dmitrii Bocharov
 * @author Andre Dietisheim
 */
@SuppressWarnings("restriction")
public class MavenProfile {
    
    public static final String OPENSHIFT_MAVEN_PROFILE = "openshift";

	private final String profileId;
	private final IProject project;
	
	private IProfileManager profileManager;

	/**
	 * Creates a maven profile for a given profile id and project
	 * @param profileId
	 * @param project
	 */
	public MavenProfile(String profileId, IProject project) {
		this.profileId = profileId;
		this.project = project;
		
		BundleContext bundleContext = FrameworkUtil.getBundle(MavenProfile.class).getBundleContext();
		profileManager = bundleContext.getService(bundleContext.getServiceReference(IProfileManager.class));
	}

	/**
	 * Activates this maven profile. Returns {@code true} if profile was activated,
	 * {@code false} otherwise. Does nothing if
	 * <ul>
	 * 	<li>no profileId was given</li>
	 * 	<li>given project has no maven nature</li>
	 * 	<li>given project has no accessible pom</li>
	 * 	<li>profile with the given id does not exist</li>
	 * </ul>
	 * 
	 * @param monitor
	 * @return true if profile was activated, false otherwise
	 * @throws CoreException
	 */
	public boolean activate(IProgressMonitor monitor) throws CoreException {
		return activate(profileId, project, monitor);
	}

    protected boolean activate(String profileId, IProject project, IProgressMonitor monitor) throws CoreException {
		IFile pom = getPom(project);
	    if (StringUtils.isEmpty(profileId)
	    		|| pom == null) {
	    	return false;
	    }

		IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(pom, true, monitor);

		List<ProfileData> profiles = profileManager.getProfileDatas(facade, monitor);
		List<String> activeProfiles = getActiveProfiles(profiles);
		if (!canActivate(profileId, activeProfiles, profiles)) {
			return false;
		}
		activeProfiles.add(profileId);
		profileManager.updateActiveProfiles(facade, activeProfiles, false, true, monitor);
		return true;
	}

	/**
	 * Deactivates this maven profile. Returns {@code true} if profile was
	 * deactivated, {@code false} otherwise. Does nothing if
	 * <ul>
	 * 	<li>no profileId was given</li>
	 * 	<li>given project has no maven nature</li>
	 * 	<li>given project has no accessible pom</li>
	 * 	<li>profile with the given id does not exist</li>
	 * </ul>
	 * 
	 * @param monitor
	 * @return true if profile was deactivated, false otherwise
	 * @throws CoreException
	 */
	public boolean deactivate(IProgressMonitor monitor) throws CoreException {
	    return deactivate(profileId, project, monitor);
	}
	
    protected boolean deactivate(String profileId, IProject project, IProgressMonitor monitor) throws CoreException {
        IFile pom = getPom(project);
        if (StringUtils.isEmpty(profileId)
                || pom == null) {
            return false;
        }
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(pom, true, monitor);

        List<ProfileData> profiles = profileManager.getProfileDatas(facade, monitor);
        List<String> activeProfiles = getActiveProfiles(profiles);
        if (!canDeactivate(profileId, activeProfiles, profiles)) {
            return false;
        }
        
        activeProfiles.remove(profileId);
        profileManager.updateActiveProfiles(facade, activeProfiles, false, true, monitor);
        return true;
    }

    protected IFile getPom(IProject project) throws CoreException {
		if (project == null) {
			return null;
		}
		IFile pom = project.getFile(IMavenConstants.POM_FILE_NAME);
	    if (!project.hasNature(IMavenConstants.NATURE_ID)
	    		|| pom == null
	    		|| !pom.isAccessible()) {
	    	return null;
	    }
	    return pom;
	}
	
    protected boolean canActivate(String profileId, List<String> activeProfiles, List<ProfileData> profiles) {
		return !activeProfiles.contains(profileId)
				&& profileExists(profiles);
	}
	
    protected boolean canDeactivate(String profileId, List<String> activeProfiles, List<ProfileData> profiles) {
        return activeProfiles.contains(profileId)
                && profileExists(profiles);
    }

    protected boolean profileExists(List<ProfileData> profiles) {
		return profiles.stream()
			.anyMatch(p -> profileId.equals(p.getId()));
	}
	
    protected List<String> getActiveProfiles(List<ProfileData> profiles) {
		return profiles.stream()
                .filter(p -> ProfileState.Active.equals(p.getActivationState())
                		&& !p.isAutoActive())
                .map(ProfileData::getId)
                .collect(Collectors.toList());
	}

}
