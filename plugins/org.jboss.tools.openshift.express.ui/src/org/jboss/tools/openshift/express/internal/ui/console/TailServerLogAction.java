package org.jboss.tools.openshift.express.internal.ui.console;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.console.TailServerLogWorker.JschToEclipseLogger;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.openshift.express.client.utils.Base64Encoder;

/**
 * The action associated with the "Show In>Remote Console" menu item.
 * 
 * @author Xavier Coulon
 * 
 */
public class TailServerLogAction extends Action implements ISelectionChangedListener, IConsoleListener {

	/** The current selection in the view. */
	private ISelection selection = null;

	/**
	 * The message consoles associated with the 'tail' workers that write the
	 * output.
	 */
	private Map<String, TailServerLogWorker> consoleWorkers = new HashMap<String, TailServerLogWorker>();

	/**
	 * Constructor
	 */
	public TailServerLogAction() {
		super(OpenShiftExpressUIMessages.TAIL_SERVER_LOG_ACTION);
		IViewRegistry reg = PlatformUI.getWorkbench().getViewRegistry();
		IViewDescriptor desc = reg.find(IConsoleConstants.ID_CONSOLE_VIEW);
		setImageDescriptor(desc.getImageDescriptor());
		ConsoleUtils.registerConsoleListener(this);
	}

	/**
	 * Operation called when the user clicks on 'Show In>Remote Console'. If no
	 * Console/Worker existed, a new one is created, otherwise, it is displayed.
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		final IServer server = getServer();
		if (ExpressServerUtils.isOpenShiftRuntime(server)) {
			MessageConsole console = ConsoleUtils.findMessageConsole(server.getId());
			String consoleName = console.getName();
			if (!this.consoleWorkers.containsKey(consoleName)) {
				try {
					final TailServerLogWorker tailServerLogWorker = startTailProcess(server, console);
					consoleWorkers.put(consoleName, tailServerLogWorker);
					consoleWorkers.put(console.getName(), tailServerLogWorker);
					Thread thread = new Thread(tailServerLogWorker);
					thread.start();
				} catch (Exception e) {
					Logger.error("Failed to retrieve remote server logs", e);
				}
			}
			ConsoleUtils.displayConsoleView(console);
		}
	}

	/**
	 * Starting the tail process on the remote OpenShift Platform. This method
	 * relies on the JGit SSH support (including JSch) to open a connection AND
	 * execute a command in a single invocation. The connection establishement
	 * requires an SSH key, and the passphrase is prompted to the user if
	 * necessary.
	 * 
	 * @param server the server adapter on which the action is perforemd
	 * @param console the console into which the tail should be writtent
	 * @return the Worker that encapsulate the established RemoteSession, the tail Process and the output console
	 * @throws JSchException in case of underlying exception
	 * @throws IOException in case of underlying exception
	 */
	private TailServerLogWorker startTailProcess(final IServer server, final MessageConsole console)
			throws JSchException, IOException {
		final String host = server.getHost();
		final String appId = ExpressServerUtils.getExpressApplicationId(server);
		final String appName = ExpressServerUtils.getExpressApplicationName(server);
		final String logFilePath = appName + "/logs/*.log";
		final String options = "-f -n 100";

		JSch.setLogger(new JschToEclipseLogger());
		final SshSessionFactory sshSessionFactory = SshSessionFactory.getInstance();
		final URIish uri = new URIish().setHost(host).setUser(appId);
		RemoteSession remoteSession = sshSessionFactory.getSession(uri, CredentialsProvider.getDefault(), FS.DETECTED,
				0);

		// the rhc-tail-files command template
		// ssh_cmd =
		// "ssh -t #{app_uuid}@#{app}-#{namespace}.#{rhc_domain} 'tail#{opt['opts'] ? ' --opts ' + Base64::encode64(opt['opts']).chomp : ''} #{file_glob}'"
		final String command = buildCommand(logFilePath, options);
		Process process = remoteSession.exec(command, 0);
		return new TailServerLogWorker(console, process, remoteSession);

	}

	/**
	 * Builds the 'ssh tail' command that should be executed on the remote
	 * OpenShift platform.
	 * 
	 * @param filePath
	 * @param options
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String buildCommand(final String filePath, final String options) throws UnsupportedEncodingException {
		StringBuilder commandBuilder = new StringBuilder("tail ");
		if (options != null && !options.isEmpty()) {
			final String opts = new String(Base64Encoder.encode(options.getBytes("UTF-8")), "UTF-8");
			commandBuilder.append("--opts ").append(opts).append(" ");
		}
		commandBuilder.append(filePath);
		final String command = commandBuilder.toString();
		Logger.debug("ssh command to execute: " + command);
		return command;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Object source = event.getSource();
		if (source instanceof CommonViewer) {
			this.selection = ((CommonViewer) source).getSelection();
		}
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;

	}

	public IServer getServer() {
		if (selection instanceof IStructuredSelection) {
			final Object selectedItem = ((IStructuredSelection) selection).getFirstElement();
			if (selectedItem instanceof IServer) {
				return ((IServer) selectedItem);
			}
			if (selectedItem instanceof IServerModule) {
				return ((IServerModule) selectedItem).getServer();
			}
		}
		return null;
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

}
