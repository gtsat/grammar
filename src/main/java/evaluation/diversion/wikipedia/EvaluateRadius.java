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

import wikipedia.DiversePedia;

public class EvaluateRadius extends Evaluate {

    public static void main (String[] args) {
        int I = defaultIndex;
        int Q = Integer.parseInt (args[0]);
        boolean minsum = true;
        if (Q<0) {Q=-Q; minsum=false;}

        for (String language : languages) {
            String folderpath = "../data/wikipedia/wikipedia-"+language+"/";
            DiversePedia dp = new DiversePedia (folderpath+"edgelist.filtered.txt",
                                                folderpath+"model.txt",
                                                folderpath+"titles.txt");

            dp.setWeightParameters (.8f,.0f,.8f);

            System.out.println ("** Running wikipedia evaluation of "+Q+" queries for '"+language+"'.");

            for (int RADIUS : radiuses) {

                long summedmemoryCoverage = 0L;
                long summeddurationCoverage = 0L;
                double summedscoreCoverage = 0.0f;

                for (int q=0; q<Q; ++q) 
                try{
                    String qcenter = dp.qcenterRandom();

                    long start = System.currentTimeMillis();
                    summedscoreCoverage += dp.scoreBestCoverage (qcenter,resultsizes[I],RADIUS,minsum);
                    summeddurationCoverage += System.currentTimeMillis() - start;
                    summedmemoryCoverage += dp.getQueryMemRequirements();
                }catch (RuntimeException e) {
                    e.printStackTrace();
                    --q;
                }
                System.out.println ("[ "+ RADIUS + "\t " + summeddurationCoverage/1000.0f/Q 
                                        + "\t " + summedscoreCoverage/Q 
                                        + "\t " + ((summedmemoryCoverage/Q)>>10) + " ]");
            }
        }
    }
}
