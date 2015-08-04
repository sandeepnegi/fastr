/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.instrument.wrappers;

import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.RSyntaxNode;

@NodeInfo(cost = NodeCost.NONE)
public final class RNodeWrapper extends com.oracle.truffle.r.nodes.RNode implements WrapperNode {
    @Child com.oracle.truffle.r.nodes.RNode child;
    @Child private ProbeNode probeNode;

    public RNodeWrapper(com.oracle.truffle.r.nodes.RNode child) {
        assert child != null;
        assert !(child instanceof RNodeWrapper);
        this.child = child;
    }

    public String instrumentationInfo() {
        return "Wrapper node for com.oracle.truffle.r.nodes.RNode";
    }

    public Node getChild() {
        return child;
    }

    public Probe getProbe() {
        try {
            return probeNode.getProbe();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("A lite-Probed wrapper has no explicit Probe");
        }
    }

    public void insertProbe(ProbeNode newProbeNode) {
        this.probeNode = newProbeNode;
    }

    @Override
    public java.lang.Object execute(com.oracle.truffle.api.frame.VirtualFrame frame) {
        probeNode.enter(child, frame);

        java.lang.Object result;
        try {
            result = child.execute(frame);
            probeNode.returnValue(child, frame, result);
            return result;
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw (e);
        }
    }

    @Override
    public RSyntaxNode getRSyntaxNode() {
        return child.asRSyntaxNode();
    }

    @Override
    public boolean isInstrumentable() {
        return false;
    }

}
