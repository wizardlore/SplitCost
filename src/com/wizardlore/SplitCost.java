/*
 * Command-line utility that splits a cost amount by a set of fractional percentages
 * 
 */
package com.wizardlore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 *
 * @author mwhite
 */
public class SplitCost {

  public SplitCost(String[] args) {
    BigDecimal temp;
    BigDecimal cost = BigDecimal.valueOf(0.0);
    BigDecimal[] fractionsOfTotal;
    BigDecimal[] amounts = new BigDecimal[0];
    BigDecimal one = new BigDecimal(1.0);
    boolean simpleOutput = false;
    BufferedReader br = null;

    if (args != null) {

      if (args.length == 0) {

        // no arguments - handle user interaction
        br = new BufferedReader(new InputStreamReader(System.in));
        String response;

        System.out.print("Enter a cost amount to split: ");
        try {
          response = br.readLine().trim();
          if (canParseAsNumber(response)) {
            //System.out.println("cost response: " + response);
            cost = BigDecimal.valueOf(Double.parseDouble(response));

          }
        } catch (IOException ex) {
          System.out.println("[Error]: IO error trying to read input from console. " + ex.getMessage());
        }
        ArrayList<BigDecimal> fots = new ArrayList();
        BigDecimal splitsT = new BigDecimal(0.0);
        while (splitsT.compareTo(one) < 0.0) {
          System.out.print("Enter a fraction of total: (used: " + splitsT.doubleValue() + "; remaining: " + one.subtract(splitsT).doubleValue() + " ) ");
          try {
            response = br.readLine().trim();

            if (canParseAsNumber(response)) {
              System.out.println("fraction of total response: " + response);
              if (splitsT.add(BigDecimal.valueOf(Double.parseDouble(response))).compareTo(one) > 0.0) {
                System.out.println("Entry ignored: used would exceed 100% .");
              } else {
                splitsT = splitsT.add(BigDecimal.valueOf(Double.parseDouble(response)));
                fots.add(BigDecimal.valueOf(Double.parseDouble(response)));
              }
            } else {
              System.out.println("Unable to parse input as a number.");
            }
          } catch (IOException ex) {
            System.out.println("[Error]: IO error trying to read input from console. " + ex.getMessage());
          }
        }
        fractionsOfTotal = new BigDecimal[fots.size()];
        for (int i = 0; i < fots.size(); i++) {
          fractionsOfTotal[i] = fots.get(i);
        }

      } else {
                // handle command line argument input

        int index = 0;

        // first argument should be the cost to split apart or an -s
        if (args[0] != null) {
          if (args[index].equalsIgnoreCase("-h")) {
            printUsage();
            System.exit(0);
          }
          // check if first arg is -s
          if (args[index].equalsIgnoreCase("-s")) {
            index = 1;
            simpleOutput = true;
          }

          if ((args.length < 2 && index == 0) || (args.length < 3 && index == 1)) {
            System.out.println("Not enough arguments.");
            printUsage();
            System.exit(-2);
          }

          if (canParseAsNumber(args[index])) {
            cost = BigDecimal.valueOf(Double.parseDouble(args[index]));
          } else {
            System.out.println("An argument is not parsable as a number.");
            printUsage();
            System.exit(-3);
          }
        }

        fractionsOfTotal = new BigDecimal[args.length - index - 1];
        amounts = new BigDecimal[args.length - index - 1];

        // followed by a list of fractions of total
        BigDecimal FractionSum = BigDecimal.valueOf(0.0);
        FractionSum.setScale(8, RoundingMode.HALF_UP);
        for (int i = 0; i < fractionsOfTotal.length; i++) {
          if (canParseAsNumber(args[i + 1])) {
            fractionsOfTotal[i] = BigDecimal.valueOf(Double.parseDouble(args[i + 1 + index]));
            FractionSum = FractionSum.add(fractionsOfTotal[i]);
          } else {
            System.out.println("An argument is not parsable as a number.");
            printUsage();
            System.exit(-3);
          }
        }

        // the sum of the fractionsOfTotal array must equal 1 aka 100%
        if (FractionSum.compareTo(one) != 0.0) {
          System.out.println("The sum of the \"fractions of total\" does not equal 1.0");
          System.exit(-4);
        }

      }

      if (!simpleOutput) {
        System.out.println("Cost to split: " + cost);
        System.out.print("Fractions of total: ");
        for (int i = 0; i < fractionsOfTotal.length; i++) {
          if (i < fractionsOfTotal.length - 1) {
            System.out.print(fractionsOfTotal[i] + ", ");
          } else {
            System.out.println(fractionsOfTotal[i]);
          }
        }
      }

      BigDecimal[] results = computeSplitAmounts(cost, fractionsOfTotal);

      if (results != null) {
        if (simpleOutput) {
          for (int i = 0; i < results.length; i++) {
            if (i < results.length - 1) {
              System.out.print(results[i] + ", ");
            } else {
              System.out.println(results[i]);
            }
          }
        } else {
          System.out.println("------------------------");
          temp = BigDecimal.valueOf(0.0);
          for (int i = 0; i < results.length; i++) {
            System.out.println("Split Amount " + i + ": " + results[i]);
            temp = temp.add(results[i]);
          }
          System.out.println("------------------------");
          System.out.println("Splits Total: " + temp);
        }
      } else {
        System.out.println("no results");
      }

      if (br != null) {
        System.out.println("");
        System.out.print("Would you like to calculate another split cost? [y/n] ");
        try {
          String response = br.readLine().trim();
          if (response.equalsIgnoreCase("y")) {
            SplitCost splitCost = new SplitCost(new String[0]);
          }
        } catch (IOException ex) {
          System.out.println("[Error]: IO error trying to read input from console. " + ex.getMessage());
        }

      }

    } else {
      // show usage message
      System.out.println("Args was null");
      printUsage();
      System.exit(-1);
    }
  }

