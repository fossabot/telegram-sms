package com.qwe7002.telegram_sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public class call_receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            TelephonyManager telephony = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            call_listener customPhoneListener = new call_listener(context);
            telephony.listen(customPhoneListener,
                    PhoneStateListener.LISTEN_CALL_STATE);
        }
    }
}

class call_listener extends PhoneStateListener {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private Context context;

    call_listener(Context context) {
        super();
        this.context = context;
    }

    public void onCallStateChanged(int state, String incomingNumber) {
        if (lastState == TelephonyManager.CALL_STATE_RINGING
                && state == TelephonyManager.CALL_STATE_IDLE) {
            sendSmgWhenMissedCall(incomingNumber);
        }

        lastState = state;
    }

    private void sendSmgWhenMissedCall(String incomingNumber) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("data", MODE_PRIVATE);

        String bot_token = sharedPreferences.getString("bot_token", "");
        String chat_id = sharedPreferences.getString("chat_id", "");
        String request_uri = "https://api.telegram.org/bot" + bot_token + "/sendMessage";
        request_json request_body = new request_json();
        request_body.chat_id = chat_id;
        request_body.text = "[Missed Call]\nIncoming numbler: " + incomingNumber;
        Gson gson = new Gson();
        String request_body_raw = gson.toJson(request_body);
        RequestBody body = RequestBody.create(JSON, request_body_raw);
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(request_uri).method("POST", body).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Looper.prepare();
                Toast.makeText(context, "Send Missed Call Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                Looper.loop();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() != 200) {
                    Looper.prepare();
                    assert response.body() != null;
                    Toast.makeText(context, "Send Missed Call Error:" + response.body().string(), Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        });
    }
}
