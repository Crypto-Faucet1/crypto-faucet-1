import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class WithdrawHandler {
    String removeBalance(Request request, Response response) {
        String res = "done";
        String address = request.queryParams("address");
        double balanceRemove = Double.parseDouble(request.queryParams("balanceRemove"));
        String currency = request.queryParams("currency");
        String table = "";
        if (currency.equals("sumo")) {
            table = "sumo";
        } else if (currency.equals("ryo")) {
            table = "ryo";
        }
        try {
            Statement stmt = ClaimHandler.conn.createStatement();
            PreparedStatement ps = ClaimHandler.conn.prepareStatement("SELECT * from " + table + " WHERE address = ?");
            ps.setString(1, address);
            ResultSet rs = ps.executeQuery();
            double balance = 0;
            double totalPaid = 0;
            if (rs.next()) {
                balance = rs.getDouble("balance");
                totalPaid = rs.getDouble("totalPaid");
            }
            balance = balance - balanceRemove;
            totalPaid = totalPaid + balanceRemove;
            PreparedStatement ps2 = ClaimHandler.conn.prepareStatement("UPDATE " + table + " SET balance=" + balance + ", totalPaid=" + totalPaid + " WHERE address = ?");
            ps2.setString(1, address);
            ps2.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            res = "SQLException Removing balance failed";
        }

        return res;
    }

    String getTop(Request request, Response response) {
        String command = "transfer ";
        double total = 0;
        JSONArray jsonArrayTop = new JSONArray();

        try {
            Statement stmt = ClaimHandler.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from Addresses WHERE balance > 0.1");
            while (rs.next()) {
                JSONObject item = new JSONObject();
                double balance = rs.getDouble("balance");
                item.put("address", rs.getString("address"));
                item.put("balance", balance);
                item.put("totalPaid", rs.getDouble("totalPaid"));
                item.put("claimsToday", rs.getDouble("claimsToday"));
                item.put("claims", rs.getDouble("claims"));
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
}
