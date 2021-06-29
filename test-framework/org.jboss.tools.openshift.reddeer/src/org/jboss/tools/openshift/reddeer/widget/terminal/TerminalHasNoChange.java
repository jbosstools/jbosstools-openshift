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

/**
 * @author Red Hat Developers
 *
 */

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.wait.TimePeriod;

/**
 * Returns true if a console has no change for the specified time period.
 * 
 * @author Andrej Podhradsky
 * 
 */
public class TerminalHasNoChange extends AbstractWaitCondition {

  private TimePeriod timePeriod;

  private String terminalText;
  private long terminalTime;

  /**
   * Construct the condition with {@link TimePeriod#DEFAULT}.
   */
  public TerminalHasNoChange() {
    this(TimePeriod.DEFAULT);
  }

  /**
   * Constructs the condition with a given time period.
   * 
   * @param timePeriod
   *            Time period
   */
  public TerminalHasNoChange(TimePeriod timePeriod) {
    this.timePeriod = timePeriod;
    this.terminalText = getTerminalText();
    this.terminalTime = System.currentTimeMillis();
  }

  /* (non-Javadoc)
   * @see org.eclipse.reddeer.common.condition.WaitCondition#test()
   */
  @Override
  public boolean test() {
    String currentConsoleText = getTerminalText();
    long currentConsoleTime = System.currentTimeMillis();

    if (!currentConsoleText.equals(terminalText)) {
      terminalText = currentConsoleText;
      terminalTime = currentConsoleTime;
      return false;
    }

    return currentConsoleTime - terminalTime - timePeriod.getSeconds() * 1000 >= 0;
  }

  /* (non-Javadoc)
   * @see org.eclipse.reddeer.common.condition.AbstractWaitCondition#description()
   */
  @Override
  public String description() {
    return "Console is still changing";
  }

  private static String getTerminalText() {
    TerminalView consoleView = new TerminalView();
    if (!consoleView.isOpen()) {
    	consoleView.open();
    }

    return consoleView.getTerminalText();
  }
}
