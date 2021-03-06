package com.spbsu.ml.io;

import com.spbsu.commons.func.types.ConversionPack;
import com.spbsu.commons.func.types.TypeConverter;
import com.spbsu.commons.seq.CharSeqReader;
import com.spbsu.commons.seq.CharSeqTools;
import com.spbsu.ml.BFGrid;
import com.spbsu.ml.GridEnabled;
import com.spbsu.ml.models.ObliviousMultiClassTree;
import com.spbsu.ml.models.ObliviousTree;

import java.io.IOException;
import java.io.LineNumberReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * User: solar
 * Date: 12.08.13
 * Time: 13:05
 */
public class ObliviousMultiClassTreeConversionPack implements ConversionPack<ObliviousMultiClassTree, CharSequence> {
  private static final MessageFormat FEATURE_LINE_PATTERN = new MessageFormat("feature: {0, number}, bin: {1, number}, condition_value: {2, number,#.#####}", Locale.US);

  static {
    final DecimalFormat format = new DecimalFormat();
    format.setDecimalSeparatorAlwaysShown(false);
    format.setGroupingUsed(false);
    format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    format.setDecimalSeparatorAlwaysShown(true);
    format.setMaximumFractionDigits(5);
    format.setParseIntegerOnly(false);
    format.setParseIntegerOnly(false);

    FEATURE_LINE_PATTERN.setFormat(2, format);
  }

  public static class To implements TypeConverter <ObliviousMultiClassTree, CharSequence> {
    @Override
    public CharSequence convert(final ObliviousMultiClassTree ot) {
      final ObliviousTree bc = ot.binaryClassifier();
      StringBuilder result = new StringBuilder();
      for (final BFGrid.BinaryFeature feature : bc.features()) {
        result.append(FEATURE_LINE_PATTERN.format(new Object[]{feature.findex, feature.binNo, feature.condition}))
              .append("\n");
      }
      final int leafsCount = 1 << bc.features().size();
      for (int i = 0; i < leafsCount; i++) {
        if (bc.values()[i] != 0.) {
          result.append(Integer.toBinaryString(i))
                  .append(":").append(bc.values()[i])
                  .append(":").append(bc.based()[i])
                  .append(":");
          for (int c = 0; c < ot.mask(i).length; c++)
            result.append(ot.mask(i)[c] ? "1" : "0");
          result.append(" ");
        }
      }
      result = result.delete(result.length() - 1, result.length());

      return result;
    }
  }

  public static class From implements GridEnabled, TypeConverter<CharSequence, ObliviousMultiClassTree> {
    private BFGrid grid;

    @Override
    public BFGrid getGrid() {
      return grid;
    }

    @Override
    public void setGrid(final BFGrid grid) {
      this.grid = grid;
    }

    @Override
    public ObliviousMultiClassTree convert(final CharSequence source) {
      if (grid == null)
        throw new RuntimeException("Grid must be setup for serialization of oblivious trees, use SerializationRepository.customize!");
      String line;
      final LineNumberReader lnr = new LineNumberReader(new CharSeqReader(source));
      final List<BFGrid.BinaryFeature> splits = new ArrayList<BFGrid.BinaryFeature>(10);
      try {
        while ((line = lnr.readLine()) != null) {
          if (line.startsWith("feature")) {
            final Object[] parts = FEATURE_LINE_PATTERN.parse(line);
            final BFGrid.BinaryFeature bf = grid.row(((Long)parts[0]).intValue()).bf(((Long)parts[1]).intValue());
            splits.add(bf);
            if (Math.abs(bf.condition - ((Number)parts[2]).doubleValue()) > 1e-4)
              throw new RuntimeException("Inconsistent grid set, conditions do not match! Grid: " + bf.condition + " Found: " + parts[2]);
          }
          else break;
        }
        final double[] values = new double[1 << splits.size()];
        final double[] based = new double[1 << splits.size()];
        final boolean[][] masks = new boolean[1 << splits.size()][];
        final CharSequence[] valuesStr = CharSeqTools.split(line, ' ');
        for (final CharSequence value : valuesStr) {
          final CharSequence[] pattern2ValueBased = CharSeqTools.split(value, ':');
          final int leafIndex = Integer.parseInt(pattern2ValueBased[0].toString(), 2);
          values[leafIndex] = Double.parseDouble(pattern2ValueBased[1].toString());
//          based[leafIndex] = Double.parseDouble(pattern2ValueBased[2].toString());
          final boolean[] mask = masks[leafIndex] = new boolean[pattern2ValueBased[3].length()];
          for (int c = 0; c < pattern2ValueBased[3].length(); c++) {
            mask[c] = pattern2ValueBased[3].charAt(c) == '1';
          }
        }
        return new ObliviousMultiClassTree(splits, values, based, masks);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (ParseException e) {
        throw new RuntimeException("Invalid oblivious tree format", e);
      }
    }
  }

  @Override
  public Class<? extends TypeConverter<ObliviousMultiClassTree, CharSequence>> to() {
    return To.class;
  }

  @Override
  public Class<? extends TypeConverter<CharSequence, ObliviousMultiClassTree>> from() {
    return From.class;
  }
}
