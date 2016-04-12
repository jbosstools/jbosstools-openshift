/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.X509CertificateParser;

public class SSLCertificateUIHelper {

	public SSLCertificateUIHelper() {
	}

	public void createTextAndStyle(X509Certificate certificate, StringBuilder builder, List<StyleRange> styles) {
		if (certificate == null) {
			return;
		}
		
		X509CertificateParser certificateParser = new X509CertificateParser(certificate);
		createTextAndStyle(certificateParser.getIssuer(), certificateParser.getValidity(), certificateParser.getFingerprint(), builder, styles);
	}

	public void writeCertificate(X509Certificate certificate, StyledText styledText) {
		if(certificate == null) {
			return;
		}
		List<StyleRange> styles = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		createTextAndStyle(certificate, builder, styles);
		styledText.setText(builder.toString());
		setStyleRanges(styledText, styles);
	}

	public void writeCertificate(String issuedBy, String validity, String fingerprint, StyledText styledText) {
		clean(styledText);
		List<StyleRange> styles = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		createTextAndStyle(issuedBy, validity, fingerprint, builder, styles);
		styledText.setText(builder.toString());
		setStyleRanges(styledText, styles);
	}

	public void clean(StyledText styledText) {
		setStyleRanges(styledText, new ArrayList<StyleRange>());
		styledText.setText("");
	}

	public void createTextAndStyle(String issuedBy, String validity, String fingerprint, StringBuilder builder, List<StyleRange> styles) {
		appendLabeledValue("Issued By:\n", issuedBy, builder, styles);
		appendLabeledValue("Validity:\n", validity, builder, styles);
		appendLabeledValue("SHA1 Fingerprint:\n", fingerprint, builder, styles);
	}

	private void appendLabeledValue(String label, String value, StringBuilder builder, List<StyleRange> styles) {
		appendLabel(label, builder, styles);
		appendValue(value, builder);
	}

	private void appendValue(String value, StringBuilder builder) {
		builder
				.append(value)
				.append(StringUtils.getLineSeparator())
				.append(StringUtils.getLineSeparator());
	}

	private void appendLabel(String label, StringBuilder builder, List<StyleRange> styles) {
		StyleRange styleRange = startBoldStyleRange(builder);
		builder.append(label);
		finishBoldStyleRange(builder, styleRange);
		styles.add(styleRange);
	}

	private StyleRange startBoldStyleRange(StringBuilder builder) {
		StyleRange styleRange = new StyleRange();
		styleRange.start = builder.length();
		styleRange.fontStyle = SWT.BOLD;
		return styleRange;
	}

	private StyleRange finishBoldStyleRange(StringBuilder builder, StyleRange styleRange) {
		styleRange.length = builder.length() - styleRange.start;
		return styleRange;
	}

	private void setStyleRanges(StyledText logText, List<StyleRange> styles) {
		for (StyleRange style : styles) {
			logText.setStyleRange(style);
		}
	}

}
