/**
 *  The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit
 *  Copyright (C) 2014 George Tsatsanifos <gtsatsanifos@gmail.com>
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

package grammar;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;


final class ThreeDigitPositiveInteger implements Comparable<ThreeDigitPositiveInteger> {
	private final Integer value;
	public ThreeDigitPositiveInteger (int init) {
		if (init<0 || init>999) 
			throw new ArithmeticException("\n!! Instance should be initialized with a positive integer less than 1000 !!");
		value = init;
	}

        @Override
	public int compareTo (ThreeDigitPositiveInteger other) {
		if (other==null) throw new NullPointerException("\n!! Domain element " + value + " compared to null !!");
		return value - other.value;
	}

	public static ThreeDigitPositiveInteger parse (String unparsed) {
		return new ThreeDigitPositiveInteger (Integer.parseInt(unparsed));
	}

        @Override
	public String toString () {return value.toString();}
}

final public class MatrixSparse<RowT, ColT> { //extends Matrix<RowT,ColT> {
	protected final Map<RowT,Map<ColT,Float>> mat = new HashMap<>();
	protected final HashSet<ColT> columnSet = new HashSet<>();
	protected final HashSet<RowT> rowSet = new HashSet<>();
	protected final Float predefined;

	public void clear () {
		rowSet.clear();
		columnSet.clear();
		for (Entry<RowT,Map<ColT,Float>> entry : mat.entrySet())
			entry.getValue().clear();
		mat.clear();
	}

	public MatrixSparse (float init) {predefined=init;}
	public MatrixSparse (MatrixSparse<RowT,ColT> other) {
                if (other == null) throw new NullPointerException();
                
		predefined = other.predefined;
		for (Entry<RowT,Map<ColT,Float>> rowEntry : other.mat.entrySet())
			for (Entry<ColT,Float> colEntry : rowEntry.getValue().entrySet())
				if (colEntry.getValue()!=predefined) 
					set (rowEntry.getKey(), colEntry.getKey(), colEntry.getValue());
	}

        @Override
	public MatrixSparse<RowT,ColT> clone () {return new MatrixSparse<> (this);}

	@SuppressWarnings("unchecked")
	public MatrixSparse (String filename, Format format, float init) {
                if (filename == null || format == null) throw new NullPointerException();
                
		predefined = init;
		float unweighted = 0.0f;
		while (unweighted==predefined)
			unweighted+=1.0;

		try{
			Scanner in = new Scanner (new File(this.getClass().getClassLoader().getResource(filename).getFile())) ;

			long edges=0;
			System.err.println ("!! Importing now data from file '" + filename + "'... !!");
			long startTime = System.nanoTime();
			while (in.hasNextLine()) {
				String[] line = in.nextLine().split("\\s+");

				if (line.length==0) continue;
				if (line.length>0 && line[0].startsWith("#")) continue;

				String row = line[0];
				String col = line[1];
				float val = line.length>2 ? Float.parseFloat(line[2]) : unweighted;

				//set ((RowT)ParseUtils.parse(RowT.class, row), (ColT)ParseUtils.parse(ColT.class, col), val);
				set   ( (RowT)format.parseObject (row, new ParsePosition(0)), 
						(ColT)format.parseObject (col, new ParsePosition(0)), 
						val);

				++edges;
				if ((edges % 1000000) == 0) 
					System.err.println("!! So far " + edges + " edges have been set !!");
			}
			long endTime = System.nanoTime();
			System.err.println("!! Loaded " + edges + " edges in " + (endTime-startTime)/Math.pow(10,9) + " secs !!");
			
			in.close();
		}catch (FileNotFoundException e){
			System.err.println ("\n!! Input file '"+filename+"' does not exist !!");
		}
	}

	public boolean isEmpty () {return mat.isEmpty();}
	public int getRank () {return echelon().mat.size();}	// NEED TO REMOVE ZEROED ROWS

	public float get (RowT row, ColT col) {
		if (mat.containsKey(row) && mat.get(row).containsKey(col)) return mat.get(row).get(col);
                //else if (rowSet.contains(row) || columnSet.contains(col)) 
                    return predefined;
		//else throw new RuntimeException ("\n!! Invalid row or column index !!");
	}

        public boolean hasSameDimensionsWith (MatrixSparse<RowT,ColT> other) {
            if (rowSet.size()!=other.rowSet.size() || columnSet.size()!=other.columnSet.size()) return false;
            for (RowT row : rowSet) if (!other.rowSet.contains(row)) return false;
            for (ColT col : columnSet) if (!other.columnSet.contains(col)) return false;
            return true; 
        }

	public float remove (RowT row, ColT col) {
                //if (!rowSet.contains(row) || !columnSet.contains(col)) 
                //    throw new RuntimeException ("\n!! Invalid row or column index !!");

		float temp = predefined;
		if (mat.containsKey(row)) {
			Map<ColT,Float> map = mat.get(row);
			if (map.containsKey(col)) {
				temp = map.remove(col);
				if (map.isEmpty()) 
					mat.remove(row);
			}
		}
		return temp;
	}

	public void set (RowT row, ColT col, float val) {
                rowSet.add (row);
                columnSet.add (col);

		if (!predefined.equals(val)) {
                    Map<ColT,Float> colvec;
                    if (!mat.containsKey(row)) {
			colvec = new HashMap<>();
			mat.put(row, colvec);
                    }else colvec = mat.get(row);
                    colvec.put(col, val);
		}else{
                    try{
                        remove (row,col);
                    }catch(RuntimeException e){
                        e.printStackTrace (System.err);
                    }
                }
	}

	@SuppressWarnings ("unchecked")
        @Override
	public boolean equals (Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (other instanceof MatrixSparse) {
                MatrixSparse<RowT,ColT> otherMat = (MatrixSparse<RowT,ColT>) other;
                for (Entry<RowT,Map<ColT,Float>> rowEntry : mat.entrySet())
                    for (Entry<ColT,Float> entry : rowEntry.getValue().entrySet())
                        if (otherMat.get(rowEntry.getKey(), entry.getKey())!=entry.getValue())
                            return false;
                return true;
            }else return false;
	}

	public MatrixSparse<RowT,ColT> getRow (RowT row) {
		MatrixSparse<RowT,ColT> rowVector = new MatrixSparse<>(predefined);
		if (mat.containsKey(row))
			for (Entry<ColT,Float> entry : mat.get(row).entrySet())
				rowVector.set (row, entry.getKey(), entry.getValue());
		rowVector.columnSet.addAll (columnSet);
		return rowVector;
	}

	public MatrixSparse<RowT,ColT> getColumn (ColT col) {
		MatrixSparse<RowT,ColT> colVector = new MatrixSparse<>(predefined);
		for (Entry<RowT,Map<ColT,Float>> rowEntry : mat.entrySet())
			if (rowEntry.getValue().containsKey(col))
				colVector.set (rowEntry.getKey(), col, rowEntry.getValue().get(col));
		colVector.rowSet.addAll (rowSet);
		return colVector;
	}
	
	public MatrixSparse<ColT,RowT> transpose () {
		MatrixSparse<ColT,RowT> newmat = new MatrixSparse<>(predefined);
                for (Entry<RowT,Map<ColT,Float>> rowEntry : mat.entrySet())
                    for (Entry<ColT,Float> entry : rowEntry.getValue().entrySet())
                        newmat.set(entry.getKey(), rowEntry.getKey(), entry.getValue());
		return newmat;
	}

	public MatrixSparse<RowT,ColT> plus (MatrixSparse<RowT,ColT> other) {
		if (other==null) throw new NullPointerException("\n!! Addition not defined for null objects !!");
                if (!hasSameDimensionsWith(other)) throw new RuntimeException ("\n!! Addition involves matrices of same dimensionality !!");

                MatrixSparse<RowT,ColT> newmat = new MatrixSparse<>(predefined - other.predefined);

		newmat.rowSet.addAll (mat.keySet());
		newmat.rowSet.addAll (other.mat.keySet());
		
		newmat.columnSet.addAll(columnSet);
		newmat.columnSet.addAll(other.columnSet);
		
		for (RowT row : newmat.rowSet)
			for (ColT col : newmat.columnSet)
				newmat.set(row, col, get(row,col) + other.get(row, col));
		return newmat;
	}

	public MatrixSparse<RowT,ColT> minus (MatrixSparse<RowT,ColT> other) {
		if (other==null) throw new NullPointerException("\n!! Subtraction not defined for null objects !!");
                if (!hasSameDimensionsWith(other)) throw new RuntimeException ("\n!! Addition involves matrices of same dimensionality !!");

		MatrixSparse<RowT,ColT> newmat = new MatrixSparse<>(predefined - other.predefined);

		newmat.rowSet.addAll (mat.keySet());
		newmat.rowSet.addAll (other.mat.keySet());
		
		newmat.columnSet.addAll(columnSet);
		newmat.columnSet.addAll(other.columnSet);
		
		for (RowT row : newmat.rowSet)
			for (ColT col : newmat.columnSet)
				newmat.set(row, col, get(row,col) - other.get(row, col));
		return newmat;
	}

        /** 
         * Assumes predefined == 0 
         * @return Matrix determinant
         */
	public float determinant () {
            float product = 1.0f;
            HashSet<ColT> processedColumns = new HashSet<>();
            for (Entry<RowT,Map<ColT,Float>> rowEntry : echelon().mat.entrySet()) {
                for (Entry<ColT,Float> entry : rowEntry.getValue().entrySet()) {
                    if (!processedColumns.contains (entry.getKey())) {
                        processedColumns.add (entry.getKey());
                        product *= entry.getValue();
                        break;
                    }
                }
            }
            return product;
        }

        /**
         * Assumes predefined == 0 
         * @return the adjoint matrix
         */
        public MatrixSparse<ColT,RowT> adjoint () {
            int i=1, j=1;
            MatrixSparse<ColT,RowT> adjoint = new MatrixSparse<> (predefined);
            for (Entry<RowT,Map<ColT,Float>> rowEntry : mat.entrySet()) {
                for (Entry<ColT,Float> entry : rowEntry.getValue().entrySet()) {
                    MatrixSparse<RowT,ColT> copy = clone();
                    copy.removeRow(rowEntry.getKey());
                    copy.removeColumn(entry.getKey());
                    float det = copy.determinant();
                    if((i+j)%2==0) adjoint.set(entry.getKey(),rowEntry.getKey(),det);
                    else adjoint.set(entry.getKey(),rowEntry.getKey(),-det);
                    ++j;
                }
                j=1;
                ++i;
            }
            return adjoint;
        }

        private void removeRow (RowT row) {if (rowSet.remove(row)) mat.remove(row);}
        private void removeColumn (ColT col) {
            columnSet.remove(col);
            for (Entry<RowT,Map<ColT,Float>> rowEntry : mat.entrySet())
                if (rowEntry.getValue().containsKey(col))
                    rowEntry.getValue().remove(col);
        }

	public MatrixSparse<RowT,ColT> times (float factor) {
		MatrixSparse<RowT,ColT> newmat = new MatrixSparse<> (predefined==0.0f?0.0f:factor*predefined);
		for (RowT row : mat.keySet()) {
			for (Entry<ColT,Float> entry : mat.get(row).entrySet()) {
				float newval = factor * entry.getValue();
				if (newval!=predefined) newmat.set(row, entry.getKey(), newval);
			}
		}
		return newmat;
	}
	/*
	@SuppressWarnings ("unused")
	private float dotProduct (Map<ColT,Float> u, Map<ColT,Float> v) {
		float sum=0.0;
		if (u.size()<v.size()) {
			for (Entry<ColT,Float> entry : u.entrySet())
				if (v.containsKey(entry.getKey()))
					sum += entry.getValue() * v.get(entry.getKey());
		}else{
			for (Entry<ColT,Float> entry : v.entrySet())
				if (u.containsKey(entry.getKey()))
					sum += entry.getValue() * u.get(entry.getKey());
		}
		return sum;
	}
	*/
