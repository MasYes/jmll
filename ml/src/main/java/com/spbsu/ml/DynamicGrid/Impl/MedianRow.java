package com.spbsu.ml.DynamicGrid.Impl;

import com.spbsu.ml.DynamicGrid.Interface.BinaryFeature;
import com.spbsu.ml.DynamicGrid.Interface.DynamicGrid;
import com.spbsu.ml.DynamicGrid.Interface.DynamicRow;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.util.*;

/**
 * Created by noxoomo on 23/07/14.
 */
public class MedianRow implements DynamicRow {
    private final double eps = 1e-9;
    private final int origFIndex;
    private DynamicGrid grid = null;
    private final double[] feature;
    private final int[] reverse;
    private TIntHashSet known = new TIntHashSet();
    private TIntHashSet bad = new TIntHashSet();
    //    private final TIntArrayList borders = new TIntArrayList();
    private final ArrayList<BinaryFeatureImpl> bfs = new ArrayList<>();
    private final int levels;


    public MedianRow(DynamicGrid grid, double[] feature, int[] reverse, int origFIndex, int minSplits) {
        this.origFIndex = origFIndex;
        this.feature = feature;
        this.grid = grid;
        this.reverse = reverse;
        int lvl = 0;
        for (int i = 1; i < feature.length; ++i)
            if (feature[i] != feature[i - 1])
                ++lvl;
        this.levels = lvl;
        BinaryFeatureImpl end = new BinaryFeatureImpl(this, origFIndex, feature[feature.length - 1], feature.length);
        end.setBinNo(Integer.MAX_VALUE);
        bfs.add(end);
//        borders.add(feature.length);
        for (int i = 0; i < minSplits; ++i)
            addSplit();
        for (BinaryFeature bf : bfs)
            bf.setActive(true);
        addSplit();
    }

    public MedianRow(double[] feature, int[] reverse, int origFIndex) {
        this(null, feature, reverse, origFIndex, 1);
    }

    public MedianRow(double[] feature, int[] reverse, int origFIndex, int minSplits) {
        this(null, feature, reverse, origFIndex, minSplits);
    }

    public ArrayList<BinaryFeatureImpl> features() {
        return bfs;
    }

    @Override
    public int origFIndex() {
        return origFIndex;
    }


    @Override
    public int size() {
        return bfs.size() - 1;
    }

    @Override
    public DynamicGrid grid() {
        return grid;
    }

    private static Random rand = new Random();


    private List<BinaryFeatureImpl> bestSplitsCache = new ArrayList<>();

    @Override
    public boolean addSplit() {
        if (bfs.size() >= levels + 1)
            return false;
        if (bestSplitsCache.size() == 0) {
            updateCache();
        }

        return addFromCache();

//        Collections.sort(bfs, BinaryFeatureImpl.borderComparator);
//        for (int i = 0; i < bfs.size(); ++i) {
//            bfs.get(i).setBinNo(i);
//        }
//        return true;
    }

    private boolean addFromCache() {
        while (bestSplitsCache.size() > 0) {
            int ind = rand.nextInt(bestSplitsCache.size());
            BinaryFeatureImpl bf = bestSplitsCache.get(ind);
            bestSplitsCache.remove(ind);
            if (grid.isKnown(bf.gridHash)) {
                bad.add(bf.borderIndex);
                continue;
            }
            bfs.add(bf);
            grid.setKnown(bf.gridHash);
            Collections.sort(bfs, BinaryFeatureImpl.borderComparator);
            for (int i = 0; i < bfs.size(); ++i) {
                bfs.get(i).setBinNo(i);
            }
            return true;
        }
        return false;
    }


