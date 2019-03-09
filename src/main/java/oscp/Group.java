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

package oscp;

import grammar.Graph;
import grammar.UndirectedGraph;
import setbased.ComputeMeetingLocation;
import topk.ConnectingLocationsIterable;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
final public class Group<NodeT> {
    final private ArrayList<Group<NodeT>> subgroups;
    final private Map<Group<NodeT>,Float> lambdas;
    final private NodeT target;

    private int meetups = 0;

    public int getMeetupsNumber () {
        if (subgroups.isEmpty()) return 0;
        else if (subgroups.size()==1) return subgroups.get(0).getMeetupsNumber();
        else{
            int meetups = 1;
            for (Group<NodeT> subgroup : subgroups)
                meetups += subgroup.getMeetupsNumber();
            return meetups;
        }
    }

    final private Graph<NodeT> graph;

    final private boolean useMinSumRanking;

    final private boolean useIterativeMeetupsComputation = true;

    public Group (NodeT omega, Graph<NodeT> net, boolean minsum) {

        subgroups = new ArrayList<>();
        lambdas = new HashMap<>();
        target = omega;
        graph = net;

        useMinSumRanking = minsum;
    }

    public Group (NodeT omega, 
                  ArrayList<Group<NodeT>> subs, 
                  Map<Group<NodeT>,Float> ells, 
                  Graph<NodeT> net, boolean minsum) {

        subgroups = new ArrayList<Group<NodeT>> (subs);
        lambdas = new HashMap<Group<NodeT>,Float> (ells);
        target = omega;
        graph = net;

        useMinSumRanking = minsum;

        if (ells==null || ells.isEmpty()) return;

        if (minsum) {
            float sumprod = 0.0f;
            for (Group<NodeT> over : subgroups) {
                float product = 1.0f;
                for (Group<NodeT> under : subgroups)
                    if (!over.equals(under) && over.lambdas.containsKey(under))
                        product *= over.lambdas.get(under);
                sumprod += product;
            }
            lambdas.put(this,sumprod);
        }else if (target!=null){

            float maxcost = 0.0f;
            Group<NodeT> maxgroup = null;
            Map<Group<NodeT>,Float> travelerscost = getTravelersCosts();
            for (Entry<Group<NodeT>,Float> entry : travelerscost.entrySet()) {
                if (entry.getValue()>maxcost) {
                    maxgroup = entry.getKey();
                    maxcost = entry.getValue();
                }
            }

            float ell = 1;
            for (Group<NodeT> traveler : travelerscost.keySet()) 
                ell *= maxgroup.lambdas.get(traveler);
            lambdas.put(this,ell);
        }else{
            /* 
             * tricky: if target not unknown (targetless group) should postpone 
             * lambda aggragation, and still that could change for the next such 
             * meeting location.
             */
        }
    }

    public Group (NodeT tgt, NodeT[] sourceArr, float[][] lambdaArrs, Graph<NodeT> net, boolean minsum) {

        target = tgt;
        graph = net;

        int n = sourceArr.length;
        if (lambdaArrs.length != n) 
            throw new RuntimeException ("!! ERRROR - Number of travelers does not match with number of parameter arrays !!");

        subgroups = new ArrayList<>();
        for (NodeT source : sourceArr) 
            addSubgroup (new Group<>(source,net,minsum));

        lambdas = new HashMap<>();
        for (int i=0; i<n; ++i) {
            if (lambdaArrs[i].length != n) 
                throw new RuntimeException ("!! ERRROR - Some lambda parameters are missing for the "+i+"-th traveler !!");

            int j=0;
            for (Group<NodeT> subgroup : subgroups) 
                subgroups.get(i).lambdas.put (subgroup, lambdaArrs[i][j++]);
        }

        useMinSumRanking = minsum;
        if (minsum) {
            float sumprod = 0.0f;
            for (Group<NodeT> over : subgroups) {
                float product = 1.0f;
                for (Group<NodeT> under : subgroups)
                    if (!over.equals(under) && over.lambdas.containsKey(under))
                        product *= over.lambdas.get(under);
                sumprod += product;
            }
            lambdas.put(this,sumprod);
        }else if (target!=null){
            float maxcost = 0.0f;
            for (Group<NodeT> subgroup : subgroups) {
                float subcost = subgroup.lambdas.get(subgroup)*
                graph.getPathCost(subgroup.getTarget(),target);
                if (subcost>maxcost){
                    maxcost = subcost;
                    lambdas.put (this,subgroup.lambdas.get(subgroup));
                }
            }
        }else{
            /* 
             * tricky: if target not unknown (targetless group) should postpone 
             * lambda aggragation, and still that could change for the next such 
             * meeting location.
             */
        }
    }

