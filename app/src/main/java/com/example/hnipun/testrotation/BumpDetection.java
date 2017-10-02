/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.hnipun.testrotation;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Created by sanjana on 11/28/2016.
 */
public class BumpDetection {

    //Outputs the input 32 point segment contains a bump or not
    public boolean BumpDet(ArrayList<Double> Signal) {
        ArrayList<Double> Result = new ArrayList<>(); //Return the bump value and error value
        double alpha = -10; // Lipschitz exponent
        double[][] S = new double[32][1]; //2D array for the input
        double[][] A = new double[][]{
            {0.4902, -0.1373},
            {-0.1373, 0.0784},};
        boolean bump = false;

        /*Convert the input into 2D array*/
        for (int i = 0; i < Signal.size(); i++) {
            S[i][0] = Signal.get(i);
            //System.out.println((i+1)+" "+S[i][0]);
        }

        /*Basis matrix for the wavelet transformation*/
        double[][] W = new double[][]{
            {0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767},
            {0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.7071068, -0.7071068},
            {0.5000000, 0.5000000, -0.5000000, -0.5000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.5000000, 0.5000000, -0.5000000, -0.5000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.5000000, 0.5000000, -0.5000000, -0.5000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.5000000, 0.5000000, -0.5000000, -0.5000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.5000000, 0.5000000, -0.5000000, -0.5000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.5000000, 0.5000000, -0.5000000, -0.5000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.5000000, 0.5000000, -0.5000000, -0.5000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.5000000, 0.5000000, -0.5000000, -0.5000000},
            {0.3535534, 0.3535534, 0.3535534, 0.3535534, -0.3535534, -0.3535534, -0.3535534, -0.3535534, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.3535534, 0.3535534, 0.3535534, 0.3535534, -0.3535534, -0.3535534, -0.3535534, -0.3535534, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.3535534, 0.3535534, 0.3535534, 0.3535534, -0.3535534, -0.3535534, -0.3535534, -0.3535534, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.3535534, 0.3535534, 0.3535534, 0.3535534, -0.3535534, -0.3535534, -0.3535534, -0.3535534},
            {0.2500000, 0.2500000, 0.2500000, 0.2500000, 0.2500000, 0.2500000, 0.2500000, 0.2500000, -0.2500000, -0.2500000, -0.2500000, -0.2500000, -0.2500000, -0.2500000, -0.2500000, -0.2500000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.2500000, 0.2500000, 0.2500000, 0.2500000, 0.2500000, 0.2500000, 0.2500000, 0.2500000, -0.2500000, -0.2500000, -0.2500000, -0.2500000, -0.2500000, -0.2500000, -0.2500000, -0.2500000},
            {0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, 0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767, -0.1767767},};

        RealMatrix basis = MatrixUtils.createRealMatrix(W);
        RealMatrix signal = new Array2DRowRealMatrix(S);

        /*Wavelet transformation*/
        RealMatrix transformed = basis.multiply(signal);

        int[] level4_index = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        int[] level3_index = new int[]{17, 18, 19, 20, 21, 22, 23, 24};
        int[] colums = new int[]{0};

        //System.out.println("----------------");
        //System.out.println("Printing level_4 dwt values");
        double[] level_4_dwt = GetAbs(transformed.getSubMatrix(level4_index, colums).getColumn(0));
        //System.out.println("----------------");
        //System.out.println("Printing level_3 dwt values");
        double[] level_3_dwt = GetAbs(transformed.getSubMatrix(level3_index, colums).getColumn(0));

        //System.out.println("----------------");
        //System.out.println("Printing level_4 max maxima value and norm position");
        double[] maxima_l4_arr = MaxPeaks(level_4_dwt);
        double maxima_l4 = maxima_l4_arr[0];
        //System.out.println(maxima_l4);
        double maxima_l4_pos = maxima_l4_arr[1];
        //System.out.println(maxima_l4_pos);

        double maxima_l3 = Integer.MIN_VALUE;

        //System.out.println("----------------");
        //System.out.println("Printing level_3 maxima");
        if (maxima_l4 != Integer.MIN_VALUE) {
            maxima_l3 = Neighbor_maxima(level_3_dwt, maxima_l4_pos);
            // System.out.println(maxima_l3);
        }

        /*Calculate alpha*/
        //System.out.println("----------------");
        if (maxima_l4 != Integer.MIN_VALUE && maxima_l3 != Integer.MIN_VALUE) {
            //System.out.println("Printing alpha values");
            alpha = A[1][0] * (Math.log(maxima_l4) / Math.log(2) + Math.log(maxima_l3) / Math.log(2)) + A[1][1] * 7 * (Math.log(maxima_l4) / Math.log(2) + Math.log(maxima_l3) / Math.log(2));
            System.out.println(alpha);
        }

        if (alpha > -3.8) {
            bump = true;
        }
        
        double noiselevel = MAD(transformed.getSubMatrix(level4_index, colums).getColumn(0)) / 0.6745;
        //System.out.println(noiselevel);
        Result.add(alpha);
        Result.add(noiselevel);
        return bump;
    }

