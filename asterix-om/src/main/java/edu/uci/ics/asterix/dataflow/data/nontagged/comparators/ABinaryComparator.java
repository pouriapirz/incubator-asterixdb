/*
 * Copyright 2009-2013 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.asterix.dataflow.data.nontagged.comparators;

import edu.uci.ics.asterix.om.types.ATypeTag;
import edu.uci.ics.asterix.om.types.EnumDeserializer;
import edu.uci.ics.asterix.om.types.hierachy.ATypeHierarchy;
import edu.uci.ics.hyracks.api.dataflow.value.BinaryComparatorConstant.ComparableResultCode;
import edu.uci.ics.hyracks.api.dataflow.value.IBinaryComparator;
import edu.uci.ics.hyracks.api.exceptions.HyracksDataException;

/*
 * Asterix-level comparators will extend this class so that they can execute isComparable() first before doing actual compare().
 */
public abstract class ABinaryComparator implements IBinaryComparator {

    public static ComparableResultCode isComparable(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
        // NULL Check. If one type is NULL, then we return NULL
        if (b1[s1] == ATypeTag.NULL.serialize() || b2[s2] == ATypeTag.NULL.serialize() || b1[s1] == 0 || b1[s2] == 0) {
            return ComparableResultCode.UNKNOWN;
        }

        // Checks whether two types are comparable or not
        ATypeTag tag1 = EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(b1[s1]);
        ATypeTag tag2 = EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(b2[s2]);

        // Are two types compatible, meaning that they can be compared? (e.g., compare between numeric types
        if (ATypeHierarchy.isCompatible(tag1, tag2)) {
            return ComparableResultCode.TRUE;
        } else {
            return ComparableResultCode.FALSE;
        }

    }

    public abstract int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) throws HyracksDataException;

}
