/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.ui.editor;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.wst.server.ui.editor.ServerEditor;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.uiforms.impl.hyperlink.DefaultHyperlink;
import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * Top level representing class of CDK/Minishift server editors
 * @author odockal
 *
 */
public class MinishiftServerEditor extends ServerEditor {

	private DefaultSection generalSection;
	
	private DefaultSection cdkSection;
	
	public static final String CDK_DETAILS = CDKLabel.Sections.CDK_DETAILS;
	
	public static final String GENERAL = CDKLabel.Sections.GENERAL;
	
	public static final String CREDENTIALS = CDKLabel.Sections.CREDENTIALS;
	
	public MinishiftServerEditor(String title) {
		super(title);
		this.generalSection = new DefaultSection(GENERAL);
		this.cdkSection = new DefaultSection(CDK_DETAILS);
	}
	
	public void openLaunchConfigurationFromLink() {
		log.info("Activate launch configuration via link");
		getLaunchConfigurationHyperLink().activate();
		ShellIsAvailable launch = new ShellIsAvailable(CDKLabel.Shell.LAUNCH_CONFIG_DIALOG);
		try {
			new WaitUntil(launch, TimePeriod.DEFAULT);
		} catch (WaitTimeoutExpiredException exc) {
			log.error("WaitTimeoutExpiredException occured while waiting for Edit Configuration dialog");
		}
	}
	
	public LabeledText getHostnameLabel() {
		return new LabeledText(generalSection, CDKLabel.Labels.HOST_NAME);
	}
	
	public LabeledText getServernameLabel() {
		return new LabeledText(generalSection, CDKLabel.Labels.SERVER_NAME);
	}
	
	public DefaultHyperlink getLaunchConfigurationHyperLink() {
		return new DefaultHyperlink(generalSection, "Open launch configuration");
	}
	
	public DefaultSection getCDKSection() {
		return this.cdkSection;
	}
	
	public DefaultSection getGeneralSection() {
		return this.generalSection;
	}
	
}
