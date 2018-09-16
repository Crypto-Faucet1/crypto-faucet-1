import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.json.JSONObject;

public class IpHub {
    public static int checkIp(String ip){
        int block = 99;
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://v2.api.iphub.info/ip/" + ip).newBuilder();
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .addHeader("X-Key", "MzIwMDoyZVgxS0YzSms5UkUxTDVXN0hpT3UwaVJJZk5EZm9lUQ==")
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONObject jsonObject = new JSONObject(response.body().string());
            block = jsonObject.getInt("block");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return block;
    }
}
