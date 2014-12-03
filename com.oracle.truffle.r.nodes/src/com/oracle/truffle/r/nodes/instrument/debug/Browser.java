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
package com.oracle.truffle.r.nodes.instrument.debug;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.r.runtime.BrowserQuitException;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RContext;
import com.oracle.truffle.r.runtime.ROptions;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * The interactive component of the {@code browser} function.
 */
public class Browser {

    public enum ExitMode {
        STEP,
        NEXT,
        CONTINUE,
        FINISH
    }

    @CompilerDirectives.TruffleBoundary
    public static ExitMode interact(MaterializedFrame frame) {
        RContext.ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
        REnvironment callerEnv = REnvironment.frameToEnvironment(frame);
        String savedPrompt = ch.getPrompt();
        ch.setPrompt(browserPrompt(RArguments.getDepth(frame)));
        ExitMode exitMode = ExitMode.NEXT;
        try {
            LW: while (true) {
                String input = ch.readLine();
                if (input.length() == 0) {
                    RLogicalVector browserNLdisabledVec = (RLogicalVector) ROptions.getValue("browserNLdisabled");
                    if (!RRuntime.fromLogical(browserNLdisabledVec.getDataAt(0))) {
                        break;
                    }
                } else {
                    input = input.trim();
                }
                switch (input) {
                    case "c":
                    case "cont":
                        exitMode = ExitMode.CONTINUE;
                        break LW;
                    case "n":
                        exitMode = ExitMode.NEXT;
                        break LW;
                    case "s":
                        exitMode = ExitMode.STEP;
                        break LW;
                    case "f":
                        exitMode = ExitMode.FINISH;
                        break LW;
                    case "Q":
                        throw new BrowserQuitException();
                    case "where": {
                        int ix = 0;
                        Frame stackFrame = frame;
                        do {
                            ch.println(RArguments.safeGetCallSourceString(stackFrame));
                            ix++;
                        } while ((stackFrame = Utils.getStackFrame(FrameInstance.FrameAccess.READ_ONLY, ix)) != null);
                        ch.println("");
                        break;
                    }

                    default:
                        RContext.getEngine().parseAndEval("<browser_input>", input, frame, callerEnv, true, false);
                        break;
                }
            }
        } finally {
            ch.setPrompt(savedPrompt);
        }
        return exitMode;
    }

    private static String browserPrompt(int depth) {
        return "Browse[" + depth + "]> ";
    }

}