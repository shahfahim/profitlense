import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
class Main_calc{
    private int invest;
    String filePath;
    String target;
    int row;
    private List<List<String>>data;
    public Main_calc(int invest, String target){
        this.invest = invest;
        this.filePath = "data_1.csv";
        this.target  = target;    
    }
    public void readCSV() {
        List<List<String>> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                List<String> row = new ArrayList<>(Arrays.asList(values));
                data.add(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.data = data;
    }
    public void searchValue() {
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
    public boolean valid(){
        return (row< 0) ? false : true;
    }

    public void source_1(){
        if(!valid()){return;}
        System.out.println("For Source 1...");
        double distance = Double.parseDouble(data.get(row).get(1));
        double travel_cost_s1_pkm = Double.parseDouble(data.get(row).get(7));
        double production_cost_s1 = Double.parseDouble(data.get(row).get(11));
        double perHeadAnualIncome = Double.parseDouble(data.get(row).get(10));
        double travel_cost_s1 = distance * travel_cost_s1_pkm;
        double production_cost = production_cost_s1+(travel_cost_s1 / 500);
        double Max_selling_price = (perHeadAnualIncome/12)*(5.0/100.0); 
        double assumed_local_market_sell_price = 450.0;
        System.out.println("Per unit calculation ------");
        System.out.printf("Production Cost : %.2f%n", production_cost);
        System.out.printf("Maximum Selling price (according to per head income) : %.2f%n", Max_selling_price);
        System.out.printf("Maximum Profit : %.2f%n", (Max_selling_price - production_cost));
        double typical_profit = assumed_local_market_sell_price - production_cost;
        System.out.printf("Typical Profit : %.2f%n", typical_profit);
        double maximum_production_you_can_effort = Math.floor(invest/production_cost);
        double nearby_amount_to_add_to_get_max_profit = (Math.ceil(maximum_production_you_can_effort * production_cost) + production_cost);
        double addtionalBudget = (Math.ceil(nearby_amount_to_add_to_get_max_profit));
        double maxProfitWithInvestment = Math.ceil((maximum_production_you_can_effort* typical_profit));
        double increasedProfit = Math.ceil(((nearby_amount_to_add_to_get_max_profit / production_cost)* typical_profit)-invest);
        System.out.printf("Maximum product we can get within the budget : %.2f%n", maximum_production_you_can_effort);
        System.out.printf("Additional budget to get maximum profit : %.2f%n", addtionalBudget);
        System.out.printf("Profit with current investment : %.2f%n", maxProfitWithInvestment);
        System.out.printf("Profit with increased investment : %.2f%n", increasedProfit);
        System.out.println();
    }

    public void source_2(){
        if(!valid()){return;}
        System.out.println("For Source 2...");
        double distance = Double.parseDouble(data.get(row).get(2));
        double travel_cost_s1_pkm = Double.parseDouble(data.get(row).get(8));
        double production_cost_s1 = Double.parseDouble(data.get(row).get(12));
        double perHeadAnualIncome = Double.parseDouble(data.get(row).get(10));
        double travel_cost_s1 = distance * travel_cost_s1_pkm;
        double production_cost = production_cost_s1+(travel_cost_s1 / 500);
        double Max_selling_price = (perHeadAnualIncome/12)*(5.0/100.0); 
        double assumed_local_market_sell_price = 450.0;
        System.out.println("Per unit calculation ------");
        System.out.printf("Production Cost : %.2f%n", production_cost);
        System.out.printf("Maximum Selling price (according to per head income) : %.2f%n", Max_selling_price);
        System.out.printf("Maximum Profit : %.2f%n", (Max_selling_price - production_cost));
        double typical_profit = assumed_local_market_sell_price - production_cost;
        System.out.printf("Typical Profit : %.2f%n", typical_profit);
        double maximum_production_you_can_effort = Math.floor(invest/production_cost);
        double nearby_amount_to_add_to_get_max_profit = (Math.ceil(maximum_production_you_can_effort * production_cost) + production_cost);
        double addtionalBudget = (Math.ceil(nearby_amount_to_add_to_get_max_profit));
        double maxProfitWithInvestment = Math.ceil((maximum_production_you_can_effort* typical_profit));
        double increasedProfit = Math.ceil(((nearby_amount_to_add_to_get_max_profit / production_cost)* typical_profit)-invest);
        System.out.printf("Maximum product we can get within the budget : %.2f%n", maximum_production_you_can_effort);
        System.out.printf("Additional budget to get maximum profit : %.2f%n", addtionalBudget);
        System.out.printf("Profit with current investment : %.2f%n", maxProfitWithInvestment);
        System.out.printf("Profit with increased investment : %.2f%n", increasedProfit);
        System.out.println();
    }

    public void source_3(){
        if(!valid()){return;}
        System.out.println("For Source 3...");
        double distance = Double.parseDouble(data.get(row).get(3));
        double travel_cost_s1_pkm = Double.parseDouble(data.get(row).get(9));
        double production_cost_s1 = Double.parseDouble(data.get(row).get(13));
        double perHeadAnualIncome = Double.parseDouble(data.get(row).get(10));
        double travel_cost_s1 = distance * travel_cost_s1_pkm;
        double production_cost = production_cost_s1+(travel_cost_s1 / 500);
        double Max_selling_price = (perHeadAnualIncome/12)*(5.0/100.0); 
        double assumed_local_market_sell_price = 450.0;
        System.out.println("Per unit calculation ------");
        System.out.printf("Production Cost : %.2f%n", production_cost);
        System.out.printf("Maximum Selling price (according to per head income) : %.2f%n", Max_selling_price);
        System.out.printf("Maximum Profit : %.2f%n", (Max_selling_price - production_cost));
        double typical_profit = assumed_local_market_sell_price - production_cost;
        System.out.printf("Typical Profit : %.2f%n", typical_profit);
        double maximum_production_you_can_effort = Math.floor(invest/production_cost);
        double nearby_amount_to_add_to_get_max_profit = (Math.ceil(maximum_production_you_can_effort * production_cost) + production_cost);
        double addtionalBudget = (Math.ceil(nearby_amount_to_add_to_get_max_profit));
        double maxProfitWithInvestment = Math.ceil((maximum_production_you_can_effort* typical_profit));
        double increasedProfit = Math.ceil(((nearby_amount_to_add_to_get_max_profit / production_cost)* typical_profit)-invest);
        System.out.printf("Maximum product we can get within the budget : %.2f%n", maximum_production_you_can_effort);
        System.out.printf("Additional budget to get maximum profit : %.2f%n", addtionalBudget);
        System.out.printf("Profit with current investment : %.2f%n", maxProfitWithInvestment);
        System.out.printf("Profit with increased investment : %.2f%n", increasedProfit);
        System.out.println();
    }
    public void load(){
        readCSV();
        searchValue();
    }
    
}



public class backend {
    public static void main(String[] args) {
        Scanner src = new Scanner(System.in);
        System.out.print("Enter District Name : ");
        String name = src.nextLine();
        System.out.print("Enter Your Investment : ");
        int invest = src.nextInt();
        Main_calc mc = new Main_calc(invest, name);
        mc.load();
        mc.source_1();
        mc.source_2();
        mc.source_3();
    }

}