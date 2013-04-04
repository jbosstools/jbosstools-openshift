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
import java.util.Collection;
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
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

import com.openshift.client.IApplication;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author André Dietisheim
 */
public class CreationLogDialog extends TitleAreaDialog {

	private static final Pattern HTTP_LINK_REGEX = Pattern.compile("(http[^ |\n]+)");

	private Collection<IEmbeddedCartridge> cartridges;
	private IApplication application;
	private List<LinkSubstring> linkSubstrings;
	
	public CreationLogDialog(Shell parentShell, Collection<IEmbeddedCartridge> cartridges) {
		this(parentShell);
		this.cartridges = cartridges;
	}

	public CreationLogDialog(Shell parentShell, IApplication application) {
		this(parentShell);
		this.application = application;
	}

	protected CreationLogDialog(Shell parentShell) {
		super(parentShell);
		this.linkSubstrings = new ArrayList<LinkSubstring>();
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		setupDialog(parent);
		return control;
	}

	private void setupDialog(Composite parent) {
		parent.getShell().setText("Embedded Cartridges");
		setTitle("Please make note of the credentials and url that were reported\nwhen your cartridges were embedded / application was created. ");
		setTitleImage(OpenShiftImages.OPENSHIFT_LOGO_WHITE_MEDIUM_IMG);
		setDialogHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(6, 6).applyTo(container);

		Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).applyTo(separator);

		StyledText logText = new StyledText(container, SWT.BORDER | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(logText);
		writeLogEntries(createLogEntries(), logText);
		logText.addListener(SWT.MouseDown, onLinkClicked(logText));
		return container;
	}

	private LogEntry[] createLogEntries() {
		if (cartridges != null) {
			return createLogEntries(cartridges);
		} else {
			return createLogEntries(application);
		}
	}

	private LogEntry[] createLogEntries(Collection<IEmbeddedCartridge> cartridges) {
		if (cartridges == null
				|| cartridges.isEmpty()) {
			return new LogEntry[] {};
		}
		ArrayList<LogEntry> logEntries = new ArrayList<LogEntry>();
		for (IEmbeddedCartridge cartridge : cartridges) {
			logEntries.add(new LogEntry(cartridge.getName(), cartridge.getCreationLog()));
		}
		return logEntries.toArray(new LogEntry[cartridges.size()]);
	}
	
	private LogEntry[] createLogEntries(IApplication application) {
		if (application == null) {
			return new LogEntry[] {};
		}
		return new LogEntry[] { new LogEntry(application.getName(), application.getCreationLog()) };
	}

	private Listener onLinkClicked(final StyledText logText) {
		return new Listener() {
			public void handleEvent(Event event) {
				try {
					String url = getUrl(logText, event);
					if (url != null && url.length() > 0) {
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
				return CreationLogDialog.this.getUrl(offset);
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

	private void appendLog(LogEntry logEntry, StringBuilder builder,
			List<StyleRange> styles) {
		String log = logEntry.getLog();
		createLinks(log, builder.length(), styles);
		builder.append(log);
		builder.append(StringUtils.getLineSeparator());
	}

	private void createLinks(String log, int baseIndex, List<StyleRange> styles) {
		if(log != null) {
			Matcher matcher = HTTP_LINK_REGEX.matcher(log);
			while (matcher.find() 
					&& matcher.groupCount() == 1) {
				int linkStart = matcher.start() + baseIndex;
				int linkStop = matcher.end() + baseIndex;
				StyleRange linkStyle = createLinkStyleRange(linkStart, linkStop);
				styles.add(linkStyle);
				String url = matcher.group(1);
				LinkSubstring linkEntry = 
						new LinkSubstring(linkStart, linkStop, url);
				linkSubstrings.add(linkEntry);
			}
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

	/**
	 * Gets the url at the given index within the log. Returns <code>null</code>
	 * if none was found. Looks through the links that were found when parsing
	 * the log.
	 * 
	 * @param offset
	 * @return the link
	 * 
	 * @see #createLinks
	 */
	private String getUrl(int offset) {
		String url = null;
		for (LinkSubstring linkSubstring : linkSubstrings) {
			if (offset < linkSubstring.getStopOffset()
					&& offset > linkSubstring.getStartOffset()) {
				url = linkSubstring.getUrl();
				break;
			}
		}
		return url;
	}

	/**
	 * A link within the log text.
	 */
	private static class LinkSubstring {

		private int startOffset;
		private int stopOffset;
		private String url;

		public LinkSubstring(int startOffset, int stopOffset, String url) {
			this.startOffset = startOffset;
			this.stopOffset = stopOffset;
			this.url = url;
		}

		private int getStartOffset() {
			return startOffset;
		}

		private int getStopOffset() {
			return stopOffset;
		}

		private String getUrl() {
			return url;
		}
	}

}
