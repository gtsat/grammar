
package wikipedia;

import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

public class ExtractTitles {
    
    public static void main (String[] args) throws IOException {
        try(BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            for(String line; (line=br.readLine())!=null; ) {
                String[] parsed = line.substring(0,line.length()-5).split("/");
                System.out.println (line+" "+parsed [parsed.length-1].replace('_',' ').replaceFirst("~"," "));
            }
            br.close();
        }
    }
}
