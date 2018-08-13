import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GetBalance {

    String getAddressInfo(Request request, Response response){
        String address = request.queryParams("address");
        String currency = request.queryParams("currency");

        double balance = 0.0;
        double totalPaid = 0.0;
        long lastClaim = 0;
        double dailyBonus = 0;
        long dailyLastClaim = 0;
        int claims = 0;
        int claimsToday = 0;
        int lastClaimDay = 0;
        int lastBonusDay = 0;
        long payoutDayReached = 0;
        String table = ClaimHandler.getTable(currency);
        String queryCheck = "SELECT * from " + table +" WHERE address = ?";
        try {
            PreparedStatement ps = ClaimHandler.conn.prepareStatement(queryCheck);
            ps.setString(1, address);
            final ResultSet resultSet = ps.executeQuery();
            ps.close();

            if (resultSet.next()) {
                balance = resultSet.getDouble(2);
                lastClaim = resultSet.getTimestamp(3).getTime();
                dailyLastClaim = resultSet.getTimestamp(4).getTime();
                dailyBonus = resultSet.getDouble(5);
                claims = resultSet.getInt(6);
                totalPaid = resultSet.getDouble(7);
                lastClaimDay = resultSet.getInt(8);
                claimsToday = resultSet.getInt(9);
                lastBonusDay = resultSet.getInt(10);
                payoutDayReached = resultSet.getTimestamp(11).getTime();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JSONObject newItem = new JSONObject();
        newItem.put("address", address);
        newItem.put("balance", balance);
        newItem.put("lastClaim", lastClaim);
        newItem.put("dailyLastClaim", dailyLastClaim);
        newItem.put("dailyBonus", dailyBonus);
        newItem.put("claims", claims);
        newItem.put("totalPaid", totalPaid);
        newItem.put("lastClaimDay", lastClaimDay);
        newItem.put("claimsToday", claimsToday);
        newItem.put("lastBonusDay", lastBonusDay);
        newItem.put("payoutDayReached", payoutDayReached);
        return newItem.toString();
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
