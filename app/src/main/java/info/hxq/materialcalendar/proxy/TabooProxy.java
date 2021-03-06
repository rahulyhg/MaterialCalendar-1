package info.hxq.materialcalendar.proxy;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import inaka.com.tinytask.DoThis;
import inaka.com.tinytask.Something;
import inaka.com.tinytask.TinyTask;
import info.hxq.materialcalendar.db.DatabaseHelper;
import info.hxq.materialcalendar.entity.Taboo;
import info.hxq.materialcalendar.tool.AssetJSONLoad;
import info.hxq.materialcalendar.web.RQManager;

import static com.android.volley.Request.Method;

/**
 * Created by haoxiqiang on 15/3/20.
 */
public final class TabooProxy {

    public static final String FETCHURL = "http://www.123cha.com/calendar/js/";

    public static final String TABLENAME = "taboo";
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLENAME + "(" +
                    "'date' text(8,0) NOT NULL," +
                    "'nongli' text," +
                    "'ganzhi' text," +
                    "'yi' text," +
                    "'ji' text," +
                    "'jishenyiqu' text," +
                    "'xiongshenyiji' text," +
                    "'taishenzhanfang' text," +
                    "'wuxing' text," +
                    "'chong' text," +
                    "'pengzubaiji' text," +
                    "PRIMARY KEY(date)" +
                    ");";
    private static final int SAVEYEAR = 2015;

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion < DatabaseHelper.DATABASE_VERSION) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLENAME + ";");
            db.execSQL(CREATE_TABLE);
        }
    }

    public static void fetchJSContent() {
        RequestQueue requestQueue = RQManager.getInstance().getRequestQueue();
        for (int i = (SAVEYEAR - 10); i < (SAVEYEAR + 10); i++) {
            final String datePrefix = String.valueOf(i);
            StringRequest stringRequest = new StringRequest(Method.GET, FETCHURL + datePrefix + ".js", new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    String result = response.substring(response.indexOf("=") + 1, response.length() - 3) + "]";
                    try {
                        AssetJSONLoad.json2file(datePrefix + ".json", result);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                /* TODO Auto-generated method stub */

                }
            });
            requestQueue.add(stringRequest);
        }
        requestQueue.start();
    }

    public static void init() {
        if (SettingProxy.hasTabooInit()) {
            return;
        }

        try {
            JSONArray jsonArray = AssetJSONLoad.loadJsonArray("taboo/" + SAVEYEAR + ".json");
            for (int j = 0; j < jsonArray.length(); j++) {
                insertTaboo(jsonArray.getJSONObject(j), String.valueOf(SAVEYEAR));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TinyTask.perform(new Something<String>() {
            @Override
            public String whichDoes() {
                for (int i = (SAVEYEAR - 10); i < (SAVEYEAR + 10); i++) {
                    if (SAVEYEAR == i) {
                        continue;
                    }
                    final String datePrefix = String.valueOf(i);
                    try {
                        JSONArray jsonArray = AssetJSONLoad.loadJsonArray("taboo/" + datePrefix + ".json");
                        for (int j = 0; j < jsonArray.length(); j++) {
                            insertTaboo(jsonArray.getJSONObject(j), datePrefix);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return "success";
            }

        }).whenDone(new DoThis<String>() {
            @Override
            public void ifOK(String result) {
                Logger.i(result);
            }

            @Override
            public void ifNotOK(Exception e) {
                Logger.e(e);
            }
        }).go();

        SettingProxy.saveTabooInit();
    }

    public static void insertTaboo(JSONObject tabooObj, String datePrefix) {
        /**
         * <pre>
         "0": "辛卯年、生肖属兔、庚子月、辛酉日",
         "1": "厨灶门外东南",
         "2": "石榴木 收执位",
         "3": "冲兔(乙卯)煞东",
         "4": "辛不合酱主人不尝 酉不宴客醉坐颠狂",
         "5": "母仓 要安 除神 明堂 鸣犬",
         "6": "祭祀 开光 理发 整手足甲 安床 作灶 扫舍 教牛马",
         "7": "五离 河魁 大时 大败 咸池 四耗 地曩",
         "8": "伐木 纳畜 破土 安葬 开生坟 嫁娶 开市 动土 交易 作梁",
         "nongli": "公元2012年01月01日 农历12月(小)08日 星期日 山羊座",
         "riqi": "d0101"
         </pre>
         */
        SQLiteDatabase db = DatabaseHelper.getDatabase();
        Cursor cursor = null;
        try {
            String riqi = tabooObj.getString("riqi");
            riqi = riqi.substring(1, riqi.length());
            String date = datePrefix + riqi;
            cursor =
                    db.query(TABLENAME, new String[]{"date"}, "date = ?", new String[]{date}, null,
                            null, null);
            ContentValues contentValues = new ContentValues();
            contentValues.put("date", date);
            contentValues.put("nongli", tabooObj.getString("nongli"));
            contentValues.put("ganzhi", tabooObj.optString("0"));
            contentValues.put("yi", tabooObj.optString("6"));
            contentValues.put("ji", tabooObj.optString("8"));
            contentValues.put("jishenyiqu", tabooObj.optString("5"));
            contentValues.put("xiongshenyiji", tabooObj.optString("4"));
            contentValues.put("taishenzhanfang", tabooObj.optString("7"));
            contentValues.put("wuxing", tabooObj.optString("2"));
            contentValues.put("chong", tabooObj.optString("3"));
            contentValues.put("pengzubaiji", tabooObj.optString("4"));
            if (cursor.getCount() == 0) {
                db.insert(TABLENAME, null, contentValues);
            } else {
                db.update(TABLENAME, contentValues, "date=?", new String[]{date});
            }
        } catch (Exception e) {
            Logger.d(e.getMessage());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public static Taboo getTodayTaboo() {
        Time time = new Time();
        time.setToNow();
        int month = time.month + 1;
        String todayDate = String.valueOf(time.year) + String.valueOf(month < 10 ? "0" + month : month) + String.valueOf(time.monthDay);
        return getTabooByDate(todayDate);
    }

    public static Taboo getTabooByDate(String dateParam) {
        Taboo taboo;
        Logger.e("dateParam:" + dateParam);
        SQLiteDatabase db = DatabaseHelper.getDatabase();
        Cursor cursor =
                db.query(TABLENAME, new String[]{"date", "nongli", "ganzhi", "yi",
                                "ji", "jishenyiqu", "xiongshenyiji", "taishenzhanfang", "wuxing", "chong", "pengzubaiji"}, "date = ?", new String[]{dateParam}, null,
                        null, null);
        if (cursor.getCount() == 0) {
            Logger.d("query todayDate:" + dateParam + "  is  0");
            taboo = null;
        } else {
            cursor.moveToFirst();
            taboo = new Taboo();
            taboo.date = cursor.getString(0);
            taboo.nongli = cursor.getString(1);
            taboo.ganzhi = cursor.getString(2);
            taboo.yi = cursor.getString(3);
            taboo.ji = cursor.getString(4);
            taboo.jishenyiqu = cursor.getString(5);
            taboo.xiongshenyiji = cursor.getString(6);
            taboo.taishenzhanfang = cursor.getString(7);
            taboo.wuxing = cursor.getString(8);
            taboo.chong = cursor.getString(9);
            taboo.pengzubaiji = cursor.getString(10);
        }
        cursor.close();
        return taboo;
    }
}
