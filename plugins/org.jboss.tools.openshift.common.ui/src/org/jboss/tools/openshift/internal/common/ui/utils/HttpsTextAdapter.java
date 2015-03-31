package org.jboss.tools.openshift.internal.common.ui.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Text;

public class HttpsTextAdapter {

	private static final String HTTP_SCHEME = "http://";
	private static final String HTTPS_SCHEME = "https://";
	
	public HttpsTextAdapter() {
	}

	public void addTo(Text text) {
		text.addKeyListener(onKeyReleased());
	}

	protected KeyListener onKeyReleased() {
		return new KeyAdapter() {
			
			
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.CR
						|| e.character == SWT.TAB
						|| e.character == SWT.DEL
						|| e.character == SWT.BS
						|| e.character == SWT.ALT
						|| e.character == SWT.CTRL
						|| e.character == SWT.SHIFT
						|| e.character == SWT.ESC
						|| e.character == SWT.HOME
						|| e.character == SWT.END
						|| e.character == SWT.PAGE_DOWN
						|| e.character == SWT.PAGE_UP
						|| e.character == SWT.INSERT
						|| e.character == SWT.CURSOR_ARROW) {
					return;
				}

				Text text = (Text) e.widget;
				String serverUrl = text.getText();
				String serverUrlBeginning = serverUrl.substring(0, text.getCaretPosition());
				if (matchesAllOrSubstring(serverUrlBeginning, HTTP_SCHEME)
						|| matchesAllOrSubstring(serverUrlBeginning, HTTPS_SCHEME)) {
					return;
				}

				serverUrl = new StringBuilder()
						.append(HTTPS_SCHEME)
						.append(serverUrl)
						.toString();
				text.setText(serverUrl);
				text.setSelection(serverUrl.length());
			}

			protected boolean matchesAllOrSubstring(String inspected, String requiredBeginning) {
				return inspected.startsWith(requiredBeginning.substring(0, Math.min(requiredBeginning.length(), inspected.length())));
			}
		};
	}
}
