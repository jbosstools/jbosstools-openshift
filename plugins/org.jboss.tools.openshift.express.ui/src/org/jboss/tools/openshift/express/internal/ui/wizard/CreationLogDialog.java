/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.BrowserUtil;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.common.StringUtils;
import org.jboss.tools.openshift.express.internal.utils.UrlUtils;

/**
 * @author André Dietisheim
 */
public class CreationLogDialog extends TitleAreaDialog {

	private static final Pattern HTTP_LINK_REGEX = Pattern.compile("http[^ |\n]+");
	
	private LogEntry[] logEntries;

	public CreationLogDialog(Shell parentShell, LogEntry... logEntries) {
		super(parentShell);
		this.logEntries = logEntries;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		setupDialog(parent);
		return control;
	}

	private void setupDialog(Composite parent) {
		parent.getShell().setText("Embedded Cartridges");
		setTitle("Please make note of the credentials and url that were reported when your cartridges were embedded / application was created. ");
		setTitleImage(OpenShiftImages.OPENSHIFT_LOGO_WHITE_MEDIUM_IMG);
		setDialogHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(6, 6).applyTo(container);

		Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(separator);

		StyledText logText = new StyledText(container, SWT.BORDER | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(logText);
		writeLogEntries(logEntries, logText);
		logText.addListener(SWT.MouseDown, onLinkClicked(logText));
		return container;
	}

	private Listener onLinkClicked(final StyledText logText) {
		return new Listener() {
			public void handleEvent(Event event) {
				try {
					String url = getUrl(logText, event);
					if (url != null
							&& url.length() > 0) {
						BrowserUtil.checkedCreateExternalBrowser(
								url, OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
					}
				} catch (IllegalArgumentException e) {
					// no character under event.x, event.y
				}
			}

			private String getUrl(StyledText logText, Event event) {
				int offset = logText.getOffsetAtLocation(new Point(event.x, event.y));
				StyleRange style = logText.getStyleRangeAtOffset(offset);
				if (style == null
						|| !style.underline) {
					return null;
				}
				return UrlUtils.getUrl(offset, logText.getText());
			}


		};
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	private void writeLogEntries(LogEntry[] logEntries, StyledText logText) {
		List<StyleRange> styles = new ArrayList<StyleRange>();
		StringBuilder builder = new StringBuilder();

		for (LogEntry logEntry : logEntries) {
			writeLogEntry(logEntry, builder, styles);
		}

		logText.setText(builder.toString());

		setStyleRanges(logText, styles);

	}

	private void writeLogEntry(LogEntry logEntry, StringBuilder builder, List<StyleRange> styles) {
		appendTitle(logEntry.getName(), builder, styles);
		appendLog(logEntry, builder, styles);
	}

	private void appendLog(LogEntry logEntry, StringBuilder builder, List<StyleRange> styles) {
		String log = logEntry.getLog();
		createLinkStyles(log, builder.length(), styles);
		builder.append(log);
		builder.append(StringUtils.getLineSeparator());
	}

	private void createLinkStyles(String log, int baseIndex, List<StyleRange> styles) {
		Matcher matcher = HTTP_LINK_REGEX.matcher(log);
		while (matcher.find()) {
			StyleRange linkStyle = createLinkStyleRange(matcher.start() + baseIndex, matcher.end() + baseIndex);
			styles.add(linkStyle);
		}
	}

	private StyleRange createLinkStyleRange(int start, int stop) {
		StyleRange styleRange = new StyleRange();
		styleRange.start = start;
		styleRange.length = stop - start;
		styleRange.fontStyle = SWT.UNDERLINE_LINK;
		styleRange.underline = true;
		styleRange.foreground = getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE);
		return styleRange;
	}

	private void appendTitle(String title, StringBuilder builder, List<StyleRange> styles) {
		StyleRange styleRange = startBoldStyleRange(builder);
		builder.append(title)
				.append(StringUtils.getLineSeparator())
				.append("---------------------------------")
				.append(StringUtils.getLineSeparator());
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

	public static class LogEntry {

		private String name;
		private String log;

		public LogEntry(String name, String log) {
			this.name = name;
			this.log = log;
		}

		public String getName() {
			return name;
		}

		public String getLog() {
			return log;
		}
	}

}
