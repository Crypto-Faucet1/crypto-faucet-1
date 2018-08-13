import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class ClaimHandler {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss");
    public static Connection conn;
    private JSONArray jsonArrayIpSumo = new JSONArray();
    private JSONArray jsonArrayIpRyo = new JSONArray();
    private JSONArray jsonArrayIpIntense = new JSONArray();

    ClaimHandler() {
        try {
            conn = DriverManager.getConnection("jdbc:mariadb://192.168.2.24:3306/faucet", "faucet", "Tsav#y2fH*7hfZy6UgTT");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Timer updateTimer2 = new Timer();
        updateTimer2.scheduleAtFixedRate(new UpdateTask2(), 0, 6000);
    }

    private class UpdateTask2 extends TimerTask {
        @Override
        public void run() {
            try {
                if (!conn.isValid(2500)) {
                    conn.close();
                    conn = DriverManager.getConnection("jdbc:mariadb://192.168.2.24:3306/faucet", "faucet", "Tsav#y2fH*7hfZy6UgTT");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    String claimSumo(Request request, Response response) {
        String captcha = request.queryParams("captcha");
        String address = request.queryParams("address").replaceAll("\\s+", "");
        String ip = request.queryParams("ip");
        String currency = request.queryParams("currency");
        return claim(address, captcha, ip, currency) + "";
    }

    static String getTable(String currency){
        String table = "";
        if (currency.equals("sumo")) {
            table = "sumo";
        } else if (currency.equals("ryo")) {
            table = "ryo";
        } else if (currency.equals("intense")){
            table = "intense";
        }
        return table;
    }

    private boolean claim(String address, String captcha, String ip, String currency) {
        String table = getTable(currency);
        boolean comp = true;

        if (checkCaptcha(captcha, ip, currency) && !address.equals("")) {
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
            boolean addressExists = false;

            String queryCheck = "SELECT * from " + table + " WHERE address = ?";
            try {
                PreparedStatement ps = conn.prepareStatement(queryCheck);
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
                    addressExists = true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            Calendar cal = Calendar.getInstance();
            int day = cal.get(Calendar.DAY_OF_MONTH);

            double claimAmount = 0;
            boolean payout = false;
            Date date = new Date();
            long dif = date.getTime() - lastClaim;
            if (dif >= 300000) {
                if (dailyLastClaim == 0) {
                    dailyLastClaim = date.getTime();
                }
                long dif2 = date.getTime() - dailyLastClaim;
                if (dif2 >= 43200000 && dif2 <= 172800000 && lastBonusDay != day) {
                    if (dailyBonus < 0.99) {
                        dailyBonus = dailyBonus + 0.01;
                        lastBonusDay = day;
                        dailyLastClaim = date.getTime();
                    }
                } else if (dif2 >= 172800000 || dif2 < -1) {
                    dailyBonus = 0;
                    dailyLastClaim = date.getTime();
                }

                if (day == lastClaimDay) {
                    claimsToday = claimsToday + 1;
                    lastClaimDay = day;
                } else {
                    claimsToday = 1;
                    lastClaimDay = day;
                }

                double keerDing = 1 + dailyBonus;
                if(balance > WithdrawHandler.getWithdrawLimit(currency)){
                    payout = true;
                }
                claimAmount = Prices.getClaimAmount(claimsToday, currency);

                balance = balance + claimAmount * keerDing;
                claims = claims + 1;
                lastClaim = date.getTime();
                if (!payout && balance > WithdrawHandler.getWithdrawLimit(currency)){
                    payoutDayReached = date.getTime();
                }
                if (addressExists) {
                    java.util.Date dt = new java.util.Date(lastClaim);
                    java.util.Date dt2 = new java.util.Date(dailyLastClaim);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String currentTime = sdf.format(dt);
                    String currentTime2 = sdf.format(dt2);

                    String insert = "UPDATE " + table + " SET balance='" + balance + "', lastClaim='" + currentTime
                            + "', dailyLastClaim='" + currentTime2 + "', dailyBonus='" + dailyBonus + "', claims='" + claims + "', totalPaid='" + totalPaid +
                            "', lastClaimDay='" + lastClaimDay + "', claimsToday='" + claimsToday + "', lastBonusDay='" + lastBonusDay + "', payoutDayReached='"
                            + Payments.getTimeString(payoutDayReached) +"', ip='" + ip + "' WHERE address=?";
                    try {
                        PreparedStatement ps = conn.prepareStatement(insert);
                        ps.setString(1, address);
                        ps.executeUpdate();
                        ps.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    java.util.Date dt = new java.util.Date(lastClaim);
                    java.util.Date dt2 = new java.util.Date(dailyLastClaim);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String currentTime = sdf.format(dt);
                    String currentTime2 = sdf.format(dt2);

                    String insert = "INSERT INTO " + table + " VALUES (?, '" + balance + "', '" + currentTime
                            + "', '" + currentTime2 + "', '" + dailyBonus + "', '" + claims + "', '" + totalPaid + "', '" + lastClaimDay + "', '" + claimsToday
                            + "', '" + lastBonusDay + "', '" + Payments.getTimeString(payoutDayReached) +"', '" + ip + "')";
                    try {
                        PreparedStatement ps = conn.prepareStatement(insert);
                        ps.setString(1, address);
                        ps.executeQuery();
                        ps.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(getTime() + "Faucet claim successful balance: " + WithdrawHandler.round(balance, 5) + currency + " Claimed:" + claimAmount + " Ip: " + ip + " Address: " + address);
            } else {
                comp = false;
            }
        } else {
            comp = false;
        }

        return comp;
    }

    private boolean checkCaptcha(String captcha, String ip, String currency) {
        boolean comp = true;
        ///recaptcha
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.google.com/recaptcha/api/siteverify").newBuilder();
        urlBuilder.addQueryParameter("secret", "6LfWYTwUAAAAAFcjH1RNrVV8WkW8rSy3nwzPSVZn");
        urlBuilder.addQueryParameter("response", captcha);
        urlBuilder.addQueryParameter("remoteip", ip);
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONObject jsonObject = new JSONObject(response.body().string());
            comp = jsonObject.getBoolean("success");
        } catch (Exception e) {
            e.printStackTrace();
        }

        ///Double ip check
        if (comp) {
            JSONArray jsonArrayIp = new JSONArray();

            if (currency.equals("sumo")) {
                jsonArrayIp = jsonArrayIpSumo;
            } else if (currency.equals("ryo")) {
                jsonArrayIp = jsonArrayIpRyo;
            } else if (currency.equals("intense")){
                jsonArrayIp = jsonArrayIpIntense;
            }

            int num = -1;
            long lastClaim = 0;
            for (int i = 0; i < jsonArrayIp.length(); i++) {
                JSONObject addr = jsonArrayIp.getJSONObject(i);
                try {
                    if (addr.getString("ip").equals(ip)) {
                        lastClaim = addr.getLong("lastClaim");
                        num = i;
                    }
                } catch (Exception ignored) {
                }
            }
            Date date = new Date();
            long dif = date.getTime() - lastClaim;
            if (dif >= 300000) {
                if (num != -1) {
                    jsonArrayIp.remove(num);
                }
                lastClaim = date.getTime();
                JSONObject newItem = new JSONObject();
                newItem.put("ip", ip);
                newItem.put("lastClaim", lastClaim);
                jsonArrayIp.put(newItem);
            } else {
                System.out.println(getTime() + "Ip double claim " + ip);
                comp = false;
            }
            if (currency.equals("sumo")) {
                jsonArrayIpSumo = jsonArrayIp;
            } else if (currency.equals("ryo")) {
                jsonArrayIpRyo = jsonArrayIp;
            } else if (currency.equals("intense")){
                 jsonArrayIpIntense = jsonArrayIp;
            }
        }
        return comp;
    }

    private static String getTime() {
        return SIMPLE_DATE_FORMAT.format(new Date()) + " - ";
    }


}
