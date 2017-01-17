/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;
import org.jboss.reddeer.core.handler.WidgetHandler;
import org.jboss.reddeer.core.util.Display;
import org.jboss.reddeer.core.util.ResultRunnable;
import org.jboss.reddeer.swt.impl.styledtext.DefaultStyledText;

/**
 * Class to handle StyledText emulated as links.
 * 
 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=471717">Some
 *      link objects don't work on Mac</a>
 * @see <a href="https://issues.jboss.org/browse/JBIDE-20092">Connection
 *      wizard: hitting retrivial link with mouse does not do anything on
 *      OSX (hitting enter works)</a>
 * @author rhopp
 *
 */

public class EmulatedLinkStyledText extends DefaultStyledText {

	public EmulatedLinkStyledText(String text) {
		super(text);
	}

	/**
	 * Click at given offset.
	 * 
	 * @param offset
	 */
	public void click(final int offset) {
		Event e = createEvent(swtWidget);
		Point p = getLocationAtOffset(offset);
		e.x = p.x;
		e.y = p.y;
		WidgetHandler.getInstance().notify(SWT.MouseUp, e, swtWidget);
	}

	// returns point (x,y) of offset in this StyledText widget.
	private Point getLocationAtOffset(final int offset) {
		return Display.syncExec(new ResultRunnable<Point>() {

			@Override
			public Point run() {
				return swtWidget.getLocationAtOffset(offset);
			}
		});
	}

	private Event createEvent(Widget widget) {
		Event event = new Event();
		event.time = (int) System.currentTimeMillis();
		event.widget = widget;
		event.display = Display.getDisplay();
		event.type = SWT.MouseUp;
		event.button = 1;
		return event;
	}
}