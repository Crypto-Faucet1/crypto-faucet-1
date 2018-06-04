import org.json.JSONArray;
import org.json.JSONObject;
import spark.Spark;

import java.io.File;
import java.sql.*;

import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.post;

public class x {
    public static void main(String args[]) {
        Spark.port(9899);
        //dataMigration();
        ClaimHandler claim = new ClaimHandler();
        GetBalance getBalance = new GetBalance();
        WithdrawHandler withdrawHandler = new WithdrawHandler();
        RateHandler rateHandler = new RateHandler();
        SetBonusHandler setBonusHandler = new SetBonusHandler();
        new Prices();

        post("/claim", claim::claimSumo);
        get("/addressInfo", getBalance::getAddressInfo);
        path("/withdraw", () -> {
            get("/top", withdrawHandler::getTop);
            post("/remove", withdrawHandler::removeBalance);
        });
        get("/rate", RateHandler::getClaimRate);
        post("/setbonus", setBonusHandler::setBonus);
    }

    private static void dataMigration() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mariadb://192.168.2.24:3306/faucet?autoReconnect=true", "faucet", "Tsav#y2fH*7hfZy6UgTT");
            Statement stmt = conn.createStatement();
            String result = "[]";
            String address = "";
            double balance = 0.0;
            long lastClaim = 0;
            double dailyBonus = 0;
            long dailyLastClaim = 0;
            int claimsToday = 0;
            int claims = 0;
            int lastClaimDay = 0;
            int lastBonusDay = 0;
            int num = -1;
            double totalPaid = 0.0;
            try {
                //File file1 = new File("addresses.json");
                //result = Files.asCharSource(file1, Charsets.UTF_8).read();
            } catch (Exception ignored) {
            }
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject addr = jsonArray.getJSONObject(i);
                try {
                    address = addr.getString("address");
                    balance = addr.getDouble("balance");
                    lastClaim = addr.getLong("lastClaim");
                    dailyLastClaim = addr.getLong("dailyLastClaim");
                    dailyBonus = addr.getDouble("dailyBonus");
                    claims = addr.getInt("claims");
                    try {
                        totalPaid = addr.getDouble("totalPaid");
                    } catch (Exception ignored) {
                    }
                    try {
                        claimsToday = addr.getInt("claimsToday");
                        lastClaimDay = addr.getInt("lastClaimDay");
                    } catch (Exception ignored) {
                    }
                    try {
                        lastBonusDay = addr.getInt("lastBonusDay");
                    } catch (Exception ignored) {

                    }

                    java.util.Date dt = new java.util.Date(lastClaim);
                    java.util.Date dt2 = new java.util.Date(dailyLastClaim);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String currentTime = sdf.format(dt);
                    String currentTime2 = sdf.format(dt2);

                    stmt.executeQuery("INSERT INTO Addresses VALUES ('" + address + "', '" + balance + "', '" + currentTime
                            + "', '" + currentTime2 + "', '" + dailyBonus + "', '" + claims + "', '" + totalPaid + "', '" + lastClaimDay + "', '" + claimsToday + "', '" + lastBonusDay + "')");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