    @Override public void finalize () throws Throwable {super.finalize(); if (lambdas.containsKey(this)) lambdas.remove(this);}

    public boolean contains (Group<NodeT> group) {
        if (group == null) 
            throw new IllegalArgumentException("\n!! ERROR - Null object argument in contains() invocation !!");

        if (equals(group)) return true;
        else{
            for (Group<NodeT> subgroup : subgroups)
                if (subgroup.contains(group))
                    return true;
            return false;
        }
    }

    @Override public Group<NodeT> clone () {
        try{super.clone();}catch(CloneNotSupportedException e){}
        Group<NodeT> clone = new Group<>(target,graph,useMinSumRanking);
        Map<Group<NodeT>,Float> xxx = new HashMap<Group<NodeT>, Float>(lambdas);

        for (Group<NodeT> subgroup : subgroups) {
            Group<NodeT> subclone = subgroup.clone();
            clone.subgroups.add (subclone);
            clone.lambdas.put(subclone,lambdas.get(subgroup));
            xxx.remove(subgroup);
        }
        clone.lambdas.put(clone,lambdas.get(this));

        xxx.remove(this);
        clone.lambdas.putAll(xxx);

        //group.subgroups.addAll (subgroups);
        //group.lambdas.putAll(lambdas);
        //group.lambdas.put(group, group.lambdas.remove(this));
        /*
        if (clone.subgroups.size() != subgroups.size()) {
            System.out.println("!! ERROR - Cloning subgroups !!");
        }
        if (clone.lambdas.size() != lambdas.size()) {
            System.out.println("!! ERROR - Cloning lambdas !!");
        }
        System.out.println("!! Cloning took place !!");
        */
        return clone;
    }

    public boolean replace (Group<NodeT> oldgroup, Group<NodeT> newgroup) {
        int i=0;
        for (Group<NodeT> subgroup : subgroups) {
            if (subgroup.equals(oldgroup)) {
                subgroups.remove (i);
                subgroups.add (newgroup);

                if (lambdas.containsKey(oldgroup)) {
                    lambdas.put (newgroup,lambdas.remove(oldgroup));
                }

                newgroup.lambdas.put(newgroup,subgroup.lambdas.remove(subgroup));
                for (Group<NodeT> other : subgroups) {
                    other.lambdas.put(newgroup, other.lambdas.remove(oldgroup));
                }
                return true;
            }else{
                boolean recretval = subgroup.replace(oldgroup, newgroup);
                if (recretval) return true;
            }
            ++i;
        }
        return false;
    }

    @Override public boolean equals (Object other) {
        if (other == null) return false;
        else if (other == this) return true;
        else if (getClass().equals(other.getClass())
                && ((Group<NodeT>)other).subgroups.size()==subgroups.size()
                && (((Group<NodeT>)other).target==target 
                || (target!=null && ((Group<NodeT>)other).target.equals(target)))) {
                    if (!subgroups.isEmpty()) {
                        for (Group<NodeT> x : ((Group<NodeT>)other).getSubgroups()) {
                            boolean matched = false;
                            for (Group<NodeT> y : subgroups) {
                                if (x.equals(y)) {
                                    matched = true;
                                    break;
                                }
                            }
                            if (!matched) return false;
                        }
                    }
                    return true;
        }
        else return false;
    }

