package org.jboss.tools.openshift.cdk.server.ui.internal;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsLaunchConfigurationMessages;
import org.eclipse.ui.externaltools.internal.model.IExternalToolsHelpContextIds;
import org.eclipse.ui.externaltools.internal.program.launchConfigurations.ProgramMainTab;

public class CDKLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
	 */
	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		RefreshTab refresh = new RefreshTab();
		refresh.setHelpContextId(IExternalToolsHelpContextIds.EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_REFRESH_TAB);
		EnvironmentTab env = createEnvironmentTab();
		env.setHelpContextId(IExternalToolsHelpContextIds.EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_ENVIRONMENT_TAB);
		CommonTab common = new CommonTab();
		common.setHelpContextId(IExternalToolsHelpContextIds.EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_COMMON_TAB);
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new ProgramMainTab() {
					@Override
					public void createControl(final Composite parent) {
						super.createControl(parent);
						if (Platform.OS_LINUX.equals(Platform.getOS())) {
							getShell().addShellListener(new ShellAdapter() {
								@Override
								public void shellActivated(ShellEvent e) {
									Point size = getShell().getSize();
									getShell().pack(true);
									getShell().setSize(size);
									getShell().removeShellListener(this);
								}
							});
						}
					}
					
					protected void createLocationComponent(Composite parent) {

						Group group = new Group(parent, SWT.NONE);
						String locationLabel = getLocationLabel();
						group.setText(locationLabel);
						GridLayout layout = new GridLayout();
						layout.numColumns = 1;
						GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
						group.setLayout(layout);
						group.setLayoutData(gridData);

						locationField = new Text(group, SWT.BORDER);
						gridData = new GridData(GridData.FILL_HORIZONTAL);
						gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
						locationField.setLayoutData(gridData);
						locationField.addModifyListener(fListener);
						addControlAccessibleListener(locationField, group.getText());

						Composite buttonComposite = new Composite(group, SWT.NONE);
						layout = new GridLayout();
						layout.marginHeight = 0;
				        layout.marginWidth = 0;
						layout.numColumns = 3;
						gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
						buttonComposite.setLayout(layout);
						buttonComposite.setLayoutData(gridData);
						buttonComposite.setFont(parent.getFont());

						fileLocationButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.ExternalToolsMainTab_Brows_e_File_System____4, null);
						fileLocationButton.addSelectionListener(fListener);
						addControlAccessibleListener(fileLocationButton, group.getText() + " " + fileLocationButton.getText()); //$NON-NLS-1$
					}
					protected void createWorkDirectoryComponent(Composite parent) {
						super.createWorkDirectoryComponent(parent);
						workDirectoryField.setEnabled(false);
						fileWorkingDirectoryButton.setVisible(false);
						workspaceWorkingDirectoryButton.setVisible(false);
						variablesWorkingDirectoryButton.setVisible(false);
					}
					
				},
//			refresh,
//			new ExternalToolsBuildTab(),
			env,
//			common
		};
		setTabs(tabs);
	}
	
	private EnvironmentTab createEnvironmentTab() {
		return new EnvironmentTab() {
			@Override
			protected void createEnvironmentTable(Composite parent) {
				SWTFactory.createLabel(parent, 
						"For security purposes, passwords may not be shown in the table below. Changes to username\n or password should be performed in the server editor. Changes made here will be overwritten.", 2);
				SWTFactory.createLabel(parent, "", 2); 
				super.createEnvironmentTable(parent);
			}
		};
	}
}
