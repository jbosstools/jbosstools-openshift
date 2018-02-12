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

import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * Interface with default implementation for CDK Details part of Minishift/CDK server editor
 * @author odockal
 *
 */
public interface CDKPart {

	public DefaultSection getCDKDefaultSection();
	
	public default LabeledText getMinishiftProfile() {
		return new LabeledText(getCDKDefaultSection(), CDKLabel.Labels.MINISHIFT_PROFILE);
	}	

	public default LabeledText getMinishiftBinaryLabel() {
		return new LabeledText(getCDKDefaultSection(), CDKLabel.Labels.MINISHIFT_BINARY);
	}
	
	public default LabeledCombo getHypervisorCombo() {
		return new LabeledCombo(getCDKDefaultSection(), CDKLabel.Labels.HYPERVISOR);
	}

	public default LabeledText getMinishiftHomeLabel() {
		return new LabeledText(getCDKDefaultSection(), CDKLabel.Labels.MINISHIFT_HOME);
	}

	public default Button getMinishiftBinaryBrowseButton() {
		return new PushButton(getCDKDefaultSection(), 0, new WithTextMatcher(CDKLabel.Buttons.BROWSE));
	}
	
	public default Button getMinishiftHomeBrowseButton() {
		return new PushButton(getCDKDefaultSection(), 1, new WithTextMatcher(CDKLabel.Buttons.BROWSE));
	}	
	
}
