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

package evaluation.diversion.demo;

abstract public class Evaluate {

    protected static final int defaultIndex   = 1;

    protected static final String[] languages = {"el"};//,"es"};

    protected static final int[] resultsizes  = {5,10,15,20};
    protected static final int[] numberseeds  = {1,1,1,1};

    protected static final float[] lambdas   = {0f,.25f,.5f,.75f,1f};
    protected static final float[] alphas    = {0f,.25f,.5f,.75f,1f};
    protected static final float[] betas     = {0f,.25f,.5f,.75f,1f};

    protected static final long[] timeouts    = {5,10,20,40};

    protected static final int[] radiuses     = {0,2,4,6,8,Integer.MAX_VALUE};
}
