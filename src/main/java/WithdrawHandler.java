import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class WithdrawHandler {
    String removeBalance(Request request, Response response) {
        String res = "done";
        String address = request.queryParams("address");
        double balanceRemove = Double.parseDouble(request.queryParams("balanceRemove"));
        String currency = request.queryParams("currency");
        if(!Payments.removeBalance(currency, address, balanceRemove)){
            res = "Failed";
        }
        return res;
    }

    String getTop(Request request, Response response) {
        String command = "transfer ";
        double total = 0;
        JSONArray jsonArrayTop = new JSONArray();
        String currency = request.queryParams("currency");
        String table = ClaimHandler.getTable(currency);
        try {
            Statement stmt = ClaimHandler.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from " + table + " WHERE balance > " + getWithdrawLimit(currency));
            while (rs.next()) {
                JSONObject item = new JSONObject();
                double balance = rs.getDouble("balance");
                item.put("address", rs.getString("address"));
                item.put("balance", balance);
                item.put("totalPaid", rs.getDouble("totalPaid"));
                item.put("claimsToday", rs.getDouble("claimsToday"));
                item.put("claims", rs.getDouble("claims"));
                item.put("payoutDayReached", Payments.getTimeString(rs.getTimestamp("payoutDayReached").getTime()));
                jsonArrayTop.put(item);
                command = command + rs.getString("address") + " " + round(balance, 5) + " ";
                total = total + balance;
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

    public static double getWithdrawLimit(String currency){
        double limit = 0;
        if (currency.equals("sumo")) {
            limit = 0.1;
        } else if (currency.equals("ryo")) {
            limit = 0.1;
        } else if (currency.equals("intense")){
            limit = 10;
        }
        return limit;
    }
}
