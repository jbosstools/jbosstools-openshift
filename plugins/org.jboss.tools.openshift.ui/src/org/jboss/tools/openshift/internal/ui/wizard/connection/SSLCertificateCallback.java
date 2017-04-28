/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLSession;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.preferences.SSLCertificatesPreference;
import org.jboss.tools.openshift.internal.ui.preferences.SSLCertificatesPreference.CertificateState;

import com.openshift.restclient.ISSLCertificateCallback;

/**
 * @author André Dietisheim
 */
public class SSLCertificateCallback implements ISSLCertificateCallback {
	
	private static final boolean REMEMBER_DECISION_DEFAULT = true;

	private static Lock lock = new ReentrantLock();
	
	private boolean rememberDecision = REMEMBER_DECISION_DEFAULT;

	@Override
	public boolean allowCertificate(final X509Certificate[] certificateChain) {
		try {
			// ensure that we have only 1 ssl dialog at once
			lock.lock();
			CertificateState state = SSLCertificatesPreference.getInstance().isAllowed(certificateChain[0]);
			switch (state) {
			case ACCEPTED:
				return true;
			case REJECTED:
				return false;
			case NOT_PRESENT:
			default:
				return openCertificateDialog(certificateChain[0]);
			}
		} finally {
			lock.unlock();
		}
	}

	protected boolean openCertificateDialog(final X509Certificate certificate) {
		final AtomicBoolean atomicBoolean = new AtomicBoolean();
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				atomicBoolean.set(
						new SSLCertificateDialog(UIUtils.getShell(), certificate).open() == Dialog.OK);
				if (rememberDecision) {
					// Save the choice as soon as possible
					SSLCertificatesPreference.getInstance().addOrReplaceCertificate(certificate, atomicBoolean.get());
				}
			}
		});
		return atomicBoolean.get();
	}

	@Override
	public boolean allowHostname(String hostname, SSLSession sslSession) {
		return true;
	}

	private class SSLCertificateDialog extends TitleAreaDialog {

		private X509Certificate certificate;

		private SSLCertificateDialog(Shell parentShell, X509Certificate certificate) {
			super(parentShell);
			this.certificate = certificate;
			setHelpAvailable(false);
		}

		@Override
		protected Control createContents(Composite parent) {
			Control control = super.createContents(parent);
			setupDialog(parent);
			return control;
		}

		private void setupDialog(Composite parent) {
			parent.getShell().setText("Untrusted SSL Certificate");
			setTitle("Do you accept the following untrusted SSL certificate?");
			setTitleImage(OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_MEDIUM_IMG);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().margins(6, 6).applyTo(container);

			Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).applyTo(separator);

			StyledText certificateText = new StyledText(container, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
			certificateText.setEditable(false);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).grab(false, true).hint(400, SWT.DEFAULT).applyTo(certificateText);
			writeCertificate(certificate, certificateText);

			Button rememberCheckbox = new Button(container, SWT.CHECK);
			rememberCheckbox.setText("Remember decision (it can be changed in preferences 'OpenShift 3/SSL certificates')");
			rememberCheckbox.setSelection(rememberDecision);
			rememberCheckbox.addSelectionListener(onRememberCertificate(certificate));
			GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(rememberCheckbox);

			return container;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.YES_LABEL, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.NO_LABEL, false);
		}

		private void writeCertificate(X509Certificate certificate, StyledText styledText) {
			if (certificate == null) {
				return;
			}

			SSLCertificateUIHelper.INSTANCE.setTextAndStyle(certificate, styledText);
		}

		private SelectionListener onRememberCertificate(final X509Certificate certificate) {
			return new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					Button rememberCheckbox = ((Button) e.getSource());
					if (rememberCheckbox != null) {
						SSLCertificateCallback.this.rememberDecision = rememberCheckbox.getSelection();
					}
				}
			};
		}
	}
}
