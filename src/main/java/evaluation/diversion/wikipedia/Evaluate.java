/**
 *  The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit
 *  Copyright (C) 2015 George Tsatsanifos <gtsatsanifos@gmail.com>
 *
 *  The GRA.M.MA.R. toolkit is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published 
 *  by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package evaluation.diversion.wikipedia;

import java.util.ArrayList;

abstract public class Evaluate {

    protected static final int defaultIndex   = 1;

    protected static final String[] languages = {"el"};//,"es"};

    protected static final int[] resultsizes  = {5,10,20,40};
    protected static final int[] numberseeds  = {1,2,3,4};

    protected static final float[] lambdas   = {.2f,.4f,.6f,.8f};
    protected static final float[] betas     = {.2f,.4f,.6f,.8f};

    protected static final long[] timeouts = {5,10,20,40};

    protected static final int[] radiuses     = {2,4,6,8};


    static protected void checkResult (ArrayList<ArrayList<String>> result, int k, int n) {
        if (result.size()<k) throw new RuntimeException ("\n!! ERROR - Not the appropriate result cardinality ("+result.size()+"<"+k+") !!");
        for (ArrayList<String> resultElement : result) 
            if (resultElement.size()<n)
                throw new RuntimeException ("\n!! ERROR - Insufficient number of result elements !!");
    }
}
