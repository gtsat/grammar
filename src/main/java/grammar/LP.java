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

public class LP {
    private final MatrixSparse<Long,Long> objective = new MatrixSparse<>(0.0f);
    private final MatrixSparse<Long,Long> constraints = new MatrixSparse<>(0.0f);
    private final MatrixSparse<Long,Long> bounds = new MatrixSparse<>(0.0f);

    LP () {}

    void pivot () {
        
    }

    /**
     * Simplex java implementation
     * @return 
     */
    MatrixSparse<Long,Long> solve () {return null;}
}
