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

package wikipedia;

import grammar.Graph;
import grammar.DirectedGraph;
import diversion.BestCoverage;
import diversion.ContentSimilarity;
import diversion.GreedyDiversion;
import diversion.SwapDiversion;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Scanner;
import java.util.ArrayList;
import org.apache.log4j.Logger;


public class DiversePedia {

    private static final Logger logger = Logger.getLogger (DiversePedia.class);

    private boolean symmetric = false;

    private float lambda = .8f;
    private float alpha = .0f;
    private float beta = .8f;

    private final Graph<String> graph;
    private final ContentSimilarity<String> context;

    private final Map<String,Map<String,Float>> index;
    private final Map<String,Set<String>> invertedIndex;
    
    private final Map<String,Float> maxFreqs;

    private final Map<String,String> titles;
    private final Map<String,Set<String>> titlesets;
    private final Map<String,Set<String>> invertedTitle;
    private final String[] titlesArray;

    public Graph<String> getGraph () {return graph;}

    public  Map<String,Map<String,Float>> getFrequencies () {return index;}
    public Map<String,Set<String>> getInvertedFrequencies () {return invertedIndex;}

    public Map<String,Float> getMaxFreqs () {return maxFreqs;}

    public Map<String,String> getTitles () {return titles;}
    public Map<String,Set<String>> getTitleSets () {return titlesets;}
    public Map<String,Set<String>> getInvertedTitleSets () {return invertedTitle;}


    private long queryRequirements = 0L;
    public long getQueryMemRequirements () {return queryRequirements;}

    public boolean isSymmetric () {return symmetric;}
    public void setSymmetric (boolean symm) {symmetric = symm;}

    public int getMaxCachingRadius () {return graph.getMaxCachingRadius();}
    public void setMaxCachingRadius (int radius) {graph.setMaxCachingRadius(radius);}

