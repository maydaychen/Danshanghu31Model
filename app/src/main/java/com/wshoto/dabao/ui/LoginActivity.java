package com.wshoto.dabao.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.umeng.message.PushAgent;
import com.wshoto.dabao.R;
import com.wshoto.dabao.Utils;
import com.wshoto.dabao.http.HttpJsonMethod;
import com.wshoto.dabao.http.ProgressErrorSubscriber;
import com.wshoto.dabao.http.ProgressSubscriber;
import com.wshoto.dabao.http.SubscriberOnNextAndErrorListener;
import com.wshoto.dabao.http.SubscriberOnNextListener;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {
    @BindView(R.id.editText)
    EditText mEditText;
    @BindView(R.id.ll_1)
    LinearLayout mLl1;
    @BindView(R.id.iv_yanzhengma)
    ImageView mIvYanzhengma;
    @BindView(R.id.bt_send_yanzhengma)
    TextView mBtSendYanzhengma;
    @BindView(R.id.et_yanzhengma)
    EditText mEtYanzhengma;
    @BindView(R.id.ll_yanzhengma)
    RelativeLayout mLlYanzhengma;
    @BindView(R.id.line_yanzhengma)
    View mLineYanzhengma;
    @BindView(R.id.ll_login)
    LinearLayout mLlLogin;
    @BindView(R.id.imageView)
    ImageView mImageView;
    @BindView(R.id.iv_start)
    LinearLayout mIvStart;
    @BindView(R.id.rl_login)
    RelativeLayout mRlLogin;
    @BindView(R.id.tv_support)
    TextView mTvSupport;
    @BindView(R.id.test)
    ImageView mTest;

    private int recLen = 60;
    private boolean flag = true;
    private SubscriberOnNextListener<JSONObject> sendOnNext;
    private SubscriberOnNextAndErrorListener<JSONObject> loginOnNext;
    private Loading_view loading;
    private Gson gson = new Gson();
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private PushAgent mPushAgent;
    private boolean IS_SHOWING = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        loading = new Loading_view(this, R.style.CustomDialog);
        preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        editor = preferences.edit();
        mPushAgent = PushAgent.getInstance(this);
        editor.putString("device_token", mPushAgent.getRegistrationId());
        editor.commit();
        getSupportActionBar().hide();

        String ANDROID_ID = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
        SharedPreferences preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("ANDROID_ID", "and_" + ANDROID_ID);
        editor.apply();

        ObjectAnimator first = ObjectAnimator.ofFloat(mTvSupport, "alpha", 1, 0).setDuration(2000);
        first.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (preferences.getBoolean("autoLog", false)) {
                    int time = (int) (System.currentTimeMillis() / 1000);
                    int session_time = preferences.getInt("session_time", time);
                    if (session_time > time) {
                        finish();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    } else {
                        mIvStart.setVisibility(View.GONE);
                        mRlLogin.setVisibility(View.VISIBLE);
                        Display display = getWindowManager().getDefaultDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        int a = mRlLogin.getMeasuredHeight();
                        int b = mImageView.getMeasuredHeight();
                        a = a / 2;
                        ObjectAnimator animator1 = ObjectAnimator.ofFloat(mImageView, "translationY", (a - b - Utils.dip2px(50)), 0).setDuration(2000);
                        ObjectAnimator animator2 = ObjectAnimator.ofFloat(mLlLogin, "alpha", 0, 1).setDuration(2000);
                        AnimatorSet set = new AnimatorSet();
                        set.play(animator2).after(animator1);//animator2在显示完animator1之后再显示
                        set.start();
                    }
                } else {
                    mIvStart.setVisibility(View.GONE);
                    mRlLogin.setVisibility(View.VISIBLE);

                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);

                    int a = mRlLogin.getMeasuredHeight();
                    int b = mImageView.getMeasuredHeight();
                    a = a / 2;
                    ObjectAnimator animator1 = ObjectAnimator.ofFloat(mImageView, "translationY", (a - b - Utils.dip2px(50)), 0).setDuration(2000);
                    ObjectAnimator animator2 = ObjectAnimator.ofFloat(mLlLogin, "alpha", 0, 1).setDuration(2000);
                    AnimatorSet set = new AnimatorSet();
                    set.play(animator2).after(animator1);//animator2在显示完animator1之后再显示
                    set.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        first.start();

            /* Create an Intent that will start the Main WordPress Activity. */

        sendOnNext = resultBean -> {
            if (resultBean.getInt("error") == 0) {
                Toast.makeText(LoginActivity.this, "发送成功！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(LoginActivity.this, resultBean.getString("messsage"), Toast.LENGTH_SHORT).show();
            }
        };
        loginOnNext = new SubscriberOnNextAndErrorListener<JSONObject>() {
            @Override
            public void onNext(JSONObject jsonObject) throws JSONException {
                if (IS_SHOWING) {
                    new Handler().postDelayed(() -> {
            /* Create an Intent that will start the Main WordPress Activity. */
                        loading.dismiss();
                        IS_SHOWING = false;
                    }, 500);
                }
                if (jsonObject.getInt("error") == 0) {
                    editor.putString("openid", jsonObject.getString("openid"));
                    editor.commit();
                    Toast.makeText(LoginActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                } else {
                    Toast.makeText(LoginActivity.this, "登录失败，请重试……", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (IS_SHOWING) {
                    new Handler().postDelayed(() -> {
            /* Create an Intent that will start the Main WordPress Activity. */
                        loading.dismiss();
                        IS_SHOWING = false;
                    }, 500);
                }
            }
        };

        RxTextView.textChanges(mEditText).subscribe(charSequence -> {
            if (charSequence.length() == 11) {
                if (Utils.isChinaPhoneLegal(charSequence.toString())) {
                    mEtYanzhengma.requestFocus();
                }
            }
        });

        RxTextView.textChanges(mEtYanzhengma).subscribe(charSequence -> {
            if (charSequence.length() == 6) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
            }
        });
    }

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (recLen >= 1) {
                recLen--;
                mBtSendYanzhengma.setText(recLen + "");
                handler.postDelayed(this, 1000);
            } else {
                flag = true;
                recLen = 60;
                mBtSendYanzhengma.setClickable(true);
                mBtSendYanzhengma.setText("获取验证码");
            }
        }
    };

    @OnClick({R.id.bt_send_yanzhengma, R.id.button})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.bt_send_yanzhengma:
                String tele = mEditText.getText().toString();
                if (flag && Utils.isChinaPhoneLegal(tele)) {
                    flag = false;
                    mBtSendYanzhengma.setClickable(false);
                    handler.post(runnable);
                    editor.putString("username", tele);
                    HttpJsonMethod.getInstance().sendCode(
                            new ProgressSubscriber(sendOnNext, LoginActivity.this), tele);
                } else {
                    Toast.makeText(LoginActivity.this, "请填写手机号！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.button:
                String tele1 = mEditText.getText().toString();
                String yan = mEtYanzhengma.getText().toString();
                if (!IS_SHOWING) {
                    loading.show();
                    IS_SHOWING = true;
                }
                HttpJsonMethod.getInstance().login(
                        new ProgressErrorSubscriber<>(loginOnNext, LoginActivity.this), tele1, yan);
                break;
            default:
                break;
        }
    }
}
