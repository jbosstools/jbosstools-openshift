/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.widget.terminal;

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.swt.widgets.AbstractControl;
import org.hamcrest.Matcher;

/**
 * @author Red Hat Developers
 *
 */
public abstract class AbstractTextCanvas extends AbstractControl<org.eclipse.tm.internal.terminal.textcanvas.TextCanvas>
    implements TextCanvas {
  protected AbstractTextCanvas(ReferencedComposite refComposite, int index, Matcher<?>... matchers) {
    super(org.eclipse.tm.internal.terminal.textcanvas.TextCanvas.class, refComposite, index, matchers);
  }

  protected AbstractTextCanvas(org.eclipse.tm.internal.terminal.textcanvas.TextCanvas widget) {
    super(widget);
  }
  
  /**
   * Returns styledtext text.
   * @return text of this StyledText
   */
  @Override
  public String getText() {
      return TextCanvasHandler.getInstance().getText(swtWidget);
  }


}
