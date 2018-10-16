package com.wshoto.dabao.ui;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.DefaultHandler;
import com.tencent.connect.UserInfo;
import com.tencent.connect.auth.QQToken;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.UiError;
import com.umeng.message.PushAgent;
import com.wshoto.dabao.Config;
import com.wshoto.dabao.MyApplication;
import com.wshoto.dabao.R;
import com.wshoto.dabao.Utils;
import com.wshoto.dabao.alipay.AliPayManager;
import com.wshoto.dabao.alipay.AliPayMessage;
import com.wshoto.dabao.http.HttpJsonMethod;
import com.wshoto.dabao.http.ProgressErrorSubscriber;
import com.wshoto.dabao.http.SubscriberOnNextAndErrorListener;
import com.wshoto.dabao.wxapi.login.WXLoginInfoMessage;
import com.wshoto.dabao.wxapi.login.WXLoginManager;
import com.wshoto.dabao.wxapi.login.WXLoginMessage;
import com.wshoto.dabao.wxapi.login.response.AccessToken;
import com.wshoto.dabao.wxapi.login.response.RefreshToken;
import com.wshoto.dabao.wxapi.pay.WXPayEntry;
import com.wshoto.dabao.wxapi.pay.WXPayMessage;
import com.wshoto.dabao.wxapi.pay.WXUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.sharesdk.onekeyshare.OnekeyShare;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.wshoto.dabao.Config.RESPONSE_TEXT_FAIL;
import static com.wshoto.dabao.Config.RESPONSE_TEXT_SUCCESS;

public class MainActivity extends InitActivity implements EasyPermissions.PermissionCallbacks {

    @BindView(R.id.wv_main)
    BridgeWebView webView;
    @BindView(R.id.bt_refresh)
    Button btn;
    @BindView(R.id.rl_gone)
    RelativeLayout rlGone;
    @BindView(R.id.rl_code)
    RelativeLayout mRlCode;

    private MyApplication app;
    //微信登录有效期
    private AccessToken mSavedAccessToken;
    private IUiListener loginListener;

    private Bitmap bmp;
    private String mUrl;
    private File picFile;
    private View inflate;
    private String webUrl;
    private String mSaveMessage;
    private boolean isOk = true;
    private boolean networkOk = true;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private ProgressDialog mSaveDialog = null;
    private ProgressDialog updateDialog = null;
    private SubscriberOnNextAndErrorListener<JSONObject> uploadOnNext;
    final public static int REQUEST_CODE_ASK_CALL_PHONE = 123;
    private static final int RC_LOCATION_CONTACTS_PERM = 124;
    private static final int RC_STORAGE_CONTACTS_PERM = 125;
    final public static int REQUEST_WRITE = 222;
    final public static int REQUEST_SAVE_WRITE = 333;

