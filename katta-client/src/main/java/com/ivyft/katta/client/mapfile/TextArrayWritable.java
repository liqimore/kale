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
package com.ivyft.katta.client.mapfile;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * This class provides a way to return a list of Text objects via Hadoop RPC. It
 * provides a zero-arg constructor, which Hadoop RPC requires for return types.
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
public class TextArrayWritable implements Writable {

    public ArrayWritable array;

    public TextArrayWritable() {
        this(Collections.EMPTY_LIST);
    }

    public TextArrayWritable(List<Text> texts) {
        array = new ArrayWritable(Text.class, texts.toArray(new Writable[texts.size()]));
    }

    public void readFields(DataInput in) throws IOException {
        array.readFields(in);
    }

    public void write(DataOutput out) throws IOException {
        array.write(out);
    }

}