    private void updateCache() {
        double bestScore = 0;
        double diff = 0;
        int bestSplit = -1;
        TIntArrayList bestSplits = new TIntArrayList();
        for (int i = 0; i < bfs.size(); ++i) {
            int start = i > 0 ? bfs.get(i - 1).borderIndex : 0;
            int end = bfs.get(i).borderIndex;
            double median = feature[start + (end - start) / 2];
            int split = Math.abs(Arrays.binarySearch(feature, start, end, median));
            while (split > 0 && Math.abs(feature[split] - median) < eps) // look for first less then median value
                split--;
            if (Math.abs(feature[split] - median) > 1e-9) split++;

//
            final double scoreLeft = Math.log(end - split) + Math.log(split - start);
            if (split > 0) {
                if (scoreLeft > bestScore && !bad.contains(split)) {
                    bestScore = scoreLeft;
                    diff = (end - start + 1) * Math.log((end - start + 1.0) / feature.length)
                            - (end - split + 1.0) * Math.log((end - split + 1.0) / feature.length) - (split - start + 1.0) * Math.log((split - start) * 1.0 / feature.length);
                    diff /= feature.length;
                    bestSplit = split;
                    bestSplits.clear();
                    bestSplits.add(bestSplit);
                } else if (Math.abs(scoreLeft - bestScore) < 1e-8) {
                    bestSplits.add(split);
                }
            }
            while (++split < end && Math.abs(feature[split] - median) < eps)
                ; // first after elements with such value

            final double scoreRight = Math.log(end - split) + Math.log(split - start);
            if (split < end && !bad.contains(split)) {
                if (scoreRight > bestScore) {
                    bestScore = scoreRight;
                    bestSplit = split;
                    diff = (end - start + 1) * Math.log((end - start + 1.0) / feature.length)
                            - (end - split + 1.0) * Math.log((end - split + 1.0) / feature.length) - (split - start + 1.0) * Math.log((split - start) * 1.0 / feature.length);
                    diff /= feature.length;
                    bestSplits.clear();
                    bestSplits.add(bestSplit);
                } else if (Math.abs(scoreRight - bestScore) < 1e-8) {
                    bestSplits.add(split);
                }
            }
        }


        bestSplitsCache.clear();

        for (int i = 0; i < bestSplits.size(); ++i) {
            bestSplit = bestSplits.get(i);
            BinaryFeatureImpl newBF = new BinaryFeatureImpl(this, origFIndex, feature[bestSplit - 1], bestSplit);
            bestSplitsCache.add(newBF);
            newBF.setRegScore(diff);
        }

        int[] crcs = new int[bestSplitsCache.size()];
        for (int i = 0; i < feature.length; i++) { // unordered index
            final int orderedIndex = reverse[i];
            for (int b = 0; b < bestSplitsCache.size() && orderedIndex >= bestSplitsCache.get(b).borderIndex; b++) {
                crcs[b] = (crcs[b] * 31) + (i + 1);
            }
        }
        for (int i = 0; i < bestSplitsCache.size(); ++i) {
            bestSplitsCache.get(i).gridHash = crcs[i];
        }
    }


//
//    public boolean addSplit() {
//        if (bfs.size() >= levels + 1)
//            return false;
//        double bestScore = Double.POSITIVE_INFINITY;
//        double diff = 0;
//        int bestSplit = -1;
//        TIntArrayList bestSplits = new TIntArrayList();
//        for (int i = 0; i < bfs.size(); ++i) {
//            int start = i > 0 ? bfs.get(i - 1).borderIndex : 0;
//            int end = bfs.get(i).borderIndex;
//            double median = feature[start + (end - start) / 2];
//            int split = Math.abs(Arrays.binarySearch(feature, start, end, median));
//            while (split > start && Math.abs(feature[split] - median) < eps) // look for first less then median value
//                split--;
//            if (Math.abs(feature[split] - median) > 1e-9) split++;
//
////
//            diff = (end - start + 1) * Math.log((end - start + 1.0) / feature.length)
//                    - (end - split + 1.0) * Math.log((end - split + 1.0) / feature.length) - (split - start + 1.0) * Math.log((split - start) * 1.0 / feature.length);
//            diff /= feature.length;
//            final double scoreLeft = diff;
//            if (split > start) {
//                if (diff < bestScore) {
//                    bestScore = scoreLeft;
//                    bestSplit = split;
//                    bestSplits.clear();
//                    bestSplits.add(bestSplit);
//                } else if (Math.abs(scoreLeft - bestScore) < 1e-8) {
//                    bestSplits.add(split);
//                }
//            }
//            while (++split < end && Math.abs(feature[split] - median) < eps)
//                ; // first after elements with such value
//
//
//            diff = (end - start + 1) * Math.log((end - start + 1.0) / feature.length)
//                    - (end - split + 1.0) * Math.log((end - split + 1.0) / feature.length) - (split - start + 1.0) * Math.log((split - start) * 1.0 / feature.length);
//            diff /= feature.length;
//            final double scoreRight = diff;
//            if (split < end) {
//                if (scoreRight < bestScore) {
//                    bestScore = scoreRight;
//                    bestSplit = split;
//                    bestSplits.clear();
//                    bestSplits.add(bestSplit);
//                } else if (Math.abs(scoreRight - bestScore) < 1e-8) {
//                    bestSplits.add(split);
//                }
//            }
//        }
//        if (bestSplit > 100000)
//            return false;
////        bestSplit = bestSplits.get(rand.nextInt(bestSplits.size()));
////        bestSplit = bestSplits.get(0);
//
//        BinaryFeatureImpl newBF = new BinaryFeatureImpl(this, origFIndex, feature[bestSplit - 1], bestSplit);
//        bfs.add(newBF);
//        newBF.setRegScore(bestScore);
//
//        Collections.sort(bfs, BinaryFeatureImpl.borderComparator);
//        for (int i = 0; i < bfs.size(); ++i) {
//            bfs.get(i).setBinNo(i);
//        }
//        candidates += bestSplits.size();
//        return true;
//    }

    @Override
    public boolean empty() {
        return size() == 0;
    }

    @Override
    public BinaryFeature bf(int binNo) {
        return bfs.get(binNo);
    }

    @Override
    public void setOwner(DynamicGrid grid) {
        this.grid = grid;
    }

    @Override
    public int bin(double value) {
        int index = 0;
        while (index < size() && value > bfs.get(index).condition)
            index++;
        return index;
    }
}
