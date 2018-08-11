import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static org.toilelibre.libe.curl.Curl.curl;

public class Payments {
    public Payments() {
        Timer updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new update(), 5000, 14400000);
    }//1,000,000,000

    public class update extends TimerTask {
        @Override
        public void run() {
            proccessPayments("sumo");
        }
    }

    static void proccessPayments(String currency) {
        String table = ClaimHandler.getTable(currency);
        JSONArray intAddress = new JSONArray();
        JSONArray jsonArray = new JSONArray();
        try {
            Statement stmt = ClaimHandler.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from " + table + " WHERE balance > " + WithdrawHandler.getWithdrawLimit(currency));
            while (rs.next()) {
                JSONObject item = new JSONObject();
                double balance = rs.getDouble("balance");
                if (rs.getString("address").substring(0, 4).equals("Sumi")) {
                    balance = balance - 0.015;
                }
                item.put("amount", Integer.parseInt(String.format("%.0f", WithdrawHandler.round(balance, 5) * 1000000000)));
                item.put("address", rs.getString("address"));
                if (rs.getString("address").substring(0, 4).equals("Sumi")) {
                    intAddress.put(item);
                } else {
                    jsonArray.put(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (jsonArray.length() >= 6) {
            makePayment(jsonArray, currency);
        } else {
            JSONArray jsonArray1 = new JSONArray();
            JSONObject jsonObject = intAddress.getJSONObject(0);
            jsonArray1.put(jsonObject);
            makePayment(jsonArray1, currency);
        }
    }

    static String getPayments(Request request, Response response) {
        String currency = "sumo";
        JSONArray jsonArray = new JSONArray();
        try {
            Statement stmt = ClaimHandler.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM payments WHERE currency='" + currency + "' ORDER BY date DESC");
            while (rs.next()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("date", getTimeString(rs.getTimestamp("date").getTime()));
                jsonObject.put("txHash", rs.getString("txHash"));
                jsonArray.put(jsonObject);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jsonArray.toString();
    }

    static boolean removeBalance(String currency, String address, Double balanceRemove) {
        System.out.println("Removing balance: " + balanceRemove);
        boolean res = true;
        String table = ClaimHandler.getTable(currency);
        try {
            PreparedStatement ps = ClaimHandler.conn.prepareStatement("SELECT * from " + table + " WHERE address = ?");
            ps.setString(1, address);
            ResultSet rs = ps.executeQuery();
            double balance = 0;
            double totalPaid = 0;
            if (rs.next()) {
                balance = rs.getDouble("balance");
                totalPaid = rs.getDouble("totalPaid");
            }
            rs.close();
            balance = balance - balanceRemove;
            totalPaid = totalPaid + balanceRemove;
            PreparedStatement ps2 = ClaimHandler.conn.prepareStatement("UPDATE " + table + " SET balance=" + balance + ", totalPaid=" + totalPaid + " WHERE address = ?");
            ps2.setString(1, address);
            ps2.executeUpdate();
            ps.close();
            ps2.close();
        } catch (SQLException e) {
            e.printStackTrace();
            res = false;
        }
        return res;
    }

    static boolean makePayment(JSONArray recipients, String currency) {
        System.out.println("Paying: " + recipients.toString());
        String command = "-X POST http://127.0.0.1:8888/json_rpc -d '{\"jsonrpc\":\"2.0\",\"id\":\"0\",\"method\":\"transfer\",\"params\":{\"destinations\":" +
                recipients
                + ",\"get_tx_key\": true}}' -H 'Content-Type: application/json'";
        System.out.println(command);
        HttpResponse res = curl(command);
        String response = "";
        try {
            response = IOUtils.toString(res.getEntity().getContent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(response);
        JSONObject jsonObject = new JSONObject(response).getJSONObject("result");
        if (jsonObject.isNull("tx_hash")) {
            System.out.println("Transaction failed");
        } else {
            String txHash = jsonObject.getString("tx_hash");
            String txKey = jsonObject.getString("tx_key");
            double amount = 0;
            for (int i = 0; i < recipients.length(); i++) {
                JSONObject jsonObject1 = recipients.getJSONObject(i);
                removeBalance(currency, jsonObject1.getString("address"), jsonObject1.getDouble("amount") / 1000000000);
                amount = amount + jsonObject1.getDouble("amount") / 1000000000;
            }
            PaymentItem item = new PaymentItem(txHash, new Date().getTime(), txKey, amount, recipients, currency);
            addPayment(item);
        }
        return false;
    }

    private String getLastestTxHash(String currency) {
        JSONObject jsonObject = new JSONObject();
        PaymentItem item = getLatestPayment(currency);
        jsonObject.put("txHash", item.getTxHash());
        jsonObject.put("date", getTimeString(item.getDate()));
        return jsonObject.toString();
    }

    private PaymentItem getLatestPayment(String currency) {
        PaymentItem lastestPayment = null;
        try {
            Statement stmt = ClaimHandler.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT TOP 1 * FROM payments WHERE currency='" + currency + "'ORDER BY date DESC");
            while (rs.next()) {
                lastestPayment = new PaymentItem(rs.getString(1), rs.getTimestamp(2).getTime(), rs.getString(3),
                        rs.getDouble(4), new JSONArray(rs.getString(5)), rs.getString(6));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lastestPayment;
    }

    static void addPayment(PaymentItem item) {
        try {
            System.out.println("Adding payment to mysql");
            Statement stmt = ClaimHandler.conn.createStatement();
            stmt.executeUpdate("INSERT INTO payments VALUES ('" + item.getTxHash() + "', '" + getTimeString(item.getDate())
                    + "', '" + item.getTxKey() + "', '" + item.getAmount() + "', '" + item.getReceipents().toString() + "', '" + item.getCurrency() + "')");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    static String getTimeString(long date) {
        java.util.Date dt = new Date(date);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(dt);
        return time;
    }
}
