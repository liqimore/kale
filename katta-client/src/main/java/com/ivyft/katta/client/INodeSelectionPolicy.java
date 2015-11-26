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
package com.ivyft.katta.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Returns a Map with Nodes and shards within those nodes that have to be
 * searched by the client.
 * <p/>
 * Since shards can be replicated over different nodes and nodes can be
 * distributed in different network sections (same switch or rack, same data
 * center etc.) we allow custom selection policies to implement the logic to
 * make the smartest possible choice of nodes the client has to query. The node
 * selection policy is also the place where an load balancing schema need to be
 * implemented.
 * <p/>
 * When there are no nodes serving a shard, implementations may behave differently depending on if a shard was
 * undeployed from the cluster or simply has no nodes serving it at this time.
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
public interface INodeSelectionPolicy {

    /**
     * During startup or as soon the client get a notification about any change in
     * the grid the client calls this method with an shard-to-nodes mapping. <br>
     *
     * @param shard
     * @param nodes all the nodes which serve the shard
     */
    public void update(String shard, Collection<String> nodes);

    /**
     * @param shard
     * @return all nodes which serves the given shard
     * @throws ShardAccessException if the shard is not deployed
     */
    public Collection<String> getShardNodes(String shard) throws ShardAccessException;

    /**
     * If an index is undeployed, this method is called for each of it shards.
     *
     * @param shard
     * @return all nodes which served the shard
     * @throws ShardAccessException if the shard is not deployed
     */
    public List<String> remove(String shard) throws ShardAccessException;

    /**
     * If a node becomes not reachable, this method is called.
     *
     * @param node
     */
    public void removeNode(String node);

    /**
     * Returns a map where as key the nodeName is used and as value a list shards
     * served by node we need to query. Ideally this method returns nodes with low
     * latency to the client and alternate between nodes to load balance high
     * traffic.
     *
     * @throws ShardAccessException if one of the shards could not be accessed
     */
    public Map<String, List<String>> createNode2ShardsMap(Collection<String> shards) throws ShardAccessException;

}
