package com.wshoto.dabao.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.wshoto.dabao.Constants;
import com.wshoto.dabao.wxapi.login.WXLoginMessage;
import com.wshoto.dabao.wxapi.pay.WXPayMessage;

import org.greenrobot.eventbus.EventBus;



public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

    private IWXAPI api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("wjj", "WXEntryActivity");
        api = WXAPIFactory.createWXAPI(this, Constants.WX_APP_ID, false);

        try {
            api.handleIntent(getIntent(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq req) {
    }


    @Override
    public void onResp(BaseResp resp) {
        int result = 0;
        if (resp.getType() == ConstantsAPI.COMMAND_LAUNCH_WX_MINIPROGRAM) {
            WXLaunchMiniProgram.Resp launchMiniProResp = (WXLaunchMiniProgram.Resp) resp;
            String extraData = launchMiniProResp.extMsg; // 对应JsApi navigateBackApplication中的extraData字段数据
            Toast.makeText(this, extraData, Toast.LENGTH_SHORT).show();
//            finish();
            EventBus.getDefault().post(extraData);
        }

        Log.i("wjj", "onResp resp.errCode " + resp.errCode);
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                String code = ((SendAuth.Resp) resp).code;
                WXLoginMessage msg = new WXLoginMessage();
                msg.loginCode = code;
                EventBus.getDefault().post(msg);
                Log.d("wjj", "err_ok");
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                break;
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                break;
            default:
                break;
        }
        finish();

    }

//    /**
//     * 处理微信发出的向第三方应用请求app message
//     * <p>
//     * 在微信客户端中的聊天页面有“添加工具”，可以将本应用的图标添加到其中
//     * 此后点击图标，下面的代码会被执行。Demo仅仅只是打开自己而已，但你可
//     * 做点其他的事情，包括根本不打开任何页面
//     */
//    @Override
//    public void onGetMessageFromWXReq(cn.sharesdk.wechat.utils.WXMediaMessage msg) {
//        if (msg != null) {
//            Intent iLaunchMyself = getPackageManager().getLaunchIntentForPackage(getPackageName());
//            startActivity(iLaunchMyself);
//        }
//    }
//
//    /**
//     * 处理微信向第三方应用发起的消息
//     * <p>
//     * 此处用来接收从微信发送过来的消息，比方说本demo在wechatpage里面分享
//     * 应用时可以不分享应用文件，而分享一段应用的自定义信息。接受方的微信
//     * 客户端会通过这个方法，将这个信息发送回接收方手机上的本demo中，当作
//     * 回调。
//     * <p>
//     * 本Demo只是将信息展示出来，但你可做点其他的事情，而不仅仅只是Toast
//     */
//    @Override
//    public void onShowMessageFromWXReq(cn.sharesdk.wechat.utils.WXMediaMessage msg) {
//        if (msg != null && msg.mediaObject != null
//                && (msg.mediaObject instanceof cn.sharesdk.wechat.utils.WXAppExtendObject)) {
//            cn.sharesdk.wechat.utils.WXAppExtendObject obj = (cn.sharesdk.wechat.utils.WXAppExtendObject) msg.mediaObject;
//            Toast.makeText(this, obj.extInfo, Toast.LENGTH_SHORT).show();
//        }
//    }
}