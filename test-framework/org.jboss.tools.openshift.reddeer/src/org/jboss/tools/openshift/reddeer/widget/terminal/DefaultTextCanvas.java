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

import org.hamcrest.Matcher;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.core.reference.ReferencedComposite;

/**
 * Default class for representing TextCanvas.
 * 
 * @author rhopp, rawagner
 * 
 */
public class DefaultTextCanvas extends AbstractTextCanvas implements TextCanvas {
  
  /**
   * StyledText with index 0.
   */
  public DefaultTextCanvas() {
    this((ReferencedComposite) null);
  }
  
  public DefaultTextCanvas(org.eclipse.tm.internal.terminal.textcanvas.TextCanvas widget){
    super(widget);
  }
  
  /**
   * StyledText inside given composite.
   *
   * @param referencedComposite the referenced composite
   */
  public DefaultTextCanvas(final ReferencedComposite referencedComposite) {
    this(referencedComposite, 0);
  }

  /**
   * StyledText with given text.
   *
   * @param text the text
   */
  public DefaultTextCanvas(final String text) {
    this(null, text);
  }
  
  /**
   * StyledText with given text inside given composite.
   *
   * @param referencedComposite the referenced composite
   * @param text the text
   */
  public DefaultTextCanvas(final ReferencedComposite referencedComposite, final String text) {
    this(referencedComposite, 0, new WithTextMatcher(text));
  }

  /**
   * StyledText matching given matchers.
   *
   * @param matchers the matchers
   */
  public DefaultTextCanvas(final Matcher<?>... matchers) {
    this(null, matchers);
  }
  
  /**
   * StyledText matching given matchers inside given composite.
   *
   * @param referencedComposite the referenced composite
   * @param matchers the matchers
   */
  public DefaultTextCanvas(final ReferencedComposite referencedComposite, final Matcher<?>... matchers) {
    this(referencedComposite, 0, matchers);
  }
  
  /**
   * StyledText with given index that matches given matchers.
   *
   * @param index the index
   * @param matchers the matchers
   */
  public DefaultTextCanvas(int index, Matcher<?>... matchers) {
    this(null, index, matchers);
  }
  
  /**
   * StyledText with given index that matches given matchers inside given composite.
   *
   * @param referencedComposite the referenced composite
   * @param index the index
   * @param matchers the matchers
   */
  public DefaultTextCanvas(ReferencedComposite referencedComposite, int index, Matcher<?>... matchers) {
    super(referencedComposite, index, matchers);
  }
}
