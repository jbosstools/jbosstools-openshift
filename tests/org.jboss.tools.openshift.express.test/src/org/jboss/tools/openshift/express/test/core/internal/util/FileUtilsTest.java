/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.core.internal.util;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.File;

import org.jboss.tools.openshift.express.internal.core.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FileUtils.class)
public class FileUtilsTest {

    private static final String PATH_WITHOUT_DOTS = "/home/userid/somerootpath/filename.tar.gz"; 
    private static final String PATH_WITH_DOTS = "/home/first.last/somerootpath/filename.tar.gz";

    private File mockedFile;

    @Before
    public void setup() throws Exception
    {
        mockedFile = mock(File.class);
        whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(mockedFile);
    }

    @Test
    public void getAvailableFilePathForEmptyString()
    {
        assertEquals("Exp. to return the same path", "", FileUtils.getAvailableFilepath(""));
        assertEquals("Exp. to return the same path", null, FileUtils.getAvailableFilepath(null));
    }

    @Test
    public void getAvailableFilepathWhenFileDoesNotExistForPathWithoutDots() throws Exception {
        when(mockedFile.exists()).thenReturn(false);
        assertEquals("Exp. the same path to be returned",PATH_WITHOUT_DOTS, FileUtils.getAvailableFilepath(PATH_WITHOUT_DOTS));
    }

    @Test
    public void getAvailableFilepathWhenFileDoesNotExistForPathWithDots() throws Exception {
        when(mockedFile.exists()).thenReturn(false);

        assertEquals("Exp. the same path to be returned",PATH_WITH_DOTS, FileUtils.getAvailableFilepath(PATH_WITH_DOTS));
    }

    @Test
    public void getAvailableFilepathWhenFileDoesExistForPathWithoutDots() throws Exception {
        when(mockedFile.exists()).thenReturn(true).thenReturn(false);
        assertEquals("Exp. the path to be incremented","/home/userid/somerootpath/filename(1).tar.gz", FileUtils.getAvailableFilepath(PATH_WITHOUT_DOTS));
    }

    @Test
    public void getAvailableFilepathWhenFileDoesExistForPathWithDots() throws Exception {
        when(mockedFile.exists()).thenReturn(true).thenReturn(false);
        assertEquals("Exp. the path to be incremented","/home/first.last/somerootpath/filename(1).tar.gz", FileUtils.getAvailableFilepath(PATH_WITH_DOTS));
    }
}
