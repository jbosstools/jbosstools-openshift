package org.jboss.tools.openshift.internal.ui.webhooks;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.openshift.restclient.model.IBuildConfig;

public class WebHooksDialog extends Dialog {

	private IBuildConfig buildConfig;

	public WebHooksDialog(Shell parent, IBuildConfig buildConfig) {
		super(parent);
		this.buildConfig = buildConfig;
		setShellStyle(getShellStyle() | SWT.RESIZE); 
	}

	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText("Copy Web Hooks urls");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		WebHooksComponent webHookComponent = new WebHooksComponent(buildConfig, container, SWT.NONE);
		webHookComponent.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		return container;
	}

}
