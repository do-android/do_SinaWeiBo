/*
 * Copyright (C) 2010-2013 The SINA WEIBO Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package doext.sina.weibo.openapi.models;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

/**
 * 我喜欢的微博信息列表结构体。
 * 
 * @author SINA
 * @since 2013-11-24
 */
public class FavoriteList {

    /** 微博列表 */
    public ArrayList<Favorite> favoriteList;
    public int total_number;
    
    public static FavoriteList parse(String jsonString) {
        if (TextUtils.isEmpty(jsonString)) {
            return null;
        }
        
        FavoriteList favorites = new FavoriteList();
        try {
            JSONObject jsonObject  = new JSONObject(jsonString);
            favorites.total_number = jsonObject.optInt("total_number", 0);
            
            JSONArray jsonArray    = jsonObject.optJSONArray("favorites");
            if (jsonArray != null && jsonArray.length() > 0) {
                int length = jsonArray.length();
                favorites.favoriteList = new ArrayList<Favorite>(length);
                for (int ix = 0; ix < length; ix++) {
                    favorites.favoriteList.add(Favorite.parse(jsonArray.optJSONObject(ix)));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return favorites;
    }
}