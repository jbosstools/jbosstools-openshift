/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;

/**
 * @author Andre Dietisheim
 */
public class HttpsPrefixingAdapter {

	private static final String HTTP_SCHEME = "http://";
	private static final String HTTPS_SCHEME = "https://";
	
	@SuppressWarnings("rawtypes")
	private static final Collection CHARACTERS = 
			Arrays.asList(SWT.CR, SWT.TAB, SWT.DEL, SWT.BS, SWT.ALT, SWT.CTRL, SWT.SHIFT,
					SWT.ESC, SWT.HOME, SWT.END, SWT.PAGE_DOWN, SWT.PAGE_UP, SWT.INSERT, SWT.CURSOR_ARROW);
			
	public void addTo(final Text text) {
		text.addKeyListener(new SchemePrefixingKeyAdapter() {

			@Override
			protected int getCursorPosition() {
				return text.getCaretPosition();
			}

			@Override
			protected void setCursorPosition(int pos) {
				text.setSelection(pos);
				
			}

			@Override
			protected String getText() {
				return text.getText();
			}

			@Override
			protected void setText(String string) {
				text.setText(string);
			}});
	}

	public void addTo(final Combo combo) {
		combo.addKeyListener(new SchemePrefixingKeyAdapter() {

				@Override
				protected int getCursorPosition() {
					return combo.getCaretPosition();
				}

				@Override
				protected void setCursorPosition(int pos) {
					combo.setSelection(new Point(pos, pos));
					
				}

				@Override
				protected String getText() {
					return combo.getText();
				}

				@Override
				protected void setText(String string) {
					combo.setText(string);
				}});
	}

	protected abstract class SchemePrefixingKeyAdapter extends KeyAdapter {
		
		@Override
		public void keyReleased(KeyEvent e) {
			if (!isCharacter(e.character)) {
				return;
			}

			String serverUrl = getText();
			String serverUrlBeginning = serverUrl.substring(0, getCursorPosition());
			if (matchesAllOrSubstring(serverUrlBeginning, HTTP_SCHEME)
					|| matchesAllOrSubstring(serverUrlBeginning, HTTPS_SCHEME)) {
				return;
			}

			serverUrl = new StringBuilder()
					.append(HTTPS_SCHEME)
					.append(serverUrl)
					.toString();
			setText(serverUrl);
			setCursorPosition(serverUrl.length());
		}


		private boolean isCharacter(char character) {
			return !CHARACTERS.contains(character);
		}

		protected boolean matchesAllOrSubstring(String inspected, String requiredBeginning) {
			return inspected.startsWith(requiredBeginning.substring(0, Math.min(requiredBeginning.length(), inspected.length())));
		}

		protected abstract int getCursorPosition();
		protected abstract void setCursorPosition(int pos);
		protected abstract String getText();
		protected abstract void setText(String text);
	}
}
