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
package com.ivyft.katta.protocol;

/**
 *
 * <p>
 *
 * Registerable with {@link InteractionProtocol}.
 *
 * @see InteractionProtocol#registerDataListener(ConnectedComponent, org.I0Itec.zkclient.IZkDataListener, String)
 * ,
 *      IndexListener)
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
public interface IAddRemoveListener {

    /**
     * 加节点了
     * @param name 增加的节点名称
     */
    public void added(String name);


    /**
     * 移除节点了
     * @param name 被移除的节点名称
     */
    public void removed(String name);
}