    public NodeT getTarget () {return target;}
    public boolean isTrivial () {return subgroups.isEmpty();}
    public Map<Group<NodeT>,Float> getLambdas () {return lambdas;}
    public ArrayList<Group<NodeT>> getSubgroups () {return subgroups;}
    public void addSubgroup (Group<NodeT> subgroup) {subgroups.add(subgroup);}
    public void addSubgroups (Collection<Group<NodeT>> groups) {subgroups.addAll(groups);}

    public boolean isValidGrouping () {
        for (Group<NodeT> over : subgroups)
            for (Group<NodeT> under : subgroups)
                if (over==under) break;
                else if (!over.isDisjoint(under)) return false;
        return true;
    }

    public int numberTravelers () {return numberTravelers (this);}
    private int numberTravelers (Group<NodeT> group) {
        if (group.subgroups.isEmpty()) return 1;
        else{
            int total = 0;
            for (Group<NodeT> subgroup : group.subgroups)
                total += numberTravelers(subgroup);
            return total;
        }
    }

    public Map<Group<NodeT>,Float> getTravelersCosts () {
        assert (graph!=null);
        Map<Group<NodeT>,ArrayDeque<Group<NodeT>>> paths = new HashMap<>();
        Map<Group<NodeT>,ArrayDeque<Float>> weights = new HashMap<>();
        Map<Group<NodeT>,Float> costs = new HashMap<>();
        getTravelersCosts (costs,weights,paths);
        return costs;
    }

    public void getTravelersCosts (Map<Group<NodeT>,Float> costs,
                                    Map<Group<NodeT>,ArrayDeque<Float>> weights,
                                    Map<Group<NodeT>,ArrayDeque<Group<NodeT>>> paths) {
        if (subgroups.isEmpty()) {
            costs.put (this,0.0f);

            ArrayDeque<Float> weightqueue = new ArrayDeque<>();
            weightqueue.addLast (lambdas.get(this));
            weights.put (this,weightqueue);

            ArrayDeque<Group<NodeT>> pathqueue = new ArrayDeque<>();
            pathqueue.addLast (this);
            paths.put (this,pathqueue);
        }else{
            for (Group<NodeT> subgroup : subgroups)
                subgroup.getTravelersCosts (costs,weights,paths);

            for (Entry<Group<NodeT>,Float> over : costs.entrySet()) {
                if (target!=null) {
                    costs.put (over.getKey(), over.getValue() + weights.get(over.getKey()).getLast() 
                           * graph.getPathCost(paths.get(over.getKey()).getLast().target,target));
                }else costs.put (over.getKey(),Float.MAX_VALUE);

                float relaxation = 1.0f;
                for (Group<NodeT> under : costs.keySet())
                    relaxation *= over.getKey().lambdas.get(under);

                weights.get (over.getKey()).addLast (relaxation);
                paths.get (over.getKey()).addLast (this);
            }
        }
    }

    public ArrayDeque<Group<NodeT>> getTravelers () {
        ArrayDeque<Group<NodeT>> travelers = new ArrayDeque<>();
        getTravelers (travelers);
        return travelers;
    }

    public void getTravelers (ArrayDeque<Group<NodeT>> travelers) {
        if (subgroups.isEmpty()) travelers.add(this);
        else for (Group<NodeT> subgroup : subgroups)
                subgroup.getTravelers(travelers);
    }

    public Group<NodeT> mergeSubgroups (Group<NodeT> x, Group<NodeT> y) {
        assert (x.isDisjoint(y));
        if(x==y) throw new IllegalArgumentException("\n!! ERROR - Group arguments to be merged should be different to each other in mergeSubgroups() invocation !!");
        if(x==null) throw new IllegalArgumentException("\n!! ERROR - Null object argument in mergeSubgroups() invocation !!");
        if(y==null) throw new IllegalArgumentException("\n!! ERROR - Null object argument in mergeSubgroups() invocation !!");

        Group<NodeT> newgrouping = new Group<>(target,graph,useMinSumRanking);
        Group<NodeT> supergroup = x.mergeWith (y,target);
        newgrouping.meetups = supergroup.meetups;
        for (Group<NodeT> subgroup : subgroups) {
           if (subgroup!=x && subgroup!=y) {
               newgrouping.addSubgroup(subgroup);
               newgrouping.meetups += subgroup.meetups;
           }
        }
        newgrouping.addSubgroup(supergroup);
        return newgrouping;
    }

