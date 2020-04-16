import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.json.JSONObject;

public class captcha {
    public static boolean checkRecaptcha(String captcha, String ip, String currency){
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
        return comp;
    }

    public static boolean checkHCaptcha(String captcha, String ip, String currency){
        boolean comp = true;
        ///recaptcha
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://hcaptcha.com/siteverify").newBuilder();
        urlBuilder.addQueryParameter("secret", "0x9Be2C2Ade63254BEA46C8eddda3De5896F35Ecc4");
        urlBuilder.addQueryParameter("response", captcha);
        urlBuilder.addQueryParameter("remoteip", ip);
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            String res = response.body().string();
            System.out.println(res);
            JSONObject jsonObject = new JSONObject(res);
            comp = jsonObject.getBoolean("success");
            if (!comp) {
                System.out.println("hCaptcha failed ip: " + ip + " " + currency);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return comp;
    }
}
