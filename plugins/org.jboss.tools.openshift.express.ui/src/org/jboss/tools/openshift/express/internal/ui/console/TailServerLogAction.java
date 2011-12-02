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
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;
import org.jboss.tools.openshift.express.internal.client.utils.Base64Encoder;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.console.TailServerLogWorker.MyLogger;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.utils.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

public class TailServerLogAction extends Action implements ISelectionChangedListener {

	/** The current selection in the view. */
	private ISelection selection = null;

	/** The threads that provide the 'log tail' in the console view. */
	private Map<String, TailServerLogWorker> tailRunners = new HashMap<String, TailServerLogWorker>();

	/**
	 * Constructor
	 */
	public TailServerLogAction() {
		super(OpenShiftExpressUIMessages.TAIL_SERVER_LOG_ACTION);
		IViewRegistry reg = PlatformUI.getWorkbench().getViewRegistry();
		IViewDescriptor desc = reg.find(IConsoleConstants.ID_CONSOLE_VIEW);
		setImageDescriptor(desc.getImageDescriptor());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		final IServer server = getServer();
		if (ExpressServerUtils.isOpenShiftRuntime(server)) {
			// start a new thread to which we delegate the remote shell
			// connection + tail command
			final String serverId = server.getId();
			MessageConsole console = ConsoleUtils.findMessageConsole(server.getId());
			if (!tailRunners.containsKey(serverId)) {
				TailServerLogWorker tailServerLogRunner;
				try {
					final Process process = startTailProcess(server); 
					tailServerLogRunner = new TailServerLogWorker(server, console, process);
					tailRunners.put(serverId, tailServerLogRunner);
					Thread thread = new Thread(tailServerLogRunner);
					thread.start();
				} catch (Exception e) {
					Logger.error("Failed to retrieve remote server logs", e);
				}
			}
			ConsoleUtils.displayConsoleView(console);
		}
	}

	private Process startTailProcess(IServer server) throws JSchException, IOException {
		final String host = server.getHost();
		final String appId = ExpressServerUtils.getExpressApplicationId(server);
		final String appName = ExpressServerUtils.getExpressApplicationName(server);
		final String logFilePath = appName + "/logs/server.log";
		final String options = new String(Base64Encoder.encode("-f -n 100".getBytes("UTF-8")), "UTF-8");

		
		JSch.setLogger(new MyLogger());
		final SshSessionFactory sshSessionFactory = SshSessionFactory.getInstance();
		final URIish uri = new URIish().setHost(host).setUser(appId);
		final RemoteSession remoteSession = sshSessionFactory.getSession(uri, CredentialsProvider.getDefault(), FS.DETECTED, 100000);
		
		// the rhc-tail-files command template
		// ssh_cmd =
		// "ssh -t #{app_uuid}@#{app}-#{namespace}.#{rhc_domain} 'tail#{opt['opts'] ? ' --opts ' + Base64::encode64(opt['opts']).chomp : ''} #{file_glob}'"
		final String command = buildCommand(logFilePath, options);
		Process process = remoteSession.exec(command, 0);
		return process;
	}

	private String buildCommand(final String filePath, final String options) throws UnsupportedEncodingException {
		StringBuilder commandBuilder = new StringBuilder("tail ");
		if (options != null && !options.isEmpty()) {
			final String opts = new String(Base64Encoder.encode(options.getBytes("UTF-8")), "UTF-8");
			commandBuilder.append("--opts ").append(opts).append(" ");
		}
		commandBuilder.append(filePath);
		final String command = commandBuilder.toString();
		System.out.println("cmd= '" + command + "'");
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

	public IServerModule getServerModule() {
		if (selection instanceof IServerModule)
			return ((IServerModule) selection);
		return null;
	}

}
