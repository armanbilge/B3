/*
 * NodeIntervals.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2015 BEAST Developers
 *
 * BEAST is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BEAST.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.evolution.coalescent;

import beast.evolution.tree.NodeRef;
import beast.evolution.util.Units;

import java.util.Arrays;

/**
 * A concrete class for a set of coalescent intervals associated with NodeRefs.
 *
 * @author Arman Bilge
 */
public class NodeIntervals implements IntervalList {

    public NodeIntervals(int maxEventCount) {
        events = new Event[maxEventCount];
        for (int i = 0; i < maxEventCount; i++) {
            events[i] = new Event();
        }
        eventCount = 0;
        sampleCount = 0;

        intervals = new double[maxEventCount - 1];
        intervalTypes = new IntervalType[maxEventCount - 1];
        lineageCounts = new int[maxEventCount - 1];

        nodeIntervals = new int[maxEventCount];

        intervalsKnown = false;
    }

    public void copyIntervals(NodeIntervals source) {
        intervalsKnown = source.intervalsKnown;
        intervalCount = source.intervalCount;
        eventCount = source.eventCount;
        sampleCount = source.sampleCount;

        for (int i = 0; i < events.length; i++) {
            events[i].time = source.events[i].time;
            events[i].type = source.events[i].type;
        }

        if (intervalsKnown) {
            System.arraycopy(source.intervals, 0, intervals, 0, intervals.length);
            System.arraycopy(source.intervalTypes, 0, intervalTypes, 0, intervals.length);
            System.arraycopy(source.lineageCounts, 0, lineageCounts, 0, intervals.length);
            System.arraycopy(source.nodeIntervals, 0, nodeIntervals, 0, nodeIntervals.length);
        }
    }

    public void resetEvents() {
        intervalsKnown = false;
        eventCount = 0;
        sampleCount = 0;
    }

    public void addSampleEvent(double time, NodeRef node) {
        events[eventCount].time = time;
        events[eventCount].type = IntervalType.SAMPLE;
        events[eventCount].node = node;
        eventCount++;
        sampleCount++;
        intervalsKnown = false;
    }

    public void addCoalescentEvent(double time, NodeRef node) {
        events[eventCount].time = time;
        events[eventCount].type = IntervalType.COALESCENT;
        events[eventCount].node = node;
        eventCount++;
        intervalsKnown = false;
    }

    public void addMigrationEvent(double time, int destination, NodeRef node) {
        events[eventCount].time = time;
        events[eventCount].type = IntervalType.MIGRATION;
        events[eventCount].info = destination;
        events[eventCount].node = node;
        eventCount++;
        intervalsKnown = false;
    }

    public void addNothingEvent(double time, NodeRef node) {
        events[eventCount].time = time;
        events[eventCount].type = IntervalType.NOTHING;
        events[eventCount].node = node;
        eventCount++;
        intervalsKnown = false;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public int getIntervalCount() {
        if (!intervalsKnown) calculateIntervals();
        return intervalCount;
    }

    public double getInterval(int i) {
        if (!intervalsKnown) calculateIntervals();
        return intervals[i];
    }

    public int getLineageCount(int i) {
        if (!intervalsKnown) calculateIntervals();
        return lineageCounts[i];
    }

    public int getCoalescentEvents(int i) {
        if (!intervalsKnown) calculateIntervals();
        if (i < intervalCount - 1) {
            return lineageCounts[i] - lineageCounts[i + 1];
        } else {
            return lineageCounts[i] - 1;
        }
    }

    public IntervalType getIntervalType(int i) {
        if (!intervalsKnown) calculateIntervals();
        return intervalTypes[i];
    }

    public double getTotalDuration() {

        if (!intervalsKnown) calculateIntervals();
        return events[eventCount - 1].time;
    }

    public int getNodeInterval(NodeRef node) {
        return nodeIntervals[node.getNumber()];
    }

    public boolean isBinaryCoalescent() {
        return true;
    }

    public boolean isCoalescentOnly() {
        return true;
    }

    private void calculateIntervals() {

        if (eventCount < 2) {
            throw new IllegalArgumentException("Too few events to construct intervals");
        }

        Arrays.sort(events, 0, eventCount);

        if (events[0].type != IntervalType.SAMPLE) {
            throw new IllegalArgumentException("First event is not a sample event");
        }

        intervalCount = eventCount - 1;

        double lastTime = events[0].time;
        nodeIntervals[events[0].node.getNumber()] = -1;

        int lineages = 1;
        for (int i = 1; i < eventCount; i++) {

            intervals[i - 1] = events[i].time - lastTime;
            intervalTypes[i - 1] = events[i].type;
            lineageCounts[i - 1] = lineages;
            if (events[i].type == IntervalType.SAMPLE) {
                lineages++;
            } else if (events[i].type == IntervalType.COALESCENT) {
                lineages--;
            }
            lastTime = events[i].time;
            nodeIntervals[events[i].node.getNumber()] = i-1;
        }
        intervalsKnown = true;
    }

    private Units.Type units = Units.Type.GENERATIONS;

    public final Units.Type getUnits() {
        return units;
    }

    public final void setUnits(Type units) {
        this.units = units;
    }

    private class Event implements Comparable<Event> {

        public int compareTo(Event e) {
            double t = e.time;
            if (t < time) {
                return 1;
            } else if (t > time) {
                return -1;
            } else {
                // events are at exact same time so sort by type
                return type.compareTo(e.type);
            }
        }

        /**
         * The type of event
         */
        IntervalType type;

        /**
         * The time of the event
         */
        double time;

        /**
         * Some extra information for the event (e.g., destination of a migration)
         */
        int info;

        /**
         * The node associated with the event
         */
        NodeRef node;
    }

    private Event[] events;
    private int eventCount;
    private int sampleCount;

    private boolean intervalsKnown = false;
    private double[] intervals;
    private int[] lineageCounts;
    private IntervalType[] intervalTypes;
    //private int[] destinations;
    private int intervalCount = 0;

    private int[] nodeIntervals;
}