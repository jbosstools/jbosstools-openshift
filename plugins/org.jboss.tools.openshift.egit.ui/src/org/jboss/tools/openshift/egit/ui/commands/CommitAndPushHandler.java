package org.jboss.tools.openshift.egit.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.egit.ui.util.CommandUtils;

public class CommitAndPushHandler extends AbstractHandler {

	private static final String EGIT_COMMIT_COMMAND_ID = "org.eclipse.egit.ui.team.Commit";
	private static final String EGIT_PUSH_COMMAND_ID = "org.eclipse.egit.ui.team.Push";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (executeCommand(EGIT_COMMIT_COMMAND_ID, selection )) {
			executeCommand(EGIT_PUSH_COMMAND_ID, selection);
		}
		return null;
	}

	private boolean executeCommand(String commandId, ISelection selection) throws ExecutionException {
		if (!(selection instanceof IStructuredSelection)) {
			throw new ExecutionException(NLS.bind("Could not execute command \"{0}\" since there's no valid selection",
					commandId));
		}
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		try {
			return CommandUtils.executeCommand(commandId, structuredSelection);
		} catch (NotDefinedException e) {
			throw new ExecutionException(NLS.bind("Could not execute command \"{0}\" since it is not defined",
					commandId), e);
		} catch (NotEnabledException e) {
			throw new ExecutionException(NLS.bind("Could not execute command \"{0}\" since it was not enabled",
					commandId), e);
		} catch (NotHandledException e) {
			throw new ExecutionException(NLS.bind(
					"Could not execute command \"{0}\" since there's no actve handler for it", commandId), e);
		}
	}
}
