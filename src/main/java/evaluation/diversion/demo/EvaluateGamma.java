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

import wikipedia.DiversePedia;
import java.util.ArrayList;

public class EvaluateGamma extends Evaluate {

    public static void main (String[] args) {
        int I = defaultIndex;
        int Q = Integer.parseInt (args[0]);
        boolean minsum = true;
        if (Q<0) {Q=-Q; minsum=false;}

        for (boolean symmetry=false;;symmetry=true) {
        for (String language : languages) {
            String folderpath = "../data/wikipedia/wikipedia-"+language+"/";
            DiversePedia dp = new DiversePedia (folderpath+"edgelist.filtered.txt",
                                                folderpath+"model.txt",
                                                folderpath+"titles.txt");

            dp.setMaxCachingRadius (Integer.MAX_VALUE);

            System.out.println ("** Running wikipedia evaluation of "+Q+" queries for '"+language+"'.");

            ArrayList<Float> scores = new ArrayList<>();

            for (float beta : betas) {

                dp.setWeightParameters (0.5f,1.0f-beta,beta);

                long summeddurationGreedy = 0L;
                long summeddurationSwap = 0L;

                long summedmemoryGreedy = 0L;
                long summedmemorySwap = 0L;

                float summedscoreGreedy = 0.0f;
                float summedscoreSwap = 0.0f;

                for (int q=0; q<Q; ++q) {
                    try{
                    String qcenter = dp.qcenterRandom();

                    long start = System.currentTimeMillis();
                    ArrayList<ArrayList<String>> seeds = dp.retrieveSeeds (qcenter,numberseeds[I],resultsizes[0],minsum,scores);
                    summeddurationGreedy += System.currentTimeMillis() - start;
                    summedmemoryGreedy += dp.getQueryMemRequirements();
                    summedscoreGreedy += scores.get(0);

                    start = System.currentTimeMillis();
                    ArrayList<String> result = dp.processSeeds (qcenter,seeds,numberseeds[I],minsum,scores).get(0);
                    summeddurationSwap += System.currentTimeMillis() - start;
                    summedmemorySwap += dp.getQueryMemRequirements();
                    summedscoreSwap += scores.get(0);

                    for (int i=1;i<=I;++i) {
                        start = System.currentTimeMillis();
                        result = dp.processGreedyIncremental (result, qcenter, resultsizes[i], minsum, scores);
                        summeddurationGreedy += System.currentTimeMillis() - start;
                        summedmemoryGreedy += dp.getQueryMemRequirements();
                        summedscoreGreedy += scores.get(0);

                        start = System.currentTimeMillis();
                        result = dp.processSwapIncremental(qcenter, result, numberseeds[I], resultsizes[i], minsum, scores);
                        summeddurationSwap += System.currentTimeMillis() - start;
                        summedmemorySwap += dp.getQueryMemRequirements();
                        summedscoreSwap += scores.get(0);
                    }
                    }catch (RuntimeException e) {
                        e.printStackTrace();
                        --q;
                    }
                }

                System.out.println ("[ "+ beta + "\t " + summeddurationGreedy/1000.0f/Q + "\t " + summeddurationSwap/1000.0f/Q
                                        + "\t " + summedscoreGreedy/(Q*I) + "\t " + summedscoreSwap/(Q*I)
                                        + "\t " + ((summedmemoryGreedy/(Q*I))>>10) + "\t " + ((summedmemorySwap/(Q*I))>>10) + " ]");
            }
        }
        if (symmetry) break;
        }
    }
}
