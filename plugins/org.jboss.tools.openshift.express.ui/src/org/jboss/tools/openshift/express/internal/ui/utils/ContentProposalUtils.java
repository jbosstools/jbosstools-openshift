/*******************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.preferencevalue.StringsPreferenceValue;

/**
 * @author Andre Dietisheim
 */
public class ContentProposalUtils {

	public static ContentProposalAdapter createPreferencesBacked(final Text text, String preferencesKey,
			String pluginId) {
		final ControlDecoration decoration = createContenProposalDecoration("History available", text);
		final StringsPreferenceValue preferencesValues =
				new StringsPreferenceValue(',', preferencesKey, pluginId);
		ContentProposalAdapter proposalAdapter = createContentProposal(text, preferencesValues.get());
		text.addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				decoration.show();
			}

			@Override
			public void focusLost(FocusEvent e) {
				decoration.hide();
				String value = text.getText();
				if (value != null && value.length() > 0) {
					preferencesValues.add(text.getText());
				}
			}

		});
		return proposalAdapter;
	}

	public static ContentProposalAdapter createContentProposal(final Text text, String[] proposedValues) {
		SimpleContentProposalProvider proposalProvider = new SimpleContentProposalProvider(proposedValues);
		proposalProvider.setFiltering(true);
		KeyStroke keyStroke = KeyStroke.getInstance(SWT.CONTROL, ' ');
		ContentProposalAdapter proposalAdapter =
				new ContentProposalAdapter(text, new TextContentAdapter(), proposalProvider, keyStroke, null);
		proposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		return proposalAdapter;
	}

	public static ControlDecoration createContenProposalDecoration(String tooltip, Control control) {
		return createDecoration(tooltip, FieldDecorationRegistry.DEC_CONTENT_PROPOSAL, SWT.RIGHT | SWT.TOP,
				control);
	}

	private static ControlDecoration createDecoration(String text, String fieldDecorationImageKey, int position,
			Control control) {
		ControlDecoration decoration = new ControlDecoration(control, position);
		Image icon = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(fieldDecorationImageKey).getImage();
		decoration.setImage(icon);
		decoration.setDescriptionText(text);
		decoration.setShowHover(true);
		decoration.hide();
		return decoration;
	}
}
