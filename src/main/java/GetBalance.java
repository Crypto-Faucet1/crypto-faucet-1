import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class GetBalance {
    private String res;

    String getAddressInfo(Request request, Response response){
        String address = request.queryParams("address");
        String result = "[]";
        try {
            File file1 = new File("addresses.json");
            result = Files.asCharSource(file1, Charsets.UTF_8).read();
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject addr = jsonArray.getJSONObject(i);
                try {
                    if (addr.getString("address").equals(address)) {
                        jsonObject = addr;
                    }
                } catch (Exception e) {

                }
            }
        } catch (Exception e){}
        return jsonObject.toString();
    }

    String getBalance(Request request, Response response){
        String address = request.queryParams("address");
        String result = "[]";
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
                try {
                    if (addr.getString("address").equals(address)) {
                        balance = addr.getDouble("balance");
                    }
                } catch (Exception e) {

                }
            }
        } catch (Exception e){}
        return round(balance, 5) + "";
    }

    String getPaid(Request request, Response response){
        String address = request.queryParams("address");
        String result = "[]";
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
                try {
                    if (addr.getString("address").equals(address)) {
                        balance = addr.getDouble("totalPaid");
                    }
                } catch (Exception e) {

                }
            }
        } catch (Exception e){}
        return balance + "";
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
