
package com.zygon.mmesh;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

/**
 *
 * @author zygon
 */
public class MathMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        Percentile percentile = new Percentile();
        
        percentile.setData(new double[]{10, 20, 30, 40, 50, 60, 70, 80, 90});
        
        System.out.println(percentile.evaluate(5));
        System.out.println(percentile.evaluate(15));
        System.out.println(percentile.evaluate(35));
        System.out.println(percentile.evaluate(55));
        System.out.println(percentile.evaluate(70));
        System.out.println(percentile.evaluate(85));
        System.out.println(percentile.evaluate(95));
    }

}
