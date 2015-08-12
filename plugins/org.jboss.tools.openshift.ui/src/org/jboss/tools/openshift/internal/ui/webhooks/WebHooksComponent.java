/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.webhooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.build.BuildTriggerType;
import com.openshift.restclient.model.build.IBuildTrigger;
import com.openshift.restclient.model.build.IWebhookTrigger;

/**
 * @author Fred Bricon
 * @author Andre Dietisheim
 */
public class WebHooksComponent extends Composite {

	private IBuildConfig buildConfig;

	public WebHooksComponent(IBuildConfig buildConfig, Composite parent, int style) {
		super(parent, style);
		this.buildConfig = buildConfig;
		
		createControls(buildConfig, parent);
	}

	private void createControls(IBuildConfig buildConfig, Composite parent) {
		GridLayoutFactory.fillDefaults()
			.applyTo(this);

		Label webhookExplanation = new Label(this, SWT.WRAP);
		webhookExplanation.setText("Webhook triggers allow you to trigger a new build by sending a request to the OpenShift API endpoint.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false)
			.applyTo(webhookExplanation);

		Group hooksGroup = new Group(this, SWT.None);
		hooksGroup.setText("Web Hooks");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(hooksGroup);
		GridLayoutFactory.fillDefaults()
			.numColumns(3).margins(10, 10)
			.applyTo(hooksGroup);

		createHookWidgets(getWebHooks(buildConfig), hooksGroup);
	}

	private void createHookWidgets(List<IWebhookTrigger> webHooks, Composite parent) {
		if (webHooks == null
				|| webHooks.isEmpty()) {
			createNoHooksMessage(parent);
		} else {
			for (IWebhookTrigger webHook : webHooks) {
				createWebhookWidget(webHook, parent);
			}
		}
	}

	private void createNoHooksMessage(Composite parent) {
		Label noHooksLabel = new Label(parent, SWT.NONE);
		noHooksLabel.setText("You have no hooks configured for your build config.");
	}

	private void createWebhookWidget(IWebhookTrigger webHook, Composite parent) {
		Link link = new Link(parent, SWT.NONE);
		link.addSelectionListener(onClickWebhook());
		String linkLabel = isGitHub(webHook) ? "<a>" + webHook.getType() + "</a>" : webHook.getType();
		link.setText(linkLabel + " web hook:");
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
		copyToClipboard.setToolTipText("Copy To Clipboard");
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

	private SelectionAdapter onClickWebhook() {
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
	}

	private boolean isGitHub(IWebhookTrigger webHook) {
		if (buildConfig == null
				|| buildConfig.getBuildSource() == null
				|| StringUtils.isEmpty(buildConfig.getBuildSource().getURI()) 
				|| !buildConfig.getBuildSource().getURI().startsWith("https://github.com/")) {
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

	public List<IWebhookTrigger> getWebHooks(IBuildConfig buildConfig) {
		List<IBuildTrigger> triggers = buildConfig.getBuildTriggers();
		List<IWebhookTrigger> webHooks = null;
		if (triggers == null || triggers.isEmpty()) {
			webHooks = Collections.emptyList();
		} else {
			webHooks = new ArrayList<>(triggers.size());
			for (IBuildTrigger trigger : triggers) {
				IWebhookTrigger webHook = getAsWebHook(trigger);
				if (webHook != null) {
					webHooks.add(webHook);
				}
			}
		}
		return webHooks;
	}

	private IWebhookTrigger getAsWebHook(IBuildTrigger trigger) {
		switch (trigger.getType()) {
		case BuildTriggerType.generic:
		case BuildTriggerType.GENERIC:
		case BuildTriggerType.github:
		case BuildTriggerType.GITHUB:
			return (IWebhookTrigger) trigger;
		default:
			return null;
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
