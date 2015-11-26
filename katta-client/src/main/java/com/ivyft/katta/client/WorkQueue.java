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
package com.ivyft.katta.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * This class manages the multiple NodeInteraction threads for a call.
 * The initial node interactions and any resulting retries go through the
 * same execute() method. We allow blocking or non-blocking access
 * to the result set, or you can provide a custom policy to control the
 * length of time spent waiting for results to complete.
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
class WorkQueue<T> implements INodeExecutor {

    private static int instanceCounter = 0;


    private INodeInteractionFactory<T> interactionFactory;

    protected ExecutorService executor;

    private INodeProxyManager shardManager;
    private Method method;
    private int shardArrayParamIndex;
    private Object[] args;
    private IResultReceiver<T> results;
    private int instanceId = instanceCounter++;
    private int callCounter = 0;



    private static final Logger log = LoggerFactory.getLogger(WorkQueue.class);


    /**
     * Normal constructor. Jobs submitted by execute() will result in a
     * NodeInteraction instance being created and run(). The WorkQueue
     * is initially emtpy. Call execute() to add jobs.
     * <p/>
     * <b>DO NOT CHANGE THE ARGUMENTS WHILE THIS CALL IS RUNNING OR YOU WILL BE
     * SORRY.</b>
     *
     * @param shardManager         The class that maintains the node/shard maps, the node selection
     *                             policy, and the node proxies.
     * @param allShards            The entire set of shards for this request. When all these shards
     *                             have reported in, the result is complete.
     * @param nodeCount 发生RPC次数, 用这个来确定返回多少个RPC立即结束
     * @param method               Which method to call on the server side.
     * @param shardArrayParamIndex Which paramater, if any, should be overwritten with an array of
     *                             the shard names (per server call). Pass -1 to disable this.
     * @param args                 The arguments to pass in to the method on the server side.
     */
    protected WorkQueue(ExecutorService executor,
                        INodeProxyManager shardManager,
                        Set<String> allShards,
                        int nodeCount,
                        Method method,
                        int shardArrayParamIndex,
                        Object... args) {
        this.executor = executor;
        init(new INodeInteractionFactory<T>() {
            public Runnable createInteraction(Method method,
                                              Object[] args,
                                              int shardArrayParamIndex,
                                              String node,
                                              Map<String, List<String>> nodeShardMap,
                                              int tryCount,
                                              int maxTryCount,
                                              INodeProxyManager shardManager,
                                              INodeExecutor nodeExecutor,
                                              IResultReceiver<T> results) {
                return new NodeInteraction<T>(method,
                        args,
                        shardArrayParamIndex,
                        node,
                        nodeShardMap,
                        tryCount,
                        maxTryCount,
                        shardManager,
                        nodeExecutor,
                        results);
            }
        }, shardManager, allShards, nodeCount, method, shardArrayParamIndex, args);
    }


    /**
     * Used by unit tests. By providing an alternate factory, this class can be tested without creating
     * and NodeInteractions.
     *
     * @param interactionFactory
     * @param shardManager         The class that maintains the node/shard maps, the node selection
     *                             policy, and the node proxies.
     * @param allShards            The entire set of shards for this request. When all these shards
     *                             have reported in, the result is complete.
     * @param method               Which method to call on the server side.
     * @param shardArrayParamIndex Which paramater, if any, should be overwritten with an array of
     *                             the shard names (per server call). Pass -1 to disable this.
     * @param args                 The arguments to pass in to the method on the server side.
     */
    private void init(INodeInteractionFactory<T> interactionFactory,
                        INodeProxyManager shardManager,
                        Set<String> allShards,
                        int nodeCount,
                        Method method,
                        int shardArrayParamIndex,
                        Object... args) {
        if (shardManager == null || allShards == null || method == null) {
            throw new IllegalArgumentException("Null passed to new WorkQueue()");
        }
        if (allShards.isEmpty()) {
            throw new IllegalArgumentException("No shards passed to new WorkQueue()");
        }
        this.interactionFactory = interactionFactory;
        this.shardManager = shardManager;
        this.method = method;
        this.shardArrayParamIndex = shardArrayParamIndex;
        this.args = args != null ? args : new Object[0];
        IClosedListener closedListener = new IClosedListener() {
            public void clientResultClosed() {
                close();
            }
        };

        //构造ClientResult, 这里可以变化
        this.results = new ClientResultReceiver<T>(closedListener, nodeCount, allShards);
    }