    public Group<NodeT> joinSubgroups (Group<NodeT> x, Group<NodeT> y) {
        assert (x.isDisjoint(y));
        if(x==y) throw new IllegalArgumentException("\n!! ERROR - Group arguments to be merged should be different to each other in joinSubgroups() invocation !!");
        if(x==null) throw new IllegalArgumentException("\n!! ERROR - Null object argument in joinSubgroups() invocation !!");
        if(y==null) throw new IllegalArgumentException("\n!! ERROR - Null object argument in joinSubgroups() invocation !!");

        Group<NodeT> newgrouping = new Group<>(target,graph,useMinSumRanking);
        Group<NodeT> supergroup = x.joinWith (y,target);
        newgrouping.meetups = supergroup.meetups;
        for (Group<NodeT> subgroup : subgroups) {
           if (subgroup!=x && subgroup!=y) {
               newgrouping.addSubgroup(subgroup);
               newgrouping.meetups += subgroup.meetups;
           }
        }

        newgrouping.addSubgroup(supergroup);
        return newgrouping;
    }

    public Group<NodeT> joinWith (Group<NodeT> other, NodeT targetarg) {
        assert (isDisjoint(other));

        Group<NodeT> supergroup = new Group<> (targetarg,graph,useMinSumRanking);
        supergroup.addSubgroup (other);
        supergroup.addSubgroup (this);

        if (useMinSumRanking) {
            ArrayDeque<Group<NodeT>> travelers = getTravelers();
            travelers.addAll(other.getTravelers());
            float sumrelax = 0.0f;
            for (Group<NodeT> over : travelers) {
                float relax = 1.0f;
                for (Group<NodeT> under : travelers)
                    relax *= over.lambdas.get(under);
                sumrelax += relax;
            }

            supergroup.lambdas.put(supergroup,sumrelax);

            NodeT meetup = useIterativeMeetupsComputation? 
                           new ConnectingLocationsIterable<>(supergroup,graph,null,useMinSumRanking).iterator().next()
                           :ComputeMeetingLocation.computeOMPfiltered(supergroup,graph,useMinSumRanking,1).pop();

            if (meetup==targetarg || (meetup!=null && meetup.equals(targetarg))) {
                supergroup.meetups = meetups + other.meetups;
                return supergroup;
            }else{
                Group<NodeT> updated = new Group<>(meetup,supergroup.subgroups,supergroup.lambdas,graph,useMinSumRanking);
                updated.lambdas.put(updated,sumrelax);
                supergroup.meetups = meetups + other.meetups + 1;
                return updated;
            }
        }else{
            float maxcost = -1.0f;
            Group<NodeT> maxgroup = null;
            Map<Group<NodeT>,Float> travelerscost = supergroup.getTravelersCosts();
            for (Entry<Group<NodeT>,Float> entry : travelerscost.entrySet()) {
                if (entry.getValue() > maxcost) {
                    maxgroup = entry.getKey();
                    maxcost = entry.getValue();
                }
            }

            float ell = 1.0f;
            //for (Group<NodeT> traveler : maxgroup.getTravelers())
            for (Group<NodeT> traveler : travelerscost.keySet()) 
                ell *= maxgroup.lambdas.get (traveler);

            supergroup.lambdas.put(supergroup,ell);

            NodeT meetup = useIterativeMeetupsComputation? 
                           new ConnectingLocationsIterable<>(supergroup,graph,null,useMinSumRanking).iterator().next()
                           :ComputeMeetingLocation.computeOMPfiltered(supergroup,graph,useMinSumRanking,1).pop();

            if (meetup==targetarg || (meetup!=null && meetup.equals(targetarg))) {
                supergroup.meetups = meetups + other.meetups;
                return supergroup;
            }else{
                Group<NodeT> updated = new Group<>(meetup,supergroup.subgroups,supergroup.lambdas,graph,useMinSumRanking);
                updated.lambdas.put(updated,ell);
                supergroup.meetups = meetups + other.meetups + 1;
                return updated;
            }
        }
    }

