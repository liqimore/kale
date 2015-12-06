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


import com.ivyft.katta.protocol.InteractionProtocol;
import com.ivyft.katta.util.ZkConfiguration;

import java.io.File;


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
public class NodeContext {


    /**
     * Node节点
     */
    private final Node node;


    /**
     * Node复制数据Manager
     */
    private final ShardManager shardManager;


    /**
     * ZK操作协议
     */
    private final InteractionProtocol protocol;


    /**
     * Node操作
     */
    private final IContentServer nodeManaged;


    /**
     * 构造方法
     * @param protocol
     * @param node
     * @param shardManager
     * @param nodeManaged
     */
    public NodeContext(InteractionProtocol protocol, Node node, ShardManager shardManager, IContentServer nodeManaged) {
        this.protocol = protocol;
        this.node = node;
        this.shardManager = shardManager;
        this.nodeManaged = nodeManaged;
    }

    public Node getNode() {
        return this.node;
    }

    public ShardManager getShardManager() {
        return this.shardManager;
    }

    public InteractionProtocol getProtocol() {
        return this.protocol;
    }

    public IContentServer getContentServer() {
        return this.nodeManaged;
    }


    public ZkConfiguration getZkConfiguration() {
        return protocol.getZkConfiguration();
    }


    public String getProperty(String key) {
        return getZkConfiguration().getString(key);
    }

    public String getProperty(String key, String defaultValue) {
        return getZkConfiguration().getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        getZkConfiguration().setProperty(key, value);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return getZkConfiguration().getBoolean(key, defaultValue);
    }

    public int getInt(String key) {
        return getZkConfiguration().getInt(key);
    }

    public int getInt(String key, int defaultValue) {
        return getZkConfiguration().getInt(key, defaultValue);
    }

    public float getFloat(String key, float defaultValue) {
        return getZkConfiguration().getFloat(key, defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        return getZkConfiguration().getDouble(key, defaultValue);
    }

    public File getFile(String key) {
        return getZkConfiguration().getFile(key);
    }
}
