package com.spbsu.ml.func;

import com.spbsu.commons.func.Computable;
import com.spbsu.commons.func.Evaluator;
import com.spbsu.commons.math.vectors.Vec;
import com.spbsu.commons.math.vectors.VecTools;
import com.spbsu.commons.math.vectors.impl.ArrayVec;
import com.spbsu.commons.util.ArrayTools;
import com.spbsu.ml.Trans;

import java.util.List;

/**
 * User: solar
 * Date: 26.11.12
 * Time: 15:56
 */
public class Ensemble<F extends Trans> extends Trans.Stub {
  public final F[] models;
  public final Vec weights;

  public Ensemble(F[] models, Vec weights) {
    this.models = models;
    this.weights = weights;
  }

  public Ensemble(List<F> weakModels, double step) {
    this(weakModels.toArray((F[])new Trans[weakModels.size()]), VecTools.fill(new ArrayVec(weakModels.size()), step));
  }

  public F last() {
    return models[size() - 1];
  }

  public int size() {
    return models.length;
  }

  public double wlast() {
    return weights.get(size() - 1);
  }

  @Override
  public int xdim() {
    return models[0].xdim() * models.length;
  }

  @Override
  public int ydim() {
    return models[ArrayTools.max(models, new Evaluator<F>() {
      @Override
      public double value(F f) {
        return f.ydim();
      }
    })].ydim();
  }

  @Override
  public Trans gradient() {
    return new Ensemble<Trans>(ArrayTools.map(models, Trans.class, new Computable<F, Trans>() {
      @Override
      public Trans compute(F argument) {
        return argument.gradient();
      }
    }), weights);
  }

  @Override
  public Vec trans(Vec x) {
    Vec result = new ArrayVec(ydim());
    for (int i = 0; i < models.length; i++) {
      VecTools.append(result, models[i].trans(x));
    }
    VecTools.scale(result, 1./models.length);
    return result;
  }
}