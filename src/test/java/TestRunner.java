/**
 *  The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit
    Copyright (C) 2014 George Tsatsanifos <gtsatsanifos@gmail.com>

    The GRA.M.MA.R. toolkit is free software: you can redistribute it and/or 
    modify it under the terms of the GNU General Public License as published 
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({TestRandomUndirectedGraph.class, TestRandomDirectedGraph.class})
public class TestRunner {
    public TestSuite suite () {
        TestSuite suite = new TestSuite ();
        suite.addTestSuite(TestRandomUndirectedGraph.class);
        return suite;
    }

    public void runSwing (String[] args) {
        String[] myClasses = {"testing.TestRunner"};
        junit.textui.TestRunner.main (myClasses);
    }
}
