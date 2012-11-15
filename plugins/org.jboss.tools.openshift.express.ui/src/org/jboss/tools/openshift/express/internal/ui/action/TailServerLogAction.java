package org.jboss.tools.openshift.express.internal.ui.action;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils;
import org.jboss.tools.openshift.express.internal.ui.console.JschToEclipseLogger;
import org.jboss.tools.openshift.express.internal.ui.console.TailFilesWizard;
import org.jboss.tools.openshift.express.internal.ui.console.TailServerLogWorker;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftSSHOperationException;
import com.openshift.client.utils.Base64Coder;

/**
 * The action associated with the "Show In>Remote Console" menu item.
 * 
 * @author Xavier Coulon
 * 
 */
public class TailServerLogAction extends AbstractAction implements IConsoleListener {

	/**
	 * The message consoles associated with the 'tail' workers that write the output.
	 */
	private Map<String, TailServerLogWorker> consoleWorkers = new HashMap<String, TailServerLogWorker>();

	/**
	 * Constructor
	 */
	public TailServerLogAction() {
		super(OpenShiftExpressUIMessages.TAIL_SERVER_LOG_ACTION, true);
		IViewRegistry reg = PlatformUI.getWorkbench().getViewRegistry();
		IViewDescriptor desc = reg.find(IConsoleConstants.ID_CONSOLE_VIEW);
		setImageDescriptor(desc.getImageDescriptor());
		ConsoleUtils.registerConsoleListener(this);
	}

	/**
	 * Operation called when the user clicks on 'Show In>Remote Console'. If no Console/Worker existed, a new one is
	 * created, otherwise, it is displayed. {@inheritDoc}
	 */
	@Override
	public void run() {
		try {
			
			if (selection instanceof IStructuredSelection) {
				final Object selectedItem = ((IStructuredSelection) selection).getFirstElement();
				if (selectedItem instanceof IServer) {
					final IServer server = ((IServer) selectedItem);
					run(server);
				} else if (selectedItem instanceof IServerModule) {
					final IServer server = ((IServerModule) selectedItem).getServer();
					run(server);
				} else if (selectedItem instanceof IApplication) {
					final IApplication application = (IApplication) selectedItem;
					run(application);
				}
			}
		} catch (Exception e) {
			Logger.error("Failed to open Remote Console", e);
		}
	}

	private void run(final IApplication application) throws OpenShiftException, MalformedURLException {
		final String host = new URL(application.getApplicationUrl()).getHost();
		final String appName = application.getName();
		final MessageConsole console = ConsoleUtils.findMessageConsole(createConsoleId(appName, host));
		ConsoleUtils.displayConsoleView(console);
		console.newMessageStream().println("Loading....");
		if (!this.consoleWorkers.containsKey(console.getName())) {
			launchTailServerJob(host, application, console);
		}
	}

	private static String createConsoleId(String appName, String host) {
		return host;
	}

	private void run(final IServer server) {
		if (ExpressServerUtils.isOpenShiftRuntime(server) || ExpressServerUtils.isInOpenshiftBehaviourMode(server)) {
			final String host = server.getHost();
			final IApplication app = ExpressServerUtils.getApplication(server);
			final MessageConsole console = ConsoleUtils.findMessageConsole(createConsoleId(app.getName(), host));
			ConsoleUtils.displayConsoleView(console);
			console.newMessageStream().println("Loading....");
			if (!this.consoleWorkers.containsKey(console.getName())) {
				launchTailServerJob(host, app, console);
			}
		}
	}

	private void launchTailServerJob(final String host, final IApplication app,
			final MessageConsole console) {
		final TailFilesWizard wizard = new TailFilesWizard(app);
		if (WizardUtils.openWizardDialog(
				wizard, Display.getCurrent().getActiveShell()) == Window.OK) {
			
			new Job("Launching Tail Server Operation") {
				protected IStatus run(IProgressMonitor monitor) {
					try {
						final TailServerLogWorker tailServerLogWorker = startTailProcess(host, app.getUUID(), app.getName(), wizard.getFilePattern(), console);
						consoleWorkers.put(console.getName(), tailServerLogWorker);
						Thread thread = new Thread(tailServerLogWorker);
						thread.start();
					} catch (IOException e) {
						return OpenShiftUIActivator.createErrorStatus(NLS.bind("Failed to tail files for application ''{0}''", app.getName()), e);
					}
					return Status.OK_STATUS;
				}
	
			}.schedule();
		}
	}

	/**
	 * Starting the tail process on the remote OpenShift Platform. This method relies on the JGit SSH support (including
	 * JSch) to open a connection AND execute a command in a single invocation. The connection establishement requires
	 * an SSH key, and the passphrase is prompted to the user if necessary.
	 * 
	 * @param host the remote host to connect to
	 * @param appId the application id, used as the user to establish the ssh connexion
	 * @param appName the application name
	 * @param filePattern the file pattern to use in the tail command
	 * @return the Worker that encapsulate the established RemoteSession, the tail Process and the output console
	 * @throws OpenShiftSSHOperationException 
	 * @throws JSchException
	 *             in case of underlying exception
	 * @throws IOException
	 *             in case of underlying exception
	 */
	private TailServerLogWorker startTailProcess(final String host, final String appId, final String appName,
			final String filePattern, final MessageConsole console) throws IOException {
		final String options = "-f -n 100";

		JSch.setLogger(new JschToEclipseLogger());
		final SshSessionFactory sshSessionFactory = SshSessionFactory.getInstance();
		final URIish uri = new URIish().setHost(host).setUser(appId);
		RemoteSession remoteSession;
		remoteSession = sshSessionFactory.getSession(uri, CredentialsProvider.getDefault(), FS.DETECTED,
				0);
		// the rhc-tail-files command template
		// ssh_cmd =
		// "ssh -t #{app_uuid}@#{app}-#{namespace}.#{rhc_domain} 'tail#{opt['opts'] ? ' --opts ' + Base64::encode64(opt['opts']).chomp : ''} #{file_glob}'"
		final String command = buildCommand(filePattern, options);
		Process process = remoteSession.exec(command, 0);
		return new TailServerLogWorker(console, process, remoteSession);
	}

	/**
	 * Builds the 'ssh tail' command that should be executed on the remote OpenShift platform.
	 * 
	 * @param filePath
	 * @param options
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String buildCommand(final String filePath, final String options) throws UnsupportedEncodingException {
		StringBuilder commandBuilder = new StringBuilder("tail ");
		if (options != null && !options.isEmpty()) {
			final String opts = new String(Base64Coder.encode(options.getBytes()));
			commandBuilder.append("--opts ").append(opts).append(" ");
		}
		commandBuilder.append(filePath);
		final String command = commandBuilder.toString();
		Logger.debug("ssh command to execute: " + command);
		return command;
	}

	public Object getSelection() {
		if (selection instanceof IStructuredSelection) {
			final Object selectedItem = ((IStructuredSelection) selection).getFirstElement();
			if (selectedItem instanceof IServer) {
				return ((IServer) selectedItem);
			}
			if (selectedItem instanceof IServerModule) {
				return ((IServerModule) selectedItem).getServer();
			}
			if (selectedItem instanceof IApplication) {

			}
		}
		return null;
	}

	@Override
	public void consolesAdded(IConsole[] consoles) {
		// don't do anything special
	}

	/**
	 * Operation to perform when the console is removed (through the CloseConsoleAction that was brung by the
	 * <code>TailConsolePageParticipant</code>). In the current case, the associated worker is stopped and the
	 * console/worker are removed from the map, so that further 'Show In>Remote Console' invocation will trigger a new
	 * worker process.
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

}