  private BigDecimal[] computeSplitAmounts(BigDecimal cost, BigDecimal[] fractionsOfTotal) {
    BigDecimal one = new BigDecimal(1.0);
    BigDecimal[] amounts = new BigDecimal[fractionsOfTotal.length];
    BigDecimal FractionSum = BigDecimal.valueOf(0.0);
    FractionSum.setScale(8, RoundingMode.HALF_UP);

    for (BigDecimal aFractionsOfTotal : fractionsOfTotal) {
      FractionSum = FractionSum.add(aFractionsOfTotal);
      //System.out.println("FractionSum: " + FractionSum.doubleValue());
    }

    // the sum of the fractionsOfTotal array must equal 1 aka 100%
    if (FractionSum.compareTo(one) != 0.0) {
      //System.out.println("Sum of Fractions do not equal 1.0: " + FractionSum);
      return null;
    }

    // calculate split amounts
    BigDecimal temp = BigDecimal.valueOf(0.0);
    for (int i = 0; i < fractionsOfTotal.length; i++) {
      amounts[i] = cost.multiply(fractionsOfTotal[i]).setScale(2, RoundingMode.HALF_UP);
      temp = temp.add(amounts[i]);
    }
    BigDecimal diff = cost.subtract(temp);
    if (diff.compareTo(BigDecimal.valueOf(0.0)) != 0.0) {
      amounts[amounts.length - 1] = amounts[amounts.length - 1].add(diff);
      //System.out.println("Leftover diffence amount was applied to last split: " + diff.doubleValue());
    }
    return amounts;
  }

  private void printUsage() {
    System.out.println("\r\nBasic usage:");
    System.out.println("\r\n  java -jar SplitCost.jar [-sh] [CostToSplit] [FractionsOfTotal, ...] ");
    System.out.println("\r\nexamples:");
    System.out.println("\r\n  java -jar SplitCost.jar 101.01 .3333 .3333 .3334");
    System.out.println("\r\n  -s Output split amounts only:");
    System.out.println("\r\n    java -jar SplitCost.jar -s 101.01 .5 .25 .10 .10 .05");
    System.out.println("\r\n  -h Output this help message:");
    System.out.println("\r\n    java -jar SplitCost.jar -h");
    System.out.println("\r\nExecute with no arguments to run interactively:");
    System.out.println("\r\n  java -jar SplitCost.jar\r\n");
  }

  private boolean canParseAsNumber(String arg) {
    try {
      Double d = Double.parseDouble(arg);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    SplitCost splitCost = new SplitCost(args);
  }
}
