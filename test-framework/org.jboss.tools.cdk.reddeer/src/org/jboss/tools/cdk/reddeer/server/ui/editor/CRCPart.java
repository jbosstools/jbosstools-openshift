/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
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
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * 
 * @author odockal
 *
 */
public interface CRCPart {
	
	public DefaultSection getCDKDefaultSection();
	
	public default LabeledText getCRCBinary() {
		return new LabeledText(getCDKDefaultSection(), CDKLabel.Labels.CRC_BINARY_EDITOR);
	}	

	public default LabeledText getCRCPullSecretFile() {
		return new LabeledText(getCDKDefaultSection(), CDKLabel.Labels.CRC_PULL_SECRET_FILE_EDITOR);
	}

	public default Button getCRCBinaryBrowseButton() {
		return new PushButton(getCDKDefaultSection(), 0, new WithTextMatcher(CDKLabel.Buttons.BROWSE));
	}
	
	public default Button getCRCPullSecretFileBrowseButton() {
		return new PushButton(getCDKDefaultSection(), 1, new WithTextMatcher(CDKLabel.Buttons.BROWSE));
	}	
}
