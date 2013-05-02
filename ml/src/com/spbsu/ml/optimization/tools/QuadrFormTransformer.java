package com.spbsu.ml.optimization.tools;

import com.spbsu.commons.math.vectors.Mx;
import com.spbsu.commons.math.vectors.Vec;
import com.spbsu.commons.math.vectors.impl.VecBasedMx;


/**
 * User: qde
 * Date: 02.05.13
 * Time: 19:34
 */
public class QuadrFormTransformer {
    public static Mx getInvertedForm(Vec x, int factors) {
        int n = x.dim();
        Mx mxA = new VecBasedMx(n*factors, n*factors);
        for (int i = 0; i < n-1; i++)
            for (int j = i+1; j < n; j++)
                for (int k = 0; k < factors; k++) {
                    mxA.set(i + k*n, j + k*n, x.get(i)*x.get(j));
                    mxA.set(j + k*n, i + k*n, x.get(i)*x.get(j));
                }
        return mxA;
    }

    //show overview of the matrix of the 'inverted' quadratic form.
    // 'i*j' equal to 'x.get(i)*x.get(j)'
    public static void printOverview(int n, int factors) {
        int size = n*factors;
        String holder = "%s*%s";

        String[][] mxA = new String[size][size];
        for (int i = 0; i < n-1; i++)
            for (int j = i+1; j < n; j++)
                for (int k = 0; k < factors; k++) {
                    mxA[i + k*n][j + k*n] = String.format(holder, i, j);
                    mxA[j + k*n][i + k*n] = String.format(holder, i, j);
                }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                builder.append(j > 0 ? "\t\t\t" : "");
                if (mxA[i][j] != null)
                    builder.append(mxA[i][j]);
                else builder.append("0");
            }
            builder.append('\n');
        }
        System.out.println(builder.toString());
    }

    public static void main(String[] args) {
        printOverview(5, 3);
    }

}