    public DiversePedia (String edgelist, String freqlist, String titlelist) {
        long start = System.currentTimeMillis();

        index = new TreeMap<>();
        invertedIndex = new TreeMap<>();

        maxFreqs = new TreeMap<>();

        titles = new TreeMap<>();
        titlesets = new TreeMap<>();
        invertedTitle = new TreeMap<>();

        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Importing now the term frequencies of the articles from file '" + freqlist + "'...");

        try{
            int j=0;

            File file = new File(this.getClass().getClassLoader().getResource(freqlist).getFile());
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            //BufferedReader br = new BufferedReader(new FileReader(new File(this.getClass().getClassLoader().getResource(freqlist).getFile())));
            for(String line; (line=br.readLine())!=null; ++j) {
                Map<String,Float> document = new TreeMap<>();
                String[] parsed = line.split(" ");
                index.put (parsed[0], document);

                String lemma = "";
                float maxDocFreq = -1;
                for (int i=1; i<parsed.length; ++i) {
                    int split = parsed[i].lastIndexOf(':');
                    try{
                        lemma = parsed[i].substring(0,split);
                    }catch (StringIndexOutOfBoundsException e){
                        logger.error(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Unable to parse lemma-frequency combination  '" + parsed[i] + "' of describing document '" + parsed[0] + "'.");
                    }
                    float weight = Float.parseFloat (parsed[i].substring(split+1));
                    document.put (lemma, weight);

                    if (weight>maxDocFreq) maxDocFreq = weight;

                    Set<String> references = invertedIndex.get (lemma);
                    if (references==null) {
                        references = new TreeSet<>();
                        invertedIndex.put (lemma,references);
                    }
                    references.add (parsed[0]);
                }
                maxFreqs.put (parsed[0],maxDocFreq);

                //if (j%50000==0)
                //    logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Having loaded " + j + " files in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");
            }
            //if (j%50000!=0)
                logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Having loaded " + j + " files in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");

            logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Loaded " + index.size() + " descriptions from file '" + freqlist + "' containing overall " + invertedIndex.size() + " lemmas.");
        }catch(IOException e){e.printStackTrace();}

        context = new ContentSimilarity<>(produceModelFromFreqs());

        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Model produced after " + (System.currentTimeMillis() - start) / 1000.0 + " seconds.");

        invertedIndex.clear();
        index.clear();
        maxFreqs.clear();


        graph = new DirectedGraph<>(edgelist,null,true);


        try{
            logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Importing now the titles of the articles from file '" + titlelist + "'...");

            File file = new File(this.getClass().getClassLoader().getResource(titlelist).getFile());
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            //BufferedReader br = new BufferedReader(new FileReader(new File(this.getClass().getClassLoader().getResource(titlelist).getFile())));
            for(String line; (line=br.readLine())!=null; ) {
                int split = line.indexOf(' ');
                String filename = line.substring(0,split);
                titles.put(filename,line.substring(split+1));

                Set<String> set = new TreeSet<>();
                titlesets.put (filename,set);
                for (String word : line.substring(split+1).split(" ")) {
                    word = word.toLowerCase();
                    Set<String> collection = invertedTitle.get(word);
                    if (collection==null) {
                        collection = new TreeSet<>();
                        invertedTitle.put (word,collection);
                    }
                    collection.add (filename);
                    set.add (word);
                }
            }
            logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Loaded " + titlesets.size() + " titles from file '" + titlelist + "'.");
        }catch(IOException e){e.printStackTrace();}


        titlesArray = titles.keySet().toArray (new String [titles.size()]);

        removeNonArticleLinks();
    }

    private void removeNonArticleLinks () {
        int removed = 0;
        for (String sink : graph.getSinks()) {
            if (!titles.containsKey(sink)) {
                for (String from : ((DirectedGraph<String>)graph).getBacklinks(sink))
                    graph.removeEdge(from,sink);
                graph.removeNode(sink);
                ++removed;
            }
        }
        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Removed " + removed + " nodes not belonging to the collection.");
    }

    public String qcenterRandom () {return titlesArray[(int)(Math.random()*titlesArray.length)];}

    public String qcenterByContent (Collection<String> query) {
        ArrayDeque<String> union = new ArrayDeque<>();
        for (String q : query) {
            Set<String> tempset = invertedIndex.get(q.toLowerCase());
            if (tempset!=null) union.addAll (tempset);
        }

        String result = null;
        float maxsim = -1.0f;
        for (String doc : union) {
            float sim = context.similarity (query, doc);
            if (sim > maxsim) {
                maxsim = sim;
                result = doc;
            }
        }
        return result;
    }

    public String qcenterByTitle (Collection<String> query) {
        ArrayDeque<String> union = new ArrayDeque<>();
        for (String q : query) {
            Set<String> tempset = invertedTitle.get (q.toLowerCase());
            if (tempset!=null) union.addAll(tempset);
        }

        String result = null;
        float maxsim = -1.0f;
        for (String doc : union) {
            Set<String> doctitle = titlesets.get(doc);

            float sim = 0.0f;
            for (String q : query) {
               if (doctitle.contains(q.toLowerCase())) {
                   sim += 1;
               }
            }
            sim /= doctitle.size();

            if (sim > maxsim) {
                maxsim = sim;
                result = doc;
            }
        }
        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Matching document to your query: '" + result + "'.");
        return result;
    }

    public float scoreBestCoverage (String qcenter, int n, int radius, boolean minsum) {
        float score = (float) BestCoverage.compute (qcenter,null,graph,context, 
                                                    n,radius,alpha,beta,beta,minsum,symmetric);
        queryRequirements = BestCoverage.getMaxMemRequirements();
        return score;
    }

    public ArrayList<ArrayList<String>> retrieveSeeds (String qcenter, 
                                                       int k, int n, boolean minsum,
                                                       ArrayList<Float> scores) {
        ArrayList<ArrayList<String>> result = GreedyDiversion.compute (qcenter,null,graph,context,k,n,minsum,symmetric,lambda,alpha,beta,scores);
        queryRequirements = GreedyDiversion.getMaxMemRequirements();
        return result;
    }

    public ArrayList<ArrayList<String>> retrieveSeeds (String qcenter, 
                                                       int k, int n, boolean minsum,
                                                       ArrayList<Float> scores,
                                                       long timeout) {
        ArrayList<ArrayList<String>> result = GreedyDiversion.compute (qcenter,null,graph,context,k,n,minsum,symmetric,lambda,alpha,beta,scores,0L);
        queryRequirements = GreedyDiversion.getMaxMemRequirements();
        return result;
    }

    public ArrayList<ArrayList<String>> processSeeds (String qcenter, 
                                                      ArrayList<ArrayList<String>> seeds, 
                                                      int l, boolean minsum,
                                                      ArrayList<Float> scores) {
        ArrayList<ArrayList<String>> result = SwapDiversion.compute (qcenter,seeds,null,graph,context,l,minsum,symmetric,lambda,alpha,beta,scores);
        queryRequirements = SwapDiversion.getMaxMemRequirements();
        return result;
    }

    public ArrayList<ArrayList<String>> processSeeds (String qcenter, 
                                                      ArrayList<ArrayList<String>> seeds, 
                                                      int l, boolean minsum,
                                                      ArrayList<Float> scores,
                                                      long timeout) {
        ArrayList<ArrayList<String>> result = SwapDiversion.compute (qcenter,seeds,null,graph,context,l,minsum,symmetric,lambda,alpha,beta,scores,timeout);
        queryRequirements = SwapDiversion.getMaxMemRequirements();
        return result;
    }

    public ArrayList<String> processGreedy (String qcenter, int k, int n, boolean minsum, ArrayList<Float> scores) {
        long start = System.currentTimeMillis();
        ArrayList<String> result = GreedyDiversion.compute (qcenter,null,graph,context,k,n,minsum,symmetric,lambda,alpha,beta,scores).get(0);
        long duration = System.currentTimeMillis() - start;
        queryRequirements = GreedyDiversion.getMaxMemRequirements();
        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + ">  Processed query '"+qcenter+"' in " + duration + " msec using " + (queryRequirements >> 10) + "kBs.");
        return result;
    }

