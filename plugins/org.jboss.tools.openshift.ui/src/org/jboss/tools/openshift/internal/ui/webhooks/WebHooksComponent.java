/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.webhooks;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.build.BuildTriggerType;
import com.openshift.restclient.model.build.IWebhookTrigger;

/**
 * @author Fred Bricon
 * @author Andre Dietisheim
 */
public class WebHooksComponent extends Composite {

	private static final int COPIED_NOTIFICATION_SHOW_DURATION = 2 * 1000;

	protected static final String WEBHOOKS_DOCS = "https://docs.openshift.org/latest/dev_guide/builds.html#webhook-triggers";
	
	private Collection<IBuildConfig> buildConfigs;

	public WebHooksComponent(IBuildConfig buildConfig, Composite parent, int style) {
		super(parent, style);
		this.buildConfigs = Collections.singleton(buildConfig);
		createControls(buildConfigs, parent);
	}

	public WebHooksComponent(Collection<IBuildConfig> buildConfigs, Composite parent, int style) {
	    super(parent, style);
	    this.buildConfigs = buildConfigs;
	    createControls(buildConfigs, parent);
	}

	private void createControls(Collection<IBuildConfig> buildConfigs, Composite parent) {
		GridLayoutFactory.fillDefaults()
			.applyTo(this);

		Link webhookExplanation = new Link(this, SWT.WRAP);
		webhookExplanation.setText("<a>Webhook triggers</a> allow you to trigger a new build by sending a request to the OpenShift API endpoint.");
		GridDataFactory.fillDefaults()
					   .align(SWT.FILL, SWT.FILL).grab(true, false)
					   .applyTo(webhookExplanation);
		webhookExplanation.addSelectionListener(onWebhookExplanationClicked());
		
		for (IBuildConfig buildConfig : buildConfigs) {
			Group hooksGroup = new Group(this, SWT.None);
			hooksGroup.setText("Webhooks for "+buildConfig.getSourceURI());
			GridDataFactory.fillDefaults()
						   .align(SWT.FILL, SWT.FILL)
						   .grab(true, true)
						   .applyTo(hooksGroup);
			GridLayoutFactory.fillDefaults()
							 .numColumns(3)
							 .margins(10, 10)
							 .applyTo(hooksGroup);

			createHookWidgets(buildConfig, hooksGroup);
		}
	}

	private SelectionListener onWebhookExplanationClicked() {
		return new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				new BrowserUtility().checkedCreateExternalBrowser(WEBHOOKS_DOCS,
						OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
			}
		};
	}

	private void createHookWidgets(IBuildConfig buildConfig, Composite parent) {
	  List<IWebhookTrigger> webHooks = WebhookUtil.getWebHooks(buildConfig);
	  if (webHooks == null
				|| webHooks.isEmpty()) {
			createNoHooksMessage(buildConfig, parent);
		} else {
			for (IWebhookTrigger webHook : webHooks) {
				createWebhookWidget(buildConfig, webHook, parent);
			}
		}
	}

	private void createNoHooksMessage(IBuildConfig buildConfig, Composite parent) {
		Label noHooksLabel = new Label(parent, SWT.NONE);
		noHooksLabel.setText("You have no webhooks configured for build config. "+buildConfig.getSourceURI());
	}

	private void createWebhookWidget(IBuildConfig buildConfig, IWebhookTrigger webHook, Composite parent) {
		Link link = new Link(parent, SWT.NONE);
		link.addSelectionListener(onClickWebhook(buildConfig));
		String gitUrl = buildConfig.getSourceURI();
		String linkLabel = isGitHub(gitUrl, webHook) ? "<a>" + webHook.getType() + "</a>" : webHook.getType();
		link.setText(linkLabel + " webhook:");
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER)
			.applyTo(link);

		final Text uriText = new Text(parent, SWT.BORDER);
		uriText.setEditable(false);
		uriText.setText(webHook.getWebhookURL());
		uriText.addMouseListener(onClickUriText(uriText));
		uriText.setToolTipText("Click to copy to the clipboard");
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.FILL).grab(true, false)
			.applyTo(uriText);
		
		Button copyToClipboard = new Button(parent, SWT.PUSH);
		copyToClipboard.setImage(OpenShiftImages.COPY_TO_CLIPBOARD_IMG);
		copyToClipboard.setToolTipText("Copy to clipboard");
		copyToClipboard.addSelectionListener(onClickCopyButton(uriText));
	}

	private SelectionAdapter onClickCopyButton(final Text uriText) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				copyToClipBoard(uriText);
			}
		};
	}

	private MouseAdapter onClickUriText(final Text uriText) {
		return new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				copyToClipBoard(uriText);
			}
		};
	}

	private SelectionAdapter onClickWebhook(final IBuildConfig buildConfig) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//TODO open https://github.com/<user>/<repo>/settings/hooks
				String url = buildConfig.getBuildSource().getURI();
				new BrowserUtility().checkedCreateExternalBrowser(url,
					OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
			}
		};
	}

	private void copyToClipBoard(final Text uriText) {
		uriText.selectAll();
		String uriToCopy = uriText.getText();
		copyToClipBoard(uriToCopy);
		
		notifyCopied(uriText);
	}

	private void notifyCopied(final Text uriText) {
		DefaultToolTip copiedNotification = new DefaultToolTip(uriText, ToolTip.NO_RECREATE, true);
		copiedNotification.setText("Webhook copied to clipboard");
		copiedNotification.setHideDelay(COPIED_NOTIFICATION_SHOW_DURATION);
		copiedNotification.show(uriText.getLocation());
		copiedNotification.deactivate();
	}

	private static boolean isGitHub(String gitUrl, IWebhookTrigger webHook) {
		if (gitUrl == null
				|| !gitUrl.startsWith("https://github.com/")) {
			return false;
		}

		switch (webHook.getType()) {
			case BuildTriggerType.github:
			case BuildTriggerType.GITHUB:
				return true;
			default:
				return false;
		}
	}

	private void copyToClipBoard(String url) {
		Clipboard clipboard = new Clipboard(getDisplay());
		Object[] data = new Object[] { url };
		Transfer[] dataTypes = new Transfer[] { TextTransfer.getInstance() };
		clipboard.setContents(data, dataTypes);
		clipboard.dispose();
	}

}
