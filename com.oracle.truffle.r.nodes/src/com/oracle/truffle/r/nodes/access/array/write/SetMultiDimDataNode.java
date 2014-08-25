/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.access.array.write;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild(value = "val", type = RNode.class), @NodeChild(value = "vec", type = RNode.class), @NodeChild(value = "pos", type = RNode.class),
                @NodeChild(value = "currDimLevel", type = RNode.class), @NodeChild(value = "srcArrayBase", type = RNode.class), @NodeChild(value = "dstArrayBase", type = RNode.class),
                @NodeChild(value = "accSrcDimensions", type = RNode.class), @NodeChild(value = "accDstDimensions", type = RNode.class)})
abstract class SetMultiDimDataNode extends RNode {

    public abstract Object executeMultiDimDataSet(VirtualFrame frame, RAbstractContainer value, RAbstractVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions);

    private final NACheck posNACheck;
    private final NACheck elementNACheck;
    private final boolean isSubset;

    @Child private SetMultiDimDataNode setMultiDimDataRecursive;

    private Object setMultiDimData(VirtualFrame frame, RAbstractVector value, RAbstractVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions, NACheck posCheck, NACheck elementCheck) {
        if (setMultiDimDataRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setMultiDimDataRecursive = insert(SetMultiDimDataNodeFactory.create(posCheck, elementCheck, this.isSubset, null, null, null, null, null, null, null, null));
        }
        return setMultiDimDataRecursive.executeMultiDimDataSet(frame, value, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
    }

    protected SetMultiDimDataNode(NACheck posNACheck, NACheck elementNACheck, boolean isSubset) {
        this.posNACheck = posNACheck;
        this.elementNACheck = elementNACheck;
        this.isSubset = isSubset;
    }

    protected SetMultiDimDataNode(SetMultiDimDataNode other) {
        this.posNACheck = other.posNACheck;
        this.elementNACheck = other.elementNACheck;
        this.isSubset = other.isSubset;
    }

    @Specialization
    protected RList setData(VirtualFrame frame, RAbstractVector value, RList vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        posNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, true, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, pos, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAtAsObject(dstIndex % value.getLength()), null);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, true, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, pos, newAccSrcDimensions);
                setMultiDimData(frame, value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RIntVector setData(VirtualFrame frame, RAbstractIntVector value, RIntVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        posNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, pos, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()), elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, pos, newAccSrcDimensions);
                setMultiDimData(frame, value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RDoubleVector setData(VirtualFrame frame, RAbstractDoubleVector value, RDoubleVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        posNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, pos, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()), elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, pos, newAccSrcDimensions);
                setMultiDimData(frame, value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RLogicalVector setData(VirtualFrame frame, RAbstractLogicalVector value, RLogicalVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        posNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, pos, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()), elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, pos, newAccSrcDimensions);
                setMultiDimData(frame, value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RStringVector setData(VirtualFrame frame, RAbstractStringVector value, RStringVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        posNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, pos, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()), elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, pos, newAccSrcDimensions);
                setMultiDimData(frame, value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RComplexVector setData(VirtualFrame frame, RAbstractComplexVector value, RComplexVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        posNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, pos, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()), elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, pos, newAccSrcDimensions);
                setMultiDimData(frame, value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RRawVector setData(VirtualFrame frame, RAbstractRawVector value, RRawVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        posNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, pos, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()));
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (UpdateArrayHelperNode.seenNAMultiDim(posNACheck.check(pos), value, false, isSubset, getEncapsulatingSourceSection())) {
                    continue;
                }
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, pos, newAccSrcDimensions);
                setMultiDimData(frame, value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    private int getNewArrayBase(int srcArrayBase, int pos, int newAccSrcDimensions) {
        int newSrcArrayBase;
        if (posNACheck.check(pos)) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NA_SUBSCRIPTED);
        } else {
            newSrcArrayBase = srcArrayBase + newAccSrcDimensions * (pos - 1);
        }
        return newSrcArrayBase;
    }

    private int getSrcIndex(int srcArrayBase, int pos, int newAccSrcDimensions) {
        if (posNACheck.check(pos)) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NA_SUBSCRIPTED);
        } else {
            return srcArrayBase + newAccSrcDimensions * (pos - 1);
        }
    }

}