    public Group<NodeT> mergeWith (Group<NodeT> other, NodeT targetarg) {
        assert (isDisjoint(other));

        Group<NodeT> supergroup = new Group<> (targetarg,graph,useMinSumRanking);

        if (other.isTrivial()) supergroup.addSubgroup (other);
        else supergroup.addSubgroups (other.subgroups);

        if (isTrivial()) supergroup.addSubgroup (this);
        else supergroup.addSubgroups (subgroups);

        if (isTrivial() && other.isTrivial()) supergroup.meetups = 1;
        else if (isTrivial()) supergroup.meetups = other.meetups;
        else if (other.isTrivial()) supergroup.meetups = meetups;
        else supergroup.meetups = meetups + other.meetups - 1;

        if (useMinSumRanking) {
            ArrayDeque<Group<NodeT>> travelers = getTravelers();
            travelers.addAll(other.getTravelers());
            float sumrelax = 0.0f;
            for (Group<NodeT> over : travelers) {
                float relax = 1.0f;
                for (Group<NodeT> under : travelers)
                    relax *= over.lambdas.get(under);
                sumrelax += relax;
            }

            supergroup.lambdas.put(supergroup,sumrelax);

            NodeT meetup = useIterativeMeetupsComputation? 
                           new ConnectingLocationsIterable<>(supergroup,graph,null,useMinSumRanking).iterator().next()
                           :ComputeMeetingLocation.computeOMPfiltered(supergroup,graph,useMinSumRanking,1).pop();

            if (meetup==targetarg || (meetup!=null && meetup.equals(targetarg))) return supergroup;
            else{
                Group<NodeT> updated = new Group<>(meetup,supergroup.subgroups,supergroup.lambdas,graph,useMinSumRanking);
                updated.lambdas.put(updated,sumrelax);
                updated.meetups = supergroup.meetups;
                return updated;
            }
        }else{
            float maxcost = 0.0f;
            Group<NodeT> maxgroup = null;
            Map<Group<NodeT>,Float> travelerscost = supergroup.getTravelersCosts();
            for (Entry<Group<NodeT>,Float> entry : travelerscost.entrySet()) {
                if (entry.getValue()>maxcost) {
                    maxgroup = entry.getKey();
                    maxcost = entry.getValue();
                }
            }

            float ell = 1;
            for (Group<NodeT> traveler : travelerscost.keySet()) 
                ell *= maxgroup.lambdas.get(traveler);

            supergroup.lambdas.put(supergroup,ell);

            NodeT meetup = useIterativeMeetupsComputation? 
                           new ConnectingLocationsIterable<>(supergroup,graph,null,useMinSumRanking).iterator().next()
                           :ComputeMeetingLocation.computeOMPfiltered(supergroup,graph,useMinSumRanking,1).pop();

            if (meetup==targetarg || (meetup!=null && meetup.equals(targetarg))) return supergroup;
            else{
                Group<NodeT> updated = new Group<>(meetup,supergroup.subgroups,supergroup.lambdas,graph,useMinSumRanking);
                updated.lambdas.put (updated,ell);
                updated.meetups = supergroup.meetups;
                return updated;
            }
        }
    }

