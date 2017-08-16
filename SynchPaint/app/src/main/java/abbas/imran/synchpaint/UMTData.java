package abbas.imran.synchpaint;

import android.graphics.Path;
import android.graphics.PointF;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by imran on 11-Jun-17.
 */

public class UMTData {

   private JSONObject jsonObject;


    public  UMTData(){

        jsonObject=new JSONObject();

    }

    public UMTData(String data){

        try {
            jsonObject=new JSONObject(data);

        }
        catch (JSONException e) {
            // e.printStackTrace();
        }


    }


    public void Put(String name,String value){


        try {
            jsonObject.put(name,value);

        }
     catch (JSONException e) {
       // e.printStackTrace();
        }

    }



    public void Put(String name, List<List<String >> value){

        try {

            JSONArray jsonArray= new JSONArray(value);
            jsonObject.put(name,jsonArray);

        }
        catch (JSONException e) {
            // e.printStackTrace();
        }

    }



    public String getDataString(){

        return jsonObject.toString();
    }


    public String get(String name){

        String val="";

        try {
            val=jsonObject.get(name).toString();

        }
        catch (JSONException e) {
            // e.printStackTrace();
        }

        return val;


    }



    public List<List<String >> getArray(String name){

        JSONArray jArray ;
        List<List<String >> coordinates= new ArrayList<List<String>>();
        jArray =null;

        try {

            jArray =jsonObject.getJSONArray(name);

            if (jArray != null) {
                for (int i = 0; i < jArray.length(); i++) {
                    JSONArray point = jArray.getJSONArray(i);
                    List<String> aL=new ArrayList();
                    aL.add(point.getString(0));
                    aL.add(point.getString(1));
                    coordinates.add(aL);
                }
            }


            }
        catch (JSONException e) {
       // e.printStackTrace();
    }

        return coordinates;

   }


    }




