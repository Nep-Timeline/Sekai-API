package jp.timeline.api.sekai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.encryptorcode.httpclient.HTTPRequest;

import java.util.ArrayList;
import java.util.List;

public class SekaiCore {
    private static final int[] ranks = new int[] {
            100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000
    };

    public static User Register()
    {
        JsonObject postData = EnvironmentInfo.CreateRegister();

        JsonObject json = (JsonObject) JsonParser.parseString(SekaiUtil.CallApi("/user", HTTPMethod.POST, postData.toString()));
        String uid = json.get("userRegistration").getAsJsonObject().get("userId").getAsString();
        String credit = json.get("credential").getAsString();

        return new User(uid, credit);
    }

    public static String Login(User user)
    {
        JsonObject valueJsonObject = new JsonObject();
        valueJsonObject.addProperty("credential", user.getCredit());

        JsonObject json = (JsonObject) JsonParser.parseString(SekaiUtil.CallUserApi("/auth?refreshUpdatedResources=False", user.uid, HTTPMethod.PUT, valueJsonObject.toString()));

        return json.get("sessionToken").getAsString();
    }

    public static int PassTutorial(String uid)
    {
        JsonObject valueJsonObject = new JsonObject();
        valueJsonObject.addProperty("tutorialStatus", "opening_1");
        JsonObject valueJsonObject1 = new JsonObject();
        valueJsonObject1.addProperty("tutorialStatus", "gameplay");
        JsonObject valueJsonObject2 = new JsonObject();
        valueJsonObject2.addProperty("tutorialStatus", "opening_2");
        JsonObject valueJsonObject3 = new JsonObject();
        valueJsonObject3.addProperty("tutorialStatus", "unit_select");
        JsonObject valueJsonObject4 = new JsonObject();
        valueJsonObject4.addProperty("tutorialStatus", "idol_opening");
        JsonObject valueJsonObject5 = new JsonObject();
        valueJsonObject5.addProperty("tutorialStatus", "summary");
        JsonObject valueJsonObject6 = new JsonObject();
        valueJsonObject6.addProperty("tutorialStatus", "end");

        //bypass turtorials
        SekaiUtil.CallUserApiNew("/tutorial", uid, HTTPRequest.Method.PATCH, valueJsonObject.toString());
        SekaiUtil.CallUserApiNew("/tutorial", uid, HTTPRequest.Method.PATCH, valueJsonObject1.toString());
        SekaiUtil.CallUserApiNew("/tutorial", uid, HTTPRequest.Method.PATCH, valueJsonObject2.toString());
        SekaiUtil.CallUserApiNew("/tutorial", uid, HTTPRequest.Method.PATCH, valueJsonObject3.toString());
        SekaiUtil.CallUserApiNew("/tutorial", uid, HTTPRequest.Method.PATCH, valueJsonObject4.toString());
        SekaiUtil.CallUserApiNew("/tutorial", uid, HTTPRequest.Method.PATCH, valueJsonObject5.toString());

        JsonObject valueJsonObjectPresents = new JsonObject();
        JsonArray array = new JsonArray();
        array.add("login_bonus");
        valueJsonObjectPresents.add("refreshableTypes", array);

        List<String> presents = new ArrayList<>();
        JsonObject json = (JsonObject) JsonParser.parseString(SekaiUtil.CallUserApi("/home/refresh", uid, HTTPMethod.PUT, valueJsonObjectPresents.toString()));
        json.get("updatedResources").getAsJsonObject().get("userPresents").getAsJsonArray().forEach(i -> presents.add(i.getAsJsonObject().get("presentId").toString().replaceAll("\"", "")));

        SekaiUtil.CallUserApiNew("/tutorial", uid, HTTPRequest.Method.PATCH, valueJsonObject6.toString());

        // 30000
        int[] episodes = new int[] { 50000, 50001, 40000, 40001, 30001, 20000, 20001, 60000, 60001, 4, 8, 12, 16, 20 };
        for (int episode : episodes)
            SekaiUtil.CallUserApi("/story/unit_story/episode/" + episode, uid, HTTPMethod.POST, null);

        JsonObject valueJsonObjectpresentIds = new JsonObject();
        JsonArray array3 = new JsonArray();
        presents.forEach(array3::add);
        valueJsonObjectpresentIds.add("presentIds", array3);

        SekaiUtil.CallUserApi("/present", uid, HTTPMethod.POST, valueJsonObjectpresentIds.toString());
        SekaiUtil.CallUserApi("/costume-3d-shop/20006", uid, HTTPMethod.POST, null);
        SekaiUtil.CallUserApi("/shop/2/item/10012", uid, HTTPMethod.POST, null);

        JsonObject valueJsonObjectmissionIds = new JsonObject();
        JsonArray array4 = new JsonArray();
        array4.add(1);
        array4.add(2);
        array4.add(3);
        array4.add(4);
        array4.add(5);
        array4.add(6);
        array4.add(8);
        array4.add(10);
        valueJsonObjectmissionIds.add("missionIds", array4);

        JsonObject json2 = (JsonObject) JsonParser.parseString(SekaiUtil.CallUserApi("/mission/beginner_mission", uid, HTTPMethod.PUT, valueJsonObjectmissionIds.toString()));
        String currency = json2.get("updatedResources").getAsJsonObject().get("user").getAsJsonObject().get("userGamedata").getAsJsonObject().get("chargedCurrency").getAsJsonObject().get("free").toString();
        return Integer.parseInt(currency);
    }

    public static String Inherit(String uid, String password)
    {
        if (password.length() < 8 || password.length() > 16)
            throw new RuntimeException("The password does not meet the standard!");

        JsonObject valueJsonObject = new JsonObject();
        valueJsonObject.addProperty("password", password);

        JsonObject json = (JsonObject) JsonParser.parseString(SekaiUtil.CallUserApi("/inherit", uid, HTTPMethod.PUT, valueJsonObject.toString()));
        return json.get("userInherit").getAsJsonObject().get("inheritId").toString().replaceAll("\"", "");
    }

    public static String createPJSKAccount(String password)
    {
        User register_info = Register();
        SekaiUtil.token = Login(register_info);
        PassTutorial(register_info.getUID());
        return Inherit(register_info.getUID(), password);
    }

    public static class User
    {
        private final String uid;
        private final String credit;

        public User(String uid, String credit)
        {
            this.uid = uid;
            this.credit = credit;
        }

        public String getUID() {
            return this.uid;
        }

        public String getCredit() {
            return this.credit;
        }
    }
}
