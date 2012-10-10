/*******************************************************************************
 * Copyright (c) 2011 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * @author Rob Stryker
 */
public class OpenshiftLaunchTabGroup extends AbstractLaunchConfigurationTabGroup {

	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		setTabs(createTabs2(dialog, mode));
	}

	public ILaunchConfigurationTab[] createTabs2(ILaunchConfigurationDialog dialog, String mode) {
		return new ILaunchConfigurationTab[] {
				new OpenshiftDefaultLaunchTab()
		};
	}

	public class OpenshiftDefaultLaunchTab extends AbstractLaunchConfigurationTab {

		public void createControl(Composite parent) {
			Font font = parent.getFont();
			Composite comp = SWTFactory.createComposite(parent, font, 1, 1, GridData.FILL_BOTH);
			GridLayout layout = new GridLayout();
			layout.verticalSpacing = 0;
			comp.setLayout(layout);
			Label l = new Label(comp, SWT.NONE);
			l.setText("OpenShift launches are not currently supported");
			setControl(comp);
		}

		@Override
		public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
			// TODO Auto-generated method stub

		}

		@Override
		public void initializeFrom(ILaunchConfiguration configuration) {
			// TODO Auto-generated method stub

		}

		@Override
		public void performApply(ILaunchConfigurationWorkingCopy configuration) {
			// TODO Auto-generated method stub

		}

		@Override
		public String getName() {
			return "OpenShift Launch";
		}

	}
}