    private String fileName = Environment.getExternalStorageDirectory() + "/" + getText(R.string.app_name) + ".png";

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void initView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        registerBroadrecevicer();
    }

    @Override
    public void initData() throws JSONException {
        PushAgent pushAgent = PushAgent.getInstance(this);
        pushAgent.onAppStart();
        preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        editor = preferences.edit();
        btn.setOnClickListener(v -> {
            webView.loadUrl(webUrl);
            isOk = true;
        });
        saveImg();
        uploadOnNext = new SubscriberOnNextAndErrorListener<JSONObject>() {
            @Override
            public void onNext(JSONObject jsonObject) throws JSONException {
                if (updateDialog != null) {
                    updateDialog.dismiss();
                    updateDialog = null;
                }
                String msg = jsonObject.toString();
                if (jsonObject.getInt("statusCode") == 1) {
                    Toast.makeText(MainActivity.this, "上传成功！", Toast.LENGTH_SHORT).show();
                    msg = jsonObject.getString("result");
                }
                deletePic();
                Log.d("chenyi", msg);
                getResult(msg);
            }

            @Override
            public void onError(Throwable e) {
                if (updateDialog != null) {
                    updateDialog.dismiss();
                }
                Toast.makeText(MainActivity.this, "上传失败，请稍后再试", Toast.LENGTH_SHORT).show();
                Log.d("wjj", "err");
                deletePic();
                e.printStackTrace();
            }
        };
        initWebview();
        webView.loadUrl(Config.BASEURL4);
    }

    private void initWebview() {
        initWebSetting(webView);
        WebSettings webSettings = webView.getSettings();
        //设置此属性，可任意比例缩放
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAppCacheMaxSize(1024 * 1024 * 8);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        webSettings.setAppCachePath(appCachePath);
        webSettings.setAllowFileAccess(true);
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setTextSize(WebSettings.TextSize.NORMAL);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setDomStorageEnabled(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua + ";wshoto");
        webView.setDefaultHandler(new DefaultHandler());
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new BridgeWebViewClient(webView) {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (isOk) {
                    rlGone.setVisibility(View.INVISIBLE);
                }
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (errorCode == -2) {
                    isOk = false;
                    webUrl = failingUrl;
                    Toast.makeText(MainActivity.this, "无网络", Toast.LENGTH_SHORT).show();
                    rlGone.setVisibility(View.VISIBLE);
                    networkOk = false;
                }
            }
        });

        webView.registerHandler("share", (responseData, function) -> showShare("测试", "https://www.baidu.com"));

        webView.registerHandler("checkLogin", (responseData, function) -> {
            Log.i("chenyi", "initWebview: checklogin");
            if ("".equals(preferences.getString("openid", ""))) {
                webView.callHandler("checkLogin", RESPONSE_TEXT_FAIL, data -> Log.i("chenyi", "callHandler checkLogin result " + data));
            } else {
                webView.callHandler("checkLogin", RESPONSE_TEXT_SUCCESS, data -> Log.i("chenyi", "callHandler checkLogin result " + data));
            }
        });
        webView.registerHandler("getOpenID", (responseData, function) -> {
            Log.i("chenyi", "initWebview:getOpenID ");
            try {
                JSONObject out = new JSONObject();
                out.put("statusCode", 1);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("OpenID", preferences.getString("openid", ""));
                out.put("data", jsonObject);
                Log.i("chenyi", "initWebview: " + out.toString());
                webView.callHandler("getOpenID", out.toString(), data -> Log.i("chenyi", "callHandler getOpenID result " + data));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        webView.registerHandler("nativeLogin", (responseData, function) -> {
            editor.clear();
            if (editor.commit()) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });
        webView.registerHandler("getUserInfo", (responseData, function) -> {
            try {
                JSONObject jsonObject = new JSONObject(responseData);
                editor.putString("access_token", jsonObject.getString("access_token"));
                editor.putString("auth_key", jsonObject.getString("auth_key"));
                editor.putString("sessionkey", jsonObject.getString("sessionkey"));
                editor.putString("timestamp", jsonObject.getString("timestamp"));
                if (editor.commit()) {
                    Toast.makeText(this, "保存成功！", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        webView.registerHandler("uploadImg", (responseData, function) -> showType());
        webView.registerHandler("shellQrcode", (responseData, function) -> {
            try {
                show(responseData);
            } catch (JSONException | MalformedURLException e) {
                e.printStackTrace();
            }
        });
        webView.registerHandler("authLogin", (data, function) -> {
            Log.i("chenyi", "start login " + data);
            try {
                JSONTokener jsonTokener = new JSONTokener(data);
                JSONObject wxJson = (JSONObject) jsonTokener.nextValue();
                String type = wxJson.getString("type");
                if ("wechat".equals(type)) {
                    Log.i("chenyi", "wechat");
                    WXLoginManager.getInstance(MainActivity.this).login();
                    function.onCallBack(RESPONSE_TEXT_SUCCESS);
                } else if ("qq".equals(type)) {
                    Log.i("chenyi", "qq");
                    loginTencent();
                    function.onCallBack(RESPONSE_TEXT_SUCCESS);
                } else {
                    function.onCallBack(RESPONSE_TEXT_FAIL);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                function.onCallBack(RESPONSE_TEXT_FAIL);
            }
        });
        webView.registerHandler("payment", (responseData, function) -> {
            try {
                JSONTokener jsonTokener = new JSONTokener(responseData);
                JSONObject wxJson = (JSONObject) jsonTokener.nextValue();
                Log.d("wjj", "payment1");
                String type = wxJson.getString("type");
                String params = wxJson.getString("params");
                Log.d("wjj", params);
                if ("wechat_app".equals(type)) {
                    Log.d("wjj", "payment2");
                    WXPayEntry entry = WXUtils.parseWXData(params);
                    if (entry != null) {
                        Log.d("wjj", "payment3");
                        WXUtils.startWeChat(MainActivity.this, entry);
                    } else {
                        function.onCallBack(RESPONSE_TEXT_FAIL);
                    }
                } else if ("alipay_app".equals(type)) {
                    AliPayManager.getInstance().payV2(MainActivity.this, params);
                }
            } catch (JSONException e) {
                function.onCallBack(RESPONSE_TEXT_FAIL);
            }
        });
    }

    public void show(String url) throws JSONException, MalformedURLException {
        Dialog dialog = new Dialog(this, R.style.BottomDialog);
        inflate = LayoutInflater.from(this).inflate(R.layout.pop_pic, null);
        Button choosePhoto = inflate.findViewById(R.id.takePhoto);
        Button cancel = inflate.findViewById(R.id.btn_cancel);
        mUrl = url;
        choosePhoto.setOnClickListener(v -> {
            dialog.dismiss();
            mSaveDialog = ProgressDialog.show(MainActivity.this, "保存图片", "图片正在保存中，请稍等...", true);
            if (Build.VERSION.SDK_INT >= 23) {
                int checkCallPhonePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_SAVE_WRITE);
                } else {
                    //上面已经写好的拍照方法
                    new Thread(saveFileRunnable).start();
                }
            } else {
                //上面已经写好的拍照方法
                new Thread(saveFileRunnable).start();
            }
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(inflate);
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = -1;
        dialogWindow.setAttributes(lp);
        dialog.show();
    }


    private Runnable saveFileRunnable = new Runnable() {
        @Override
        public void run() {
            savePicture(getHttpBitmap(mUrl));
            mSaveMessage = "图片保存成功！";
            messageHandler.sendMessage(messageHandler.obtainMessage());
        }

    };
    private Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mSaveDialog.dismiss();
            Toast.makeText(MainActivity.this, mSaveMessage, Toast.LENGTH_SHORT).show();
        }
    };

    public Bitmap getHttpBitmap(String url) {
        Bitmap bitmap = null;
        try {
            JSONObject jsonObject = new JSONObject(url);
            url = jsonObject.getString("params");
            URL pictureUrl = new URL(url);
            InputStream in = pictureUrl.openStream();
            bitmap = BitmapFactory.decodeStream(in);
            in.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public void savePicture(Bitmap bitmap) {
        String pictureName = Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".jpg";
        File file = new File(pictureName);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class IntenterBoradCastReceiver extends BroadcastReceiver {
        private ConnectivityManager mConnectivityManager;
        private NetworkInfo netInfo;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                assert mConnectivityManager != null;
                netInfo = mConnectivityManager.getActiveNetworkInfo();
                if (!networkOk && netInfo.isAvailable()) {
                    webView.loadUrl(webUrl);
                    isOk = true;
                }
            }
        }
    }

    private void registerBroadrecevicer() {
        //获取广播对象
        IntenterBoradCastReceiver receiver = new IntenterBoradCastReceiver();
        //创建意图过滤器
        IntentFilter filter = new IntentFilter();
        //添加动作，监听网络
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);
    }

    /**
     * EventBus阿里支付结果回调事件
     *
     * @param payMessage 支付宝支付结果
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMoonEvent(AliPayMessage payMessage) {
        String payString = payMessage.getJsonString();
        webView.callHandler("payment", payString, data -> Log.i("chenyi", "callHandler AliPayMessage result " + data));
    }

    /**
     * EventBus微信支付结果回调事件
     *
     * @param payMessage 微信支付结果
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMoonEvent(WXPayMessage payMessage) {
        String payString = payMessage.getJsonString();
        //java调用js，通知服务端支付完成
        webView.callHandler("payment", payString, jsResponseData -> Log.i("chenyi", "callHandler WXPayMessage result " + jsResponseData));
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMoonEvent(WXLoginMessage loginMessage) {
        Log.i("chenyi", "onMoonEvent WXLoginMessage " + loginMessage.loginCode);
        WXLoginManager manager = WXLoginManager.getInstance(this);
        try {
            //拉起微信授权后通过code向自己服务器获取accessToken
            AccessToken token = manager.requestAccessToken(loginMessage.loginCode);
            Log.i("chenyi", "token == " + token);
            if (token == null || TextUtils.isEmpty(token.getAccess_token())) {
                sendLoginError();
                return;
            }
            Log.i("chenyi", "onMoonEvent WXLoginMessage AccessToken " + token);
            mSavedAccessToken = token;
            //服务器验证accessToken返回
            String userinfo = manager.requestUserInfo(mSavedAccessToken.getAccess_token(), mSavedAccessToken.getOpenid());
            Log.i("chenyi", "onMoonEvent WXLoginMessage userinfo " + userinfo);
            if (!TextUtils.isEmpty(userinfo)) {
                WXLoginInfoMessage msg = new WXLoginInfoMessage();
                msg.status = 0;
                msg.data = userinfo;
                EventBus.getDefault().post(msg);

                String expiresStr = mSavedAccessToken.getExpires_in();
                if (!TextUtils.isEmpty(expiresStr)) {
                    Integer expires = Integer.valueOf(expiresStr);
                    if (expires > 60) {
                        expires /= 2;
                    } else {
                        expires = 60;
                    }
                    Log.i("chenyi", "start time " + expires);
                    timer.schedule(task, expires * 1000, expires * 1000);
                }
            } else {
                sendLoginError();
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendLoginError();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMoonEvent(WXLoginInfoMessage loginInfoMessage) {
        String info = loginInfoMessage.getJsonString();
        Log.i("chenyi", "onMoonEvent WXLoginInfoMessage " + info);
        webView.callHandler("authLogin", info, data -> Log.i("chenyi", "wxLogin error result " + data));
    }

    private void sendLoginError() {
        Log.i("chenyi", "sendLoginError");
        WXLoginInfoMessage msg = new WXLoginInfoMessage();
        msg.status = -1;
        msg.data = "";
        EventBus.getDefault().post(msg);
    }

    Timer timer = new Timer();
    TimerTask task = new TimerTask() {

        @Override
        public void run() {
            if (mSavedAccessToken == null) return;
            try {
                RefreshToken refreshToken = WXLoginManager.getInstance(MainActivity.this).requestRefreshToken(mSavedAccessToken.getRefresh_token());
                if (refreshToken != null && refreshToken.getAccess_token() != null) {
                    mSavedAccessToken = new AccessToken(refreshToken);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public void camera() {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_ASK_CALL_PHONE);
            } else {
                //上面已经写好的拍照方法
                write(true);
            }
        } else {
            //上面已经写好的拍照方法
            write(true);
        }
    }

    public void write(boolean iscamera) {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE);
            } else {
                if (iscamera) {
                    //上面已经写好的拍照方法
                    takePhoto();
                } else {
                    selectImage();
                }
            }
        } else {
            if (iscamera) {
                //上面已经写好的拍照方法
                takePhoto();
            } else {
                selectImage();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 100:
//         从图库裁减返回
                    Log.d("wjj", "100");
                    if (data != null) {
                        Uri uri = data.getData();
                        ContentResolver cr = this.getContentResolver();
                        try {
                            assert uri != null;
                            bmp = BitmapFactory.decodeStream(cr.openInputStream(uri));
                            Matrix matrix = new Matrix();
                            matrix.setScale(0.5f, 0.5f);
                            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
                                    bmp.getHeight(), matrix, true);
                            upDataHeadImg();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case 101:
                    // 从拍照返回
                    if (data != null) {
                        Bundle bundle = data.getExtras();
                        assert bundle != null;
                        bmp = (Bitmap) bundle.get("data");
                        Matrix matrix = new Matrix();
                        matrix.setScale(0.5f, 0.5f);
                        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
                                bmp.getHeight(), matrix, true);
                        upDataHeadImg();
                    }
                    break;
                default:
                    break;
            }
        }
    }


    public void deletePic() {
        if (picFile.exists()) {
            picFile.delete();
            if (bmp != null) {
                bmp.recycle();
            }
        }
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        createPicFile();
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, 101);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从相册选择
     */
    private void selectImage() {
        createPicFile();
        Intent intent;
        if (Build.VERSION.SDK_INT >= 23) {
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
        }
        if (isIntentAvailable(MainActivity.this, intent)) {
            startActivityForResult(Intent.createChooser(intent, "选择图片"), 100);
        } else {
            Toast.makeText(MainActivity.this, "请安装相关图片查看应用。", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 创建上传图片文件
     */
    private void createPicFile() {
        String sdStatus = Environment.getExternalStorageState();
        // 检测sd是否可用
        if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this, "请检查SD卡是否可用", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(Environment.getExternalStorageDirectory().toString());
        if (!file.exists()) {
            file.mkdirs();
        }
        picFile = new File(file
                + "/seawaterHeadImg.jpg");
    }

    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.GET_ACTIVITIES);
        return list.size() > 0;
    }

    /**
     * 上传用户头像
     */
    private void upDataHeadImg() {
        if (updateDialog == null) {
            updateDialog = ProgressDialog.show(MainActivity.this, "上传头像", "头像正在上传中，请稍等...", true, false);
        }
        int time = (int) (System.currentTimeMillis() / 1000);
        String sign = "";
        sign = sign + "avatar=" + Utils.bitmaptoString(bmp) + "&";
        sign = sign + "sessionkey=" + preferences.getString("sessionkey", "") + "&";
        sign = sign + "timestamp=" + time + "&";
        sign = sign + "key=" + preferences.getString("auth_key", "");
        sign = Utils.md5(sign);

        HttpJsonMethod.getInstance().getAva(
                new ProgressErrorSubscriber<>(uploadOnNext, MainActivity.this), preferences.getString("access_token", ""), Utils.bitmaptoString(bmp),
                preferences.getString("sessionkey", ""), sign, time);
    }

    public void getResult(String msg) {
        String result = "{\"statusCode\":\"1\", \"data\":\"" + msg + "\"}";
        webView.callHandler("uploadImg", result, jsResponseData -> {
            Log.d("wjj", "uploadImg " + jsResponseData);
            webView.reload();
        });
    }


    @AfterPermissionGranted(RC_LOCATION_CONTACTS_PERM)
    public void showType() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Dialog dialog = new Dialog(this, R.style.BottomDialog);
            inflate = LayoutInflater.from(this).inflate(R.layout.pop_avatar, null);
            Button camera = inflate.findViewById(R.id.camera);
            Button imgLib = inflate.findViewById(R.id.img_lib);
            Button cancel = inflate.findViewById(R.id.btn_cancel);
            camera.setOnClickListener(v -> {
                dialog.dismiss();
                camera();
            });
            imgLib.setOnClickListener(v -> {
                selectImage();
                dialog.dismiss();
            });
            cancel.setOnClickListener(v -> dialog.dismiss());
            dialog.setContentView(inflate);
            Window dialogWindow = dialog.getWindow();
            dialogWindow.setGravity(Gravity.BOTTOM);
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
//        lp.y = 20;
            lp.width = -1;
            dialogWindow.setAttributes(lp);
            dialog.show();
        } else {
            // Ask for both permissions
            EasyPermissions.requestPermissions(this, getString(R.string.permition),
                    RC_LOCATION_CONTACTS_PERM, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d("chenyi", "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d("chenyi", "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @AfterPermissionGranted(RC_STORAGE_CONTACTS_PERM)
    public void saveImg() {
        String[] perms = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            File file = new File(fileName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 50, fos);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_locate),
                    RC_STORAGE_CONTACTS_PERM, perms);
        }
    }

    private void showShare(String title, String url) {
        OnekeyShare oks = new OnekeyShare();
        //关闭sso授权
        oks.disableSSOWhenAuthorize();
        oks.setTitle(getText(R.string.app_name).toString());
        oks.setText(title);
        //确保SDcard下面存在此张图片
        oks.setImagePath(fileName);
        oks.setUrl(url);
        oks.setTitleUrl(url);
        oks.show(this);
    }

    public void loginTencent() {
        app = (MyApplication) getApplication();
        loginListener = new IUiListener() {
            @Override
            public void onComplete(Object o) {
                //登录成功后回调该方法,可以跳转相关的页面
                Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                JSONObject object = (JSONObject) o;
                try {
                    String accessToken = object.getString("access_token");
                    String expires = object.getString("expires_in");
                    String openID = object.getString("openid");
                    app.mTencent.setAccessToken(accessToken, expires);
                    app.mTencent.setOpenId(openID);
                    getUserInfo(openID);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(UiError uiError) {
                Log.i("chenyi", "onError: " + uiError.errorDetail);
            }

            @Override
            public void onCancel() {
                Log.i("chenyi", "onCancel: ");
            }
        };
        if (!app.mTencent.isSessionValid()) {
            app.mTencent.login(this, "all", loginListener);
        }

    }

    private void getUserInfo(String openId) {
        QQToken token = app.mTencent.getQQToken();
        UserInfo mInfo = new UserInfo(MainActivity.this, token);
        mInfo.getUserInfo(new IUiListener() {
            @Override
            public void onComplete(Object object) {
                JSONObject jb = (JSONObject) object;
                try {
                    jb.put("openId", openId);
                    jb.put("statusCode", "1");
                    Log.i("chenyi", "onComplete: " + jb.toString());
                    webView.callHandler("authLogin", jb.toString(), data -> Log.i("chenyi", "qqLogin error result " + data));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(UiError uiError) {
            }

            @Override
            public void onCancel() {
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
