package org.jboss.tools.openshift.egit.core.module;

import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.core.IRepositoryProviderListener;
import org.eclipse.team.internal.core.RepositoryProviderManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ModuleFile;
import org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate;

public class GitProjectModuleFactoryDelegate extends ProjectModuleFactoryDelegate {
	public static final String FACTORY_ID = "org.jboss.ide.eclipse.as.egit.core.module.gitProjectModuleFactory"; //$NON-NLS-1$
	public static final String MODULE_TYPE = "jbt.egit"; //$NON-NLS-1$
	public static final String VERSION = "1.0"; //$NON-NLS-1$
	private HashMap<String, IModule> moduleIdToModule;
	private HashMap<IModule, GitProjectModuleDelegate> moduleToDelegate;
	private IResourceChangeListener resourceListener;
	public GitProjectModuleFactoryDelegate() {
		System.out.println("test");
	}
	public void initialize() {
		moduleIdToModule = new HashMap<String, IModule>();
		moduleToDelegate = new HashMap<IModule, GitProjectModuleDelegate>();
		resourceListener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					event.getDelta().accept(new IResourceDeltaVisitor() {
						public boolean visit(IResourceDelta delta) throws CoreException {
							IResource r = delta.getResource();
							if( r instanceof IProject ) {
								IResourceDelta[] kids = delta.getAffectedChildren();
								if( kids.length > 1 )
									incrementChanged((IProject)r);
								else if( kids.length == 1 ) {
									String changedkid = kids[0].getResource().getName();
									if( !changedkid.equals(".git"))
										incrementChanged((IProject)r);
								}
							}
							return !(r instanceof IProject);
						}
					});
				} catch( CoreException ce) {
					// Um, in what circumstances does this happen? TODO
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE);
		RepositoryProviderManager.getInstance().addListener(new IRepositoryProviderListener() {
			public void providerUnmapped(IProject project) {
				if( moduleIdToModule != null ) {
					IModule mod = moduleIdToModule.get(project.getName());
					if( mod != null ) {
						moduleIdToModule.remove(project.getName());
						moduleToDelegate.remove(mod);
					}
				}
			}
			public void providerMapped(RepositoryProvider provider) {
				clearCache(provider.getProject());
			}
		});
	}	
	protected void incrementChanged(IProject p) {
		IModule mod = moduleIdToModule.get(p.getName());
		if( mod != null ) {
			GitProjectModuleDelegate del = moduleToDelegate.get(mod);
			del.updateTimestamp();
		}
	}

	protected IModule createModule(IProject project) {
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if( mapping != null ) {
			IModule module = createModule(project.getName(), project.getName(), MODULE_TYPE, VERSION, project);
			moduleIdToModule.put(project.getName(), module);
			moduleToDelegate.put(module, new GitProjectModuleDelegate(project));
			return module;
		}
		return null;
	}

	@Override
	public ModuleDelegate getModuleDelegate(IModule module) {
		return moduleToDelegate.get(module);
	}

	public class GitProjectModuleDelegate extends ModuleDelegate  {
		private IProject project;
		private long lastUpdated = System.currentTimeMillis();
		public GitProjectModuleDelegate(IProject project) {
			this.project = project;
		}
		public IModuleResource[] members() throws CoreException {
			return new IModuleResource[]{
					// This resource will be ignored, but, we need *SOMETHING* to sit here
					new ModuleFile(project.getName(), new Path("/"), lastUpdated)
			};
		}

		protected void updateTimestamp() {
			lastUpdated = System.currentTimeMillis();
		}
		@Override
		public IStatus validate() {
			return Status.OK_STATUS;
		}

		@Override
		public IModule[] getChildModules() {
			return new IModule[0];
		}
	}
}
