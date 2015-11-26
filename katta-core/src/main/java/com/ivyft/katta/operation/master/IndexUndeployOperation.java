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
package com.ivyft.katta.operation.master;

import com.ivyft.katta.master.MasterContext;
import com.ivyft.katta.operation.OperationId;
import com.ivyft.katta.operation.node.OperationResult;
import com.ivyft.katta.operation.node.ShardUndeployOperation;
import com.ivyft.katta.protocol.InteractionProtocol;
import com.ivyft.katta.protocol.metadata.IndexMetaData;
import com.ivyft.katta.protocol.metadata.Shard;
import com.ivyft.katta.util.CollectionUtil;
import com.ivyft.katta.util.ZkConfiguration;
import com.ivyft.katta.util.ZkConfiguration.PathDef;
import org.I0Itec.zkclient.ZkClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 *
 * <pre>
 *
 * Created by IntelliJ IDEA.
 * User: zhenqin
 * Date: 13-11-19
 * Time: 上午8:59
 * To change this template use File | Settings | File Templates.
 *
 * </pre>
 *
 * @author zhenqin
 */
public class IndexUndeployOperation implements MasterOperation {

    private static final long serialVersionUID = 1L;


    /**
     * 要卸载、删除的Index Name
     */
    private String indexName;


    /**
     * Index Shard信息
     */
    private IndexMetaData indexMD;


    /**
     * 卸载Index 构造方法
     * @param indexName
     */
    public IndexUndeployOperation(String indexName) {
        this.indexName = indexName;
    }

    @Override
    public List<OperationId> execute(MasterContext context, List<MasterOperation> runningOperations) throws Exception {
        InteractionProtocol protocol = context.getProtocol();
        this.indexMD = protocol.getIndexMD(this.indexName);

        Map<String, List<String>> shard2NodesMap = protocol.getShard2NodesMap(
                Shard.getShardNames(this.indexMD.getShards()));
        Map<String, List<String>> node2ShardsMap = CollectionUtil.invertListMap(shard2NodesMap);
        Set<String> nodes = node2ShardsMap.keySet();
        List<OperationId> nodeOperationIds = new ArrayList<OperationId>(nodes.size());
        for (String node : nodes) {
            List<String> nodeShards = node2ShardsMap.get(node);
            //给负载Index Shard的每个节点发送一个卸载的消息
            OperationId operationId = protocol.addNodeOperation(node, new ShardUndeployOperation(nodeShards));
            nodeOperationIds.add(operationId);
        }
        protocol.unpublishIndex(this.indexName);
        return nodeOperationIds;
    }

    @Override
    public void nodeOperationsComplete(MasterContext context, List<OperationResult> results) throws Exception {
        ZkClient zkClient = context.getProtocol().getZkClient();
        ZkConfiguration zkConf = context.getProtocol().getZkConfiguration();
        for (Shard shard : this.indexMD.getShards()) {
            zkClient.deleteRecursive(zkConf.getZkPath(PathDef.SHARD_TO_NODES, shard.getName()));
        }
    }

    @Override
    public ExecutionInstruction getExecutionInstruction(List<MasterOperation> runningOperations) throws Exception {
        for (MasterOperation operation : runningOperations) {
            if (operation instanceof IndexUndeployOperation
                    && ((IndexUndeployOperation) operation).indexName.equals(this.indexName)) {
                return ExecutionInstruction.CANCEL;
            }
        }
        return ExecutionInstruction.EXECUTE;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + Integer.toHexString(hashCode()) + ":" + indexName;
    }

}
