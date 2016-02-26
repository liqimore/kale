/**
 * Copyright 2009 the original author or authors.
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
package com.ivyft.katta.lib.mapfile;

import com.ivyft.katta.lib.lucene.SearcherHandle;
import com.ivyft.katta.lib.lucene.SolrHandler;
import com.ivyft.katta.node.IContentServer;
import com.ivyft.katta.util.HadoopUtil;
import com.ivyft.katta.util.NodeConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.MapFile.Reader;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.ipc.ProtocolSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implements search over a set of Hadoop <code>MapFile</code>s.
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
public class MapFileServer implements IContentServer, IMapFileServer {

    private final static Logger LOG = LoggerFactory.getLogger(MapFileServer.class);

    private final Configuration _conf = HadoopUtil.getHadoopConf();
    private final FileSystem _fileSystem;
    private final Map<String, Reader> _readerByShard = new ConcurrentHashMap<String, Reader>();
    private String _nodeName;

    public MapFileServer() throws IOException {
        _fileSystem = FileSystem.getLocal(_conf);
    }


    @Override
    public long getProtocolVersion(String protocol, long clientVersion) throws IOException {
        return versionID;
    }

    @Override
    public ProtocolSignature getProtocolSignature(String protocol, long clientVersion, int clientMethodsHash) throws IOException {
        return new ProtocolSignature(versionID, null);
    }

    @Override
    public void init(String nodeName, NodeConfiguration nodeConfiguration) {
        _nodeName = nodeName;
    }


    /**
     * Adds an shard index search for given name to the list of shards
     * MultiSearcher search in.
     *
     * @param shardName
     * @throws IOException
     */
    public void addShard(final String shardName, final URI path, final String collectionName) throws IOException {
        LOG.debug("LuceneServer " + _nodeName + " got shard " + shardName);
        File shardDir = new File(path);
        if (!shardDir.exists()) {
            throw new IOException("Shard " + shardName + " dir " + shardDir.getAbsolutePath() + " does not exist!");
        }
        if (!shardDir.canRead()) {
            throw new IOException("Can not read shard " + shardName + " dir " + shardDir.getAbsolutePath() + "!");
        }
        try {
            final Reader reader = new Reader(_fileSystem, shardDir.getAbsolutePath(), _conf);
            synchronized (_readerByShard) {
                _readerByShard.put(shardName, reader);
            }
        } catch (IOException e) {
            LOG.error("Error opening shard " + shardName + " " + shardDir.getAbsolutePath(), e);
            throw e;
        }
    }

    @Override
    public Collection<String> getShards() {
        return Collections.unmodifiableCollection(_readerByShard.keySet());
    }

    /**
     * Removes a search by given shardName from the list of searchers.
     */
    public void removeShard(final String shardName) throws IOException {
        LOG.debug("LuceneServer " + _nodeName + " removing shard " + shardName);
        synchronized (_readerByShard) {
            final Reader reader = _readerByShard.get(shardName);
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.error("Error closing shard " + shardName, e);
                    throw e;
                }
                _readerByShard.remove(shardName);
            } else {
                LOG.warn("Shard " + shardName + " not found!");
            }
        }
    }

    /**
     * Returns data about a shard. Currently the only standard key is
     * SHARD_SIZE_KEY. This value will be reported by the listIndexes command. The
     * units depend on the type of server. It is OK to return an empty map or
     * null.
     *
     * @param shardName The name of the shard to measure. This was the name provided in
     *                  addShard().
     * @return a map of key/value pairs which describe the shard.
     * @throws Exception
     */
    public Map<String, String> getShardMetaData(String shardName) throws Exception {
        final Reader reader = _readerByShard.get(shardName);
        if (reader != null) {
            int count = 0;
            synchronized (reader) {
                reader.reset();
                WritableComparable<?> key = (WritableComparable<?>) reader.getKeyClass().newInstance();
                Writable value = (Writable) reader.getValueClass().newInstance();
                while (reader.next(key, value)) {
                    count++;
                }
            }
            Map<String, String> metaData = new HashMap<String, String>();
            metaData.put(SHARD_SIZE_KEY, Integer.toString(count));
            return metaData;
        }
        LOG.warn("Shard " + shardName + " not found!");
        throw new IllegalArgumentException("Shard " + shardName + " unknown");
    }

    @Override
    public SearcherHandle getSearcherHandleByShard(String shardName) {
        return null;
    }

    @Override
    public SolrHandler getSolrHandlerByShard(String shardName) {
        return null;
    }

    /**
     * Close all MapFiles. No further calls will be made after this one.
     */
    public void shutdown() throws IOException {
        for (final Reader reader : _readerByShard.values()) {
            try {
                reader.close();
            } catch (IOException e) {
                LOG.error("Error in shutdown", e);
            }
        }
        _readerByShard.clear();
    }

    public TextArrayWritable get(Text key, String[] shards) throws IOException {
        ExecutorService executor = Executors.newCachedThreadPool();
        Collection<Future<Text>> futures = new ArrayList<Future<Text>>();
        for (String shard : shards) {
            final Reader reader = _readerByShard.get(shard);
            if (reader == null) {
                LOG.warn("Shard " + shard + " unknown");
                continue;
            }
            Callable<Text> callable = new MapLookup(reader, key);
            futures.add(executor.submit(callable));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting on MapLookup threads", e);
        }
        executor.shutdownNow();
        List<Text> resultList = new ArrayList<Text>();
        for (Future<Text> future : futures) {
            try {
                Text result = future.get(0, TimeUnit.MILLISECONDS);
                if (result != null) {
                    resultList.add(result);
                }
            } catch (ExecutionException e) {
        /*
         * This MapFile red threw an exception. Stop processing and throw an
         * IOE.
         */
                Throwable t = e.getCause();
                if (t instanceof IOException) {
                    // Throw the same IOException that the MapFile.Reader threw.
                    throw (IOException) t;
                }
                // Wrap MapFile.Reader's exception in an IOException.
                throw new IOException("Error in MapLookup", t);
            } catch (TimeoutException e) {
        /*
         * Result is not ready. Should not happen, because future is done.
         * Continue as if MapLookup had returned null.
         */
                LOG.warn("Timed out while getting MapLookup", e);
            } catch (InterruptedException e) {
        /*
         * Something went wrong while waiting for result. Should not happen
         * because we wait for 0 msec, and the future is done. Continue as if
         * the MapLookup had returned null.
         */
                LOG.warn("Interrupted while getting RPC result", e);
            }
        }
        return new TextArrayWritable(resultList);
    }

    private class MapLookup implements Callable<Text> {

        private Reader _reader;
        private WritableComparable<?> _key;

        public MapLookup(Reader reader, WritableComparable<?> key) {
            _reader = reader;
            _key = key;
        }

        public Text call() throws Exception {
            synchronized (_reader) {
                Writable result = (Writable) _reader.getValueClass().newInstance();
                result = _reader.get(_key, result);
                return (Text) result;
            }
        }

    }

}