    public ArrayList<Group<NodeT>> mergeSubgroups (Group<NodeT> x, Group<NodeT> y, int k) {
        assert (x.isDisjoint(y));
        if(x==y) throw new IllegalArgumentException("\n!! ERROR - Group arguments to be merged should be different to each other in mergeSubgroups() invocation !!");
        if(x==null) throw new IllegalArgumentException("\n!! ERROR - Null object argument in mergeSubgroups() invocation !!");
        if(y==null) throw new IllegalArgumentException("\n!! ERROR - Null object argument in mergeSubgroups() invocation !!");

        ArrayList<Group<NodeT>> result = new ArrayList<>();
        for (Group<NodeT> supergroup : x.mergeWith (y,target,k)) {
            Group<NodeT> newgrouping = new Group<>(target,graph,useMinSumRanking);
            newgrouping.meetups = supergroup.meetups;
            for (Group<NodeT> subgroup : subgroups) {
                if (subgroup!=x && subgroup!=y) {
                    newgrouping.addSubgroup(subgroup);
                    newgrouping.meetups += subgroup.meetups;
                }
            }

            newgrouping.addSubgroup(supergroup);
            result.add(newgrouping);
        }
        return result;
    }

    public ArrayList<Group<NodeT>> joinSubgroups (Group<NodeT> x, Group<NodeT> y, int k) {
        assert (x.isDisjoint(y));
        if(x==y) throw new IllegalArgumentException("\n!! ERROR - Group arguments to be merged should be different to each other in joinSubgroups() invocation !!");
        if(x==null) throw new IllegalArgumentException("\n!! ERROR - Null object argument in joinSubgroups() invocation !!");
        if(y==null) throw new IllegalArgumentException("\n!! ERROR - Null object argument in joinSubgroups() invocation !!");

        ArrayList<Group<NodeT>> result = new ArrayList<>();
        for (Group<NodeT> supergroup : x.joinWith (y,target,k)) {
            Group<NodeT> newgrouping = new Group<>(target,graph,useMinSumRanking);
            for (Group<NodeT> subgroup : subgroups)
                if (subgroup!=x && subgroup!=y)
                    newgrouping.addSubgroup(subgroup);

            newgrouping.addSubgroup(supergroup);
            newgrouping.meetups = target.equals(supergroup.getTarget())?meetups:meetups+1;
            result.add(newgrouping);
        }
        return result;
    }

    public ArrayList<Group<NodeT>> joinWith (Group<NodeT> other, NodeT targetarg, int k) {
        assert (isDisjoint(other));

        Group<NodeT> supergroup = new Group<> (targetarg,graph,useMinSumRanking);
        supergroup.addSubgroup(other);
        supergroup.addSubgroup(this);

        if (useMinSumRanking) {
            ArrayDeque<Group<NodeT>> travelers = getTravelers();
            travelers.addAll (other.getTravelers());
            float sumrelax = 0.0f;
            for (Group<NodeT> over : travelers) {
                float relax = 1.0f;
                for (Group<NodeT> under : travelers)
                    relax *= over.lambdas.get(under);
                sumrelax += relax;
            }
            supergroup.lambdas.put(supergroup,sumrelax);


            Collection<NodeT> meetups;
            if (!useIterativeMeetupsComputation) {
                meetups = ComputeMeetingLocation.computeOMPfiltered(supergroup,graph,useMinSumRanking,k);
            }else{
                int i = 0;
                meetups = new ArrayList<>();
                for (NodeT meetup : new ConnectingLocationsIterable<>(supergroup,graph,null,useMinSumRanking)) {
                    if (i++>=k) break;
                    meetups.add(meetup);
                }
            }


            ArrayList<Group<NodeT>> result = new ArrayList<>();
            for (NodeT meetup : meetups) {
                if (meetup.equals(targetarg)){
                    supergroup.meetups = this.meetups + other.meetups;
                    result.add(supergroup);
                }else{
                    Group<NodeT> updated = new Group<>(meetup,supergroup.subgroups,
                                                       supergroup.lambdas,graph,useMinSumRanking);
                    updated.lambdas.put(updated,sumrelax);
                    updated.meetups = this.meetups + other.meetups + 1;
                    result.add(updated);
                }
            }
            return result;
        }else{
            float maxcost = 0.0f;
            Group<NodeT> maxgroup = null;
            Map<Group<NodeT>,Float> travelerscost = supergroup.getTravelersCosts();
            for (Entry<Group<NodeT>,Float> entry : travelerscost.entrySet()) {
                if (entry.getValue()>maxcost) {
                    maxgroup = entry.getKey();
                    maxcost = entry.getValue();
                }
            }

            float ell = 1;
            for (Group<NodeT> traveler : travelerscost.keySet()) 
                ell *= maxgroup.lambdas.get(traveler);
            supergroup.lambdas.put(supergroup,ell);

            //System.out.println("** [Group] Joining super-parameter: "+maxgroup.lambdas.get(maxgroup));


            Collection<NodeT> meetups;
            if (!useIterativeMeetupsComputation) {
                meetups = ComputeMeetingLocation.computeOMPfiltered(supergroup,graph,useMinSumRanking,k);
            }else{
                int i = 0;
                meetups = new ArrayList<>();
                for (NodeT meetup : new ConnectingLocationsIterable<>(supergroup,graph,null,useMinSumRanking)) {
                    if (i++>=k) break;
                    meetups.add(meetup);
                }
            }


            ArrayList<Group<NodeT>> result = new ArrayList<>();
            for (NodeT meetup : meetups) {
                if (meetup.equals(targetarg)){
                    supergroup.meetups = this.meetups + other.meetups;
                    result.add(supergroup);
                }else{
                    Group<NodeT> updated = new Group<>(meetup,supergroup.subgroups,supergroup.lambdas,graph,useMinSumRanking);
                    updated.lambdas.put(updated,ell);
                    updated.meetups = this.meetups + other.meetups + 1;
                    result.add(updated);
                }
            }
            return result;
        }
    }

