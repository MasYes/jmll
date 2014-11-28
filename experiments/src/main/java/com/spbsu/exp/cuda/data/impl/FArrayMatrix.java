package com.spbsu.exp.cuda.data.impl;

import com.spbsu.exp.cuda.data.FMatrix;
import com.spbsu.exp.cuda.data.FVector;
import gnu.trove.list.TIntList;
import org.jetbrains.annotations.NotNull;

/**
 * jmll
 * ksen
 * 22.October.2014 at 22:48
 */
public class FArrayMatrix implements FMatrix {

  private float[] data;
  private int rows;

  public FArrayMatrix(final int rows, final int columns) {
    data = new float[rows * columns];
    this.rows = rows;
  }

  public FArrayMatrix(final int rows, final float[] data) {
    this.data = data;
    this.rows = rows;
  }

  @Override
  public FMatrix set(final int i, final int j, final float value) {
    data[i + j * rows] = value;
    return this;
  }

  @Override
  public float get(final int i, final int j) {
    return data[i + j * rows];
  }

  @NotNull
  @Override
  public FVector getColumn(final int j) {
    final float[] destination = new float[rows];
    System.arraycopy(data, rows * j, destination, 0, rows);
    return new FArrayVector(destination);
  }

  @NotNull
  @Override
  public FMatrix getColumnsRange(final int begin, final int length) {
    final float[] destination = new float[rows * length];
    System.arraycopy(data, rows * begin, destination, 0, rows * length);
    return new FArrayMatrix(rows, destination);
  }

  @NotNull
  @Override
  public FMatrix getColumnsRange(final @NotNull TIntList indexes) {
    final int size = indexes.size();
    final float[] destination = new float[rows * size];
    for (int i = 0; i < size; i++) {
      System.arraycopy(data, rows * indexes.get(i), destination, rows * i, rows);
    }
    return new FArrayMatrix(rows, destination);
  }

  @Override
  public void setColumn(final int j, final @NotNull FVector column) {
    setColumn(j, column.toArray());
  }

  @Override
  public void setColumn(int j, float[] column) {
    System.arraycopy(column, 0, data, rows * j, rows);
  }

  @Override
  public float[] toArray() {
    return data;
  }

  public int getRows() {
    return rows;
  }

  public int getColumns() {
    return data.length / rows;
  }

}
