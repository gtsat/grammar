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

package omcp;

import oscp.Group;
import grammar.Graph;
import java.util.Stack;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;

class GroupingTree<NodeT> {

    private int size = 0;

    final private TreeNode<NodeT> root;
    final private Graph<NodeT> graph;

    public int size () {return size;}

    public GroupingTree (Graph<NodeT> net) {
        root = new TreeNode<>(new Group<>(null,new ArrayList<Group<NodeT>>(),
                              new HashMap<Group<NodeT>,Float>(),net,true)); // here doesn't matter true or false
        graph = net;
    }

    private static final class TreeNode<NodeT> {
        final private Group<NodeT> group;
        final private ArrayDeque<TreeNode<NodeT>> children = new ArrayDeque<>();

        public TreeNode (Group<NodeT> g) {group=g;}
        public Group<NodeT> getGroup () {return group;}
        public boolean isLeaf () {return children.isEmpty();}
        public Collection<TreeNode<NodeT>> getChildren () {return children;}
        public boolean isDisjoint (Group<NodeT> othergroup) {return group.isDisjoint(othergroup);} 

        public TreeNode<NodeT> addChild (Group<NodeT> newgroup) {
            TreeNode<NodeT> newnode = new TreeNode<> (newgroup);
            children.add(newnode);
            return newnode;
        }
    }

    public void addGroup (Group<NodeT> newgroup, boolean isDerived) {
        addGroup (newgroup,isDerived,Integer.MAX_VALUE);
    }

    public void addGroup (Group<NodeT> newgroup, boolean isDerived, int M) {
        HashMap<TreeNode<NodeT>,Integer> meetups = new HashMap<>();
        Stack<TreeNode<NodeT>> stack = new Stack<>();
        meetups.put (root,0);
        stack.push (root);

        while (!stack.isEmpty()) {
            TreeNode<NodeT> top = stack.pop();
            if (top.isLeaf()) {
                if (top==root || top.isDisjoint(newgroup)) {
                    if (meetups.get(top) + newgroup.getMeetupsNumber() <= M) {
                        meetups.put (top.addChild(newgroup),
                                     meetups.get(top)+newgroup.getMeetupsNumber());
                        ++size;
                    }
                }
            }else{
                boolean hasDisjointChild = false;
                for (TreeNode<NodeT> child : top.getChildren()) {
                    if (child.isDisjoint(newgroup)) {
                        if (meetups.get(top) + newgroup.getMeetupsNumber() <= M) { // redundant check
                            meetups.put (child,top.group.getMeetupsNumber());
                            hasDisjointChild = true;
                            stack.push (child);
                        }
                    }
                }
                if (isDerived && !hasDisjointChild) {
                    if (meetups.get(top) + newgroup.getMeetupsNumber() <= M) {
                        meetups.put (top.addChild(newgroup),
                                     meetups.get(top)+newgroup.getMeetupsNumber());
                        ++size;
                    }
                }
            }
        }
    }

    public Collection<Group<NodeT>> produceGroupings (NodeT target) {
        ArrayDeque<Group<NodeT>> result = new ArrayDeque<>();
        ArrayDeque<TreeNode<NodeT>> path = new ArrayDeque<>();
        path.add(root);
        produceGroupings (target,path,result);
        return result;
    }

    private void produceGroupings (NodeT target, 
            ArrayDeque<TreeNode<NodeT>> path, 
            ArrayDeque<Group<NodeT>> result) {
        TreeNode<NodeT> tailnode = path.peekLast();
        if (tailnode.isLeaf()) {
            Group<NodeT> newgrouping = new Group<>(target,
                                                   new ArrayList<Group<NodeT>>(),
                                                   new HashMap<Group<NodeT>,Float>(),
                                                   graph,true); // here doesn't matter if true or false

            path.pollFirst(); // remove the root node that corresponds to an empty group
            for (TreeNode<NodeT> treenode : path)
                newgrouping.addSubgroup (treenode.group);
            //if (!newgrouping.isValidGrouping())
            //    throw new RuntimeException("!! ERROR - Grouping-tree produced invalid grouping !!");
            result.add(newgrouping);
        }else{
            for (TreeNode<NodeT> child : tailnode.getChildren()) {
                //if (!tailnode.isDisjoint(child.group))
                //    throw new RuntimeException("!! ERROR - Non-disjoint child-node accessed.");
                ArrayDeque<TreeNode<NodeT>> newpath = new ArrayDeque<>();
                newpath.addAll(path);
                newpath.addLast(child);
                produceGroupings(target,newpath,result);
            }
        }
    }

    @Override public String toString () {
        StringBuilder output = new StringBuilder();
        ArrayDeque<TreeNode<NodeT>> queue = new ArrayDeque<>();
        TreeNode<NodeT> dummy = new TreeNode<>(null);
        queue.addLast (dummy);
        queue.addLast (root);
        int level=0;
        while (queue.size()>1) {
            TreeNode<NodeT> tail=queue.pollFirst();
            if (tail==dummy) {
                System.out.println ("LEVEL "+level);
                queue.addLast(dummy);
                ++level;
            }else{
                System.out.println (tail+" has "+tail.group.getSubgroups().size() 
                        +" subgroups "+tail.getChildren().size()+" children and includes "
                        +tail.group.getTravelers().size()+" travelers.");//\n"+tail.group+"\n");
                for (TreeNode<NodeT> child : tail.getChildren()) {
                    //if (!child.isDisjoint(tail.group) || !tail.isDisjoint(child.group)) System.out.println ("!! ERROR !!");
                    queue.addLast(child);
                }
            }
        }
        return output.toString();
    }
}
