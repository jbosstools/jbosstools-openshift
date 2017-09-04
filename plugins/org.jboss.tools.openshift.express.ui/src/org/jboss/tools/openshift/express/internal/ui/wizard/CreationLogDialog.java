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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
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
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressImages;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;

/**
 * @author André Dietisheim
 */
public class CreationLogDialog extends TitleAreaDialog {

    private static final Pattern HTTP_LINK_REGEX = Pattern.compile("(http[^ |\n]+)");

    private List<Link> links;
    private LogEntry[] logEntries;

    public CreationLogDialog(Shell parentShell, LogEntry[] logEntries) {
        super(parentShell);
        this.logEntries = logEntries;
        this.links = new ArrayList<>();
        setHelpAvailable(false);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        setupDialog(parent);
        return control;
    }

    private void setupDialog(Composite parent) {
        parent.getShell().setText("Embedded Cartridges");
        setTitle(
                "Please make note of the credentials and url that were reported\nwhen your cartridges were embedded / application was created. ");
        setTitleImage(ExpressImages.OPENSHIFT_LOGO_WHITE_MEDIUM_IMG);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
        GridLayoutFactory.fillDefaults().margins(6, 6).applyTo(container);

        Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).applyTo(separator);

        StyledText logText = new StyledText(container, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, true).hint(400, SWT.DEFAULT).applyTo(logText);
        logText.setEditable(false);
        writeLogEntries(logEntries, logText);
        logText.addListener(SWT.MouseDown, onLinkClicked(logText));
        return container;
    }

    private Listener onLinkClicked(final StyledText logText) {
        return new Listener() {
            @Override
            public void handleEvent(Event event) {
                try {
                    int offset = logText.getOffsetAtLocation(new Point(event.x, event.y));
                    Link link = getLink(offset);
                    if (link == null || !isLinkStyle(offset)) {
                        return;
                    }
                    try {
                        link.execute();
                    } catch (Exception e) {
                        ExpressUIActivator.log(e);
                        MessageDialog.openError(getShell(), "Could not execute link", e.getMessage());
                    }
                } catch (IllegalArgumentException e) {
                    // intentional swallowing: empty line clicked
                }
            }

            private boolean isLinkStyle(int offset) {
                StyleRange style = logText.getStyleRangeAtOffset(offset);
                return style != null && style.underline;
            }

        };
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    private void writeLogEntries(LogEntry[] logEntries, StyledText logText) {
        List<StyleRange> styles = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        for (LogEntry logEntry : logEntries) {
            createTextAndStyle(logEntry, builder, styles);
        }

        logText.setText(builder.toString());

        setStyleRanges(logText, styles);

    }

    private void createTextAndStyle(LogEntry logEntry, StringBuilder builder, List<StyleRange> styles) {
        if (logEntry == null) {
            return;
        }
        appendTitle(logEntry.getName(), builder, styles);
        appendLog(logEntry, builder, styles);

    }

    private void appendLog(LogEntry logEntry, StringBuilder builder, List<StyleRange> styles) {
        String log = logEntry.getLog();
        if (logEntry.isTimeouted) {
            builder.append(NLS.bind("<The request timed out but we could create {0}. "
                    + "You can look up eventual credentials in the environment variables>", logEntry.getName()));
        } else if (StringUtils.isEmpty(log)) {
            builder.append("<no information reported by OpenShift>");
        } else {
            createUrlLinks(log, builder.length(), styles);
            builder.append(log);
        }
        builder.append(StringUtils.getLineSeparator());
    }

    private void createUrlLinks(String log, int baseIndex, List<StyleRange> styles) {
        if (log == null) {
            return;
        }
        Matcher matcher = HTTP_LINK_REGEX.matcher(log);
        while (matcher.find() && matcher.groupCount() == 1) {
            int linkStart = matcher.start() + baseIndex;
            int linkStop = matcher.end() + baseIndex;

            String url = matcher.group(1);
            Link linkEntry = new UrlLink(linkStart, linkStop, url);
            links.add(linkEntry);

            StyleRange linkStyle = StyledTextUtils.createLinkStyle(linkStart, linkStop, getShell());
            styles.add(linkStyle);
        }
    }

    private void appendTitle(String title, StringBuilder builder, List<StyleRange> styles) {
        StyleRange styleRange = startBoldStyleRange(builder);
        builder.append(title).append(StringUtils.getLineSeparator()).append("---------------------------------")
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
        private boolean isTimeouted;
        private Object element;

        public LogEntry(String name, String log, boolean isTimeouted, Object element) {
            this.name = name;
            this.log = log;
            this.isTimeouted = isTimeouted;
            this.element = element;
        }

        public String getName() {
            return name;
        }

        public String getLog() {
            return log;
        }

        public boolean isTimeouted() {
            return isTimeouted;
        }

        public Object getElement() {
            return element;
        }
    }

    /**
     * Gets the link at the given index within the log. Returns <code>null</code>
     * if none was found. Looks through the links that were found when parsing
     * the log.
     * 
     * @param offset
     * @return the links
     * 
     * @see #createLinks
     */
    private Link getLink(int offset) {
        for (Link link : links) {
            if (offset < link.getStopOffset() && offset > link.getStartOffset()) {
                return link;
            }
        }
        return null;
    }

    /**
     * A links within the log text.
     */
    private abstract static class Link {

        private int startOffset;
        private int stopOffset;

        public Link(int startOffset, int stopOffset) {
            this.startOffset = startOffset;
            this.stopOffset = stopOffset;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getStopOffset() {
            return stopOffset;
        }

        public abstract void execute() throws Exception;
    }

    private static class UrlLink extends Link {

        private String url;

        public UrlLink(int startOffset, int stopOffset, String url) {
            super(startOffset, stopOffset);
            this.url = url;
        }

        @Override
        public void execute() {
            if (url == null || url.length() == 0) {
                return;
            }
            BrowserUtil.checkedCreateExternalBrowser(url, ExpressUIActivator.PLUGIN_ID, ExpressUIActivator.getDefault().getLog());
        }
    }

}
