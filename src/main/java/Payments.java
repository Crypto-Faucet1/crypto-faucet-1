import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Payments {
    private static HttpURLConnection con;

    public Payments() {
        Timer updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new update(), 5000, 43200000);
    }//1,000,000,000

    public class update extends TimerTask {
        @Override
        public void run() {
            Thread t = new Thread(() -> {
                try {
                    System.out.println("Processing Sumo payments");
                    processPayments("sumo");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    System.out.println("Processing Ryo payments");
                    processPayments("ryo");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    System.out.println("Processing masari payments");
                    processPayments("masari");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    System.out.println("Processing intense payments");
                    processPayments("intense");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    System.out.println("Processing loki payments");
                    processPayments("loki");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
        }
    }

    static String startProcessPayment(Request request, Response response) {
        if (request.queryParams("currency").equals("sumo")) {
            try {
                System.out.println("Processing Sumo payments");
                processPayments("sumo");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (request.queryParams("currency").equals("ryo")) {
            try {
                System.out.println("Processing Ryo payments");
                processPayments("ryo");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (request.queryParams("currency").equals("intense")) {
            try {
                System.out.println("Processing intense payments");
                processPayments("intense");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (request.queryParams("currency").equals("masari")) {
            try {
                System.out.println("Processing masari payments");
                processPayments("masari");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (request.queryParams("currency").equals("loki")) {
            try {
                System.out.println("Processing loki payments");
                processPayments("loki");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    static void processPayments(String currency) {
        String table = ClaimHandler.getTable(currency);
        JSONArray intAddress = new JSONArray();
        JSONArray jsonArray = new JSONArray();
        long lastTime = 0;

        long normalTime = 345600000;
        if (currency.equals("intense") || currency.equals("masari") || currency.equals("sumo")) {
            normalTime = 0;
        }
        try {
            Statement stmt = ClaimHandler.conn.createStatement();
            ResultSet rs;
            rs = stmt.executeQuery("SELECT * from " + table + " WHERE balance > " + WithdrawHandler.getWithdrawLimit(currency) + " AND payment = 1");

            while (rs.next()) {
                JSONObject item = new JSONObject();
                double balance = rs.getDouble("balance");
                item.put("address", rs.getString("address"));
                long payoutDayReached = 0;
                try {
                    payoutDayReached = rs.getTimestamp("payoutDayReached").getTime();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                boolean masariIntAdr = false;
                if (currency.equals("masari") || currency.equals("loki")) {
                    masariIntAdr = intAdrPostRequest(rs.getString("address"), currency);
                }
                if (currency.equals("loki")) {
                    balance = balance - 0.04;
                }
                if (rs.getString("address").substring(0, 4).equals("Sumi") || rs.getString("address").substring(0, 4).equals("RYoN") ||
                        rs.getString("address").substring(0, 2).equals("Na") || masariIntAdr) {
                    if (new Date().getTime() - payoutDayReached > normalTime) {
                        double ma = balance;
                        if (currency.equals("masari")) {
                            ma = balance * 1000;
                        } else if (currency.equals("intense") || currency.equals("lethean")) {
                            ma = balance / 10;
                        }
                        double ase = WithdrawHandler.round(ma, 2) * 1000000000;
                        item.put("amount", Math.round(ase));
                        intAddress.put(item);
                    }
                } else {
                    if (lastTime == 0) {
                        lastTime = payoutDayReached;
                    }
                    if (payoutDayReached < lastTime) {
                        lastTime = payoutDayReached;
                    }
                    double ma = balance;
                    if (currency.equals("masari")) {
                        ma = balance * 1000;
                    } else if (currency.equals("intense")) {
                        ma = balance / 10;
                    }
                    double ase = WithdrawHandler.round(ma, 2) * 1000000000;
                    item.put("amount", Math.round(ase));
                    jsonArray.put(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        long diff = new Date().getTime() - lastTime;

        //payments for normal/sub addresses
        if (jsonArray.length() >= 3 || diff > normalTime && jsonArray.length() > 0) {
            makePayment(jsonArray, currency);
        } else { //Payments for integrated addresses.
            if (intAddress.length() > 0) {
                JSONArray jsonArray1 = new JSONArray();
                JSONObject jsonObject = intAddress.getJSONObject(0);
                jsonArray1.put(jsonObject);
                makePayment(jsonArray1, currency);
            }
        }
    }

    static String requestPayment(Request request, Response response) {
        String res = "";
        String address = request.queryParams("address");
        String currency = request.queryParams("currency");
        String table = ClaimHandler.getTable(currency);
        double balance = 0;
        boolean paymentPending = false;
        String queryCheck = "SELECT * from " + table + " WHERE address = ?";
        try {
            PreparedStatement ps = ClaimHandler.conn.prepareStatement(queryCheck);
            ps.setString(1, address);
            final ResultSet resultSet = ps.executeQuery();
            ps.close();

            if (resultSet.next()) {
                balance = resultSet.getDouble(2);
                paymentPending = resultSet.getBoolean("payment");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (balance > WithdrawHandler.getWithdrawLimit(currency) && !paymentPending) {
            Date date = new Date();
            String insert = "UPDATE " + table + " SET payoutDayReached='"
                    + Payments.getTimeString(date.getTime()) + "', payment=1 WHERE address=?";
            try {
                PreparedStatement ps = ClaimHandler.conn.prepareStatement(insert);
                ps.setString(1, address);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (currency.equals("sumo") || currency.equals("masari") || currency.equals("intense")) {
                processPayments(currency);
            }
        }
        return res;
    }

    static String getPayments(Request request, Response response) {
        String currency = request.queryParams("currency");
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
        if (currency.equals("loki")) {
            balanceRemove = balanceRemove + 0.04;
        }
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
            PreparedStatement ps2 = ClaimHandler.conn.prepareStatement("UPDATE " + table + " SET balance=" + balance + ", totalPaid=" + totalPaid + ", payment=0 WHERE address = ?");
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
        String response = "";
        try {
            response = sendPostRequest(recipients, currency);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            JSONObject jsonObject11 = new JSONObject(response).getJSONObject("result");
            if (jsonObject11.getJSONObject("error").getInt("code") == -38) {
                response = sendPostRequest(recipients, currency);
            }
        } catch (Exception e) {
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
                double am = jsonObject1.getDouble("amount");
                if (currency.equals("masari")) {
                    am = am / 1000;
                } else if (currency.equals("intense")) {
                    am = am * 10;
                }
                removeBalance(currency, jsonObject1.getString("address"), am / 1000000000);
                amount = amount + am / 1000000000;
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
                    + "', '" + item.getTxKey() + "', '" + item.getAmount() + "', '" + item.getReceipents().toString() + "', '"
                    + item.getCurrency() + "', '" + getAmountEur(item.getAmount(), item.getCurrency()) + "')");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static Double getAmountEur(Double amount, String currency) {
        Double amountEur = 0.0;
        if (currency.equals("sumo")) {
            amountEur = amount * Prices.sumoRate;
        } else if (currency.equals("ryo")) {
            amountEur = amount * Prices.ryoRate;
        } else if (currency.equals("intense") || currency.equals("lethean")) {
            amountEur = amount * Prices.intenseRate;
        } else if (currency.equals("masari")) {
            amountEur = amount * Prices.masariRate;
        } else if (currency.equals("loki")) {
            amountEur = amount * Prices.lokiRate;
        }
        return amountEur;
    }

    static String getTimeString(long date) {
        java.util.Date dt = new Date(date);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(dt);
        return time;
    }

    static String sendPostRequest(JSONArray recipients, String currency) throws IOException {
        String url = ValidateAddress.getRpcAddress(currency);
        String cont = "";

        String asydfuh = "";
        if (currency.equals("intense")) {
            asydfuh = ",\"mixin\":10";
        }
        String urlParameters = "{\"jsonrpc\":\"2.0\",\"id\":\"0\",\"method\":\"transfer\",\"params\":{\"destinations\":" +
                recipients
                + ",\"account_index\":0,\"subaddr_indices\":[0],\"priority\":0,\"get_tx_key\":true" + asydfuh + "}}";
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

        try {

            URL myurl = new URL(url);
            con = (HttpURLConnection) myurl.openConnection();

            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Java client");
            con.setRequestProperty("Content-Type", "application/json");
            String encoding = Base64.getEncoder().encodeToString(("koen:UZuvPkNm&Ex2U0132anpZJSejy").getBytes());
            con.setRequestProperty("Authorization", "Basic " + encoding);

            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postData);
            }

            StringBuilder content;

            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }
            cont = content.toString();
        } finally {

            con.disconnect();
        }
        return cont;
    }


    static boolean intAdrPostRequest(String address, String currency) {
        String cont = "";
        boolean isIntAddress = false;
        String url = ValidateAddress.getRpcAddress(currency);
        String urlParameters = "{\"jsonrpc\":\"2.0\",\"id\":\"0\",\"method\":\"split_integrated_address\",\"params\"" +
                ":{\"integrated_address\": \"" + address + "\"}}' " +
                "-H 'Content-Type: application/json";
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

        try {
            try {

                URL myurl = new URL(url);
                con = (HttpURLConnection) myurl.openConnection();

                con.setDoOutput(true);
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", "Java client");
                con.setRequestProperty("Content-Type", "application/json");
                String encoding = Base64.getEncoder().encodeToString(("koen:UZuvPkNm&Ex2U0132anpZJSejy").getBytes());
                con.setRequestProperty("Authorization", "Basic " + encoding);
                try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                    wr.write(postData);
                }
                StringBuilder content;

                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()))) {
                    String line;
                    content = new StringBuilder();

                    while ((line = in.readLine()) != null) {
                        content.append(line);
                        content.append(System.lineSeparator());
                    }
                }
                cont = content.toString();
                JSONObject jsonObject = new JSONObject(cont);
                if (jsonObject.has("result")) {
                    System.out.println("is intaddress");
                    isIntAddress = true;
                }
            } finally {

                con.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isIntAddress;
    }
}