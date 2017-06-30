/*
 * Command-line utility that splits an order cost amount by a set of fractional percentages
 * 
 */
package com.wizardlore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
//import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 *
 * @author mwhite
 */
public class SplitCost {

  public SplitCost(String[] args) {
    BigDecimal temp;
    BigDecimal orderCost = null;
    ArrayList<BigDecimal> foos = new ArrayList<>();
    BigDecimal FractionSum = BigDecimal.valueOf(0.0);
    FractionSum.setScale(8, RoundingMode.HALF_UP);
    BigDecimal[] results;
    BigDecimal one = BigDecimal.valueOf(1.0);
    boolean simpleOutput = false;
    BufferedReader br = null;

    if (args != null) {

      if (args.length == 0) {

        // no arguments - handle user interaction
        br = new BufferedReader(new InputStreamReader(System.in));
        String response;

        System.out.print("Enter an order cost amount to split: ");
        try {
          response = br.readLine().trim();
          if (canParseAsNumber(response)) {
            //System.out.println("order cost response: " + response);
            orderCost = BigDecimal.valueOf(Double.parseDouble(response));
          }
        } catch (IOException ex) {
          System.out.println("[Error]: IO error trying to read input from console. " + ex.getMessage());
        }

        BigDecimal splitsT = new BigDecimal(0.0);
        while (splitsT.compareTo(one) < 0.0) {
          System.out.print("Enter a fraction of order: (used: " + splitsT.doubleValue() + "; remaining: " + one.subtract(splitsT).doubleValue() + " ) ");
          try {
            response = br.readLine().trim();
            if (canParseAsNumber(response)) {
              //System.out.println("fraction of order response: " + response);
              if (splitsT.add(BigDecimal.valueOf(Double.parseDouble(response))).compareTo(one) > 0.0) {
                System.out.println("Fraction Entry ignored. Would exceed 100%");
              } else {
                splitsT = splitsT.add(BigDecimal.valueOf(Double.parseDouble(response)));
                foos.add(BigDecimal.valueOf(Double.parseDouble(response)));
              }
            } else {
              System.out.println("Unable to parse input as a number. Please try again.");
            }
          } catch (IOException ex) {
            System.out.println("[Error]: IO error trying to read input from console. " + ex.getMessage());
          }
        }
      } else {
        // parse command line arguments
        int index = 0;
        for (String arg : args) {
          if (arg != null) {
            if (canParseAsNumber(arg)) {
              if (orderCost == null) {
                orderCost = BigDecimal.valueOf(Double.parseDouble(arg));
              } else {
                foos.add(BigDecimal.valueOf(Double.parseDouble(arg)));
                FractionSum = FractionSum.add(BigDecimal.valueOf(Double.parseDouble(arg)));
              }
            } else {
              //check if arg is a command trigger
              if (arg.equalsIgnoreCase("-h")
                      || args[index].equalsIgnoreCase("-sh")
                      || args[index].equalsIgnoreCase("-hs")) {
                // display usage help
                printUsage();
                return;
              } else if (arg.equalsIgnoreCase("-s")) {
                // simple output
                simpleOutput = true;
              } else {
                // unknown 
                System.out.println("-1: Unable to parse argument \""+arg+"\" as a number or command trigger.");
              }
            }
          }
        }

      }

      //verify input
      if (orderCost != null) {
        //the sum of the Fractions Of Order must equal 1 aka 100%
        if (FractionSum.compareTo(one) == 0.0) {
          results = computeSplitAmounts(orderCost, foos);
        } else {
          System.out.println("-3: The sum of \"Fractions Of Order\" does not equal 1.0");
          return;
        }
      } else {
        System.out.println("-2: Unable to parse required Order Cost argument.");
        printUsage();
        return;
      }

      //output
      if (!simpleOutput) {
        System.out.println("Order Cost to split: " + orderCost);
        System.out.print("Fractions of Order: ");
        int i = 1;
        for (BigDecimal foo : foos) {
          if (i < foos.size()) {
            System.out.print(foo + ", ");
          } else {
            System.out.println(foo);
          }
          i++;
        }
      }

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
        System.out.println("no results produced");
      }
      // continue user interaction, if any
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
      // this should never be reached
      // show usage message
      System.out.println("Args was null");
      printUsage();
    }
  }

  private BigDecimal[] computeSplitAmounts(BigDecimal cost, ArrayList<BigDecimal> fractionsOfOrder) {
    BigDecimal one = new BigDecimal(1.0);
    BigDecimal[] amounts = new BigDecimal[fractionsOfOrder.size()];
    BigDecimal FractionSum = BigDecimal.valueOf(0.0);
    FractionSum.setScale(8, RoundingMode.HALF_UP);

    for (BigDecimal aFractionsOfOrder : fractionsOfOrder) {
      FractionSum = FractionSum.add(aFractionsOfOrder);
    }

    // the sum of the fractionsOfOrder array must equal 1 aka 100%
    if (FractionSum.compareTo(one) != 0.0) {
      return null;
    }

    // calculate split amounts
    BigDecimal temp = BigDecimal.valueOf(0.0);
    for (int i = 0; i < fractionsOfOrder.size(); i++) {
      amounts[i] = cost.multiply(fractionsOfOrder.get(i)).setScale(2, RoundingMode.HALF_UP);
      temp = temp.add(amounts[i]);
    }
    BigDecimal diff = cost.subtract(temp);
    if (diff.compareTo(BigDecimal.valueOf(0.0)) != 0.0) {
      // apply difference to largest split
      int largestSplitIndex = getIndexofLargest(amounts);
      amounts[largestSplitIndex] = amounts[largestSplitIndex].add(diff);
      System.out.print("Applied Diff of " + diff + ", to index: " + largestSplitIndex);

    }
    return amounts;
  }

  private void printUsage() {
    System.out.println("Basic usage:");
    System.out.println("  java -jar SplitCost.jar [-sh] [OrderCostToSplit] [FractionsOfOrder ...] ");
    System.out.println("");
    System.out.println("OrderCostToSplit - numeric order total amount");
    System.out.println("FractionsOfOrders - space separated list of the fractions to split the order by.");
    System.out.println("  The sum of the list of Fractions of Order must equal 1.0");
    System.out.println("");
    System.out.println("examples:");
    System.out.println("  java -jar SplitCost.jar 101.01 .3333 .3333 .3334");
    System.out.println("");
    System.out.println("-s Output split amounts only:");
    System.out.println("  java -jar SplitCost.jar -s 101.01 .5 .25 .10 .10 .05");
    System.out.println("");
    System.out.println("-h Output this help message:");
    System.out.println("  java -jar SplitCost.jar -h");
    System.out.println("");
    System.out.println("Execute with no arguments to run interactively:");
    System.out.println("  java -jar SplitCost.jar\r\n");
  }

  private boolean canParseAsNumber(String arg) {
    try {
      Double d = Double.parseDouble(arg);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  private int getIndexofLargest(BigDecimal[] amounts) {
    int i = 0;
    int index = 0;
    double largest = Double.MIN_VALUE;
    for (BigDecimal amount : amounts) {
      if (amount.doubleValue() > largest) {
        largest = amount.doubleValue();
        index = i;
      }
      i++;
    }
    return index;
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    SplitCost splitCost = new SplitCost(args);
  }

}
