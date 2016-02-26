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
package com.ivyft.katta.node;

import com.ivyft.katta.lib.lucene.SearcherHandle;
import com.ivyft.katta.lib.lucene.SolrHandler;
import com.ivyft.katta.util.NodeConfiguration;
import org.apache.hadoop.ipc.VersionedProtocol;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * This describes the interaction between the general Node class and a specific
 * Katta server instance it is managing. The Node class talks to Zookeeper, and
 * manages the shards on disk. It tells the server when to start and stop using
 * the shards, and when to shut down.
 * <p/>
 * The RPC calls from the client will not go through the Node. The Hadoop
 * RPC.Server uses a separate interface for those calls.
 * <p/>
 * Implementations need to have a default constructor.
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
public interface IContentServer extends VersionedProtocol {



    /**
     * hadoop 序列化
     */
    public final static long versionID = 1L;


    /**
     * The key fetched from getShardMetadata() which in order to report the size
     * of the shard in the listIndexes command. The value must be parsable as an
     * integer. The units depend on the type of data in the shard. Reporting the
     * shard size is optional.
     */
    public static final String SHARD_SIZE_KEY = "shard-size";


    /**
     * Initializes the content-server after the instance has been created. Passes
     * the node-name.
     *
     * @param nodeName          the name of the local machine, for example "sever21.foo.com:8000".
     *                          Use this name in your results if you need to refer to the current
     *                          node
     * @param nodeConfiguration katta.node.properties which may contain custom properties for the
     *                          content-server
     */
    public void init(String nodeName, NodeConfiguration nodeConfiguration);

    /**
     * Include the shard (directory of data) when computing results. The shard is
     * a directory, ready to be used.
     *
     * @param shardName The name of the shard. Will be used in removeShard(). May also be
     *                  used in requests.
     * @param shardDir  The directory where the shard data is.
     * @throws Exception
     */
    public void addShard(String shardName, URI shardDir, String collectionName) throws Exception;


    /**
     * Stop including the shard (directory of data). After this call returns, the
     * server should use the directory, or even assume that it exists.
     *
     * @param shardName Which shard to stop using. This was the name provided in
     *                  addShard().
     * @throws Exception
     */
    public void removeShard(String shardName) throws Exception;



    /**
     * @return all included shards
     */
    public Collection<String> getShards();


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
    public Map<String, String> getShardMetaData(String shardName) throws Exception;


    /**
     * get IndexSearcher
     * @param shardName shard
     * @return
     */
    public SearcherHandle getSearcherHandleByShard(String shardName);


    /**
     * get Solr Core
     * @param shardName ShardName
     * @return
     */
    public SolrHandler getSolrHandlerByShard(String shardName);




    /**
     * Release all resources. No further calls will happen after this call.
     *
     * @throws Exception
     */
    public void shutdown() throws Exception;

}
