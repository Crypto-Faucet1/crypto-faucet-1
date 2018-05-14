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
    static Double sumoRate = 0.00013826;
    static Double bitcoinRate = 0.0;
    public static Connection conn;
    private JSONArray jsonArrayIp = new JSONArray();

    ClaimHandler() {
        try {
            conn = DriverManager.getConnection("jdbc:mariadb://192.168.2.24:3306/faucet?autoReconnect=true", "faucet", "Tsav#y2fH*7hfZy6UgTT");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Timer updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new UpdateTask(), 0, 1800000);
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            updateRate();
            updateBitcoinRate();
        }
    }

    String claimSumo(Request request, Response response) {
        String captcha = request.queryParams("captcha");
        String address = request.queryParams("address").replaceAll("\\s+", "");
        String ip = request.queryParams("ip");
        return claim(address, captcha, ip) + "";
    }

    private boolean claim(String address, String captcha, String ip) {
        boolean comp = true;

        if (checkCaptcha(captcha, ip) && !address.equals("")) {
            double balance = 0.0;
            double totalPaid = 0.0;
            long lastClaim = 0;
            double dailyBonus = 0;
            long dailyLastClaim = 0;
            int claims = 0;
            int claimsToday = 0;
            int lastClaimDay = 0;
            int lastBonusDay = 0;
            boolean addressExists = false;
            String queryCheck = "SELECT * from Addresses WHERE address = ?";
            try {
                PreparedStatement ps = conn.prepareStatement(queryCheck);
                ps.setString(1, address);
                final ResultSet resultSet = ps.executeQuery();

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
                    addressExists = true;
                } else {
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            Calendar cal = Calendar.getInstance();
            int day = cal.get(Calendar.DAY_OF_MONTH);

            double claimAmount = 0;
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
                claimAmount = getClaimAmount(claimsToday);

                balance = balance + claimAmount * keerDing;
                claims = claims + 1;
                lastClaim = date.getTime();

                if (addressExists) {
                    java.util.Date dt = new java.util.Date(lastClaim);
                    java.util.Date dt2 = new java.util.Date(dailyLastClaim);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String currentTime = sdf.format(dt);
                    String currentTime2 = sdf.format(dt2);

                    String insert = "UPDATE Addresses SET balance='" + balance + "', lastClaim='" + currentTime
                            + "', dailyLastClaim='" + currentTime2 + "', dailyBonus='" + dailyBonus + "', claims='" + claims + "', totalPaid='" + totalPaid +
                            "', lastClaimDay='" + lastClaimDay + "', claimsToday='" + claimsToday + "', lastBonusDay='" + lastBonusDay + "' WHERE address=?";
                    try {
                        PreparedStatement ps = conn.prepareStatement(insert);
                        ps.setString(1, address);
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    java.util.Date dt = new java.util.Date(lastClaim);
                    java.util.Date dt2 = new java.util.Date(dailyLastClaim);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String currentTime = sdf.format(dt);
                    String currentTime2 = sdf.format(dt2);

                    String insert = "INSERT INTO Addresses VALUES (?, '" + balance + "', '" + currentTime
                            + "', '" + currentTime2 + "', '" + dailyBonus + "', '" + claims + "', '" + totalPaid + "', '" + lastClaimDay + "', '" + claimsToday + "', '" + lastBonusDay + "')";
                    try {
                        PreparedStatement ps = conn.prepareStatement(insert);
                        ps.setString(1, address);
                        ps.executeQuery();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(getTime() + "Faucet claim successful balance: " + WithdrawHandler.round(balance, 5) + " Claimed:" + claimAmount + " Ip: " + ip + " Address: " + address);
            } else {
                comp = false;
            }
        } else {
            comp = false;
        }

        return comp;
    }

    private boolean checkCaptcha(String captcha, String ip) {
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
        if (!comp) {
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
            }
        }
        return comp;
    }

    private double getClaimAmount(int claimsToday) {
        Random rand = new Random();
        int value = rand.nextInt(100) + 1;
        double amount = 0.00000020 / sumoRate;
        if (value <= 50) {
            amount = 0.00000006 / sumoRate;
        } else if (value <= 90) {
            amount = 0.00000015 / sumoRate;
        } else if (value <= 94) {
            amount = 0.00000280 / sumoRate;
        } else if (value <= 97) {
            amount = 0.00000638 / sumoRate;
        }
        if (bitcoinRate <= 11000) {
            amount = amount * 0.7;
        } else if (bitcoinRate <= 13000) {
            amount = amount * 0.81;
        } else if (bitcoinRate <= 14000) {
            amount = amount * 0.85;
        } else if (bitcoinRate <= 15000) {
            amount = amount * 0.9;
        } else if (bitcoinRate <= 16000) {

        } else if (bitcoinRate <= 20000) {
            amount = amount * 1.05;
        }

        if (claimsToday >= 10 && claimsToday <= 15) {
            amount = amount * 0.80;
        } else if (claimsToday >= 16 && claimsToday <= 20) {
            amount = amount * 0.70;
        } else if (claimsToday >= 21 && claimsToday <= 26) {
            amount = amount * 0.50;
        } else if (claimsToday >= 27 && claimsToday <= 35) {
            amount = amount * 0.38;
        } else if (claimsToday >= 36 && claimsToday <= 45) {
            amount = amount * 0.2;
        } else if (claimsToday >= 46 && claimsToday <= 100) {
            amount = amount * 0.1;
        }

        amount = amount * 0.89;
        amount = amount * 0.6;
        amount = amount * 0.75;
        amount = amount * 0.92; //21-04-2018; -0,45 totaal ; 0,87

        if (claimsToday == 1) {
            amount = amount * 3;
        }

        return WithdrawHandler.round(amount, 5);
    }

    private static String getTime() {
        return SIMPLE_DATE_FORMAT.format(new Date()) + " - ";
    }

    private void updateRate() {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.coinmarketcap.com/v1/ticker/sumokoin").newBuilder();
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONArray jsonArray = new JSONArray(response.body().string());
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            sumoRate = jsonObject.getDouble("price_btc");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(getTime() + "Sumo rate updated " + sumoRate);
    }

    private void updateBitcoinRate() {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.coinmarketcap.com/v1/ticker/bitcoin").newBuilder();
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONArray jsonArray = new JSONArray(response.body().string());
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            bitcoinRate = jsonObject.getDouble("price_usd");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(getTime() + "Bitcoin rate updated " + bitcoinRate);
    }
}
