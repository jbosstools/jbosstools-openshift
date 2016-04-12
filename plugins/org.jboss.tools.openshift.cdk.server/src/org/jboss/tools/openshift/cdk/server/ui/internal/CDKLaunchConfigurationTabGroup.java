package org.jboss.tools.openshift.cdk.server.ui.internal;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
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
						"For security purposes, passwords may not be shown in the table below.", 2);
				SWTFactory.createLabel(parent, "", 2); 
				super.createEnvironmentTable(parent);
			}
		};
	}
}
