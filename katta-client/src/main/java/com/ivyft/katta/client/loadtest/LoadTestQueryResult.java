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
package com.ivyft.katta.client.loadtest;

import java.io.Serializable;


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
public class LoadTestQueryResult implements Serializable {

    private long _startTime;
    private long _endTime;
    private String _query;
    private String _nodeId;

    public LoadTestQueryResult() {
        // do nothing
    }

    public LoadTestQueryResult(long startTime, long endTime, String query, String nodeId) {
        _startTime = startTime;
        _endTime = endTime;
        _query = query;
        _nodeId = nodeId;
    }

    public long getStartTime() {
        return _startTime;
    }

    public void setStartTime(long startTime) {
        _startTime = startTime;
    }

    public long getEndTime() {
        return _endTime;
    }

    public void setEndTime(long endTime) {
        _endTime = endTime;
    }

    public String getQuery() {
        return _query;
    }

    public void setQuery(String query) {
        _query = query;
    }

    public String getNodeId() {
        return _nodeId;
    }

    public void setNodeId(String nodeId) {
        _nodeId = nodeId;
    }

}
