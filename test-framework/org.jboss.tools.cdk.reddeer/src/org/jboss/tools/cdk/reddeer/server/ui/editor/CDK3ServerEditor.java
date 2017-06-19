/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.ui.editor;

import org.jboss.reddeer.core.matcher.WithTextMatcher;
import org.jboss.reddeer.swt.api.Button;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.combo.LabeledCombo;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.reddeer.uiforms.impl.section.DefaultSection;

/**
 * Class representing CDK3 Server Editor part
 * @author odockal
 *
 */
public class CDK3ServerEditor extends CDEServerEditor {
	
	public CDK3ServerEditor(String title) {
		super(title);
	}

	public LabeledText getMinishiftBinaryLabel() {
		return new LabeledText(new DefaultSection(CDK_DETAILS),"Minishift Binary: ");
	}
	
	public LabeledCombo getHypervisorCombo() {
		return new LabeledCombo(new DefaultSection(CDK_DETAILS), "Hypervisor:");
	}

	public LabeledText getMinishiftHomeLabel() {
		return new LabeledText(new DefaultSection(CDK_DETAILS), "Minishift Home:");
	}

	public Button getMinishiftBinaryBrowseButton() {
		return new PushButton(new DefaultSection(CDK_DETAILS), 0, new WithTextMatcher("Browse..."));
	}
	
	public Button getMinishiftHomeBrowseButton() {
		return new PushButton(new DefaultSection(CDK_DETAILS), 1, new WithTextMatcher("Browse..."));
	}
	
}