    //Returns the position and value of the maximum modulus maxima
    public static double[] MaxPeaks(double arrr[]) {
        int N = arrr.length;
        double[] arr = new double[N + 2];
        double max = Integer.MIN_VALUE;
        double max_position = Integer.MIN_VALUE;

        /* set corner values to -infinity */
        arr[0] = Integer.MAX_VALUE;
        arr[N + 1] = Integer.MAX_VALUE;

        for (int i = 1; i <= N; i++) {
            arr[i] = arrr[i - 1];
        }

        /* Find All Peak Elements */
        // System.out.println("\nAll Peak Elements : ");
        for (int i = 1; i <= N; i++) {
            if (arr[i - 1] < arr[i] && arr[i] > arr[i + 1]) {
                //System.out.println(arr[i] + " at position " + i);
                if (arr[i] > max) {
                    max = arr[i];
                    max_position = i / 16.0;
                }
            }
        }
        return new double[]{max, max_position};
    }

    //Returns the maxima of the neighborhood
    public static double Neighbor_maxima(double arrr[], double maxima_l4_pos) {
        int N = arrr.length;
        double[] arr = new double[N + 2];
        double min_pos = Integer.MAX_VALUE;
        double maxima = 0;

        /* set corner values to -infinity */
        arr[0] = Integer.MAX_VALUE;
        arr[N + 1] = Integer.MAX_VALUE;

        for (int i = 1; i <= N; i++) {
            arr[i] = arrr[i - 1];
        }

        /* Find All Peak Elements */
        // System.out.println("\nAll Peak Elements : ");
        for (int i = 1; i <= N; i++) {
            if (arr[i - 1] < arr[i] && arr[i] > arr[i + 1]) {
                //System.out.println(arr[i] + " at position " + i);
                double relpos = Math.abs(i / 8.0 - maxima_l4_pos);
                if (relpos < min_pos) {
                    min_pos = relpos;
                    maxima = arr[i];
                }
            }
        }
        return maxima;
    }

    //Returns the absolute values of the vector
    public static double[] GetAbs(double arrr[]) {
        int N = arrr.length;

        for (int i = 0; i <= N - 1; i++) {
            arrr[i] = Math.abs(arrr[i]);
            //System.out.println(arrr[i]);
        }
        return arrr;
    }

    // Returns the meadian absolute deviation(MAD)
    public static double MAD(double arrr[]) {
        int N = arrr.length;
        double[] arr = new double[N];
        double median = Median(arrr);

        for (int i = 0; i <= N - 1; i++) {
            arr[i] = Math.abs(arrr[i] - median);
        }
        return Median(arr);
    }

    //Returns the meadian value of the given array
    public static double Median(double arrr[]) {
        Arrays.sort(arrr);
        double median = 0;
        if (arrr.length % 2 == 0) {
            median = ((double) arrr[arrr.length / 2] + (double) arrr[arrr.length / 2 - 1]) / 2;
        } else {
            median = (double) arrr[arrr.length / 2];
        }
        return median;
    }
}