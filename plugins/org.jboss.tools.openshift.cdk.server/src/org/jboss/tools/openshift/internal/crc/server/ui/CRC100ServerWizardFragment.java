package org.jboss.tools.openshift.internal.crc.server.ui;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.tools.as.runtimes.integration.ui.composites.DownloadRuntimeHyperlinkComposite;
import org.jboss.tools.openshift.internal.cdk.server.core.BinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.ui.CDKServerWizardFragment;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Server;
import org.jboss.tools.runtime.ui.wizard.DownloadRuntimesTaskWizard;

public class CRC100ServerWizardFragment extends CDKServerWizardFragment {
	private String pullSecretFile;
	private Text pullSecretText;
	private Button pullSecretBrowse;
	private ControlDecoration pullSecretDecorator;
	
	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		String title = "Red Hat CodeReady Containers";
		String desc = "A server adapter representing a Red Hat CodeReady Container.";
		String label = "crc binary: ";
		return createComposite(parent, handle, title, desc, label);
	}
	protected boolean shouldCreateCredentialWidgets() {
		return false;
	}
	protected void browseHomeDirClicked() {
		browseHomeDirClicked(false);
	}

	protected String validateHomeDirectory() {
		String retString = null;
		if (homeDir == null || !(new File(homeDir)).exists()) {
			retString = "The selected file does not exist.";
		} 
		toggleHomeDecorator(retString);
		return retString;
	}
	@Override
	protected void fillTextField() {
		if (homeDir != null) {
			homeText.setText(homeDir);
		} else {
			homeDir = BinaryUtility.CRC_BINARY.findLocation();
			if (homeDir != null) {
				homeText.setText(homeDir);
			}
		}
	}
	protected Composite createComposite(Composite parent, IWizardHandle handle, String title, String desc,
			String homeLabel) {
		// boilerplate
		Composite main = setupComposite(parent, handle, title, desc);
		createDownloadWidgets(main, handle);
		createLocationWidgets(main, homeLabel);
		createSecretWidgets(main);
		validateAndPack(main);
		return main;
	}
	protected void createSecretWidgets(Composite main) {

		// Point to file / folder to run
		Label l = new Label(main, SWT.NONE);
		l.setText("CRC Pull Secret File: ");
		GridData pullSecretData = new GridData();
		pullSecretData.grabExcessHorizontalSpace = true;
		pullSecretData.horizontalAlignment = SWT.FILL;
		pullSecretData.widthHint = 100;
		pullSecretText = new Text(main, SWT.BORDER);
		pullSecretText.setLayoutData(pullSecretData);
		
		
		pullSecretBrowse = new Button(main, SWT.PUSH);
		pullSecretBrowse.setText("Browse...");
		GridData browseData = new GridData();
		browseData.grabExcessHorizontalSpace = true;
		browseData.horizontalAlignment = SWT.FILL;
		pullSecretBrowse.setLayoutData(browseData);
		pullSecretDecorator = new ControlDecoration(pullSecretText, SWT.TOP | SWT.LEFT);
		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
		Image img = fieldDecoration.getImage();
		pullSecretDecorator.setImage(img);
		pullSecretDecorator.hide(); // hiding it initially

		pullSecretText.addModifyListener(new PullSecretModifyListener());
		pullSecretBrowse.addSelectionListener(new PullSecretBrowseListener());
	}
	protected class PullSecretModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			pullSecretFile = pullSecretText.getText();
			validate();
		}
	}

	protected void togglePullSecretDecorator(String message) {
		if (message == null) {
			pullSecretDecorator.hide();
		} else {
			pullSecretDecorator.setDescriptionText(message);
			pullSecretDecorator.show();
		}
	}
	protected String findError() {
		String err = super.findError();
		if( err != null )
			return err;
		String pullSecretErr = validatePullSecret();
		if( pullSecretErr != null )
			return pullSecretErr;
		return null;
	}
	
	private String validatePullSecret() {
		String msg = null;
		if(pullSecretFile == null || !(new File(pullSecretFile)).isFile() ) {
			msg = "Please select a valid Image Pull Secret file."; 
		}
		togglePullSecretDecorator(msg);
		return msg;
	}
	protected class PullSecretBrowseListener implements SelectionListener {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			File file = pullSecretFile == null ? null : new File(pullSecretFile);
			if (file != null && !file.exists()) {
				file = null;
			}

			File f = getFile(file, pullSecretText.getShell());

			if (f != null) {
				pullSecretFile = f.getAbsolutePath();
				pullSecretText.setText(pullSecretFile);
			}
			validate();
		}
	}
	
	protected void createDownloadWidgets(Composite main, IWizardHandle handle) {
		DownloadRuntimeHyperlinkComposite hyperlink = new DownloadRuntimeHyperlinkComposite(main, 
				SWT.NONE, handle, getTaskModel()) {
			
			@Override
			protected void postDownloadRuntimeUpdateWizard(String newHomeDir) {
				handleDownloadedFile(newHomeDir);
				getWizardHandle().update();
			}

			@Override
			protected String getServerHomeFromTaskModel(DownloadRuntimesTaskWizard taskWizard) {
				String ret = (String)taskWizard.getTaskModel().getObject(DownloadRuntimesTaskWizard.UNZIPPED_SERVER_BIN);
				if( ret == null ) {
					ret = (String)taskWizard.getTaskModel().getObject(DownloadRuntimesTaskWizard.UNZIPPED_SERVER_HOME_DIRECTORY);
				}
				return ret;
			}

		};
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.END, SWT.END).applyTo(hyperlink);
	}
	protected void handleDownloadedFile(String newHome) {
		if( !homeText.isDisposed()) {
			homeText.setText(newHome);
		}
	}
	

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		exit();
		IServer s = getServerFromTaskModel();
		if (s instanceof IServerWorkingCopy) {
			IServerWorkingCopy swc = (IServerWorkingCopy) s;
			swc.setAttribute(CRC100Server.PROPERTY_BINARY_FILE, homeDir);
			swc.setAttribute(CRC100Server.PROPERTY_PULL_SECRET_FILE, pullSecretFile);
		}
	}
}