    public ArrayList<String> processGreedyIncremental (ArrayList<String> S, String qcenter, int n, boolean minsum, ArrayList<Float> scores) {
        ArrayList<ArrayList<String>> candidates = new ArrayList<>();
        candidates.add(S);
        ArrayList<String> result = GreedyDiversion.compute (qcenter,null,candidates,graph,context,n,minsum,symmetric,lambda,alpha,beta,scores).get(0);
        queryRequirements = GreedyDiversion.getMaxMemRequirements();
        return result;
    }

    public ArrayList<String> processSwap (String qcenter, int k, int l, int n, boolean minsum, ArrayList<Float> scores) {
        long start = System.currentTimeMillis();
        Collection<ArrayList<String>> seeds = GreedyDiversion.compute (qcenter,null,graph,context,k,n,minsum,symmetric,lambda,alpha,beta,scores);
        long durationgreedy = System.currentTimeMillis() - start;
        queryRequirements = GreedyDiversion.getMaxMemRequirements();

        ArrayList<String> result = SwapDiversion.compute (qcenter,seeds,null,graph,context,l,minsum,symmetric,lambda,alpha,beta,scores).get(0);
        long duration = System.currentTimeMillis() - start;
        queryRequirements = queryRequirements<SwapDiversion.getMaxMemRequirements()?SwapDiversion.getMaxMemRequirements():queryRequirements;
        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + ">  Processed query '"+qcenter+"' in " + duration + " msec of which " + durationgreedy + " were spent for computing the initial result.");
        return result;
    }

    public ArrayList<String> processSwap (String qcenter, Collection<ArrayList<String>> seeds, int l, boolean minsum, ArrayList<Float> scores) {
        long start = System.currentTimeMillis();
        ArrayList<String> result = SwapDiversion.compute (qcenter,seeds,null,graph,context,l,minsum,symmetric,lambda,alpha,beta,scores).get(0);
        long duration = System.currentTimeMillis() - start;
        queryRequirements = SwapDiversion.getMaxMemRequirements();
        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + ">  Processed query '"+qcenter+"' in " + duration + " msec using " + (queryRequirements >> 10) + " kBs.");
        return result;
    }

    public ArrayList<String> processGreedySwapIncremental (String qcenter, ArrayList<String> seeds, int l, int n, boolean minsum, ArrayList<Float> scores) {
        ArrayList<ArrayList<String>> candidates = new ArrayList<>();
        candidates.add (seeds);
        ArrayList<ArrayList<String>> tentative = GreedyDiversion.compute (qcenter,null,candidates,graph,context,n,minsum,symmetric,lambda,alpha,beta,scores);

        ArrayList<String> result = SwapDiversion.compute (qcenter,tentative,null,graph,context,l,n,minsum,symmetric,lambda,alpha,beta,scores).get(0);
        return result;
    }

    public ArrayList<String> processSwapIncremental (String qcenter, ArrayList<String> seeds, int l, int n, boolean minsum, ArrayList<Float> scores) {
        ArrayList<ArrayList<String>> candidates = new ArrayList<>();
        candidates.add (seeds);

        ArrayList<String> result = SwapDiversion.compute (qcenter,candidates,null,graph,context,l,n,minsum,symmetric,lambda,alpha,beta,scores).get(0);
        return result;
    }
    public String getTitle (String u) {return titles.get(u);} // returns null if node u not indexed.
    public float getContextSimilarity (String u, String v) {return context.similarity (u,v);}
    public float getGraphDistance (String from, String to) {return graph.getPathCost (from,to);}
    public void setWeightParameters (float l,float a,float b)  {lambda=l; alpha=a; beta=b;}

    private Map<String,Map<String,Float>> produceModelFromFreqs () {
        Map<String,Map<String,Float>> model = new TreeMap<>();
        for (Entry<String,Map<String,Float>> over : index.entrySet()) {
            Map<String,Float> termWeights = new TreeMap<>();
            Map<String,Float> termFreqs = over.getValue();
            float maxDocFreq = maxFreqs.get(over.getKey());
            for (Entry<String,Float> under : termFreqs.entrySet()) {
                float tf = .5f + .5f * under.getValue() / maxDocFreq;
                float idf = (float) Math.log(index.size()/invertedIndex.get(under.getKey()).size());
                termWeights.put (under.getKey(),tf*idf);
            }
            model.put(over.getKey(),termWeights);
        }
        return model;
    }

    private void saveModelToDirectory (Map<String,Map<String,Float>> model, String dirname) {
        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Saving Vector Space Model in current working directory.");
        try{
            FileWriter fw = new FileWriter (dirname+"/model.txt");
            for (Entry<String,Map<String,Float>> over : model.entrySet()) {
                fw.write (over.getKey());
                for (Entry<String,Float> under : over.getValue().entrySet())
                    fw.write (" "+under.getKey()+":"+under.getValue());
                fw.write ("\n");
            }
            fw.close();
        }catch (IOException e) {e.printStackTrace();}
    }


    public static void main (String[] args) {
        if (args.length<2) throw new IllegalArgumentException ("!! ERROR - Should provide a path to an edge-list and nodes' textual descriptions !!");
        DiversePedia dp = new DiversePedia (args[0],args[1],args[2]);
        dp.saveModelToDirectory (dp.context.getModel(),"model.txt");

        while (true) {
            ArrayDeque<String> query = new ArrayDeque<>();
            Scanner sc = new Scanner(System.in);
            System.out.print ("** Enter query: ");
            while (sc.hasNext()) {
                query.add(sc.next());
            }
            System.out.println("\n-----------------------------------");

            int i=0;
            String qcenter = dp.qcenterByTitle (query);
            if (qcenter==null) qcenter = dp.qcenterByContent (query);
            for (String result : dp.processGreedy(qcenter,1,5,true,null)) {
                System.out.println(++i+": "+result);
            }

            System.out.println("\n===================================");
        }
    }
}
