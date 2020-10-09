import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ValidateAddress {
    private static HttpURLConnection con;

    public static String getRpcAddress(String currency){
        String url = "";
        if (currency.equals("sumo")) {
            url = "https://server.koenhabets.nl/sumo/json_rpc";
        } else if (currency.equals("ryo")) {
            url = "http://vps.koenhabets.nl:4110/ryo/json_rpc";
        } else if (currency.equals("intense")) {
            url = "http://vps.koenhabets.nl:4110/lethean/json_rpc";
        } else if (currency.equals("masari")) {
            url = "http://vps.koenhabets.nl:4110/masari/json_rpc";
        } else if (currency.equals("loki")) {
            url = "http://127.0.0.1:8886/json_rpc";
        }
        return url;
    }

    public static boolean validateAddress(String address, String currency){
        boolean valid = true;
        String url = getRpcAddress(currency);

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
                String cont = content.toString();
                JSONObject jsonObject = new JSONObject(cont);
                if (jsonObject.getJSONObject("error").getString("message").equals("Invalid address")) {
                    valid = false;
                }
            } finally {
                con.disconnect();
            }
        } catch (Exception e) {
        }
        System.out.println(valid + " " + address);

        return valid;
    }
}
