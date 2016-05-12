package com.mossige.finseth.follo.inf219_mitt_uib.courseBank;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by patrickfinseth on 22.04.2016.
 */
public class CourseBank {

    private Context mContext;

    public CourseBank(Context context) {
        this.mContext = context;
    }

    public List<String> readLine(String path) {
        List<String> mLines = new ArrayList<>();

        AssetManager am = mContext.getAssets();

        try {
            InputStream is = am.open(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = reader.readLine()) != null)
                mLines.add(line);
        } catch (FileNotFoundException fe){
            //Institute filter doesn't work properly
            Log.e("CourseBank", "readLine: Institute filter doesn't work properly");
        } catch (IOException e) {
            Log.e("CourseBank", "readLine: Institute filter doesn't work properly");
        }

        return mLines;
    }
}