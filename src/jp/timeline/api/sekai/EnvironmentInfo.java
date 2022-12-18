package jp.timeline.api.sekai;

import com.google.gson.JsonObject;

import java.util.Random;
import java.util.UUID;

public class EnvironmentInfo
{
    public static String Content_Type = "application/octet-stream";
    public static String Accept = "application/octet-stream";
    public static String Accept_Encoding = "deflate, gzip";
    //public static String Host = "production-game-api.sekai.colorfulpalette.org";
    public static String X_Unity_Version = "2020.3.32f1";
    public static String User_Agent = "UnityPlayer/" + X_Unity_Version + " (UnityWebRequest/1.0, libcurl/7.80.0-DEV)";
    public static String X_Install_Id = UUID.randomUUID().toString();
    public static String X_App_Version = "2.3.5";
    public static String X_Asset_Version = "2.3.5.50";
    public static String X_Data_Version = "2.3.5.50";
    //public static String X_Platform = "Android";
    //public static String X_DeviceModel = "Mi 114514 Pro 5G";
    //public static String X_OperatingSystem = "Android OS 11 / API-30";
    public static String X_Platform = "iOS";
    public static String X_DeviceModel = "iPhone 14 Pro Max";
    public static String X_OperatingSystem = "iOS 16";
    public static String X_MA = RandomMac();
    public static JsonObject CreateRegister()
    {
        JsonObject valueJsonObject = new JsonObject();
        valueJsonObject.addProperty("platform", X_Platform);
        valueJsonObject.addProperty("deviceModel", X_DeviceModel);
        valueJsonObject.addProperty("operatingSystem", X_OperatingSystem);
        return valueJsonObject;
    }

    public static String RandomMac()
    {
        Random random = new Random();
        String[] mac = {String.format("%02x", random.nextInt(0xff)), String.format("%02x", random.nextInt(0xff)), String.format("%02x", random.nextInt(0xff)), String.format("%02x", random.nextInt(0xff)), String.format("%02x", random.nextInt(0xff)), String.format("%02x", random.nextInt(0xff))};
        return String.join(":", mac).toUpperCase();
    }
}