    public ArrayList<Group<NodeT>> mergeWith (Group<NodeT> other, NodeT targetarg, int k) {
        assert (isDisjoint(other));

        Group<NodeT> supergroup = new Group<> (targetarg,graph,useMinSumRanking);

        if (other.isTrivial()) supergroup.addSubgroup(other);
        else supergroup.addSubgroups(other.subgroups);

        if (isTrivial()) supergroup.addSubgroup(this);
        else supergroup.addSubgroups(subgroups);

        if (isTrivial() && other.isTrivial()) supergroup.meetups = 1;
        else if (isTrivial()) supergroup.meetups = other.meetups;
        else if (other.isTrivial()) supergroup.meetups = meetups;
        else supergroup.meetups += meetups + other.meetups - 1;

        if (useMinSumRanking) {
            ArrayDeque<Group<NodeT>> travelers = getTravelers();
            travelers.addAll(other.getTravelers());
            float sumrelax = 0.0f;
            for (Group<NodeT> over : travelers) {
                float relax = 1.0f;
                for (Group<NodeT> under : travelers)
                    relax *= over.lambdas.get(under);
                sumrelax += relax;
            }
            supergroup.lambdas.put(supergroup,sumrelax);


            Collection<NodeT> meetups;
            if (!useIterativeMeetupsComputation) {
                meetups = ComputeMeetingLocation.computeOMPfiltered(supergroup,graph,useMinSumRanking,k);
            }else{
                int i = 0;
                meetups = new ArrayList<>();
/*
                for (Group g : this.getTravelers()) {
                    System.out.println (" %% [this] "+(NodeT)g.getTarget() + " of elements: " + ((Group)g.getTravelers().getFirst()).getTarget());
                }
                for (Group g : other.getTravelers()) {
                    System.out.println (" %% [other] "+(NodeT)g.getTarget() + " of elements: " + ((Group)g.getTravelers().getFirst()).getTarget());
                }
                System.out.println("----------------");
*/
                for (NodeT meetup : new ConnectingLocationsIterable<>(supergroup,graph,null,useMinSumRanking)) {
                    if (i++>=k) break;
                    meetups.add(meetup);
                }
            }


            ArrayList<Group<NodeT>> result = new ArrayList<>();
            for (NodeT meetup : meetups) {
                if (meetup.equals(targetarg)) {
                    result.add(supergroup);
                }else{
                    Group<NodeT> updated = new Group<>(meetup,supergroup.subgroups,supergroup.lambdas,graph,useMinSumRanking);
                    updated.lambdas.put(updated,sumrelax);
                    result.add(updated);
                }
            }
            return result;
        }else{
            float maxcost = 0.0f;
            Group<NodeT> maxgroup = null;
            Map<Group<NodeT>,Float> travelerscost = supergroup.getTravelersCosts();
            for (Entry<Group<NodeT>,Float> entry : travelerscost.entrySet()) {
                if (entry.getValue()>maxcost) {
                    maxgroup = entry.getKey();
                    maxcost = entry.getValue();
                }
            }

            float ell = 1;
            for (Group<NodeT> traveler : travelerscost.keySet()) 
                ell *= maxgroup.lambdas.get(traveler);
            supergroup.lambdas.put(supergroup,ell);

            //System.out.println("** [Group] Merging super-parameter: "+maxgroup.lambdas.get(maxgroup));


            Collection<NodeT> meetups;
            if (!useIterativeMeetupsComputation) {
                meetups = ComputeMeetingLocation.computeOMPfiltered(supergroup,graph,useMinSumRanking,k);
            }else{
                int i = 0;
                meetups = new ArrayList<>();
                for (NodeT meetup : new ConnectingLocationsIterable<>(supergroup,graph,null,useMinSumRanking)) {
                    if (i++>=k) break;
                    meetups.add(meetup);
                }
            }


            ArrayList<Group<NodeT>> result = new ArrayList<>();
            for (NodeT meetup : meetups) {
                if (meetup.equals(targetarg)){
                    result.add(supergroup);
                }else{
                    Group<NodeT> updated = new Group<>(meetup,supergroup.subgroups,supergroup.lambdas,graph,useMinSumRanking);
                    updated.lambdas.put(updated,ell);
                    result.add(updated);
                }
            }
            return result;
        }
    }

