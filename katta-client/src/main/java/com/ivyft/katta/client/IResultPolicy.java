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

/**
 * Allows user to get results immediately or wait for more results as they see fit.
 * Also specifies if the broadcast call should be terminated and the result closed.
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
public interface IResultPolicy<T> {

    /**
     * How much longer, if any, should we wait for results to arrive.
     * Also, should we shutdown the WorkQueue and close the ClientResult?
     *
     * @param result The results we have so far.
     * @return if > 0, sleep at most that many msec, or until a new result
     *         arrives, whichever comes first. Then call this method again.
     *         If 0, return the result immediately.
     *         if < 0, shutdown the WorkQueue, close the result, and return it immediately.
     */
    public long waitTime(IResultReceiver<T> result);

}
