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
package org.jboss.tools.openshift.internal.common.ui.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;

/**
 * @author Andre Dietisheim
 */
public class StyledTextUtils {

	// Ex. "This is a text with a <a>link</a> in it"
	private static Pattern LINK_REGEX = Pattern.compile("([^<]*)(<a>)?([^<]*)(<\\/a>)?(.*)");

	private StyledTextUtils() {
	}

	/**
	 * Configures the given styled text so that it looks and behaves as if it was a link widget. 
	 * 
	 * @param text
	 * @param styledText
	 * 
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=471717">Some link objects don't work on Mac</a>
	 * @see <a href="https://issues.jboss.org/browse/JBIDE-20092">Connection wizard: hitting retrivial link with mouse does not do anything on OSX (hitting enter works)</a>
	 */
	public static StyledText emulateLinkWidget(String text, StyledText styledText) {
		setLinkText(text, styledText);
		setTransparent(styledText);
		styledText.setEditable(false);
		styledText.setCursor(new Cursor(styledText.getShell().getDisplay(), SWT.CURSOR_HAND));
		
		//emulate disablement
		styledText.setCaret(null);
		styledText.setSelectionBackground(styledText.getBackground()); //even with selection listener, prevent 'shimmering'
		styledText.setSelectionForeground(styledText.getForeground()); //even with selection listener, prevent 'shimmering'
		styledText.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Point s = styledText.getSelection();
				if(s != null && s.x != s.y) {
					styledText.setSelection(s.x, s.x);
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		return styledText;
	}

	public static void emulateLinkAction(final StyledText styledText, EmulatedLinkClickListener listener) {
		Listener mouseListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				int offset = getOffsetAtEvent(styledText, event);
				if(offset < 0) {
					return;
				}
				StyleRange r = styledText.getStyleRangeAtOffset(offset);
				if(event.type == SWT.MouseUp) {
					if(r != null) {
						listener.handleClick(r);
					}
				} else if(event.type == SWT.MouseMove) {
					if(r != null) {
						styledText.setCursor(styledText.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
					} else {
						styledText.setCursor(styledText.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
					}
				}

			}
		};
		styledText.addListener(SWT.MouseMove, mouseListener);
		styledText.addListener(SWT.MouseUp, mouseListener);
	}

	private static int getOffsetAtEvent(StyledText styledText, Event event) {
		Point p = new Point(event.x, event.y);
		int offset = -1;
		try {
			offset = styledText.getOffsetAtLocation(p);
		} catch (IllegalArgumentException e) {
			//ignore
		}
		if(offset < 0 || offset >= styledText.getCharCount()) {
			return -1;
		}
		return offset;
	}

	public static interface EmulatedLinkClickListener {
		public void handleClick(StyleRange range);
	}

	/**
	 * Sets a given text (with link markers <a></a>) to a given styled text.
	 * Applies a link-styled alike style range to the text within the markers
	 * while removing the markers.
	 * This is a  replacemenet for the link widget which is not clickable in on MacOS in certain circumstances.
	 *
	 * Ex. "This is a text with a {@code<a>link</a>} in it"
	 *
	 * @param text
	 * @param styledText
	 *
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=471717">Some link objects don't work on Mac</a>
	 * @see <a href="https://issues.jboss.org/browse/JBIDE-20092">Connection wizard: hitting retrivial link with mouse does not do anything on OSX (hitting enter works)</a>
	 */
	public static void setLinkText(String text, StyledText styledText) {
		Matcher matcher = LINK_REGEX.matcher(text);
		if (!matcher.matches()) {
			styledText.setText(text);
			return;
		}

		if (StringUtils.isEmpty(matcher.group(2))
				&& StringUtils.isEmpty(matcher.group(4))) {
			styledText.setText(text);
		} else {
			styledText.setText(removeLinkMarkers(matcher));
			int start = matcher.group(1).length();
			int stop = start + matcher.group(3).length();
			StyleRange linkStyle = createLinkStyle(start, stop, styledText.getShell());
			styledText.setStyleRange(linkStyle);
		}
	}

	private static String removeLinkMarkers(Matcher matcher) {
		StringBuffer buffer = new StringBuffer();
		matcher.appendReplacement(buffer, "$1$3$5");
		return buffer.toString();
	}

	public static StyleRange createLinkStyle(int start, int stop, Shell shell) {
		Assert.isLegal(shell != null && !shell.isDisposed());

		StyleRange styleRange = new StyleRange();
		styleRange.start = start;
		styleRange.length = stop - start;
		styleRange.fontStyle = SWT.UNDERLINE_LINK;
		styleRange.underline = true;
		styleRange.foreground = shell.getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND);
		return styleRange;
	}

	/**
	 * Causes the given StyledText to be transparent. Uses a transparent
	 * background image (since SWT flags wont work for StyledText) or sets the
	 * widget to the default widget background color. Different strategies are
	 * used for the different platforms.
	 *
	 * @param styledText
	 *            the styled text widget that shall get transparent background
	 *
	 * @see Control#setBackgroundImage(org.eclipse.swt.graphics.Image)
	 * @see StyledText
	 */
	public static void setTransparent(StyledText styledText) {
		if (Platform.WS_COCOA.equals(Platform.getWS())) {
			// MacOS has no default widget background in groups (JBIDE-16913)
			styledText.setBackgroundImage(OpenShiftCommonImages.TRANSPARENT_PIXEL_IMG);
		} else {
			// RHEL 6.5 cannot display transparent pixels in images (neither
			// png, gif, bmp, etc.)
			// Win8 cannot display transparent pixels in images (JBIDE-18704)
			styledText.setBackground(styledText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		}
	}

	public static StyleRange createBoldStyle(String string, Color background) {
		StyleRange styleRange = new StyleRange();
		styleRange.fontStyle = SWT.BOLD;
		if (background != null) {
			styleRange.background = background;
		}
		styleRange.start = 0;
		styleRange.length = string.length();
		return styleRange;
	}
}
