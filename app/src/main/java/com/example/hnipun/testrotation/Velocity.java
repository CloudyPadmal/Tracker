package com.example.hnipun.testrotation;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;

/**
 * Created by sanjana on 12/1/2016.
 */
public class Velocity {



        public static final int Fs = 50;// Sampling frequency
        public static final int L = 32; // Length of segmented signal

        FastFourierTransformer fourier = new FastFourierTransformer(DftNormalization.STANDARD);

        public double CalVelocity(ArrayList<Double> input) {
            double[] arrr=new double[32];
            for (int i = 0; i < input.size(); i++) {
                arrr[i] = input.get(i);
            }

            Complex[] velocityFFT = new Complex[L];
            double maxVelocity = Integer.MIN_VALUE;
            double meanVelocity = 0;

            Complex[] accelFft = fourier.transform(arrr, TransformType.FORWARD);
            accelFft = removeElement(accelFft, 16);

        /*descrete two sided frequcies((-L/2:L/2-1)*Fs/(L))*/
            double[] freq = new double[]{-25, -23.4375000000000, -21.8750000000000, -20.3125000000000, -18.7500000000000, -17.1875000000000, -15.6250000000000, -14.0625000000000, -12.5000000000000, -10.9375000000000, -9.37500000000000, -7.81250000000000, -6.25000000000000, -4.68750000000000, -3.12500000000000, -1.56250000000000, 1.56250000000000, 3.12500000000000, 4.68750000000000, 6.25000000000000, 7.81250000000000, 9.37500000000000, 10.9375000000000, 12.5000000000000, 14.0625000000000, 15.6250000000000, 17.1875000000000, 18.7500000000000, 20.3125000000000, 21.8750000000000, 23.4375000000000};

        /*Get the velocity from accelaration in freq domain*/
            for (int i = 0; i < accelFft.length; i++) {
                velocityFFT[i] = accelFft[i].divide(new Complex(0, 1).multiply(2 * Math.PI * freq[i]));
            }

        /*Zero pad for perform IFFT*/
            velocityFFT[L - 1] = new Complex(0, 0);
            Complex[] velocityIfft = fourier.transform(velocityFFT, TransformType.INVERSE);

        /*Return the maximum velocity of time domain*/
            for (int i = 0; i < velocityIfft.length; i++) {
                double real = (velocityIfft[i].getReal());
                double img = (velocityIfft[i].getImaginary());
                double velocity = Math.sqrt((real * real) + (img * img));

                if (velocity > maxVelocity) {
                    maxVelocity = velocity;
                }

                meanVelocity = meanVelocity+velocity;
            }
            return (meanVelocity/32);
        }

        //Remove the specified element from the array
        public static Complex[] removeElement(Complex[] original, int element) {
            Complex[] n = new Complex[original.length - 1];
            System.arraycopy(original, 0, n, 0, element);
            System.arraycopy(original, element + 1, n, element, original.length - element - 1);
            return n;
        }

}
