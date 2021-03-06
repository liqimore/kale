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
package com.ivyft.katta.node;

import com.google.common.collect.ImmutableList;
import com.ivyft.katta.codec.Serializer;
import com.ivyft.katta.lib.writer.DocumentFactory;
import com.ivyft.katta.lib.writer.LuceneDocumentMerger;
import com.ivyft.katta.lib.writer.SerialFactory;
import com.ivyft.katta.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


/**
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
public class ShardManager {



    public final static String HDFS = "hdfs";


    public final static String FILE = "file";


    /**
     * 索引文件复制的本地路径
     */
    private final File shardsFolder;


    /**
     * 把对象转 LuceneDocument Class
     */
    protected final Class<? extends DocumentFactory> documentFactoryClass;


    /**
     * 合并索引 Class
     */
    protected final Class<? extends LuceneDocumentMerger> mergeDocumentClass;



    /**
     *
     * Copy Index to Local
     */
    protected final boolean copyIndexToLocal;


    /**
     * 复制数据的文件系统
     */
    private final ThrottledInputStream.ThrottleSemaphore throttleSemaphore;


    /**
     * 索引被更新时回调
     */
    protected final IndexUpdateListener updateListener;



    /**
     * Merge Index Analyzer
     */
    protected String analyzerClass;


    /**
     * Log
     */
    protected final static Logger LOG = LoggerFactory.getLogger(ShardManager.class);


    /**
     *
     * @param shardsFolder
     */
    public ShardManager(NodeConfiguration conf, File shardsFolder, IndexUpdateListener updateListener) {
        this(conf, shardsFolder, updateListener, null);
    }


    /**
     *
     * @param shardsFolder
     * @param throttleSemaphore
     */
    public ShardManager(NodeConfiguration conf,
                        File shardsFolder,
                        IndexUpdateListener updateListener,
                        ThrottledInputStream.ThrottleSemaphore throttleSemaphore) {
        this.shardsFolder = shardsFolder;
        this.throttleSemaphore = throttleSemaphore;
        this.updateListener = updateListener;

        if (!this.shardsFolder.exists()) {
            this.shardsFolder.mkdirs();
        }
        if (!this.shardsFolder.exists()) {
            throw new IllegalStateException("could not create local shard folder '" +
                    this.shardsFolder.getAbsolutePath() + "'");
        }

        setAnalyzerClass(conf.getString("lucene.index.writer.analyzer.class", StandardAnalyzer.class.getName()));
        this.copyIndexToLocal = conf.getBoolean("lucene.index.copyTo.local", true);

        try {
            documentFactoryClass = (Class<? extends DocumentFactory>) conf.getClass("lucene.index.document.factory.class");
            documentFactoryClass.newInstance();

            mergeDocumentClass = (Class<? extends LuceneDocumentMerger>) conf.getClass("lucene.document.merger.class");
            Constructor<?> constructor = mergeDocumentClass.getConstructor(
                    String.class,
                    Serializer.class,
                    Analyzer.class,
                    File.class,
                    DocumentFactory.class,
                    LuceneIndexMergeManager.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }


    /**
     * 安装Shard，提供一个ShardName和一个Path
     * @param shardName shardName
     * @param shardPath IndexFile Path
     * @return 返回本地索引文件的路径
     * @throws Exception
     */
    public URI installShard(String shardName,
                             String shardPath) throws Exception {
        URI path = new URI(shardPath);
        String scheme = path.getScheme();
        File localShardFolder = getShardFolder(shardName);

        if(StringUtils.equals(ShardManager.HDFS, scheme)) {
            try {
                LOG.info("hdfs use local dir: " + localShardFolder.getAbsolutePath());
                if (!localShardFolder.exists()) {
                    //把索引 copy 到本地
                    if(copyIndexToLocal) {
                        installShard(shardName, shardPath, localShardFolder);
                    } else {
                        if(!localShardFolder.mkdirs()) {
                            throw new IOException("can't mkdir: " + localShardFolder);
                        }

                        LOG.info("mkdir: " + localShardFolder.getAbsolutePath());
                    }
                }
                return copyIndexToLocal ? localShardFolder.toURI() : path;
            } catch (Exception e) {
                FileUtil.deleteFolder(localShardFolder);
                throw e;
            }
        } else {
            //本地文件系统 copy 文件
            try {
                if (!localShardFolder.exists()) {
                    installShard(shardName, shardPath, localShardFolder);
                }
                return localShardFolder.toURI();
            } catch (Exception e) {
                FileUtil.deleteFolder(localShardFolder);
                throw e;
            }
        }
    }






    /**
     * 安装Shard，提供一个ShardName和一个Path, 如果该 Path 是 HDFS 索引, 则从 HDFS copy 到本地
     *
     * <p>
     *     该方法为 SingleServer 调用，用于安装多个 Index 到同一个目录下，
     *     多个 HDFS 索引要写到 addIndex 到同一个目录下，
     *     第一个 Merge 为 false，从第二个开始，Merge 为 true，以后都是合并索引
     * </p>
     *
     * @param shardName shardName
     * @param shardPath IndexFile Path
     * @param merge 是否覆盖
     *
     * @return 返回本地索引文件的路径
     * @throws Exception
     */
    public URI installShard2(String shardName,
                            String shardPath, boolean merge) throws Exception {
        URI path = new URI(shardPath);
        String scheme = path.getScheme();
        if(StringUtils.equals(ShardManager.HDFS, scheme)) {
            //本地文件系统 copy 文件
            File localShardFolder = getShardFolder(shardName);
            try {
                LOG.info("hdfs use local dir: " + localShardFolder.getAbsolutePath());
                if (!localShardFolder.exists()) {
                    if(!localShardFolder.mkdirs()) {
                        throw new IOException("can't mkdir: " + localShardFolder);
                    }

                    LOG.info("mkdir: " + localShardFolder.getAbsolutePath());
                } else {
                    //文件夹已经存在，并且需要合并的
                    if(merge) {
                        //合并两个目录的索引
                        installShardMergeIndex(shardName, shardPath, localShardFolder);
                        return localShardFolder.toURI();
                    }
                }

                //from hdfs copy to Local
                installShard(shardName, shardPath, localShardFolder);
                return localShardFolder.toURI();
            } catch (Exception e) {
                FileUtil.deleteFolder(localShardFolder);
                throw e;
            }
        } else {
            //本地文件系统 copy 文件
            //本地索引安装，不需要 copy
            File localShardFolder = getShardFolder(shardName);
            try {
                if (!localShardFolder.exists()) {
                    //如果不存在本地索引目录，则安装， 可能是从另一个本地 copy 到 shardFolder
                    installShard(shardName, shardPath, localShardFolder);
                }

                //已经存在，则直接返回
                return localShardFolder.toURI();
            } catch (Exception e) {
                FileUtil.deleteFolder(localShardFolder);
                throw e;
            }
        }
    }



    /**
     * 移除shardName的shard
     * @param shardName shardName
     */
    public void uninstallShard(String shardName) {
        File localShardFolder = getShardFolder(shardName);
        try {
            if (localShardFolder.exists()) {
                LOG.info("正在卸载索引：{}", shardName);
                FileUtils.deleteDirectory(localShardFolder);
                LOG.info("卸载索引：{} 成功。", shardName);
            }
        } catch (IOException e) {
            throw new RuntimeException("could not delete folder '" + localShardFolder + "'");
        }
    }



    /**
     * 返回 shardName 的合并索引目录
     * @param shardName 索引 Name
     * @return 返回本地合并索引目录
     *
     */
    public File getShardTmpPath(String shardName) {
        return new File(this.shardsFolder, shardName + "_index");
    }




    /**
     *
     * 返回本地所有Shard索引的路径
     *
     * @return 返回所有shardName
     */
    public Collection<String> getInstalledShards() {
        String[] folderList = this.shardsFolder.list(FileUtil.VISIBLE_FILES_FILTER);
        if (folderList == null || folderList.length == 0) {
            return Collections.EMPTY_LIST;
        }
        return Arrays.asList(folderList);
    }


    /**
     * 本地存放Shard Index路径
     * @return
     */
    public File getShardsFolder() {
        return this.shardsFolder;
    }


    public String getAnalyzerClass() {
        return analyzerClass;
    }

    public void setAnalyzerClass(String analyzerClass) {
        try {
            Class.forName(analyzerClass);
            this.analyzerClass = analyzerClass;
            LOG.info("analyzer class {}", analyzerClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }


    public boolean isCopyIndexToLocal() {
        return copyIndexToLocal;
    }

    /**
     * 拼接本地Shard路径
     * @param shardName shardName
     * @return
     */
    public File getShardFolder(String shardName) {
        return new File(this.shardsFolder, shardName);
    }



    /**
     * 檢驗本地Shard是否需要重新安裝。本地文件存在否？修改時間是否匹配
     * @param shardName shardName Shard Name
     * @param shardPath Shard在遠程路徑
     * @return 返回true則重新安裝
     */
    public boolean validChanges(String shardName, String shardPath, File localShardFolder) {
        URI uri;
        try {
            uri = new URI(shardPath);
            FileSystem fileSystem = HadoopUtil.getFileSystem(uri);
            if (this.throttleSemaphore != null) {
                fileSystem =
                        new ThrottledFileSystem(fileSystem, this.throttleSemaphore);
            }
            final Path path = new Path(shardPath);
            //Shard原索引是否還存在
            if(!fileSystem.exists(path)) {
                LOG.info("server index was delete, shard path: " + shardPath);
                //Shard遠程索引被刪除了，不需要重新安裝
                return false;
            }

            //本地索引不存在了，重新安装
            if (!localShardFolder.exists()) {
                LOG.info("local index was delete, will install agent, shard path: " + shardPath);
                return true;
            }

            //boolean isZip = fileSystem.isFile(path) && shardPath.endsWith(".zip");
            //zip就算了，不要解压了。如果是文件夹，需要比对一下日期
            //日期是否相同？大小是否相同（文件夹不能比大小）？
            long l = fileSystem.getFileStatus(path).getModificationTime();
            long l2 = localShardFolder.lastModified();
            LOG.info("local by server diff " + (l2 - l));

            //两个索引时间相差在10分钟之内, 不需要重新安装
            return Math.abs(l2 - l) >= 10 * 60 * 1000L;
        } catch (Exception e) {
            LOG.warn(ExceptionUtils.getFullStackTrace(e));
            return true;
        }
    }

    /*
     * Loads a shard from the given URI. The uri is handled bye the hadoop file
     * system. So all hadoop support file systems can be used, like local hdfs s3
     * etc. In case the shard is compressed we also unzip the content. If the
     * system property katta.spool.zip.shards is true, the zip file is staged to
     * the local disk before being unzipped.
     */
    private void installShard(String shardName,
                              String shardPath,
                              File localShardFolder) throws KattaException {
        LOG.info("install shard '" + shardName + "' from " + shardPath);
        URI uri = null;
        FileSystem fileSystem = null;
        try {
            uri = new URI(shardPath);
            fileSystem = HadoopUtil.getFileSystem(uri);
            if (this.throttleSemaphore != null) {
                fileSystem =
                        new ThrottledFileSystem(fileSystem, this.throttleSemaphore);
            }
        } catch (URISyntaxException e) {
            throw new KattaException("Can not parse uri for path: " + shardPath, e);
        } catch (Exception e) {
            throw new KattaException("Can not init file system " + uri, e);
        }
        int maxTries = 3;
        for (int i = 0; i < maxTries; i++) {
            try {
                //原路径
                final Path path = new Path(shardPath);
                boolean isZip = fileSystem.isFile(path) && shardPath.endsWith(".zip");

                File shardTmpFolder = new File(localShardFolder.getAbsolutePath() + "_tmp");

                //删除旧的
                FileUtil.deleteFolder(shardTmpFolder);
                LOG.info("delete folder: " + shardTmpFolder.getAbsolutePath());

                if (isZip) {
                    FileUtil.unzip(path, shardTmpFolder, fileSystem, System.getProperty("katta.spool.zip.shards", "false")
                            .equalsIgnoreCase("true"));
                } else {
                    LOG.info("copy shard to local: " + shardTmpFolder.getAbsolutePath());
                    fileSystem.copyToLocalFile(false, path, new Path(shardTmpFolder.getAbsolutePath()), true);
                }

                boolean b = shardTmpFolder.renameTo(localShardFolder);
                LOG.info(shardTmpFolder.getName() + " rename to " + localShardFolder.getAbsolutePath() + " " + b);

                //文件修改時間
                long lastModified = fileSystem.getFileStatus(path).getModificationTime();
                b = localShardFolder.setLastModified(lastModified);
                LOG.info(localShardFolder.getAbsolutePath() + " lastModifies " + new Date(lastModified) + " " + b);

                // Looks like we were successful.
                if (i > 0) {
                    LOG.error("Loaded shard:" + shardPath);
                }
                return;
            } catch (Exception e) {
                LOG.error(String.format("Error loading shard: %s (try %d of %d)", shardPath, i, maxTries), e);
                if (i >= maxTries - 1) {
                    throw new KattaException("Can not load shard: " + shardPath, e);
                }
            }
        }
    }



    /*
     * Loads a shard from the given URI. The uri is handled bye the hadoop file
     * system. So all hadoop support file systems can be used, like local hdfs s3
     * etc. In case the shard is compressed we also unzip the content. If the
     * system property katta.spool.zip.shards is true, the zip file is staged to
     * the local disk before being unzipped.
     */
    private void installShardMergeIndex(String shardName,
                              String shardPath,
                              File localShardFolder) throws KattaException {
        LOG.info("install shard '" + shardName + "' from " + shardPath);
        URI uri = null;
        FileSystem fileSystem = null;
        try {
            uri = new URI(shardPath);
            fileSystem = HadoopUtil.getFileSystem(uri);
            if (this.throttleSemaphore != null) {
                fileSystem =
                        new ThrottledFileSystem(fileSystem, this.throttleSemaphore);
            }
        } catch (URISyntaxException e) {
            throw new KattaException("Can not parse uri for path: " + shardPath, e);
        } catch (Exception e) {
            throw new KattaException("Can not init file system " + uri, e);
        }
        int maxTries = 3;
        updateListener.onBeforeUpdate(null, shardName);

        for (int i = 0; i < maxTries; i++) {
            try {
                //原路径
                final Path path = new Path(shardPath);
                boolean isZip = fileSystem.isFile(path) && shardPath.endsWith(".zip");

                File shardTmpFolder = new File(localShardFolder.getAbsolutePath() + "_tmp");

                //删除旧的
                FileUtil.deleteFolder(shardTmpFolder);
                LOG.info("delete folder: " + shardTmpFolder.getAbsolutePath());

                if (isZip) {
                    FileUtil.unzip(path, shardTmpFolder, fileSystem, System.getProperty("katta.spool.zip.shards", "false")
                            .equalsIgnoreCase("true"));
                } else {
                    LOG.info("copy shard to local: " + shardTmpFolder.getAbsolutePath());
                    fileSystem.copyToLocalFile(false, path, new Path(shardTmpFolder.getAbsolutePath()), true);
                }

                LuceneIndexMergeManager mergeManager =
                        new LuceneIndexMergeManager(localShardFolder.toURI(),
                                null,
                                shardName,
                                updateListener, getAnalyzer());
                try {
                    LOG.info(localShardFolder.getAbsolutePath() + " add index path " + shardTmpFolder.getName());
                    mergeManager.mergeIndex(shardTmpFolder.toURI(), false);


                    mergeManager.optimize(5);
                } finally {
                    mergeManager.close();

                    try {
                        LOG.info("clean folder: " + shardTmpFolder.getAbsolutePath());
                        FileUtil.deleteFolder(shardTmpFolder);
                    } catch (Exception e) {

                    }
                }

                //文件修改時間
                long lastModified = fileSystem.getFileStatus(path).getModificationTime();
                localShardFolder.setLastModified(lastModified);
                LOG.info(localShardFolder.getAbsolutePath() + " lastModifies " + new Date(lastModified));

                // Looks like we were successful.
                if (i > 0) {
                    LOG.error("Loaded shard:" + shardPath);
                }
                return;
            } catch (Exception e) {
                LOG.error(String.format("Error loading shard: %s (try %d of %d)", shardPath, i, maxTries), e);
                if (i >= maxTries - 1) {
                    throw new KattaException("Can not load shard: " + shardPath, e);
                }
            }
        }
    }


    /**
     * 返回一个合并索引的 Merger
     *
     *
     * @param contentType 序列化类型
     * @param indexName 索引名称
     * @param shardName 索引 Shard Name
     * @return 返回一个合并策略
     */
    public LuceneDocumentMerger getMergeDocument(String contentType, String indexName, String shardName) {
        Serializer<Object> serializer = SerialFactory.get(contentType);
        File localShardFolder = getShardFolder(shardName);
        File shardIndexPath = getShardTmpPath(shardName);
        LuceneIndexMergeManager mergeManager =
                new LuceneIndexMergeManager(localShardFolder.toURI(), indexName, shardName, updateListener, getAnalyzer());

        try {
            DocumentFactory documentFactory = documentFactoryClass.newInstance().build(indexName, shardName);
            Constructor<LuceneDocumentMerger> constructor = (Constructor<LuceneDocumentMerger>) mergeDocumentClass.getConstructor(
                    String.class,
                    Serializer.class,
                    Analyzer.class,
                    File.class,
                    DocumentFactory.class,
                    LuceneIndexMergeManager.class);

            LuceneDocumentMerger luceneDocumentMerger = constructor.newInstance(
                    indexName,
                    serializer,
                    getAnalyzer(),
                    shardIndexPath,
                    documentFactory,
                    mergeManager);
            updateListener.onBeforeUpdate(indexName, shardName);
            return luceneDocumentMerger;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 获取内部配置的 Analyzer
     *
     * @return Lucene Analyzer
     */
    protected Analyzer getAnalyzer() {
        try {
            return getAnalyzer((Class<? extends Analyzer>) Class.forName(this.analyzerClass));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }



    /**
     * 根据 Analyzer Class，反射获取 Analyzer
     * @param analysisClass class
     * @return 返回 Analyzer 实例对象
     */
    public static Analyzer getAnalyzer(Class<? extends Analyzer> analysisClass) {
        Constructor<? extends Analyzer> constructor;
        try {
            constructor = analysisClass.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            try {
                constructor = analysisClass.getDeclaredConstructor(Version.class);
                return constructor.newInstance(Version.LUCENE_46);
            } catch (Exception e1) {

            }
            throw new IllegalStateException("analyzer class: " + analysisClass.getName() +
                    " no Default Constructor() or  Constructor(Version);");
        }
    }


    /**
     * 返回 shardPath 下 *.dat 文件, 且文件不为 . 开头
     *
     *
     * @param shardPath Path
     * @return 返回文件 List
     */
    public List<Path> getDataPaths(Path shardPath) {
        try {
            final FileSystem fileSystem = HadoopUtil.getFileSystem(shardPath);
            if (fileSystem.isDirectory(shardPath)) {
                /*
                FileStatus[] statuses = fileSystem.listStatus(shardPath, new PathFilter() {
                    @Override
                    public boolean accept(Path path) {
                        return !path.getName().startsWith(".") && path.getName().endsWith(".dat");
                    }
                });
                */

                FileStatus[] statuses = fileSystem.listStatus(shardPath, (path) -> {
                    return !path.getName().startsWith(".") && path.getName().endsWith(".dat");
                });

                if(statuses != null) {
                    List<Path> paths = new ArrayList<Path>(statuses.length);
                    for (FileStatus status : statuses) {
                        paths.add(status.getPath());
                    }

                    return paths;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return ImmutableList.of();
    }




    public static void main(String[] args) throws Exception {
        File local = new File("./data/test");
        ShardManager shardManager = new ShardManager(null, local, null);

        shardManager.installShard2("2gj3oTQbG5TV0XJfWDJ", "hdfs:///user/katta/luce200/2gj3oTQbG5TV0XJfWDJ", true);
    }

}