    public boolean isDisjoint (Group<NodeT> other) {
        if (other == null) 
            throw new IllegalArgumentException("\n!! ERROR - Null object argument in isDisjoint() !!");

        if (target.equals(other.target)) return false;

        ArrayDeque<Group<NodeT>> travelers = getTravelers();
        ArrayDeque<Group<NodeT>> othertravelers = other.getTravelers();

        travelers.retainAll(othertravelers);
        return travelers.isEmpty();
    }

    @Override
    public String toString () {
        StringBuilder output = new StringBuilder();
        output.append (" -------------------------------------------\n");
        output.append (" Description of group with ID " + hashCode()+"\n");
        output.append (" Associated with " + (lambdas.size()-1) + " other groups.\n");
        //for (Entry<Group<NodeT>,Float> entry : lambdas.entrySet())
        //    output.append("    Group "+entry.getKey().hashCode()+" with weight "+entry.getValue()+"\n");
        output.append (" Contains " + subgroups.size() + " subgroups that meet at location " + target + ".\n");

        int counter = 0;
        for (Group<NodeT> subgroup : subgroups)
            output.append ("\n Subgroup "+(++counter)+":\n"+subgroup);
        output.append (" -------------------------------------------\n");
        return output.toString();
    }

    public static void main (String[] args) {
        boolean useMinSumRanking = true;

        long target = 4000L;
        Long[] sources = {1000L,2000L,3000L,5000L};
        float[][] lambdas = {{.7f,.1f,.1f,.1f},
                             {.1f,.7f,.1f,.1f},
                             {.1f,.1f,.7f,.1f},
                             {.1f,.1f,.1f,.7f}};

        Graph<Long> graph = new UndirectedGraph<>("/home/gtsat/roadnet/data/lifeifei/OL.graph",NumberFormat.getNumberInstance(),true);
        Group<Long> group = new Group<>(target,sources,lambdas,graph,useMinSumRanking);
        for (Entry<Group<Long>,Float> entry : group.getTravelersCosts().entrySet()) {
            System.out.println (entry.getKey().target+": "+entry.getValue());
        }
    }
}
