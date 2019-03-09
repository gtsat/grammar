
package grammar;

abstract public class Matrix<RowT,ColT> {
	/** getters and setters **/
	abstract public double get (RowT i, ColT j);
	abstract public void set (RowT i, ColT j, double v);
	abstract public double remove (RowT row, ColT col);

	abstract public Matrix<ColT,RowT> transpose ();

	/** basic functionality **/

	abstract public boolean equals (Object m);
	abstract public Matrix<RowT,ColT> clone ();
	
	abstract public Matrix<RowT,ColT> plus (Matrix<RowT,ColT> m);
	abstract public Matrix<RowT,ColT> minus (Matrix<RowT,ColT> m);
	abstract public Matrix<RowT,RowT> times (Matrix<ColT,RowT> m);
	
	abstract public int getRank ();
}
