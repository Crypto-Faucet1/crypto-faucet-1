import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class WithdrawHandler {
    String removeBalance(Request request, Response response) {
        String res = "done";
        String address = request.queryParams("address");
        double balanceRemove = Double.parseDouble(request.queryParams("balanceRemove"));

        String result = "[]";
        double balance = 0.0;
        long lastClaim = 0;
        double dailyBonus = 0;
        long dailyLastClaim = 0;
        int claims = 0;
        int num = -1;
        double totalPaid = 0.0;
        try {
            File file1 = new File("addresses.json");
            result = Files.asCharSource(file1, Charsets.UTF_8).read();
        } catch (Exception e) {
        }
        JSONArray jsonArray = new JSONArray(result);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject addr = jsonArray.getJSONObject(i);
            try {
                if (addr.getString("address").equals(address)) {
                    System.out.println("Balance: " + addr.getDouble("balance") + " Address found " + address);
                    balance = addr.getDouble("balance");
                    lastClaim = addr.getLong("lastClaim");
                    dailyLastClaim = addr.getLong("dailyLastClaim");
                    dailyBonus = addr.getDouble("dailyBonus");
                    claims = addr.getInt("claims");
                    totalPaid = addr.getDouble("totalPaid");
                    num = i;
                }
            } catch (Exception e) {
                res = "Couldn't find address";
            }
        }
        if (num != -1) {
            jsonArray.remove(num);
        }
        totalPaid = totalPaid + balanceRemove;
        JSONObject newItem = new JSONObject();
        newItem.put("address", address);
        newItem.put("balance", balance - balanceRemove);
        newItem.put("lastClaim", lastClaim);
        newItem.put("dailyLastClaim", dailyLastClaim);
        newItem.put("dailyBonus", dailyBonus);
        newItem.put("claims", claims);
        newItem.put("totalPaid", totalPaid);
        jsonArray.put(newItem);


        try {
            File fileS = new File("addresses.json");
            Files.asCharSink(fileS, Charsets.UTF_8).write(jsonArray.toString());
        } catch (IOException e) {
            e.printStackTrace();
            res = "failed write file";
        }

        return res;
    }

    String getTop(Request request, Response response) {
        String result = "[]";
        String command = "transfer ";
        double total = 0;
        JSONArray jsonArrayTop = new JSONArray();
        try {
            File file1 = new File("addresses.json");
            result = Files.asCharSource(file1, Charsets.UTF_8).read();
        } catch (Exception e) {
            e.printStackTrace();
        }
        double balance = 0;
        try {
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject addr = jsonArray.getJSONObject(i);
                balance = addr.getDouble("balance");
                if (balance >= 0.1) {
                    jsonArrayTop.put(addr);
                    command = command + addr.getString("address") + " " + round(balance, 5) + " ";
                    total = total + balance;
                }
            }
        } catch (Exception e) {
        }
        jsonArrayTop.put(command);
        jsonArrayTop.put(total);
        return jsonArrayTop.toString();
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
