/**
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ivyft.katta.util;

import com.ivyft.katta.operation.FilenameNDotFilter;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 *
 *
 *
 * <pre>
 *
 * Created by IntelliJ IDEA.
 * User: zhenqin
 * Date: 13-11-13
 * Time: 上午8:58
 * To change this template use File | Settings | File Templates.
 *
 * </pre>
 *
 * @author zhenqin
 */
public class FileUtil {

    /**
     * 默认的缓冲区大小
     */
    private static final int BUFFER = 4096;


    /**
     * 去掉前缀是.的文件夹及文件
     */
    public static final FilenameFilter VISIBLE_FILES_FILTER = new FilenameNDotFilter();


    /**
     * Log
     */
    private final static Logger LOG = LoggerFactory.getLogger(FileUtil.class);


    /**
     * 删除一个文件夹或者文件，文件夹递归删除。
     * @param folder
     */
    public static void deleteFolder(File folder) {
        if (folder.exists()) {
            FileUtils.deleteQuietly(folder);
        }
    }

    /**
     * 解压一个zip文件到targetFolder目录
     *
     * @param sourceZip zip文件路径
     * @param targetFolder 解压的目标路径
     */
    public static void unzip(final File sourceZip, final File targetFolder) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(sourceZip);
            final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            LOG.debug("Extracting zip file '" + sourceZip.getAbsolutePath() + "' to '" + targetFolder + "'");
            unzip(zis, targetFolder);
        } catch (final Exception e) {
            throw new RuntimeException("unable to expand upgrade files for " + sourceZip + " to " + targetFolder, e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }
    }

    /**
     * Simply unzips the content from the source zip to the target folder. The
     * first level folder of the zip content is removed.
     *
     * @param sourceZip    the path to the source zip file, hadoop's IO services are used to
     *                     open this path
     * @param targetFolder The directory that the zip file will be unpacked into
     * @param fileSystem   the hadoop file system object to use to open
     *                     <code>sourceZip</code>
     * @param localSpool   If true, the zip file is copied to the local file system before
     *                     being unzipped. The name used is <code>targetFolder.zip</code>. If
     *                     false, the unzip is streamed.
     */
    public static void unzip(final Path sourceZip, final File targetFolder, final FileSystem fileSystem,
                             final boolean localSpool) {
        try {
            if (localSpool) {
                targetFolder.mkdirs();
                final File shardZipLocal = new File(targetFolder + ".zip");
                if (shardZipLocal.exists()) {
                    // make sure we overwrite cleanly
                    shardZipLocal.delete();
                }
                try {
                    fileSystem.copyToLocalFile(sourceZip, new Path(shardZipLocal.getAbsolutePath()));
                    FileUtil.unzip(shardZipLocal, targetFolder);
                } finally {
                    shardZipLocal.delete();
                }
            } else {
                FSDataInputStream fis = fileSystem.open(sourceZip);
                try {
                    ZipInputStream zis = new ZipInputStream(fis);
                    unzip(zis, targetFolder);
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Exception ignore) {
                            // ignore
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("unable to expand upgrade files for " + sourceZip + " to " + targetFolder, e);
        }

    }

    /**
     * Unpack a zip stream to a directory usually called by {@link #unzip(File, File)} or {@link #unzip(Path, File, FileSystem, boolean).
     *
     * @param zis          Zip data strip to unpack
     * @param targetFolder The folder to unpack to. This directory and path is created if needed.
     * @throws IOException If there is an error.
     */
    public static void unzip(final ZipInputStream zis, final File targetFolder) throws IOException {
        ZipEntry entry;
        BufferedOutputStream dest = null;

        targetFolder.mkdirs();
        while ((entry = zis.getNextEntry()) != null) {
            LOG.debug("Extracting:   " + entry);
            // we need to remove the first element of the path since the
            // folder was compressed but we only want the folders content
            final String entryPath = entry.getName();
            final int indexOf = entryPath.indexOf("/");
            final String cleanUpPath = entryPath.substring(indexOf + 1, entryPath.length());
            final File targetFile = new File(targetFolder, cleanUpPath);
            if (entry.isDirectory()) {
                targetFile.mkdirs();
            } else {
                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs(); // KATTA-130
                }
                int count;
                final byte data[] = new byte[BUFFER];
                final FileOutputStream fos = new FileOutputStream(targetFile);
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
        }

    }


    /**
     *
     * 压缩文件
     *
     * @param inputFolder 输入压缩的文件和文件夹
     * @param outputFile 输出的zip目录
     * @throws IOException
     */
    public static void zip(final File inputFolder, final File outputFile) throws IOException {
        final FileOutputStream fileWriter = new FileOutputStream(outputFile);
        final ZipOutputStream zip = new ZipOutputStream(fileWriter);
        addFolderToZip("", inputFolder, zip);
        zip.flush();
        zip.close();
    }


    /**
     * 把文件,文件夹压缩到一个zip文件
     * @param path
     * @param folder
     * @param zip
     * @throws IOException
     */
    private static void addFolderToZip(final String path, final File folder, final ZipOutputStream zip)
            throws IOException {
        final String zipEnry = path + (path.equals("") ? "" : File.separator) + folder.getName();
        final File[] listFiles = folder.listFiles();
        for (final File file : listFiles) {
            if (file.isDirectory()) {
                addFolderToZip(zipEnry, file, zip);
            } else {
                addFileToZip(zipEnry, file, zip);
            }
        }
    }

    private static void addFileToZip(final String path, final File file, final ZipOutputStream zip) throws IOException {
        final byte[] buffer = new byte[1024];
        int read = -1;
        final FileInputStream in = new FileInputStream(file);
        try {
            final String zipEntry = path + File.separator + file.getName();
            LOG.debug("add zip entry: " + zipEntry);
            zip.putNextEntry(new ZipEntry(zipEntry));
            while ((read = in.read(buffer)) > -1) {
                zip.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
    }

    public static void unzipInDfs(FileSystem fileSystem,
                                  final Path source,
                                  final Path target) {
        try {
            FSDataInputStream dfsInputStream = fileSystem.open(source);
            fileSystem.mkdirs(target);
            final ZipInputStream zipInputStream = new ZipInputStream(dfsInputStream);
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                final String entryPath = entry.getName();
                final int indexOf = entryPath.indexOf("/");
                final String cleanUpPath = entryPath.substring(indexOf + 1, entryPath.length());
                Path path = target;
                if (!cleanUpPath.equals("")) {
                    path = new Path(target, cleanUpPath);
                }
                LOG.info("Extracting: " + entry + " to " + path);
                if (entry.isDirectory()) {
                    fileSystem.mkdirs(path);
                } else {
                    int count;
                    final byte data[] = new byte[4096];
                    FSDataOutputStream fsDataOutputStream = fileSystem.create(path);
                    while ((count = zipInputStream.read(data, 0, 4096)) != -1) {
                        fsDataOutputStream.write(data, 0, count);
                    }
                    fsDataOutputStream.flush();
                    fsDataOutputStream.close();
                }
            }
            zipInputStream.close();
        } catch (final Exception e) {
            LOG.error("can not open zip file", e);
            throw new RuntimeException("unable to expand upgrade files", e);
        }

    }
}
