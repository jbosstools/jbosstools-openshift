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
package org.jboss.tools.openshift.express.internal.ui.command;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils;
import org.jboss.tools.openshift.express.internal.ui.console.GearGroupsUtils;
import org.jboss.tools.openshift.express.internal.ui.console.JschToEclipseLogger;
import org.jboss.tools.openshift.express.internal.ui.console.TailFilesWizard;
import org.jboss.tools.openshift.express.internal.ui.console.TailServerLogWorker;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.jcraft.jsch.JSch;
import com.openshift.client.IApplication;
import com.openshift.client.IGear;
import com.openshift.client.IGearGroup;
import com.openshift.client.utils.Base64Coder;

/**
 * The action associated with the "Show In>Remote Console" menu item.
 * 
 * @author Xavier Coulon
 * @author Andre Dietisheim
 * 
 */
public class TailFilesHandler extends AbstractHandler implements IConsoleListener {

	/**
	 * The message consoles associated with the 'tail' workers that write the
	 * output.
	 */
	private Map<String, TailServerLogWorker> consoleWorkers = new HashMap<String, TailServerLogWorker>();

	public TailFilesHandler() {
		ConsoleUtils.registerConsoleListener(this);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			Shell shell = HandlerUtil.getActiveShell(event);
			final Object selectedItem = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event));
			if (selectedItem instanceof IServer) {
				return execute((IServer) selectedItem, shell);
			} else if (selectedItem instanceof IServerModule) {
				return execute(((IServerModule) selectedItem).getServer(), shell);
			} else if (selectedItem instanceof IApplication) {
				return execute((IApplication) selectedItem, shell);
			}
			return Status.OK_STATUS;
		} catch (Exception e) {
			return ExpressUIActivator.createErrorStatus("Could not open OpenShift console", e);
		}
	}

	private IStatus execute(final IApplication application, Shell shell) {
		final TailFilesWizard wizard = new TailFilesWizard(application);
		if (WizardUtils.openWizardDialog(wizard, shell) == Window.OK) {
			try {
				final String host = new URL(application.getApplicationUrl()).getHost();
				for(IGearGroup gearGroup : wizard.getSelectedGearGroups()) {
					final Collection<IGear> gears = gearGroup.getGears();
					final String cartridgeNames = GearGroupsUtils.getCartridgeDisplayNames(gearGroup);
					for(IGear gear : gears) {
						final MessageConsole console = ConsoleUtils.findMessageConsole(createConsoleId(host, gear.getId(), cartridgeNames));
						ConsoleUtils.displayConsoleView(console);
						if (!this.consoleWorkers.containsKey(console.getName())) {
							launchTailServerJob(gear.getSshUrl(), wizard.getFilePattern(), console);
						}
					}
				}
			} catch (MalformedURLException e) {
				return ExpressUIActivator.createErrorStatus(
						NLS.bind("Could tail files for application {0}", application.getName()), e);
			}
		}
		return Status.OK_STATUS;
	}

	private static String createConsoleId(final String host, final String gearId, final String cartridgeNames) {
		return host + " [" + cartridgeNames + " on gear #" + gearId + "]" ;
	}

	private IStatus execute(final IServer server, final Shell shell) throws MalformedURLException {
		if (!OpenShiftServerUtils.isOpenShiftRuntime(server)
				|| !OpenShiftServerUtils.isInOpenshiftBehaviourMode(server)) {
			return ExpressUIActivator.createErrorStatus(
					NLS.bind("Server {0} is not an OpenShift Server Adapter", server.getName()));
		}
		final LoadApplicationJob applicationJob = new LoadApplicationJob(server);
		new JobChainBuilder(applicationJob)
			.runWhenSuccessfullyDone(new UIJob("Tail files") {
				
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					IApplication application = applicationJob.getApplication();
					if (application == null) {
						return ExpressUIActivator.createErrorStatus(
								NLS.bind("Could not retrieve application for server adapter {0}", server.getName()));
					}
					return execute(application, shell);
				}
			}).schedule();
		return Status.OK_STATUS;
	}

	private void launchTailServerJob(final String sshUrl, final String filePattern, final MessageConsole console) {
		new Job("Launching Tail Logs Operation") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final TailServerLogWorker tailServerLogWorker =
							startTailProcess(sshUrl, filePattern, console);
					consoleWorkers.put(console.getName(), tailServerLogWorker);
					Thread thread = new Thread(tailServerLogWorker);
					thread.start();
				} catch (IOException e) {
					return ExpressUIActivator.createErrorStatus(
							NLS.bind("Failed to tail files from ''{0}''", sshUrl), e);
				} catch (URISyntaxException e) {
					return ExpressUIActivator.createErrorStatus(
							NLS.bind("Failed to tail files from ''{0}''", sshUrl), e);
				}
				return Status.OK_STATUS;
			}

		}.schedule();
	}

	/**
	 * Starting the tail process on the remote OpenShift Platform. This method
	 * relies on the JGit SSH support (including JSch) to open a connection AND
	 * execute a command in a single invocation. The connection establishement
	 * requires an SSH key, and the passphrase is prompted to the user if
	 * necessary.
	 
	 * @param sshUrl
	 * @param filePattern
	 * @param optionsAndFile
	 * @param console
	 * @return
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	private TailServerLogWorker startTailProcess(final String sshUrl, final String optionsAndFile, final MessageConsole console) throws URISyntaxException, IOException {
		JSch.setLogger(new JschToEclipseLogger());
		final SshSessionFactory sshSessionFactory = SshSessionFactory.getInstance();
		URI uri = new URI(sshUrl);
		uri.getHost();
		final URIish urish = new URIish().setHost(uri.getHost()).setUser(uri.getUserInfo());
		RemoteSession remoteSession =
				sshSessionFactory.getSession(urish, CredentialsProvider.getDefault(), FS.DETECTED, 0);
		final String command = new TailCommandBuilder(optionsAndFile).build();

		Logger.debug("ssh command to execute: " + command);
		Process process = remoteSession.exec(command, 0);
		return new TailServerLogWorker(console, process, remoteSession);
	}


	@Override
	public void consolesAdded(IConsole[] consoles) {
		// don't do anything special
	}

	/**
	 * Operation to perform when the console is removed (through the
	 * CloseConsoleAction that was brung by the
	 * <code>TailConsolePageParticipant</code>). In the current case, the
	 * associated worker is stopped and the console/worker are removed from the
	 * map, so that further 'Show In>Remote Console' invocation will trigger a
	 * new worker process.
	 */
	@Override
	public void consolesRemoved(IConsole[] consoles) {
		// if the console is associated with a 'tail' process, stop that process
		for (IConsole console : consoles) {
			final String consoleName = console.getName();
			if (consoleWorkers.containsKey(consoleName)) {
				final TailServerLogWorker worker = consoleWorkers.get(consoleName);
				worker.stop();
				consoleWorkers.remove(consoleName);
			}
		}

	}

	private class TailCommandBuilder {

		private String options;
		private String file;

		public TailCommandBuilder(String optionsAndFile) {
			init(optionsAndFile);
		}

		private void init(String optionsAndFile) {
			if (!StringUtils.isEmpty(optionsAndFile)) {
				int filePatternStart = optionsAndFile.lastIndexOf(' ');
				if (filePatternStart > -1) {
					this.options = optionsAndFile.substring(0, filePatternStart);
					this.file = optionsAndFile.substring(filePatternStart).trim();
				} else {
					this.file = optionsAndFile;
				}
			}
		}

		public String build() {
			StringBuilder builder = new StringBuilder("tail");
			if (options != null) {
				builder.append(" --opts ").append(Base64Coder.encode(options));
			}
			if (file != null) {
				builder.append(' ').append(file);
			}
			return builder.toString();
		}
	}
}
