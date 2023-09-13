/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.common.CommonPlugin;

public class DownloadHelper {

	public static final String HOME_FOLDER = System.getProperty("user.home"); //$NON-NLS-1$

	private final class ToolDownloadRunnable implements IRunnableWithProgress {
		private final String toolName;
		private final Path path;
		private final ToolsConfig.Platform platform;
		private final Path dlFilePath;

		ToolDownloadRunnable(String toolName, Path path, ToolsConfig.Platform platform, Path dlFilePath) {
			this.toolName = toolName;
			this.path = path;
			this.platform = platform;
			this.dlFilePath = dlFilePath;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			monitor.beginTask("Connecting to remote url...", IProgressMonitor.UNKNOWN);
			try (CloseableHttpClient client = HttpClients.createDefault()) {
				HttpGet request = new HttpGet(platform.getUrl().toString());
				try (CloseableHttpResponse response = client.execute(request)) {
					monitor.beginTask("Downloading " + toolName + "...", (int) response.getEntity().getContentLength());
					try (InputStream content = response.getEntity().getContent()) {
						downloadFile(content, dlFilePath, monitor);
						if (monitor.isCanceled()) {
							throw new InterruptedException("Interrupted");
						}
					}
					uncompress(dlFilePath, path);
					monitor.done();
				} catch (UnsupportedOperationException e) {
					throw new InvocationTargetException(e);
				} catch (IOException e) {
					throw new InvocationTargetException(e);
				}
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}

		}
	}

	private static final UnaryOperator<InputStream> UNCOMPRESSOR = new UnaryOperator<InputStream>() {
		@Override
		public InputStream apply(InputStream input) {

			try {
				return new CompressorStreamFactory().createCompressorInputStream(input);
			} catch (CompressorException e) {
				throw new RuntimeException(e);
			}
		}
	};

	private static final UnaryOperator<InputStream> UNTAR = new UnaryOperator<InputStream>() {
		@Override
		public InputStream apply(InputStream input) {
			return new TarArchiveInputStream(input);
		}
	};

	private static final UnaryOperator<InputStream> UNZIP = new UnaryOperator<InputStream>() {
		@Override
		public InputStream apply(InputStream input) {
			return new ZipArchiveInputStream(input);
		}
	};

	private static final Map<String, UnaryOperator<InputStream>> MAPPERS = new HashMap<String, UnaryOperator<InputStream>>();

	static {
		MAPPERS.put("gz", UNCOMPRESSOR); //$NON-NLS-1$
		MAPPERS.put("zip", UNZIP); //$NON-NLS-1$
		MAPPERS.put("tar", UNTAR); //$NON-NLS-1$
	}

	private DownloadHelper() {
	}

	private static DownloadHelper instance;

	public static DownloadHelper getInstance() {
		if (instance == null) {
			instance = new DownloadHelper();
		}
		return instance;
	}

