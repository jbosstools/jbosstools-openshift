/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.test.core.devfile;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DevfileTest {
  private static final Random RANDOM = new Random();
  
  private IProject project;
  
  @Before
  public void setUp() throws CoreException {
    IIntroPart intro = PlatformUI.getWorkbench().getIntroManager().getIntro();
    if (intro != null) {
      PlatformUI.getWorkbench().getIntroManager().closeIntro(intro);
    }
    IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    for (IViewReference ref : activePage.getViewReferences()) {
      activePage.hideView(ref);
    }
    project = ResourcesPlugin.getWorkspace().getRoot().getProject("p" + RANDOM.nextInt(Integer.MAX_VALUE));
    project.create(new NullProgressMonitor());
    project.open(new NullProgressMonitor());
  }

  @Test
  public void testInvalidDevfile() throws Exception {
    IFile file = project.getFile("devfile.yaml");
    file.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
    IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    ITextEditor editor = (ITextEditor) IDE.openEditor(activePage, file, true);
    IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
    document.set("s: 1");
    boolean markerFound = new DisplayHelper() {
      @Override
      protected boolean condition() {
        try {
          return file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO).length > 0;
        } catch (CoreException e) {
          return false;
        }
      }
    }.waitForCondition(activePage.getWorkbenchWindow().getShell().getDisplay(), 10000);
    assertTrue(markerFound);
  }

  @Test
  public void testValidDevfile() throws Exception {
    IFile file = project.getFile("devfile.yaml");
    file.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
    IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    ITextEditor editor = (ITextEditor) IDE.openEditor(activePage, file, true);
    IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
    document.set("schemaVersion: \"2.0.0\"");
    boolean markerFound = new DisplayHelper() {
      @Override
      protected boolean condition() {
        try {
          return file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO).length > 0;
        } catch (CoreException e) {
          return false;
        }
      }
    }.waitForCondition(activePage.getWorkbenchWindow().getShell().getDisplay(), 10000);
    assertFalse(Arrays.stream(file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO))
        .map(Object::toString).collect(Collectors.joining("\n")), markerFound);
  }
}
