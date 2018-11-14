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

    String claim(Request request, Response response) {
        String captcha = request.queryParams("captcha");
        String address = request.queryParams("address").replaceAll("\\s+", "");
        String ip = request.queryParams("ip");
        String currency = request.queryParams("currency");
        String userAgent = request.queryParamOrDefault("user-agent", "no");
        JSONObject jsonObject = claim(address, captcha, ip, currency, userAgent);
        return jsonObject.getBoolean("success") + "";
    }

    String claimV2(Request request, Response response) {
        String captcha = request.queryParams("captcha");
        String address = request.queryParams("address").replaceAll("\\s+", "");
        String ip = request.queryParams("ip");
        String currency = request.queryParams("currency");
        String userAgent = request.queryParamOrDefault("user-agent", "no");
        return claim(address, captcha, ip, currency, userAgent).toString();
    }

    static String getTable(String currency) {
        String table = "";
        if (currency.equals("sumo")) {
            table = "sumo";
        } else if (currency.equals("ryo")) {
            table = "ryo";
        } else if (currency.equals("intense")) {
            table = "intense";
        }
        return table;
    }

    private JSONObject claim(String address, String captcha, String ip, String currency, String userAgent) {
        String table = getTable(currency);
        boolean comp = true;
        int fraudScore = -1;
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
        int error = 0;

        if (checkCaptcha(captcha, ip, currency) && !address.equals("")) {
            if (!userAgent.equals("no")) {
                try {
                    fraudScore = IpHub.getIpq(ip, userAgent).getFraudScore();//get fraudscore
                    System.out.println("Fraud score: " + fraudScore);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

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

                if (balance > WithdrawHandler.getWithdrawLimit(currency)) {
                    payout = true;
                }
                claimAmount = Prices.getClaimAmount(claimsToday, currency);
                if (currency.equals("sumo") || currency.equals("ryo")) {
                    if (balance >= 0.3) {
                        claimAmount = claimAmount * 0.6;
                    } else if (balance > 0.12) {
                        claimAmount = claimAmount * 0.85;
                    }
                } else if (currency.equals("intense")) {
                    if (balance > 45) {
                        claimAmount = claimAmount * 0.6;
                    } else if (balance > 15) {
                        claimAmount = claimAmount * 0.85;
                    }
                }
                if (fraudScore == 100) {
                    claimAmount = claimAmount * 0.66;
                } else if (fraudScore >= 75) {
                    claimAmount = claimAmount * 0.80;
                }

                claimAmount = claimAmount * (1 + dailyBonus);

                balance = balance + claimAmount;
                claims = claims + 1;
                lastClaim = date.getTime();
                if (!payout && balance > WithdrawHandler.getWithdrawLimit(currency)) {
                    payoutDayReached = date.getTime();
                }
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                boolean validAddress = true;
                if (!addressExists) {
                    if (currency.equals("ryo")) {
                        String result = ExecuteShellCommand.executeCommand("/home/pi/ryo-wallet/ryo-address-validator " + address);
                        JSONObject jsonObject = new JSONArray(result).getJSONObject(0);
                        validAddress = jsonObject.getBoolean("valid");
                    }
                    if (currency.equals("sumo")) {
                        String address4Ch = address.substring(0,4);
                        if (!address4Ch.equals("Sumi") && !address4Ch.equals("Sumo") && !address4Ch.equals("Subo")) {
                            validAddress = false;
                        }
                    }
                }
                if (validAddress) {
                    if (addressExists) {
                        java.util.Date dt = new java.util.Date(lastClaim);
                        java.util.Date dt2 = new java.util.Date(dailyLastClaim);
                        String currentTime = sdf.format(dt);
                        String currentTime2 = sdf.format(dt2);

                        String insert = "UPDATE " + table + " SET balance='" + balance + "', lastClaim='" + currentTime
                                + "', dailyLastClaim='" + currentTime2 + "', dailyBonus='" + dailyBonus + "', claims='" + claims + "', totalPaid='" + totalPaid +
                                "', lastClaimDay='" + lastClaimDay + "', claimsToday='" + claimsToday + "', lastBonusDay='" + lastBonusDay + "', payoutDayReached='"
                                + Payments.getTimeString(payoutDayReached) + "', ip='" + ip + "' WHERE address=?";
                        try {
                            PreparedStatement ps = conn.prepareStatement(insert);
                            ps.setString(1, address);
                            ps.executeUpdate();
                            ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            comp = false;
                            error = 500;
                        }
                    } else {
                        java.util.Date dt = new java.util.Date(lastClaim);
                        java.util.Date dt2 = new java.util.Date(dailyLastClaim);
                        String currentTime = sdf.format(dt);
                        String currentTime2 = sdf.format(dt2);

                        String insert = "INSERT INTO " + table + " VALUES (?, '" + balance + "', '" + currentTime
                                + "', '" + currentTime2 + "', '" + dailyBonus + "', '" + claims + "', '" + totalPaid + "', '" + lastClaimDay + "', '" + claimsToday
                                + "', '" + lastBonusDay + "', '" + Payments.getTimeString(payoutDayReached) + "', '" + ip + "')";
                        try {
                            PreparedStatement ps = conn.prepareStatement(insert);
                            ps.setString(1, address);
                            ps.executeQuery();
                            ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            error = 500;
                            comp = false;
                        }
                    }
                    String insert2 = "INSERT INTO claims VALUES (NULL, '" + sdf.format(new Date()) + "', ?, '" + claimAmount + "', '" + ip + "', '" + currency + "')";
                    try {
                        PreparedStatement ps = conn.prepareStatement(insert2);
                        ps.setString(1, address);
                        ps.executeQuery();
                        ps.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    System.out.println(getTime() + "Faucet claim " + comp + " balance: " + WithdrawHandler.round(balance, 5) + currency
                            + " Claimed:" + claimAmount + " Ip: " + ip + " Address: " + address);
                } else {
                    comp = false;
                    error = 3;//invalid address
                }
            } else {
                comp = false;
                error = 1;//5 minutes not over
            }
        } else {
            comp = false;
            error = 2;//Wrong captcha / ip 5 minutes not over
        }
        JSONObject claimItem = new JSONObject();
        claimItem.put("success", comp);
        try {
            claimItem.put("balance", balance);
            claimItem.put("dailyBonus", dailyBonus);
            claimItem.put("totalPaid", totalPaid);
            claimItem.put("lastClaim", lastClaim);
            claimItem.put("claimsToday", claimsToday);
        } catch (Exception ignored) {

        }
        if (error != 0) {
            claimItem.put("error", error);
        }

        return claimItem;
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
            if (!comp) {
                System.out.println("Recaptcha failed ip: " + ip + " " + currency);
            }
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
            } else if (currency.equals("intense")) {
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
            } else if (currency.equals("intense")) {
                jsonArrayIpIntense = jsonArrayIp;
            }
            /*
            try {
                if(IpHub.checkIp(ip) != 0) {
                    System.out.println("Iphub not 0");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }*/
        }
        return comp;
    }

    private static String getTime() {
        return SIMPLE_DATE_FORMAT.format(new Date()) + " - ";
    }


}