	/**
	 * Download tool if required. First look at PATH then use the configuration file
	 * provided by the url to download the tool. The format of the file is the
	 * following:
	 * 
	 * <pre>
	 * {
	 *   "tools": {
	 *     "tool": {
	 *       "version": "1.0.0",
	 *       "versionCmd": "version", //the argument(s) to add to cmdFileName to get the version
	 *       "versionExtractRegExp": "", //the regular expression to extract the version string from the version command
	 *       "versionMatchRegExpr": "", //the regular expression use to match the extracted version to decide if download if required
	 *       "baseDir": "" //the basedir to install to, a sub folder named after version will be created, can use $HOME
	 *       "platforms": {
	 *         "win": {
	 *           "url": "https://tool.com/tool/v1.0.0/odo-windows-amd64.exe.tar.gz",
	 *           "cmdFileName": "tool.exe",
	 *           "dlFileName": "tool-windows-amd64.exe.gz"
	 *         },
	 *         "osx": {
	 *           "url": "https://tool.com/tool/v1.0.0/odo-darwin-amd64.tar.gz",
	 *           "cmdFileName": "tool",
	 *           "dlFileName": "tool-darwin-amd64.gz"
	 *         },
	 *         "lnx": {
	 *           "url": "https://tool.com/tool/v1.0.0/odo-linux-amd64.tar.gz",
	 *           "cmdFileName": "odo",
	 *           "dlFileName": "odo-linux-amd64.gz"
	 *         }
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param toolName
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public String downloadIfRequired(final String toolName, URL url) throws IOException {
		ToolsConfig config = ToolsConfig.loadToolsConfig(url);
		ToolsConfig.Tool tool = config.getTools().get(toolName);
		if (tool == null) {
			throw new IOException("Tool " + toolName + " not found in config file " + url);
		}
		final ToolsConfig.Platform platform = getPlatformBasedOnOs(tool);
		if (platform == null) {
			throw new IOException("Platform for Tool " + toolName + " not found in config file " + url);
		}
		String command = platform.getCmdFileName();
		String version = getVersionFromPath(tool, platform);
		if (!areCompatible(version, tool.getVersionMatchRegExpr())) {
			final Path path = Paths.get(tool.getBaseDir().replace("$HOME", HOME_FOLDER), "cache", tool.getVersion(),
					command);
			if (!Files.exists(path)) {
				return askAndDownloadToolinUI(toolName, tool, platform, version, path);
			}
			return path.toString();
		}
		return command;
	}

	private ToolsConfig.Platform getPlatformBasedOnOs(ToolsConfig.Tool tool) {
		String osArch = Platform.getOSArch();
		String osId = Platform.getOS();
		if (tool.getPlatforms().containsKey(osId + "-" + osArch)) {
			return tool.getPlatforms().get(osId + "-" + osArch);
		}
		return tool.getPlatforms().get(osId);

	}

	public CompletableFuture<String> downloadIfRequiredAsync(String toolName, URL url) {
		CompletableFuture<String> result = new CompletableFuture<>();
		Display.getDefault().asyncExec(() -> {
			try {
				result.complete(downloadIfRequired(toolName, url));
			} catch (IOException e) {
				result.completeExceptionally(e);
			}
		});
		return result;
	}

	private String askAndDownloadToolinUI(final String toolName, ToolsConfig.Tool tool,
			final ToolsConfig.Platform platform, String version, final Path path) throws IOException {
		Display display = Display.getCurrent();
		if (display != null) {
			return askAndDownloadTool(toolName, tool, platform, version, path);
		}
		String[] result = new String[1];
		IOException[] error = new IOException[1];
		Display.getDefault().syncExec(() -> {
			try {
				result[0] = askAndDownloadTool(toolName, tool, platform, version, path);
			} catch (IOException e) {
				error[0] = e;
			}

		});
		if (error[0] != null) {
			throw error[0];
		}
		return result[0];
	}

	private String askAndDownloadTool(final String toolName, ToolsConfig.Tool tool, final ToolsConfig.Platform platform,
			String version, final Path path) throws IOException {
		final Path dlFilePath = path.resolveSibling(platform.getDlFileName());
		final String cmd = path.toString();
		if (isDownloadAllowed(toolName, version, tool.getVersion())) {
			IRunnableWithProgress op = new ToolDownloadRunnable(toolName, path, platform, dlFilePath);
			try {
				new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()).run(true,
						true, op);
			} catch (InvocationTargetException e) {
				throw new IOException(e.getLocalizedMessage(), e);
			} catch (InterruptedException e) {
				throw new IOException(toolName + " download interrupted.", e);
			}
			return cmd;

		}
		// return default from config file
		return platform.getCmdFileName();
	}

	public boolean isDownloadAllowed(String tool, String currentVersion, String requiredVersion) {
		String message = StringUtils.isEmpty(currentVersion)
				? tool + " not found, do you want to download " + tool + " " + requiredVersion + " ?"
				: tool + " " + currentVersion + " found, required version is " + requiredVersion
						+ ", do you want to download " + tool + " ?";
		String title = tool + " tool required";
		return MessageDialog.open(MessageDialog.QUESTION_WITH_CANCEL,
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), title, message, SWT.NONE);
	}

	private boolean areCompatible(String version, String versionMatchRegExpr) {
		boolean compatible = true;
		if (StringUtils.isNotBlank(versionMatchRegExpr)) {
			Pattern pattern = Pattern.compile(versionMatchRegExpr);
			compatible = pattern.matcher(version).matches();
		} else if (StringUtils.isBlank(version)) {
			compatible = false;
		}
		return compatible;
	}

	private String getVersionFromPath(ToolsConfig.Tool tool, ToolsConfig.Platform platform) {

		final Pattern pattern = Pattern.compile(tool.getVersionExtractRegExp());
		String[] arguments = tool.getVersionCmd().split(" ");
		String version = "";
		try {
			String output = executeInCommandline(platform.getCmdFileName(), new File(HOME_FOLDER), arguments);
			try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
				version = reader.lines().map(new Function<String, Matcher>() {
					@Override
					public Matcher apply(String line) {
						return pattern.matcher(line);
					}
				}).filter(new Predicate<Matcher>() {
					@Override
					public boolean test(Matcher matcher) {
						return matcher.matches();
					}
				}).map(new Function<Matcher, String>() {
					@Override
					public String apply(Matcher matcher) {
						return matcher.group(1);
					}
				}).findFirst().orElse("");
			}
		} catch (IOException e) {
			// do not throw or verify error, as the tool can be not present in the first
			// try.
		}
		return version;

	}

	void downloadFile(InputStream input, Path dlFileName, IProgressMonitor monitor) throws IOException {
		byte[] buffer = new byte[4096];
		Files.createDirectories(dlFileName.getParent());
		try (OutputStream output = Files.newOutputStream(dlFileName)) {
			int lg;
			while (((lg = input.read(buffer)) > 0) && !monitor.isCanceled()) {
				output.write(buffer, 0, lg);
				monitor.worked(lg);
			}
		}
	}

	private InputStream mapStream(String filename, InputStream input) {
		String extension;
		while (((extension = FilenameUtils.getExtension(filename)) != null) && MAPPERS.containsKey(extension)) {
			filename = FilenameUtils.removeExtension(filename);
			input = MAPPERS.get(extension).apply(input);
		}
		return input;
	}

	void uncompress(Path dlFilePath, Path cmd) throws IOException {
		try (InputStream input = new BufferedInputStream(Files.newInputStream(dlFilePath));
				InputStream subStream = mapStream(dlFilePath.toString(), input)) {
			if (subStream instanceof ArchiveInputStream) {
				ArchiveEntry entry;

				while ((entry = ((ArchiveInputStream) subStream).getNextEntry()) != null) {
					save(subStream, cmd.resolveSibling(entry.getName()), entry.getSize());
				}
			} else {
				save(subStream, cmd, -1L);
			}
		}
	}

	private void save(InputStream source, Path destination, long length) throws IOException {
		try (OutputStream stream = Files.newOutputStream(destination)) {
			if (length == -1L) {
				IOUtils.copy(source, stream);
			} else {
				IOUtils.copyLarge(source, stream, 0L, length);
			}
			if (!destination.toFile().setExecutable(true)) {
				CommonPlugin.getDefault().logError("Cannot set executable bit for: " + destination.toFile().getPath());
			}
		}
	}

	private String executeInCommandline(String executable, File workingDirectory, String... arguments)
			throws IOException {
		DefaultExecutor executor = new DefaultExecutor();
		try (StringWriter writer = new StringWriter();
				WriterOutputStream outStream = new WriterOutputStream(writer, StandardCharsets.UTF_8)) {
			PumpStreamHandler handler = new PumpStreamHandler(outStream);
			executor.setStreamHandler(handler);
			executor.setWorkingDirectory(workingDirectory);
			CommandLine command = new CommandLine(executable).addArguments(arguments);
			String result = "";
			try {
				executor.execute(command);
				result = writer.toString();
			} catch (IOException e) {
				throw new IOException(e.getLocalizedMessage() + " " + result, e);
			}
			return result;
		}
	}
}
