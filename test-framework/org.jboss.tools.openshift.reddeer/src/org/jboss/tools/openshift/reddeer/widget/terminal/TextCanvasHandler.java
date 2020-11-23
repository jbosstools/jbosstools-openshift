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


import org.eclipse.swt.custom.StyledText;
import org.eclipse.tm.internal.terminal.textcanvas.TextCanvas;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.util.ResultRunnable;
import org.eclipse.reddeer.core.handler.ControlHandler;

/**
 * Contains methods for handling UI operations on {@link StyledText} widgets.
 * 
 * @author Jiri Peterka
 * @author Vlado Pakan
 * 
 */
public class TextCanvasHandler extends ControlHandler{
  
  private static TextCanvasHandler instance;
  
  /**
   * Gets instance of StyledTextHandler.
   * 
   * @return instance of StyledTextHandler
   */
  public static TextCanvasHandler getInstance(){
    if(instance == null){
      instance = new TextCanvasHandler();
    }
    return instance;
  }

  /**
   * Gets text of text canvas
   * @param textCanvas to handle
   * @return text of specified text canvas
   */
  public String getText(final TextCanvas textCanvas){
    return Display.syncExec(new ResultRunnable<String>() {

      @Override
      public String run() {
        return textCanvas.getAllText();
      }
    });
  }
}
