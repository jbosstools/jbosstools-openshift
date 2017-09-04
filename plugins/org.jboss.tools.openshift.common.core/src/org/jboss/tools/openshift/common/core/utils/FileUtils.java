/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.Assert;

/**
 * @author André Dietisheim
 */
public class FileUtils {

    private static final String EXT_TAR_GZ = ".tar.gz";
    private static final String NUMERIC_SUFFIX_FILENAME_PATTERN = "{0}/{1}({2}){3}";
    private static final Pattern NUMERIC_SUFFIX_FILENAME_REGEX = Pattern.compile("(.*)\\([0-9]+\\)");

    private static final byte[] buffer = new byte[1024];

    private FileUtils() {
        // private default constructor for utils class
    }

    public static boolean canRead(String path) {
        if (path == null) {
            return false;
        }
        return canRead(new File(path));
    }

    public static boolean canRead(File file) {
        if (file == null) {
            return false;
        }
        return file.canRead();
    }

    public static boolean canWrite(String path) {
        if (path == null) {
            return false;
        }
        return canWrite(new File(path));
    }

    public static boolean canWrite(File file) {
        if (file == null) {
            return false;
        }
        if (!file.exists()) {
            return canWrite(file.getParent());
        }
        return file.canWrite();
    }

    public static boolean exists(File file) {
        return file != null && file.exists();
    }

    public static boolean isDirectory(File file) {
        return file != null && file.isDirectory();
    }

    public static File getSystemTmpFolder() {
        String tmpFolder = System.getProperty("java.io.tmpdir");
        return new File(tmpFolder);
    }

    public static File getRandomTmpFolder() {
        String randomName = String.valueOf(System.currentTimeMillis());
        return new File(getSystemTmpFolder(), randomName);
    }

    /**
     * Copies the ginve source to the given destination recursively. Overwrites
     * existing files/directory on the destination path if told so.
     * 
     * @param source
     *            the source file/directory to copy
     * @param destination
     *            the destination to copy to
     * @param overwrite
     *            overwrites existing files/directories if <code>true</code>.
     *            Does not overwrite otherwise.
     * @throws IOException
     */
    public static void copy(File source, File destination, boolean overwrite) throws IOException {
        if (!exists(source) || destination == null) {
            return;
        }

        if (source.isDirectory()) {
            copyDirectory(source, destination, overwrite);
        } else {
            copyFile(source, destination, overwrite);
        }
    }

    private static void copyDirectory(File source, File destination, boolean overwrite) throws IOException {
        Assert.isLegal(source != null);
        Assert.isLegal(source.isDirectory());
        Assert.isLegal(destination != null);

        destination = getDestinationDirectory(source, destination);

        if (!destination.exists()) {
            destination.mkdir();
            copyPermissions(source, destination);
        }

        for (File content : source.listFiles()) {
            if (content.isDirectory()) {
                copyDirectory(content, new File(destination, content.getName()), overwrite);
            } else {
                copyFile(content, new File(destination, content.getName()), overwrite);
            }
        }
    }

    private static File getDestinationDirectory(File source, File destination) {
        if (!source.getName().equals(destination.getName())) {
            destination = new File(destination, source.getName());
        }
        return destination;
    }

    private static void copyFile(File source, File destination, boolean overwrite) throws IOException {
        Assert.isLegal(source != null);
        Assert.isLegal(source.isFile());
        Assert.isLegal(destination != null);

        destination = getDestinationFile(source, destination);

        if (exists(destination) && !overwrite) {
            return;
        }

        if (isDirectory(destination)) {
            if (!overwrite) {
                return;
            }
            destination.delete();
        }

        writeTo(source, destination);
    }

    private static File getDestinationFile(File source, File destination) {
        if (!source.getName().equals(destination.getName())) {
            destination = new File(destination, source.getName());
        }
        return destination;
    }

    private static final void writeTo(File source, File destination) throws IOException {
        Assert.isLegal(source != null);
        Assert.isLegal(destination != null);

        writeTo(new BufferedInputStream(new FileInputStream(source)), destination);
        copyPermissions(source, destination);
    }

    public static final void writeTo(String content, File destination) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(destination);
        writer.write(content);
        writer.flush();
        writer.close();
    }

    private static final void writeTo(InputStream in, File destination) throws IOException {
        Assert.isLegal(in != null);
        Assert.isLegal(destination != null);

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
            for (int read = -1; (read = in.read(buffer)) != -1;) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            silentlyClose(in);
        }
    }

    /**
     * Replicates the owner permissions from the source to the destination. Due
     * to limitation in java6 this is the best we can do (there's no way in
     * java6 to know if rights are due to owner or group)
     * 
     * @param source
     * @param destination
     * 
     * @see File#canRead()
     * @see File#setReadable(boolean)
     * @see File#canWrite()
     * @see File#setWritable(boolean)
     * @see File#canExecute()
     * @see File#setExecutable(boolean)
     */
    private static void copyPermissions(File source, File destination) {
        Assert.isLegal(source != null);
        Assert.isLegal(destination != null);

        destination.setReadable(source.canRead());
        destination.setWritable(source.canWrite());
        destination.setExecutable(source.canExecute());
    }

    private static void silentlyClose(InputStream in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private static void silentlyClose(OutputStream out) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public static String getParent(String filepath) {
        String parent = null;
        if (!StringUtils.isEmpty(filepath)) {
            parent = new File(filepath).getParent();
        }
        return parent;
    }

    /**
     * Returns the given filepath with a suffix if the given filepath already
     * exists.
     * 
     * @param filepath
     * @return the filepath or filepath + numeric suffix if not available.
     * 
     * @see #NUMERIC_SUFFIX_FILENAME_PATTERN
     */
    public static String getAvailableFilepath(String filepath) {
        if (StringUtils.isEmpty(filepath)) {
            return filepath;
        }
        String extension = getExtension(filepath);
        String dir = FilenameUtils.getFullPathNoEndSeparator(filepath);
        String filenameWithoutExtension = stripNumericSuffix(getBaseName(filepath));

        String newFilename = filepath;
        int i = 1;
        while (new File(newFilename).exists()) {
            newFilename = MessageFormat.format(NUMERIC_SUFFIX_FILENAME_PATTERN, dir, filenameWithoutExtension, i++, extension);
        }
        return newFilename;
    }

    /**
     * Strips the numeric suffix off the given filepath if present. Returns the
     * given filepath otherwise.
     * 
     * @param filepath
     * @return
     */
    private static String stripNumericSuffix(String filepath) {
        if (StringUtils.isEmpty(filepath)) {
            return filepath;
        }
        Matcher matcher = NUMERIC_SUFFIX_FILENAME_REGEX.matcher(filepath);
        if (!matcher.matches() || matcher.groupCount() < 1) {
            return filepath;
        }
        return matcher.group(1);
    }

    private static String getExtension(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return filename;
        }
        if (filename.endsWith(EXT_TAR_GZ)) {
            return EXT_TAR_GZ;
        }
        return FilenameUtils.getExtension(filename);
    }

    /**
     * Returns the filename without the suffix
     * 
     * @param filename
     * @return
     */
    public static String getBaseName(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return filename;
        }
        if (filename.endsWith(EXT_TAR_GZ)) {
            filename = filename.substring(0, filename.length() - EXT_TAR_GZ.length());
        }

        return FilenameUtils.getBaseName(filename);
    }
}