/**
        private MatrixSparse<RowT,Object> timesWithZeroDefaultValues (MatrixSparse<ColT,Object> other) {
            if (other==null) throw new NullPointerException ("\n!! Multiplication not defined for null objects !!");
            if (columnSet.size()!=other.rowSet.size()) throw new RuntimeException ("\n!! Matrix dimensions do not match for multiplication !!");
            if (predefined!=0.0 || other.predefined!=0.0) 
                throw new RuntimeException ("\n!! This operation is permitted only in the presence of zero default values !!");
            MatrixSparse<RowT,Object> newmat = new MatrixSparse<>(0.0);
            newmat.columnSet.addAll(other.columnSet);
            newmat.rowSet.addAll(rowSet);
            for (Entry<RowT,Map<ColT,Float>> rowEntry : mat.entrySet())
                for (Entry<ColT,Float> entry : rowEntry.getValue().entrySet())
                    for (Entry<Object,Float> otherEntry : other.mat.get(entry.getKey()).entrySet())
                        newmat.set (rowEntry.getKey(), otherEntry.getKey(), 
                                newmat.get (rowEntry.getKey(), otherEntry.getKey()) + entry.getValue() * otherEntry.getValue());
            return newmat;
        }

        private MatrixSparse<RowT,Object> timesWithLocalZeroDefaultValues (MatrixSparse<ColT,Object> other) {
            if (other==null) throw new NullPointerException ("\n!! Multiplication not defined for null objects !!");
            if (columnSet.size()!=other.rowSet.size()) throw new RuntimeException ("\n!! Matrix dimensions do not match for multiplication !!");
            if (predefined!=0.0) 
                throw new RuntimeException ("\n!! This operation is permitted only in the presence of zero default value !!");
            MatrixSparse<RowT,Object> newmat = new MatrixSparse<>(0.0);
            newmat.columnSet.addAll(other.columnSet);
            newmat.rowSet.addAll(rowSet);
            for (Entry<RowT,Map<ColT,Float>> rowEntry : mat.entrySet())
                for (Entry<ColT,Float> entry : rowEntry.getValue().entrySet())
                    for (Object otherColumn : other.columnSet)
                        newmat.set (rowEntry.getKey(), otherColumn, 
                                newmat.get (rowEntry.getKey(), otherColumn) + entry.getValue() * other.get(entry.getKey(),otherColumn));
            return newmat;
        }

        private MatrixSparse<RowT,Object> timesWithOtherZeroDefaultValues (MatrixSparse<ColT,Object> other) {
            if (other==null) throw new NullPointerException ("\n!! Multiplication not defined for null objects !!");
            if (columnSet.size()!=other.rowSet.size()) throw new RuntimeException ("\n!! Matrix dimensions do not match for multiplication !!");
            if (other.predefined!=0.0) 
                throw new RuntimeException ("\n!! This operation is permitted only in the presence of zero default value !!");
            MatrixSparse<RowT,Object> newmat = new MatrixSparse<> (0.0);
            newmat.columnSet.addAll(other.columnSet);
            newmat.rowSet.addAll(rowSet);
            for (Entry<ColT,Map<Object,Float>> otherRowEntry : other.mat.entrySet())
                for (Entry<Object,Float> otherEntry : otherRowEntry.getValue().entrySet())
                    for (RowT row : rowSet)
                        newmat.set(row, otherEntry.getKey(), 
                                newmat.get(row, otherEntry.getKey()) + otherEntry.getValue() * get(row,otherRowEntry.getKey()));
            return newmat;
        }

        private MatrixSparse<RowT,Object> timesWithNoZeroDefaultValues (MatrixSparse<ColT,Object> other) {
            if (other==null) throw new NullPointerException ("\n!! Multiplication not defined for null objects !!");
            if (columnSet.size()!=other.rowSet.size()) throw new RuntimeException ("\n!! Matrix dimensions do not match for multiplication !!");
            MatrixSparse<RowT,Object> newmat = new MatrixSparse<>(0.0);
            newmat.columnSet.addAll(other.columnSet);
            newmat.rowSet.addAll(rowSet);
            for (RowT row : rowSet)
                for (ColT col : columnSet)
                    for (Object otherCol : other.columnSet)
                        newmat.set (row, otherCol, newmat.get(row,otherCol) + get(row,col)*other.get(col,otherCol));
            return newmat;
        }
        
        private static void testMultiplication () {
            MatrixSparse <String,Integer> A = new MatrixSparse<> (3.0);

            A.set("a", 1, 1);
            A.set("a", 2, -1);
            A.set("b", 5, 5);
            A.set("c", 4, 11);
            A.set("c", 2, -7);
            A.set("d", 3, -2);

            
            System.out.println (A);
            System.out.println (A.transpose());
            System.out.println (A.times(A.transpose()));
            System.out.println (A.transpose().times(A));
        }
                

	public MatrixSparse<RowT,Object> times (MatrixSparse<ColT,Object> other) {
            if (!columnSet.getClass().equals(other.rowSet.getClass())) {
                throw new RuntimeException ("\n!! Cannot multiply matrices of incompatible typles  !!");
            }else{
		if (other==null) throw new NullPointerException ("\n!! Multiplication not defined for null objects !!");
                if (columnSet.size()!=other.rowSet.size()) throw new RuntimeException ("\n!! Matrix dimensions do not match for multiplication !!");

                if (predefined==0.0) return other.predefined==0.0?timesWithZeroDefaultValues(other):timesWithLocalZeroDefaultValues(other);
                else return other.predefined==0.0?timesWithOtherZeroDefaultValues(other):timesWithNoZeroDefaultValues(other);
            }
	}
**/
        public MatrixSparse<RowT,ColT> echelon () {return echelon (true,true);}

	@SuppressWarnings ("unchecked")
	private MatrixSparse<RowT,ColT> echelon (boolean roundEnabled, boolean swapRowsEnabled) {
		MatrixSparse<RowT,ColT> echelon = new MatrixSparse<>(this);
		BitsetSparse<RowT> processedRows = new BitsetSparse<>();
                BitsetSparse<ColT> processedColumns = new BitsetSparse<>();
		RowT[] rows = (RowT[]) rowSet.toArray();
		int irow = 0;
		for (ColT column : columnSet) {
			MatrixSparse<RowT,ColT> colVector = echelon.getColumn (column);
			if (!colVector.isEmpty()) {
				RowT maxAbsValueRow = null;
				float maxAbsValue = 0.0f, maxValue = 0.0f;
                                if (swapRowsEnabled) {
                                    for (Entry<RowT,Map<ColT,Float>> entry : colVector.mat.entrySet()) {
					if (!processedRows.get (entry.getKey()) && Math.abs(entry.getValue().get(column)) > maxAbsValue) {
                                            maxValue = entry.getValue().get(column);
                                            maxAbsValue = Math.abs(maxValue);
                                            maxAbsValueRow = entry.getKey();
					}
                                    }
                                }else{
                                    maxAbsValueRow = rows[irow];
                                    maxValue = mat.get(maxAbsValueRow).get(column);
                                }

				if (maxAbsValueRow!=null) { // therefore maxValue != 0.0
                                    if (swapRowsEnabled && !maxAbsValueRow.equals(rows[irow])) {
					Map<ColT,Float> swap = echelon.mat.containsKey(rows[irow]) ? echelon.mat.get(rows[irow]) : null;
					echelon.mat.put (rows[irow], echelon.mat.get (maxAbsValueRow));
					if (swap!=null) echelon.mat.put (maxAbsValueRow, swap);
                                    }

                                    maxAbsValueRow = rows[irow++];
                                    processedRows.set (maxAbsValueRow);

                                    for (Entry<RowT,Map<ColT,Float>> rowEntry : echelon.mat.entrySet()) {
					if (!processedRows.get(rowEntry.getKey())) {
                                            float factor = rowEntry.getValue().containsKey(column) ? rowEntry.getValue().get(column) : 0.0f;
                                            for (Entry<ColT,Float> entry : rowEntry.getValue().entrySet()) {
                                                if (roundEnabled && processedColumns.contains(entry.getKey())) continue;
                                                    float newvalue = factor/maxValue*echelon.get(maxAbsValueRow,entry.getKey()) - entry.getValue();
                                                    entry.setValue (newvalue);
                                                }    
					}
                                    }
				}
			}
                        processedColumns.set (column);
		}
/*
		for (RowT row : rowSet)
			if (!processedRows.get(row) || (echelon.mat.containsKey(row) && echelon.mat.get(row).isEmpty())) 
				echelon.mat.remove(row);
*/
		return echelon;
	}

	@SuppressWarnings ("unchecked")
	private MatrixSparse<RowT,ColT> echelon (MatrixSparse<RowT,RowT> b) {
		MatrixSparse<RowT,ColT> echelon = new MatrixSparse<>(this);
		BitsetSparse<RowT> processedRows = new BitsetSparse<>();
		RowT[] rows = (RowT[]) rowSet.toArray();
		int irow = 0;
		if (b.columnSet.isEmpty()) b.columnSet.add (rows[0]);
		for (ColT column : columnSet) {
			MatrixSparse<RowT,ColT> colVector = echelon.getColumn (column);
			if (!colVector.isEmpty()) {
				RowT maxAbsValueRow = null;
				float maxAbsValue = 0.0f, maxValue = 0.0f;
				for (Entry<RowT,Map<ColT,Float>> entry : colVector.mat.entrySet()) {
					if (!processedRows.get (entry.getKey()) 
							&& Math.abs(entry.getValue().get(column)) > maxAbsValue) {
						maxValue = entry.getValue().get(column);
						maxAbsValue = Math.abs (maxValue);
						maxAbsValueRow = entry.getKey();
					}
				}

				if (maxAbsValueRow!=null) {
					if (maxAbsValueRow != rows[irow]) {
						Map<ColT,Float> swapRow = echelon.mat.containsKey(rows[irow]) ? echelon.mat.get(rows[irow]) : null;
						echelon.mat.put (rows[irow], echelon.mat.get (maxAbsValueRow));
						echelon.mat.put (maxAbsValueRow, swapRow);

						//RowT bCol = b.columnSet.first(); ////////////////// CHANGE TO TREESET AND TREEMAP FOR THIS
						//float swapVal = b.get (rows[irow], bCol);
						//b.set(rows[irow], bCol, b.get (maxAbsValueRow, bCol));
						//b.set(maxAbsValueRow, bCol, swapVal);
					}
					
					maxAbsValueRow = rows[irow++];
					processedRows.set (maxAbsValueRow);
					for (Entry<RowT,Map<ColT,Float>> row : echelon.mat.entrySet()) {
						if (!processedRows.get (row.getKey())) {
							float factor = row.getValue().containsKey(column) ? row.getValue().get(column) : 0.0f;
							for (Entry<ColT,Float> entry : row.getValue().entrySet())
								entry.setValue (entry.getValue() - echelon.get(maxAbsValueRow, entry.getKey()) * factor / maxValue);

							//b.set (row.getKey(), b.columnSet.first(),  ////////////////// CHANGE TO TREESET AND TREEMAP FOR THIS
							//		b.get(row.getKey(), b.columnSet.first()) - b.get(maxAbsValueRow, b.columnSet.first()) * factor / maxValue );
						}
					}
				}
			}
		}
		return echelon;
	}

	public MatrixSparse<ColT,RowT> solve (MatrixSparse<RowT,RowT> other) {
                if (other == null) throw new NullPointerException();
                
		MatrixSparse<ColT,RowT> solution; 
		if (predefined!=0.0) solution = new MatrixSparse<>(other.predefined / predefined);
		else solution = new MatrixSparse<>(0.0f);

		for (RowT column : other.columnSet) {
			MatrixSparse<ColT,RowT> partial = triangular_solve (other.getColumn(column));
			for (Entry<ColT,Map<RowT,Float>> entry : partial.mat.entrySet())
				solution.set (entry.getKey(), column, entry.getValue().get(column) );
		}
		return solution;
	}

	@SuppressWarnings ("unchecked")
	public MatrixSparse<ColT,RowT> triangular_solve (MatrixSparse<RowT,RowT> b) {
		if (predefined!=0) {
			////////////////////////////////////////////////////////////////
		}
		MatrixSparse<ColT,RowT> solution = new MatrixSparse<> (0.0f);
		MatrixSparse<RowT,ColT> echelon = echelon (b);
		RowT[] rows = (RowT[]) echelon.mat.keySet().toArray();
		ColT[] columns = (ColT[]) echelon.columnSet.toArray();
		//RowT bColumnSet = b.columnSet.isEmpty() ? rows[0] : b.columnSet.first(); ////////////////// CHANGE TO TREESET AND TREEMAP FOR THIS
		for (int irow=rows.length-1, icol=columns.length-1; irow>=0 && icol>=0; --irow) {
			if (echelon.mat.get(rows[irow]).isEmpty()) continue;

			float sum=0.0f;
			for (int rest=irow+1; rest<columns.length; ++rest) {
				//sum += echelon.get (rows[irow],columns[rest]) * solution.get (columns[rest] , bColumnSet); ////////////////// CHANGE TO TREESET AND TREEMAP FOR THIS

			//float Aij = echelon.get(rows[irow],columns[icol]); ////////////////// CHANGE TO TREESET AND TREEMAP FOR THIS
			//if (Math.abs(Aij) >= Float.MIN_VALUE) ////////////////// CHANGE TO TREESET AND TREEMAP FOR THIS
				//solution.set (columns[icol--], bColumnSet, (b.get(rows[irow],bColumnSet)-sum) / Aij);
			//else solution.set (columns[icol], bColumnSet, Float.NaN); ////////////////// CHANGE TO TREESET AND TREEMAP FOR THIS
                        }
		}
		return solution;
	}

        /**
         * 
         * @return 
         */
        private MatrixSparse<RowT,ColT> orthogonalize () {
            MatrixSparse<RowT,ColT> orthonormal = new MatrixSparse<>(predefined);
            MatrixSparse<ColT,RowT> coeff = new MatrixSparse<>(0.0f);
            
            for (Entry<RowT,Map<ColT,Float>> rowEntry : mat.entrySet()) {
                float norm = normEuclidean (rowEntry.getKey());
                
                for (Entry<RowT,Map<ColT,Float>> orthonormalRowEntry : orthonormal.mat.entrySet()) {
                    
                }
                //coeff.set(row, col, 1.0);
            }
            
            return orthonormal;
        }
        
        /**
         * Projects the columns of <>param</> other to the row-span
         * @param other
         * @return each column corresponds to the coefficients of the prejected column of <>other</>
         */
