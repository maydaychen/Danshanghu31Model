package com.wshoto.dabao.http;

import com.wshoto.dabao.bean.ResultBean;
import com.wshoto.dabao.bean.TimeBean;

import org.json.JSONObject;

import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/**
 * 作者：JTR on 2016/11/24 14:15
 * 邮箱：2091320109@qq.com
 */
public interface BlueService {
//    @GET("themes")
//    rx.Observable<ThemeBean> getString();
//
//    @GET("news/latest")
//    rx.Observable<StoryBean> getLatest();
//
//    @GET("news/{id}")
//    rx.Observable<ContentBean> getContent(@Path("id") String id);
//
//    @GET("story-extra/{id}")
//    rx.Observable<CommentBean> getComment(@Path("id") String id);


    @GET("/login/ApiLogin")
    rx.Observable<JSONObject> index_info(@Query("apiname") String apiname, @Query("apipass") String apipass);

    @GET("/login/ApiLogin")
    rx.Observable<JSONObject> getNewToken(@Query("apiname") String apiname, @Query("apipass") String apipass, @Query("access_token") String access_token);


//    @Headers("addons: ewei_shop")
//    @FormUrlEncoded
//    @POST("/login")
//    rx.Observable<JSONObject> login(@Field("access_token") String access_token, @Query("device_tokens") String device_tokens, @Field("kapkey") String kapkey,
//                                    @Field("mobile") String mobile, @Field("sign") String sign, @Field("timestamp") int timestamp);

    @Headers("addons: ewei_shop")
    @FormUrlEncoded
    @PUT("/login/replaceKey")
    rx.Observable<JSONObject> change_token(@Field("access_token") String access_token, @Field("sessionkey") String sessionkey,
                                           @Field("sign") String sign, @Field("timestamp") int timestamp);

    @Headers("addons: ewei_shop")
    @DELETE("/login/sessionkey")
    rx.Observable<JSONObject> dalete_token(@Query("access_token") String access_token, @Query("sessionkey") String sessionkey,
                                           @Query("sign") String sign, @Query("timestamp") int timestamp);


    @Headers({"Content-Type:application/x-www-form-urlencoded", "addons: ewei_shop"})
    @FormUrlEncoded
    @POST("/uploads")
    rx.Observable<JSONObject> getAva(@Field("access_token") String access_token, @Field("avatar") String avatar,
                                     @Field("sessionkey") String sessionkey, @Field("sign") String sign, @Field("timestamp") int timestamp);

    @Headers("addons: superman_mall")
    @GET("index.php?i=1&c=auth&a=api_session&do=getSession&apiToken=")
    rx.Observable<ResultBean> getSession(@Query("apiToken") String apiToken);

    @Headers("addons: superman_mall")
    @GET("index.php?i=1&c=auth&a=api_session&do=getTime")
    rx.Observable<TimeBean> getTime();

//    @Headers("addons: superman_mall")
//    @GET("index.php")
//    rx.Observable<ResponseBody> sendCode(@Query("i") String i, @Query("c") String c, @Query("a") String a, @Query("do") String d, @Query("mobile") String tele, @Query("session_id") String id);

//    @Headers("addons: superman_mall")
//    @GET("index.php?i=1&c=auth&a=api_register&do=sendCode")
//    rx.Observable<JSONObject> sendCode(@Query("mobile") String tele, @Query("session_id") String id);

    @Headers("addons: superman_mall")
    @FormUrlEncoded
    @POST("wshoto_shop_v3_api.php?i=1&comefrom=app&r=wsapp.login")
    rx.Observable<JSONObject> login(@Field("mobile") String mobile, @Field("code") String code);

    @Headers("addons: superman_mall")
    @FormUrlEncoded
    @POST("wshoto_shop_v3_api.php?i=1&comefrom=app&r=wsapp.send")
    rx.Observable<JSONObject> sendCode(@Field("mobile") String mobile);
}