    /**
     * Submit a job, which is a call to a server node via an RPC proxy using a NodeInteraction.
     * Ignored if called after shutdown(), or after result set is closed.
     *
     * @param node         The node on which to execute the method.
     * @param nodeShardMap The current node shard map, with failed nodes removed if this is a retry.
     * @param tryCount     This call is the Nth retry. Starts at 1.
     * @param maxTryCount  How often the call should be repeated in maximum.
     */
    public void execute(String node, Map<String, List<String>> nodeShardMap, int tryCount, int maxTryCount) {
        if (!executor.isShutdown() && !results.isClosed()) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Creating interaction with %s, will use shards: %s, tryCount=%d (id=%d)", node,
                        nodeShardMap.get(node), tryCount, instanceId));
            }
            Runnable interaction = interactionFactory.createInteraction(method,
                    args,
                    shardArrayParamIndex,
                    node,
                    nodeShardMap,
                    tryCount,
                    maxTryCount,
                    shardManager,
                    this,
                    results);
            if (interaction != null) {
                try {
                    executor.execute(interaction);
                } catch (RejectedExecutionException e) {
                    // This could happen, but should be rare.
                    log.warn(String.format("Failed to submit node interaction %s (id=%d)", interaction, instanceId));
                }
            } else {
                log.error("Null node interaction runnable for node " + node);
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "Not creating interaction with %s, shards=%s, tryCount=%d, executor=%s, result=%s (id=%d)", node,
                        nodeShardMap.get(node), tryCount, executor.isShutdown() ? "shutdown" : "running", results, instanceId));
            }
        }
    }

    /**
     * Stop all threads. Close the result set (making it immutable).
     * Any calls to execute() after this will be ignored.
     */
    public void close() {
        if (!results.isClosed()) {
            results.close();
        }
    }



    /**
     * Used by unit tests to make toString() output repeatable.
     */
    public static void resetInstanceCounter() {
        instanceCounter = 0;
    }


    /**
     * Wait up to timeout msec for the results to be complete (all shards
     * reporting) then stop the threads and return what we have so far.
     *
     * @param timeout maximum msec to wait for.
     * @return the results of the call, which will be closed.
     */
    public IResultReceiver<T> getResults(long timeout) {
        return getResults(new ResultCompletePolicy<T>(timeout, true));
    }

    /**
     * Wait up to timeout msec for the results to be complete (all shards
     * reporting) then return what we have so far. If shutdown is true, the result
     * will be closed and any remaining threads will be killed.
     * <p/>
     * If you want to do your own polling, pass in 0, true. If you want a simple
     * all-or-nothing result, pass in N, true, then check isOK() on the result. If
     * you want to wait for a while then decide for yourself what to do, pass in
     * N, false (or see IResultPolicy).
     *
     * @param timeout  maximum msec to wait for.
     * @param shutdown if true, stops the search.
     * @return the results of the call, which will be closed.
     */
    public IResultReceiver<T> getResults(long timeout, boolean shutdown) {
        return getResults(new ResultCompletePolicy<T>(timeout, shutdown));
    }

    /**
     * //TODO 客户端搜索等待时长, 在这里控制
     *
     * Use a user-provided policy to decide how long to wait for and whether to
     * terminate the call.
     *
     * @param policy How to decide when to return and to terminate the call.
     * @return the results, which may or may not be complete and/or closed.
     */
    public IResultReceiver<T> getResults(IResultPolicy<T> policy) {
        int callId = callCounter++;
        long start = 0;
        if (log.isTraceEnabled()) {
            log.trace(String.format("getResults() policy = %s (id=%d:%d)", policy, instanceId, callId));
            start = System.currentTimeMillis();
        }
        long waitTime = 0;
        while (true) {
            synchronized (results) {
                // Need to stay synchronized before waitTime() through wait() or we will
                // miss notifications.
                waitTime = policy.waitTime(results);
                if (waitTime > 0 && !results.isClosed()) {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("Waiting %d ms, results = %s (id=%d:%d)", waitTime, results, instanceId, callId));
                    }
                    try {
                        //客户端在这里睡眠, 要注意控制这个时间
                        results.wait(waitTime);
                    } catch (InterruptedException e) {
                        log.debug("Interrupted", e);
                    }
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("Done waiting, results = %s (id=%d:%d)", results, instanceId, callId));
                    }
                } else {
                    break;
                }
            }
        }
        if (waitTime < 0) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Shutting down work queue, results = %s (id=%d:%d)", results, instanceId, callId));
            }
            if(!results.isClosed()) {
                results.close();
            }
        }
        if (log.isTraceEnabled()) {
            long time = System.currentTimeMillis() - start;
            log.trace(String.format("Returning results = %s, took %d ms (id=%d:%d)", results, time, instanceId, callId));
        }
        return results;
    }

    @Override
    public String toString() {
        String argsStr = Arrays.asList(args).toString();
        argsStr = argsStr.substring(1, argsStr.length() - 1);
        return String.format("WorkQueue[%s.%s(%s) (id=%d)]", method.getDeclaringClass().getSimpleName(), method.getName(),
                argsStr, instanceId);
    }

}
