package evaluation.diversion.synthetic;

import java.util.Random;

// Based on code by Hyunsik Choi
// http://diveintodata.org/2009/09/zipf-distribution-generator-in-java/
class ZipfSimple {

    private Random rnd = new Random (System.currentTimeMillis());
    private double bottom = 0;
    private double skew;
    private int size;

    public ZipfSimple (int size, double skew) {
        this.size = size;
        this.skew = skew;

        for (int i=1;i<size;i++)
            this.bottom += (1 / Math.pow(i,this.skew));
    }

    public void setSeed (long seed) {rnd = new Random(seed);}

    // the next() method returns an rank id. The frequency of returned rank ids
    // are follows Zipf distribution.
    public int nextInt() {
        double friquency = 0;
        double dice;
        int rank;

        rank = rnd.nextInt(size);
        friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
        dice = rnd.nextDouble();

        while (!(dice < friquency)) {
            rank = rnd.nextInt(size);
            friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
            dice = rnd.nextDouble();
        }

        return rank;
    }

    // This method returns a probability that the given rank occurs.
    public double getProbability (int rank) {return (1.0d/Math.pow(rank, this.skew))/this.bottom;}
    
    
    public static void main (String[] args) {
        
        ZipfSimple zipf = new ZipfSimple(10,.1);
        
    }
}

