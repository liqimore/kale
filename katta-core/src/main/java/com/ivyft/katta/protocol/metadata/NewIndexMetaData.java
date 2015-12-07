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
package com.ivyft.katta.protocol.metadata;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * <p>
 *     该类是一份索引的对应Bean类
 * </p>
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
public class NewIndexMetaData implements Serializable {


    /**
     * 序列化
     */
    private static final long serialVersionUID = 1L;


    /**
     * 索引名称
     */
    private String name;


    /**
     * 索引Path
     */
    private String path;


    /**
     * 分片数
     */
    private int shardNum;


    /**
     * 每个分片内部可落入的数据step
     */
    private int shardStep;


    /**
     * 索引下的 shards
     */
    private Set<Shard> shards = new HashSet<Shard>();


    /**
     * 部署错误的error
     */
    private IndexDeployError deployError;


    /**
     *
     * @param name 名字
     * @param path 地址
     * @param shardNum 分区数
     * @param shardStep 每个分区的 Step
     */
    public NewIndexMetaData(String name, String path,
                            int shardNum,
                            int shardStep) {
        this.name = name;
        this.path = path;
        this.shardNum = shardNum;
        this.shardStep = shardStep;
    }

    public String getPath() {
        return this.path;
    }


    public int getShardNum() {
        return shardNum;
    }

    public void setShardNum(int shardNum) {
        this.shardNum = shardNum;
    }

    public int getShardStep() {
        return shardStep;
    }

    public void setShardStep(int shardStep) {
        this.shardStep = shardStep;
    }

    public String getName() {
        return this.name;
    }


    public void addShard(Shard shard) {
        this.shards.add(shard);
    }



    public void addShards(Collection<Shard> shards) {
        this.shards.addAll(shards);
    }


    public Set<Shard> getShards() {
        return this.shards;
    }

    public Shard getShard(String shardName) {
        for (Shard shard : this.shards) {
            if (shard.getName().equals(shardName)) {
                return shard;
            }
        }
        return null;
    }

    public String getShardPath(String shardName) {
        String shardPath = null;
        Shard shard = getShard(shardName);
        if (shard != null) {
            shardPath = shard.getPath();
        }
        return shardPath;
    }

    public void setDeployError(IndexDeployError deployError) {
        this.deployError = deployError;
    }

    public IndexDeployError getDeployError() {
        return this.deployError;
    }

    public boolean hasDeployError() {
        return this.deployError != null;
    }

    @Override
    public String toString() {
        return "name: " + this.name +
                ", shardNum: " + this.shardNum +
                ", shardStep: " + this.shardStep +
                ", path: " + this.path;
    }

}
