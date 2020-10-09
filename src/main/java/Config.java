import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.*;

public class Config {
    public static ConfigItem config;

    public Config() {
        try {
            config = readSavedConfig();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error reading config");
        }
    }

    private ConfigItem readSavedConfig() {
        Gson gson = new Gson();
        ConfigItem config = null;

        try {
            JsonReader reader = new JsonReader(new FileReader("config.json"));
            config = gson.fromJson(reader, ConfigItem.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return config;
    }
}