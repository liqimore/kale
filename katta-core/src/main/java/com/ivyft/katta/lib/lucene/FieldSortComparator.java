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
package com.ivyft.katta.lib.lucene;

import com.ivyft.katta.util.WritableType;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import java.util.Comparator;

/**
 * Implementation of an {@link Comparator} that compares two {@link Hit} objects
 * based on a given {@link Sort} specification. This comparator helps sorting a
 * result list by field terms rather then by sore.
 * <p/>
 * This code leans on the lucene code from {@link FieldSortComparator}
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
class FieldSortComparator implements Comparator<Hit> {

    private static Comparator<Comparable> COMPARABLE_COMPARATOR = new ComparableComparator();
    private static Comparator<Comparable> REVERSED_COMPARABLE_COMPARATOR = new ComparableComparator(true);

    private final SortField[] _sortFields;
    private final WritableType[] _fieldTypes;
    private final Comparator<Comparable>[] _fieldComparators;

    public FieldSortComparator(SortField[] sortFields, WritableType[] fieldTypes) {
        _sortFields = sortFields;
        _fieldTypes = fieldTypes;
        _fieldComparators = new Comparator[sortFields.length];

        // prepare a array of comparators, for each field one. For type-information
        // we use the user provided SortField[] and the WritableType[] which are
        // auto-detected on the node side.
        for (int i = 0; i < sortFields.length; i++) {
            if (_fieldTypes[i] == WritableType.TEXT) {
                throw new UnsupportedOperationException("locale-sensitive field sort currently not supported");
                // jz: therefore we could use java.text.Collator class (see lucenes
                // FieldSortedHitQueue)
            } else if (sortFields[i].getType() == SortField.Type.CUSTOM) {
                throw new UnsupportedOperationException("custom field sort currently not supported");
            }

            if (_sortFields[i].getType() == SortField.Type.SCORE) {
                _fieldComparators[i] = REVERSED_COMPARABLE_COMPARATOR;
            } else {
                _fieldComparators[i] = COMPARABLE_COMPARATOR;
            }
        }
    }

    public SortField[] getSortFields() {
        return _sortFields;
    }

    public WritableType[] getFieldTypes() {
        return _fieldTypes;
    }

    @Override
    public int compare(Hit hit1, Hit hit2) {
        return hit1.compareTo(hit2);
    }

    public int compare(Object[] fields1, Object[] fields2) {
        int n = _sortFields.length;
        int c = 0;
        for (int i = 0; i < n && c == 0; ++i) {
            Comparable fieldTerm1 = (Comparable) fields1[i];
            Comparable fieldTerm2 = (Comparable) fields2[i];
            c = (_sortFields[i].getReverse()) ? _fieldComparators[i].compare(fieldTerm2, fieldTerm1) : _fieldComparators[i]
                    .compare(fieldTerm1, fieldTerm2);
        }
        return c;
    }

    static class ComparableComparator implements Comparator<Comparable> {

        private final boolean _reverse;

        public ComparableComparator() {
            this(false);
        }

        public ComparableComparator(boolean reverse) {
            _reverse = reverse;
        }

        @Override
        public int compare(Comparable o1, Comparable o2) {
            if (_reverse) {
                return o2.compareTo(o1);
            }
            return o1.compareTo(o2);
        }

    }

}