/*        private MatrixSparse<RowT,Object> projectionCoefficients (MatrixSparse<ColT,Object> other) {
            MatrixSparse<RowT,Object> coeff = times(other);
            for (Entry<RowT,Map<ColT,Float>> rowEntry : mat.entrySet()) {
                float norm = 0.0;
                for (Entry<ColT,Float> entry : rowEntry.getValue().entrySet())
                    norm += entry.getValue() * entry.getValue();

                for (Entry<Object,Float> entry : coeff.mat.get(rowEntry.getKey()).entrySet())
                    entry.setValue (entry.getValue() / norm);
            }
            return coeff;
        }
  */      
        private MatrixSparse<RowT,RowT> projectOrthogonal (MatrixSparse<ColT,RowT> other) {
            MatrixSparse<RowT,RowT> projected = null;
            /**
             * transposing the coefficients for multiplication 
             * is faster than transposing the matrix itself 
             */
            return null;
        }
        
        private float normEuclidean (RowT row) {
            float norm = 0.0f;
            for (Entry<ColT,Float> entry : mat.get(row).entrySet())
                norm += entry.getValue() * entry.getValue();
            return (float) Math.sqrt(norm);
        }
        
        private MatrixSparse<RowT,ColT> LUdecomposition () {
            MatrixSparse<RowT,ColT> U = new MatrixSparse<> (0.0f);
            MatrixSparse<RowT,ColT> L = new MatrixSparse<> (0.0f);

            RowT rowFirst = null;
            ColT columnFirst = null;
            for (RowT row : mat.keySet()) {
                for (ColT column : mat.get(row).keySet()) {
                    columnFirst = column;
                    break;
                }
                if (columnFirst!=null) {
                    rowFirst = row;
                    break;
                }
            }


            
            for (ColT col : columnSet) {
                Map<RowT,Float> ujr = U.getColumn(col).transpose().mat.get(col);
                for (Entry<RowT,Map<ColT,Float>> rowEntry : mat.entrySet()) {
                    float sum = 0.0f;
                    Map<ColT,Float> lij = L.mat.get(rowEntry.getKey());
                    
                    if (rowEntry.getValue().containsKey(col)) {
                        //Lcol.put(rowEntry.getKey(), get(rowEntry.getKey(), col));
                    }
                }
                //Ltransposed.mat.put (columnFirst, Lcol);
            }
           return null;
        
        }
            


   
        
        @Override
	public String toString () {
        StringBuilder print = new StringBuilder();

        int charcounter=1;
		print.append ("                 ");
		for (ColT column : columnSet) {
			print.append (column);
			for (int i=column.toString().length(); i<13; ++i)
				print.append (" ");
			charcounter += 13;
		}
		//if (!columnSet.isEmpty()) ////////////////// CHANGE TO TREESET AND TREEMAP FOR THIS
		//	charcounter -= 13 - columnSet.last().toString().length();
		print.append ("\n                 ");

		for (int i=0 ;i<charcounter; ++i) 
			print.append ("-");
		print.append ("\n");

		for (RowT row : rowSet) { //mat.keySet()) {
			print.append (row + ":");
			for (int i=row.toString().length(); i<8; ++i) 
				print.append(" ");
			print.append ("[");

			for (ColT column : columnSet)
				print.append (String.format("%12.3f ", get (row,column))) ;
			print.append (" ]\n");
		}
		return print.toString();
	}



	/* test client */
	public static void main (String[] args) {
            //testMultiplication();
            /*
		testMatrix (15,11,100);
		if (args.length>0) testFileread (args[0]);
            */
	}

        private static void test () {
            MatrixSparse<Integer,Integer> matrix = new MatrixSparse<>(0.0f);

            //matrix.set(0,0,1.0);
            //matrix.set(0,1,3.0);
            //matrix.set(0,2,1.0);

            //matrix.set(1,0,1.0);
            matrix.set(1,1,1.0f);
            matrix.set(1,2,-1.0f);
            
            //matrix.set(2,0,3.0);
            matrix.set(2,1,11.0f);
            matrix.set(2,2,5.0f);
            /**/
/*
            for (int i=0; i<3; ++i) 
                for (int j=0; j<3; ++j)
                    matrix.set(i, j, 10*Math.random());
*/
            System.out.println (matrix);
            System.out.println ("false,true\n"+matrix.echelon(false,true));
            System.out.println ("false,false\n"+matrix.echelon(false,false));
            System.out.println ("true,true\n"+matrix.echelon(true,true));
            System.out.println ("true,false\n"+matrix.echelon(true,false));

            System.out.println ("determinant\t"+matrix.determinant());
            //System.out.println ("Adjoint\n"+matrix.adjoint());
        }

	private static void testFileread (String filename) {
		MatrixSparse<Integer,Integer> matrix = new MatrixSparse<>(filename,NumberFormat.getNumberInstance(),0.0f);
		System.out.println ("M =\n"+matrix + "\n\n");
		//System.out.println("M'*M =\n" + matrix.transpose().times(matrix)+"\n\n");
	}

	private static void testMatrix (int m, int n, float maxValue) {
		MatrixSparse<ThreeDigitPositiveInteger,ThreeDigitPositiveInteger> A = new MatrixSparse<>(0.0f);
		float threshold = (float) Math.log(Math.min(m, n))/Math.max(m, n);
		for (int i=0; i<m; ++i) {
			for (int j=0; j<n; ++j) {
				if (Math.random()>=threshold) {
					ThreeDigitPositiveInteger I = new ThreeDigitPositiveInteger (i);
					ThreeDigitPositiveInteger J = new ThreeDigitPositiveInteger (j);
					A.set (I, J, System.nanoTime()%maxValue);
				}
			}
		}
/*
		A.set (0, 0, 4.0);
		A.set (0, 1, 2.0);
		A.set (0, 2, 4.0);
		A.set (1, 0, 15.0);
		A.set (1, 1, 1.0);
		A.set (1, 2, 8.0);
		A.set (2, 0, 2.0);
		A.set (2, 1, 16.0);
		A.set (2, 2, 2.0);
		A.set (3, 0, 1.0);
		A.set (3, 1, 1.0);
		A.set (3, 2, 8.0);
*/
		System.out.println ("A.echelon() =\n" + A.echelon());
/*
		System.out.println ("B = A*A' =\n" + A.times (A.transpose()) + "\n");
		System.out.println ("B.echelon() =\n" + A.times (A.transpose()).echelon());

		System.out.println ("C = A'*A =\n" + A.transpose().times (A) + "\n");
		System.out.println ("C.echelon() =\n" + A.transpose().times (A).echelon());

		MatrixSparse<ThreeDigitPositiveInteger,ThreeDigitPositiveInteger> x = A.triangular_solve ( new MatrixSparse<ThreeDigitPositiveInteger,ThreeDigitPositiveInteger>(10.0) );
		System.out.println ("x =\n" + x + "\n with squarred norm:\n" + x.transpose().times(x) + "\n");
		System.out.println("Confirmation:\n" + A.times(x));

		MatrixSparse<ThreeDigitPositiveInteger,ThreeDigitPositiveInteger> square = A.transpose().times(A);
		MatrixSparse<ThreeDigitPositiveInteger,ThreeDigitPositiveInteger> b = A.transpose().times (new MatrixSparse<ThreeDigitPositiveInteger,ThreeDigitPositiveInteger>(10.0));
		x = square.triangular_solve (b);
		System.out.println ("\nA'A =\n" + square);
		System.out.println ("M*b =\n" + b);
		System.out.println ("x =\n" + x + "\n with squarred norm:\n" + x.transpose().times(x) + "\n");
		System.out.println("Confirmation:\n" + A.times(x));

		MatrixSparse<ThreeDigitPositiveInteger,ThreeDigitPositiveInteger> eye = new MatrixSparse<>(0.0);
		for (int i=0; i<Math.min(m, n); ++i) {
			ThreeDigitPositiveInteger I = new ThreeDigitPositiveInteger (i);
			eye.set (I, I, 1.0);
		}

		MatrixSparse<ThreeDigitPositiveInteger,ThreeDigitPositiveInteger> Cinv = square.solve (eye);
		System.out.println ("inv(A'A) =\n" + Cinv + "\n");
		System.out.println ("Result verification 1:\n" + Cinv.times(square) + "\n") ;
		System.out.println ("Result verification 2:\n" + square.times(Cinv) + "\n") ;
*/
		MatrixSparse<String,String> M = new MatrixSparse<>(0.0f);
		M.set ("doc1","term0",0.2367f);
		M.set ("doc1","term1",0.8797f);		
		M.set ("doc1","term2",0.6767f);
		M.set ("doc2","term3",0.7854f);
		M.set ("doc2","term4",0.98765f);		
		M.set ("doc2","term5",0.567676f);
		M.set ("doc3","term6",0.456876f);
		M.set ("doc3","term7",0.876765f);
		M.set ("doc3","term8",0.2345678f);

		System.out.println (M);
		System.out.println (M.transpose());
		System.out.println (M.transpose().getColumn("doc2"));
		System.out.println (M.transpose().transpose().getColumn("term2"));
		System.out.println (M.getRow("doc2"));
	}
}
