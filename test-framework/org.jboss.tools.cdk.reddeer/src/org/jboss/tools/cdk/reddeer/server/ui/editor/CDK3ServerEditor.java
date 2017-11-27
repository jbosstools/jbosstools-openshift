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

import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.text.LabeledText;

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
		return new LabeledText(getCDKSection(), "Minishift Binary: ");
	}
	
	public LabeledCombo getHypervisorCombo() {
		return new LabeledCombo(getCDKSection(), "Hypervisor:");
	}

	public LabeledText getMinishiftHomeLabel() {
		return new LabeledText(getCDKSection(), "Minishift Home:");
	}

	public Button getMinishiftBinaryBrowseButton() {
		return new PushButton(getCDKSection(), 0, new WithTextMatcher("Browse..."));
	}
	
	public Button getMinishiftHomeBrowseButton() {
		return new PushButton(getCDKSection(), 1, new WithTextMatcher("Browse..."));
	}
	
}
