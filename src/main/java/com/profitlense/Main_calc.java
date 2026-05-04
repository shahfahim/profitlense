package com.profitlense;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class Main_calc {
    private int invest;
    String target;
    int row;
    private List<List<String>> data;
    private List<Double> Max_prof_curr = new ArrayList<>();
    private List<Double> Max_prof_inc = new ArrayList<>();

    public Main_calc(int invest, String target) {
        this.invest = invest;
        this.target = target;
    }

    // --- (UPDATED) Method to log user activity ---
    public void logCalculation(String userEmail) {
        // Create the log entry using the ActivityLogger
        ActivityLogger.log(
                userEmail,
                "Calculation Run",
                "District: " + this.target + ", Investment: " + this.invest
        );
    }
    // --- (END OF UPDATE) ---

    public void readCSV() {
        List<List<String>> data = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/data_calc.csv")) {
            if (is == null) {
                System.err.println("CRITICAL ERROR: data_calc.csv not found in resources!");
                this.data = data;
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                List<String> row = new ArrayList<>();
                for (String val : values) {
                    row.add(val);
                }
                data.add(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.data = data;
    }

    public void searchValue() {
        if (data == null) {
            this.row = -1;
            return;
        }
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).size(); j++) {
                if (data.get(i).get(j).equalsIgnoreCase(target)) {
                    this.row = i;
                    return;
                }
            }
        }
        this.row = -1;
    }

    public boolean valid() {
        return (row >= 0);
    }

    public String source_1() {
        return calculateSource("Source 1", 1, 7, 11);
    }

    public String source_2() {
        return calculateSource("Source 2", 2, 8, 12);
    }

    public String source_3() {
        return calculateSource("Source 3", 3, 9, 13);
    }

    private String calculateSource(String label, int distanceIndex, int travelCostIndex, int productionCostIndex) {
        if (!valid()) return label + ": Invalid district or data.";
        if (data == null || data.get(row).size() <= productionCostIndex) {
            return label + ": Data is missing or incomplete for this district.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("For ").append(label).append("...\n");
        sb.append("Per unit calculation ------\n");

        try {
            double distance = Double.parseDouble(data.get(row).get(distanceIndex));
            double travelCostPerKm = Double.parseDouble(data.get(row).get(travelCostIndex));
            double baseProductionCost = Double.parseDouble(data.get(row).get(productionCostIndex));
            double perHeadAnnualIncome = Double.parseDouble(data.get(row).get(10));

            double travelCost = distance * travelCostPerKm;
            double productionCost = baseProductionCost + (travelCost / 500.0);
            double maxSellingPrice = (perHeadAnnualIncome / 12.0) * (5.0 / 100.0);
            double assumedLocalSellPrice = maxSellingPrice * 0.9;
            double typicalProfit = assumedLocalSellPrice - productionCost;

            sb.append(String.format("Production Cost : %.2f%n", productionCost));
            sb.append(String.format("Maximum Selling price : %.2f%n", maxSellingPrice));
            sb.append(String.format("Maximum Profit : %.2f%n", (maxSellingPrice - productionCost)));

            double maxUnitsWithinBudget = Math.floor(invest / productionCost);
            double maxProfitWithInvestment = maxUnitsWithinBudget * typicalProfit;

            double nearbyAmountToAdd = Math.ceil(maxUnitsWithinBudget * productionCost + productionCost);
            double newTotalUnits = Math.floor(nearbyAmountToAdd / productionCost);
            double profitWithIncreasedInvestment = newTotalUnits * typicalProfit;
            double increasedProfit = profitWithIncreasedInvestment - maxProfitWithInvestment;

            sb.append(String.format("Maximum product we can get : %.2f%n", maxUnitsWithinBudget));
            sb.append(String.format("Additional budget for max profit : %.2f%n", nearbyAmountToAdd));
            sb.append(String.format("Profit with current investment : %.2f%n", maxProfitWithInvestment));
            sb.append(String.format("Profit with increased investment : %.2f%n", profitWithIncreasedInvestment));
            sb.append(String.format("Increase in profit : %.2f%n", increasedProfit));

            Max_prof_curr.add(maxProfitWithInvestment);
            Max_prof_inc.add(profitWithIncreasedInvestment);
            sb.append("\n");

        } catch (Exception e) {
            e.printStackTrace();
            return "Error calculating " + label + ". Check CSV data format.\n" + e.getMessage() + "\n";
        }

        return sb.toString();
    }

    public void load() {
        Max_prof_curr.clear();
        Max_prof_inc.clear();
        readCSV();
        searchValue();
    }

    public String suggestion() {
        if (!valid()) return "No suggestion available. District not found.";
        if (Max_prof_curr.isEmpty() || Max_prof_inc.isEmpty()) {
            return "No suggestion available. Please run calculations first.";
        }

        StringBuilder sb = new StringBuilder();
        double max_ = Double.MIN_VALUE;
        int index = 0;

        for (int i = 0; i < Max_prof_curr.size(); i++) {
            if (Max_prof_curr.get(i) > max_) {
                max_ = Max_prof_curr.get(i);
                index = i + 1;
            }
        }

        if (data == null || data.isEmpty() || data.get(0).size() <= index) {
            return "Error: Cannot retrieve source name for suggestion.";
        }

        sb.append("Current investment \nProfit : ")
                .append(Math.round(max_ * 100.0) / 100.0)
                .append("  Source : ").append(data.get(0).get(index))
                .append("\n\n");

        max_ = Double.MIN_VALUE;
        index = 0;
        for (int i = 0; i < Max_prof_inc.size(); i++) {
            if (Max_prof_inc.get(i) > max_) {
                max_ = Max_prof_inc.get(i);
                index = i + 1;
            }
        }

        if (data.get(0).size() <= index) {
            return "Error: Cannot retrieve source name for suggestion.";
        }

        sb.append("Increased investment \nProfit : ")
                .append(Math.round(max_ * 100.0) / 100.0)
                .append("  Source : ").append(data.get(0).get(index));

        return sb.toString();
    }

    public List<Double> getMaxProfCurr() {
        return Max_prof_curr;
    }
}