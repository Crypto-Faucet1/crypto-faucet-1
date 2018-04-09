import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.File;
import java.io.IOException;

public class SetBonusHandler {
    String setBonus(Request request, Response response) {
        String res = "done";
        String address = request.queryParams("address");
        double bonus = Double.parseDouble(request.queryParams("bonus"));

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
        } catch (Exception ignored) {
        }
        JSONArray jsonArray = new JSONArray(result);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject addr = jsonArray.getJSONObject(i);
            try {
                if (addr.getString("address").equals(address)) {
                    System.out.println("Bonus: " + addr.getDouble("dailyBonus") + " Address found " + address);
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
        JSONObject newItem = new JSONObject();
        newItem.put("address", address);
        newItem.put("balance", balance);
        newItem.put("lastClaim", lastClaim);
        newItem.put("dailyLastClaim", dailyLastClaim);
        newItem.put("dailyBonus", bonus);
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
}
