/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_scale extends TestBase {

    @Test
    public void testscale1() {
        assertEval("argv <- structure(list(x = structure(c(0.0280387932434678, 0.789736648323014,     0.825624888762832, 0.102816025260836, 0.290661531267688,     0.0517604837659746, 0.610383243998513, 0.78207225818187,     0.136790128657594, 0.8915234063752, 0.0216042066458613, 0.408875584136695,     0.69190051057376, 0.595735886832699, 0.936268283519894, 0.592950375983492,     0.852736486820504, 0.610123937483877, 0.600582004291937,     0.38303488586098, 0.412859325064346, 0.388432375853881, 0.457582515198737,     0.701614629011601, 0.449137942166999, 0.533179924823344,     0.317685069283471, 0.800954289967194, 0.0273033923003823,     0.496913943905383, 0.903582146391273, 0.725298138801008,     0.616459952667356, 0.341360273305327, 0.0613401387818158,     0.7339238144923, 0.720672776456922, 0.214702291414142, 0.283225567312911,     0.515186718199402, 0.558621872216463, 0.770191126968712,     0.959201833466068, 0.80451478343457, 0.307586128590629, 0.902739278972149,     0.992322677979246, 0.167487781029195, 0.796250741928816,     0.549091263208538, 0.0876540709286928, 0.424049312015995,     0.573274190537632, 0.763274750672281, 0.405174027662724,     0.828049632022157, 0.128607030957937, 0.479592794785276,     0.631105397362262, 0.406053610146046, 0.661386628635228,     0.958720558788627, 0.576542558381334, 0.0483133427333087,     0.615997062064707, 0.341076754732057, 0.901286069769412,     0.521056747529656, 0.92834516079165, 0.228773980634287, 0.458389508537948,     0.987496873131022, 0.0315267851110548, 0.872887850506231,     0.59517983533442, 0.935472247190773, 0.145392092177644, 0.255368477664888,     0.322336541488767, 0.507066876627505, 0.0745627176947892,     0.0313172969035804, 0.499229126842692, 0.868204665370286,     0.232835006900132, 0.422810809221119, 0.803322346881032,     0.00151223805733025, 0.175151102710515, 0.469289294909686),     .Dim = c(10L, 9L))), .Names = 'x');"
                        + "do.call('scale', argv)");
    }

}
