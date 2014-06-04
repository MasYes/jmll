package com.spbsu.ml.methods.spoc;

import com.spbsu.commons.math.MathTools;
import com.spbsu.commons.math.vectors.Mx;
import com.spbsu.commons.math.vectors.Vec;
import com.spbsu.commons.math.vectors.VecTools;
import com.spbsu.commons.math.vectors.impl.ArrayVec;
import com.spbsu.commons.math.vectors.impl.VecBasedMx;
import com.spbsu.ml.Func;
import com.spbsu.ml.FuncC1;
import com.spbsu.ml.data.DSIterator;
import com.spbsu.ml.data.DataSet;
import com.spbsu.ml.data.tools.MCTools;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;

/**
 * User: qdeee
 * Date: 21.05.14
 */
public class CMLMetricOptimization {
  private final DataSet ds;
  private final TIntObjectMap<TIntList> classesIdxs;
  private final Mx laplacian;
  private final double c;
  private final int iters;
  private final double step;

  public CMLMetricOptimization(final DataSet ds, final Mx S, final double c, final int iters, final double step) {
    this.ds = ds;
    this.iters = iters;
    this.step = step;
    this.classesIdxs = MCTools.splitClassesIdxs(ds);
    this.laplacian = VecTools.copy(S);
    VecTools.scale(laplacian, -1.0);
    for (int i = 0; i < laplacian.rows(); i++) {
      final double diagElem = VecTools.sum(S.row(i));
      laplacian.adjust(i, i, diagElem);
    }
    this.c = c;
  }

  public class ColumnTargetFunction extends FuncC1.Stub {
    private final Func binClassifier;

    public ColumnTargetFunction(final Func binClassifier) {
      this.binClassifier = binClassifier;
    }

    public Vec gradient(final Vec mu) {
      final Vec grad = new ArrayVec(mu.dim());
      for (int k = 0; k < grad.dim(); k++) {
        final TIntList idxs = classesIdxs.get(k);
        double val = 0.0;
        for (TIntIterator listIter = idxs.iterator(); listIter.hasNext(); ) {
          final Vec x = ds.data().row(listIter.next());
          final double trans = binClassifier.value(x);
          final double sigmoid = MathTools.sigmoid(trans);
          val -= (2 * sigmoid - 1) / (mu.get(k) * sigmoid + (1 - mu.get(k)) * (1 - sigmoid));
          grad.set(k, val);
        }
      }

      final double norm = VecTools.norm(grad);
      VecTools.scale(grad, 1 / norm);

      for (int k = 0; k < grad.dim(); k++) {
        double val = VecTools.multiply(laplacian.row(k), mu);
        grad.adjust(k, val);
      }
      return grad;
    }

    @Override
    public double value(final Vec mu) {
      double result = 0.0;
      for (DSIterator iter = ds.iterator(); iter.advance(); ) {
        final double trans = binClassifier.value(iter.x());
        final double sigmoid = MathTools.sigmoid(trans);
        final double underLog = mu.get((int)iter.y()) * sigmoid + (1 - mu.get((int)iter.y())) * (1 - sigmoid);
        result -= Math.log(underLog);
      }
      result += c * VecTools.multiply(VecTools.multiply(laplacian, mu), mu);
      return result;
    }

    @Override
    public int dim() {
      return laplacian.rows();
    }
  }

  public Mx trainProbs(final Mx codingMatrix, final Func[] binClassifiers) {
    final Mx result = new VecBasedMx(codingMatrix.rows(), codingMatrix.columns());
    for (int l = 0; l < result.columns(); l++) {
      System.out.println("Optimize column " + l);
      final FuncC1 columnTargetFunction = new ColumnTargetFunction(binClassifiers[l]);
      final Vec muColumn = optimizeColumn(columnTargetFunction, codingMatrix.col(l));
      VecTools.assign(result.col(l), muColumn);
    }
    return result;
  }

  public Vec optimizeColumn(FuncC1 func, Vec codingColumn) {
    final Vec mu = new ArrayVec(codingColumn.dim());
    for (int i = 0; i < mu.dim(); i++) {
      final double code = codingColumn.get(i);
      if (code == 1.0)
        mu.set(i, 1.0);
      else if (code == -1.0)
        mu.set(i, 0.0);
      else
        mu.set(i, 0.5);
    }

    double error = 100500;
    while (error > 1e-3) {
      final Vec muPrev = VecTools.copy(mu);
      final Vec gradient = func.gradient(mu);
      VecTools.incscale(mu, gradient, -step);

      for (int i = 0; i < mu.dim(); i++) {
        final double code = codingColumn.get(i);
        final double val = mu.get(i);
        if (code == 1.0 || val > 1.0) {
          mu.set(i, 1.0);
        }
        else if (code == -1.0 || val < 0) {
          mu.set(i, 0);
        }
      }
      System.out.println(mu);
      error = VecTools.norm(VecTools.subtract(muPrev, mu));
    }

    return new ArrayVec(codingColumn.dim());
  }
}