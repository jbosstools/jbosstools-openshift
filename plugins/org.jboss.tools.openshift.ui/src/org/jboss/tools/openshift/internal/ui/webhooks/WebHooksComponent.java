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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.build.BuildTriggerType;
import com.openshift.restclient.model.build.IBuildTrigger;
import com.openshift.restclient.model.build.IWebhookTrigger;

public class WebHooksComponent extends Composite {

	protected List<IWebhookTrigger> webHooks;

	private Label msgArea;

	private IBuildConfig buildConfig;

	public WebHooksComponent(IBuildConfig buildConfig, Composite parent, int style) {
		super(parent, style);
		this.buildConfig = buildConfig;
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		msgArea = new Label(container, SWT.NONE);
		msgArea.setText("Click on a link to copy it to the clipboard");
		webHooks = getWebHooks(buildConfig);
		for (IWebhookTrigger webHook : webHooks) {
			createRow(container, webHook);
		}
	}

	private void createRow(Composite container, IWebhookTrigger webHook) {
		Composite row = new Composite(container, SWT.NONE);

		GridDataFactory.fillDefaults().applyTo(row);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(row);
		Link label = new Link(row, SWT.NONE);
		label.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//TODO open https://github.com/<user>/<repo>/settings/hooks
				String url = buildConfig.getBuildSource().getURI();
				new BrowserUtility().checkedCreateExternalBrowser(url,
					OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
			}
		});
		String linkLabel = isGitHub(webHook) ? "<a>" + webHook.getType() + "</a>" : webHook.getType();
		
		label.setText(linkLabel + " web hook");

		Text uri = new Text(row, SWT.NONE);
		uri.setEditable(false);
		uri.setText(webHook.getWebhookURL());
		uri.setSize(150, SWT.DEFAULT);
		uri.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				Text text = ((Text) event.widget);
				text.selectAll();
				String uriToCopy = text.getText();
				copyToClipBoard(uriToCopy);
				//msgArea.setText(uriToCopy + " was copied to the clipboard");
			}
		});

		Label label2 = new Label(row, SWT.NONE);
		label2.setText("");
	}

	private boolean isGitHub(IWebhookTrigger webHook) {
		if (!buildConfig.getBuildSource().getURI().startsWith("https://github.com/")) {
			return false;
		}
		switch (webHook.getType()) {
			case BuildTriggerType.github:
			case BuildTriggerType.GITHUB:
				return true;
		}
		return false;
	}

	static List<IWebhookTrigger> getWebHooks(IBuildConfig buildConfig) {
		List<IBuildTrigger> triggers = buildConfig.getBuildTriggers();
		List<IWebhookTrigger> webHooks;
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

	static IWebhookTrigger getAsWebHook(IBuildTrigger trigger) {
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
