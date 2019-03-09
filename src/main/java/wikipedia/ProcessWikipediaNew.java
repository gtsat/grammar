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

import diversion.ContentSimilarity;
import grammar.DirectedGraph;
import grammar.Edge;
import grammar.Graph;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ProcessWikipediaNew {
    private static final Logger logger = Logger.getLogger (ProcessWikipediaNew.class);

    /* graph structure */
    private static final Graph<String> graph = new DirectedGraph<>();
    private static final HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();

    /* context */
    private static ContentSimilarity<String> content;
    private static final Map<String,ArrayDeque<String>> inverted = new HashMap<>();
    private static final Map<String,Map<String,Integer>> index = new HashMap<>();
    private static final Map<String,Integer> maxFreq = new HashMap<>();

    //private static final Analyzer analyzer = new StandardAnalyzer();
    private static final Analyzer analyzer = new GreekAnalyzer();//////////////////////////////////


    private static Map<String,Map<String,Double>> produceTextModel () {
        Map<String,Map<String,Double>> model = new HashMap<>();
        for (Entry<String,Map<String,Integer>> over : index.entrySet()) {
            Map<String,Double> termWeights = new HashMap<>();
            Map<String,Integer> termFreqs = over.getValue();
            double maxDocFreq = maxFreq.get(over.getKey());
            for (Entry<String,Integer> under : termFreqs.entrySet()) {
                double tf = .5 + .5 * under.getValue() / maxDocFreq;
                double idf = Math.log(index.size()/inverted.get(under.getKey()).size());
                termWeights.put(under.getKey(),tf);
            }
            model.put(over.getKey(),termWeights);
        }
        return model;
    }

    private static void processTextDirectory (Directory directory, String field) throws IOException {
        IndexReader reader = DirectoryReader.open (directory);

        Map<String,Integer> dirindex = new HashMap<>();
        index.put (reader.document(0).get("title"),dirindex);

        int maxDirFreq = 0;
        BytesRef bytesRef;
        TermsEnum termEnum = MultiFields.getTerms(reader,field).iterator();
        while ((bytesRef = termEnum.next()) != null) {
            if (termEnum.seekExact(bytesRef)) {
                String term = bytesRef.utf8ToString();

                Integer termDocFreq = termEnum.docFreq();

                if (termDocFreq>maxDirFreq) 
                    maxDirFreq = termDocFreq;

                Integer prevfreq = dirindex.get (term);
                if (prevfreq==null) dirindex.put (term,termDocFreq);
                else dirindex.put (term,prevfreq+termDocFreq);

                ArrayDeque<String> queue = inverted.get(term);
                if (queue==null) {
                    queue = new ArrayDeque<>();
                    inverted.put (term,queue);
                }
                queue.add (reader.document(0).get("title"));
            }
        }
        maxFreq.put (reader.document(0).get("title"),maxDirFreq);
    }

    private static String readArticle (String filename) {
        String content = null;
        try{
            //File file = new File (filename);
            //BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            Path path = FileSystems.getDefault().getPath(URLDecoder.decode(filename,"UTF-8"));
            BufferedReader br = Files.newBufferedReader(path,StandardCharsets.UTF_8);
            //FileReader reader = null;

            StringBuilder xxx = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) {
                xxx.append(line);
            }
            br.close();
            return xxx.toString();
/*
            reader = new FileReader (file);
            char[] chars = new char [(int) file.length()];
            reader.read (chars);
            content = new String (chars);
            reader.close ();
        }catch (IOException e){
            e.printStackTrace ();
        }finally{
            if(reader!=null)
                reader.close ();
        }
        return content;
*/
        }catch (Exception e){
            logger.error(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Error while reading file: " + filename);
        }
        return null;
    }

    private static void addDoc (IndexWriter w, String title, String body) throws IOException {
        org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document ();
        doc.add (new TextField ("title", title, Field.Store.YES));
        doc.add (new Field ("body", body, Field.Store.YES, Field.Index.ANALYZED));
        w.addDocument (doc);
        w.prepareCommit();
        w.commit();
    }

    private static void processArticle (String filename) throws IOException {
        String html = readArticle (filename);
        //System.out.println ("** File '" + filename + "' consists of " + html.length() + " characters.");

        Document doc = Jsoup.parse (html);

        /* process text */
        //String storefilename = "/tmp/lucene.fsdirectory.dump"; //+filename.replace('/', '_');
        //Directory directory = new SimpleFSDirectory (java.nio.file.Paths.get(storefilename));
        Directory directory = new RAMDirectory();
        IndexWriter w = new IndexWriter (directory, new IndexWriterConfig (analyzer));
        addDoc (w, filename, doc.body().text());
        w.close ();

        processTextDirectory (directory, "body");

        directory.close();
        //directory.deleteFile (storefilename);
        //w.deleteUnusedFiles();

        /* process links */
        ArrayList<HtmlLink> hyperlinks = htmlLinkExtractor.grabHTMLLinks (doc.body().html());

        //System.out.println ("** File '" + filename + "' has " + hyperlinks.size() + " links to other pages.");

        //Charset utf8charset = Charset.forName("UTF-16");
        for (HtmlLink link : hyperlinks) {
            if (link.getLink().charAt(0)!='#') {
                //graph.setEdge (filename, utf8charset.decode(ByteBuffer.wrap(link.getLink().getBytes())).toString(), 1.0);
                graph.setEdge (filename, new String(link.getLink().getBytes(),"UTF-8"), 1.0f);
            }
        }
/*
        ArrayDeque<String> queue = new ArrayDeque<>();
        links.put(filename, queue);
	for (HtmlLink link : hyperlinks) 
            queue.add(link.getLink());
*/
    }

    private static void processFileList (String filelist, String prefix) {
        try(BufferedReader br = new BufferedReader(new FileReader(filelist))) {
            //StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            long start = System.currentTimeMillis();

            int i;
            for (i=1;line!=null;++i) {
                processArticle (prefix+"/"+line);

                //sb.append(line);
                //sb.append(System.lineSeparator());
                line = br.readLine();

                if (i%50000==0)
                    logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Having processed " + i + " files in " + (System.currentTimeMillis() - start) / 1000 / 60 + " minutes.");
            }

            //if (i%50000!=0)
            logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Having processed " + i + " files in " + (System.currentTimeMillis() - start) / 1000 / 60 + " minutes.");

            logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Done with processing input. About to produce text model.");
            start = System.currentTimeMillis();

            content = new ContentSimilarity (produceTextModel());
            //String everything = sb.toString();

            logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Vector Space Model derived in " + (System.currentTimeMillis() - start) / 1000 / 60 + " minutes.");
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static String getParagraph (String filename) {
        try {
            String body = Jsoup.parse(readArticle(filename)).body().html();
            int from = 3 + body.indexOf("<p>");
            int to = body.indexOf("</p>");
            return Jsoup.parse(body.substring(from, to)).text();
        }catch (Exception e){
            e.printStackTrace();
            return "<i>Unable to retrieve content from '"+filename+"'.</i>";
        }
    }


    public static void main (String[] args) throws IOException {

        if (args.length<3)
            throw new IllegalArgumentException ("\n!! ERROR - Three arguments are necessary !!");

        processFileList (args[0], args[1]);

        //processFileList ("../data/wikipedia/wikipedia-es/articles.filenames.lst", 
        //                 "../data/wikipedia/wikipedia-es/");



        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Saving Vector Space Model in current working directory !!");

        FileWriter fw = new FileWriter (args[2]+"/model.txt");
        for (Entry<String,Map<String,Float>> over : content.getModel().entrySet()) {
            fw.write (over.getKey());
            for (Entry<String,Float> under : over.getValue().entrySet()) {
                fw.write (" "+under.getKey()+":"+under.getValue());
            }
            fw.write ("\n");

            over.getValue().clear();
        }
        fw.close();

        content.getModel().clear();



        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Saving lemma frequencies in current working directory !!");

        fw = new FileWriter (args[2]+"/context.txt");
        for (Entry<String,Map<String,Integer>> over : index.entrySet()) {
            fw.write (over.getKey());
            for (Entry<String,Integer> under : over.getValue().entrySet()) {
                fw.write (" "+under.getKey()+":"+under.getValue());
            }
            fw.write ("\n");

            over.getValue().clear();
        }
        fw.close();

        index.clear();



        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Saving web-graph structure in current working directory !!");

        fw = new FileWriter (args[2]+"/edgelist.txt");
        for (Edge<String> edge : graph)
            fw.write (edge.from+"\t"+edge.to+"\n");
        fw.close();
    }
}
