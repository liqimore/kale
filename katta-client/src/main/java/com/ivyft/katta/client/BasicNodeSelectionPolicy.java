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

import com.ivyft.katta.util.CircularList;
import com.ivyft.katta.util.One2ManyListMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic node selection policy.  The nodes to search will always be returned in
 * the same order, which is ideal for tests, but may cause an uneven
 * distribution of load in a multi-node cluster.  This implementation should
 * only be used for testing.  Real clusters should use
 * ShuffleNodeSelectionPolicy.
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
public class BasicNodeSelectionPolicy implements INodeSelectionPolicy {

    private Map<String, CircularList<String>> _shardsToNodeMap = new ConcurrentHashMap<String, CircularList<String>>();


    public BasicNodeSelectionPolicy() {
    }

    public void update(String shard, Collection<String> nodes) {
        _shardsToNodeMap.put(shard, new CircularList<String>(nodes));
    }

    @Override
    public Collection<String> getShardNodes(String shard) throws ShardAccessException {
        CircularList<String> nodeList = _shardsToNodeMap.get(shard);
        if (nodeList == null) {
            throw new ShardAccessException(shard);
        }
        return nodeList.asList();
    }

    @Override
    public List<String> remove(String shard) throws ShardAccessException {
        CircularList<String> nodes = _shardsToNodeMap.remove(shard);
        if (nodes == null) {
            throw new ShardAccessException(shard);
        }
        return nodes.asList();
    }

    public void removeNode(String node) {
        Set<String> shards = _shardsToNodeMap.keySet();
        for (String shard : shards) {
            CircularList<String> nodes = _shardsToNodeMap.get(shard);
            synchronized (nodes) {
                nodes.remove(node);
            }
        }
    }


    /**
     * //TODO 在客户端查询的时候创建分布式查询计划，随机的给每个节点分配shard的查询。
     *
     * @param shards 一个Index的没一个shard
     * @return 返回查询计划
     * @throws ShardAccessException
     */
    public Map<String, List<String>> createNode2ShardsMap(Collection<String> shards) throws ShardAccessException {
        One2ManyListMap<String, String> node2ShardsMap = new One2ManyListMap<String, String>();
        for (String shard : shards) {
            CircularList<String> nodeList = _shardsToNodeMap.get(shard);
            if (nodeList == null) {
                // no entry in the map means the shard is undeployed/never was deployed
                continue;
            }
            if (nodeList.isEmpty()) {
                // vs. empty entry in the map means the shard was deployed, but now is inaccessible
                throw new ShardAccessException(shard);
            }
            String node;
            synchronized (nodeList) {
                //TODO 分布式查询，每次轮迅，负载均衡
                node = nodeList.getNext();
            }
            node2ShardsMap.add(node, shard);
        }

        return node2ShardsMap.asMap();
    }



    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DefaultNodeSelectionPolicy: ");
        String sep = "";
        for (Map.Entry<String, CircularList<String>> e : _shardsToNodeMap.entrySet()) {
            builder.append(sep);
            builder.append(e.getKey());
            builder.append(" --> ");
            builder.append(e.getValue());
            sep = " ";
        }
        return builder.toString();
    }

}
