package com.example.saar;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.saar.ChangeCredentials.ChangeEmailFragment;
import com.example.saar.Login_SignUp.LoginSignupActivity;
import com.goodiebag.pinview.Pinview;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

import static java.lang.Integer.valueOf;

public class OtpActivity extends AppCompatActivity {

    private Button resend;
    private TextView timer;
    EditText otpRollNo;
    private int counter;
    Pinview pinview;
    ProgressBar otp_progress;
    FloatingActionButton sendOTP;
    String otpValue, rollno;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        pinview = (Pinview) findViewById(R.id.otpView);

        pinview.setPinViewEventListener(new Pinview.PinViewEventListener() {
            @Override
            public void onDataEntered(Pinview pinview, boolean fromUser) {
                Toast.makeText(getApplicationContext(), pinview.getValue(), Toast.LENGTH_SHORT).show();
                otpValue = pinview.getValue();
            }
        });

        otpRollNo = (EditText) findViewById(R.id.otp_rollno);
        otp_progress=(ProgressBar) findViewById(R.id.otp_progress);
        otp_progress.setVisibility(View.GONE);

        timer = (TextView) findViewById(R.id.timer);
        resend = (Button) findViewById(R.id.resend_button);

        setUpTimer();

        //Test case if user wants to verify after some time
        if (getIntent().hasExtra("rollno")) {
            rollno = getIntent().getStringExtra("rollno");
            getIntent().removeExtra("rollno");
            otpRollNo.setVisibility(View.INVISIBLE);
            otpRollNo.setText(rollno);
        } else {
            otpRollNo.setVisibility(View.VISIBLE);
        }

        //Action to be performed when the sending otp button is pressed
        sendOTP = (FloatingActionButton) findViewById(R.id.otp_next);
        sendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (otpValue.length() != 0) {
                    rollno = otpRollNo.getText().toString();
                    otp_progress.setVisibility(View.VISIBLE);
                    verifyOTP();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.error_entering_otp), Toast.LENGTH_SHORT).show();
                    Timber.d(getString(R.string.error_entering_otp));
                }
            }
        });

    }

    private void setUpTimer() {

        new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                int time = valueOf(counter);
                time = 30 - time;
                if (time >= 10)
                    timer.setText("0:" + time);
                else
                    timer.setText("0:0" + time);
                counter++;
            }

            public void onFinish() {
                timer.setVisibility(View.GONE);
                resend.setVisibility(View.VISIBLE);
            }
        }.start();

        resend.postDelayed(new Runnable() {
            public void run() {
                resend.setVisibility(View.VISIBLE);
            }
        }, 30000);
    }

    private void verifyOTP() {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, Constant.OTP_URL, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Timber.d(response);
                otp_progress.setVisibility(View.GONE);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    int status = Integer.parseInt(jsonObject.getString("status"));
                    if (status == 201) {
                        Timber.d(getString(R.string.otp_verified));
                        Toast.makeText(OtpActivity.this, getString(R.string.otp_verified), Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(OtpActivity.this, LoginSignupActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);

                    } else {

                        JSONArray jsonArray = jsonObject.getJSONArray("messages");
                        Toast.makeText(OtpActivity.this, jsonArray.toString(), Toast.LENGTH_LONG).show();
                        Timber.d(jsonArray.toString());
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                otp_progress.setVisibility(View.GONE);
                Timber.d(error.toString());
            }

        }) {
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("rollno", rollno);
                params.put("verification_code", otpValue);
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(OtpActivity.this);
        Request<String> data = requestQueue.add(stringRequest);
    }

    @Override
    public boolean onSupportNavigateUp() {
        //handle back button action
        onBackPressed();
        return true;
    }
}