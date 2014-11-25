package org.jboss.tools.openshift.internal.ui.wizard.deployment;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.core.Connection;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.IOpenShiftApplicationWizardModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

public class IOpenShiftApplicationWizardModelAdapter extends ObservablePojo implements IOpenShiftApplicationWizardModel{

	private DeploymentWizardContext context;

	public IOpenShiftApplicationWizardModelAdapter(DeploymentWizardContext context) {
		this.context = context;
	}

	@Override
	public Connection getConnection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasConnection() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Connection setConnection(Connection connection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.jboss.tools.openshift.express.internal.core.connection.Connection getLegacyConnection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.jboss.tools.openshift.express.internal.core.connection.Connection setLegacyConnection(
			org.jboss.tools.openshift.express.internal.core.connection.Connection connection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IProject importProject(IProgressMonitor monitor) throws OpenShiftException, CoreException,
			InterruptedException, URISyntaxException, InvocationTargetException, IOException, NoWorkTreeException,
			GitAPIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IProject mergeIntoUnsharedProject(IProgressMonitor monitor) throws OpenShiftException,
			InvocationTargetException, InterruptedException, IOException, CoreException, URISyntaxException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IProject mergeIntoGitSharedProject(IProgressMonitor monitor) throws OpenShiftException,
			InvocationTargetException, InterruptedException, IOException, CoreException, URISyntaxException,
			NoWorkTreeException, GitAPIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getRepositoryFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IDomain setDomain(IDomain domain) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDefaultDomainIfRequired() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasDomain() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IDomain getDomain() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IApplication getApplication() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IApplication setApplication(IApplication application) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String setApplicationName(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getApplicationName() {
		return context.getProjectName();
	}

	@Override
	public List<IStandaloneCartridge> setAvailableStandaloneCartridges(List<IStandaloneCartridge> cartridges) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IStandaloneCartridge> getAvailableStandaloneCartridges() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String setRemoteName(String remoteName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String setRepositoryPath(String repositoryPath) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRepositoryPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isNewProject() {
		return false;
	}

	@Override
	public Boolean setNewProject(boolean newProject) {
		return false;
	}

	@Override
	public String setProjectName(String projectName) {
		String old = context.getProjectName();
		context.setProjectName(projectName);
		firePropertyChange("projectName", old, projectName);
		return context.getProjectName();
	}

	@Override
	public String getProjectName() {
			return context.getProjectName();
	}

	@Override
	public IProject setProject(IProject project) {
		return context.getProject();
	}

	@Override
	public IProject getProject() {
		String projectName = getProjectName();
		if (StringUtils.isEmpty(projectName)) {
			return null;
		}
		return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	}

	@Override
	public boolean isGitSharedProject() {
		return false;
	}

	@Override
	public Boolean setCreateServerAdapter(Boolean createServerAdapter) {
		context.createServerAdapter(createServerAdapter);
		return context.createServerAdapter();
	}

	@Override
	public boolean isCreateServerAdapter() {
		return false;
	}

	@Override
	public IServer createServerAdapter(IProgressMonitor monitor) throws OpenShiftException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean setSkipMavenBuild(Boolean skipMavenBuild) {
		return true;
	}

	@Override
	public boolean isSkipMavenBuild() {
		return true;
	}

	@Override
	public String setMergeUri(String mergeUri) {
		return null;
	}

	@Override
	public String getMergeUri() {
		return null;
	}

	@Override
	public boolean isUseExistingApplication() {
		return true;
	}

	@Override
	public boolean setUseExistingApplication(boolean useExistingApplication) {
		return true;
	}

	@Override
	public void addEmbeddedCartridges(List<ICartridge> cartridges) {
	}

	@Override
	public void removeEmbeddedCartridge(ICartridge cartridge) {
	}

	@Override
	public void removeEmbeddedCartridges(List<ICartridge> cartridges) {
	}

	@Override
	public List<ICartridge> getAvailableEmbeddableCartridges() {
		return null;
	}

	@Override
	public List<ICartridge> setAvailableEmbeddableCartridges(List<ICartridge> embeddableCartridges) {
		return null;
	}

	@Override
	public Set<ICartridge> getEmbeddedCartridges() {
		return null;
	}

	@Override
	public Set<ICartridge> setEmbeddedCartridges(Set<ICartridge> selectedEmbeddableCartridges) {
		return null;
	}

	@Override
	public ICartridge getStandaloneCartridge() {
		return null;
	}

	@Override
	public Set<ICartridge> getCartridges() {
		return null;
	}

	@Override
	public IGearProfile setApplicationGearProfile(IGearProfile gearProfile) {
		return null;
	}

	@Override
	public IGearProfile getApplicationGearProfile() {
		return null;
	}

	@Override
	public ApplicationScale setApplicationScale(ApplicationScale scale) {
		return null;
	}

	@Override
	public ApplicationScale getApplicationScale() {
		return null;
	}

	@Override
	public IApplicationTemplate setSelectedApplicationTemplate(IApplicationTemplate template) {
		return null;
	}

	@Override
	public IApplicationTemplate getSelectedApplicationTemplate() {
		return null;
	}

	@Override
	public String getInitialGitUrl() {
		return null;
	}

	@Override
	public String setInitialGitUrl(String initialGitUrl) {
		return null;
	}

	@Override
	public boolean isUseInitialGitUrl() {
		return false;
	}

	@Override
	public boolean setUseInitialGitUrl(boolean useInitialGitUrl) {
		return false;
	}

	@Override
	public List<IDomain> setDomains(List<IDomain> domains) {
		return null;
	}

	@Override
	public List<IDomain> getDomains() {
		return null;
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		return null;
	}

	@Override
	public Map<String, String> setEnvironmentVariables(Map<String, String> environmentVariables) {
		return null;
	}

}
