package com.selectro.presence.selectropresence;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.InputStream;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import com.taptrack.tcmptappy.tappy.constants.TagTypes;

import butterknife.BindView;

public class ApiActivity extends AppCompatActivity {
    private static final String TAG = ApiActivity.class.getName();
    private static final String EXTRA_TAG_TYPE = "com.taptrack.roaring.extra.TAG_TYPE";

    @Nullable
    TagResultView.ViewState currentViewState;

    EditText etPincode; // This will be a reference to our GitHub username input.
    EditText etUsername; // This will be a reference to our GitHub username input.

    Button btnOpenRelays;  // This is a reference to the "Get Repos" button.
    Button btnGetRepos;  // This is a reference to the "Get Repos" button.
    TextView tvRepoList;  // This will reference our repo list text box.
    TextView tvTagResult;

    ImageView imgPicure;
    Button btnBarcode;
    public static TextView tvresult;

    RequestQueue requestQueue;  // This is our requests queue to process our HTTP requests.

    //String baseUrl = "http://mbl-laptop:45455/api/todoitems/";  // This is the API base URL (GitHub API)
    String baseUrl = "http://presence.selectroweb.nl/presenceservice.svc/getuser/";  // This is the API base URL (GitHub API)
    String uid;
    String url;  // This will hold the full URL which will include the username entered in the etGitHubUser.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  // This is some magic for Android to load a previously saved state for when you are switching between actvities.
        setContentView(R.layout.activity_api);  // This links our code to our layout which we defined earlier.

        this.etPincode = (EditText) findViewById(R.id.et_pincode);  // Link our github user text box.
        this.etUsername = (EditText) findViewById(R.id.et_username);  // Link our github user text box.

        this.btnGetRepos = (Button) findViewById(R.id.btn_get_repos);  // Link our clicky button.
        this.btnOpenRelays = (Button) findViewById(R.id.btnOpenRelays);  // Link our clicky button.
        this.tvRepoList = (TextView) findViewById(R.id.tv_repo_list);  // Link our repository list text output box.
        this.tvRepoList.setMovementMethod(new ScrollingMovementMethod());  // This makes our text box scrollable, for those big GitHub contributors with lots of repos :)
        this.tvTagResult = (TextView) findViewById(R.id.tv_tag_results);  // Link our repository list text output box.

        btnGetRepos.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            getReposClicked();
            }
        });

        btnOpenRelays.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnOpenRelaysClicked();
            }
        });

        etUsername.requestFocus();
        requestQueue = Volley.newRequestQueue(this);  // This setups up a new request queue which we will need to make HTTP requests.

        etPincode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                switch (actionId) {

                    case EditorInfo.IME_ACTION_SEND:
                        getReposClicked();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }


    private void clearRepoList() {
        // This will clear the repo list (set it as a blank string).
        this.tvRepoList.setText("");
    }

    private void addToRepoList(String uid, String lastname, String pictureurl) {
        // This will add a new repo to our list.
        // It combines the repoName and lastUpdated strings together.
        // And then adds them followed by a new line (\n\n make two new lines).
        String strRow = uid + " / " + lastname + " / " + pictureurl;
        String currentText = tvRepoList.getText().toString();
        this.tvRepoList.setText(currentText + "\n\n" + strRow);

        Log.e("Url picture", pictureurl);
        new DownloadImageFromInternet((ImageView) findViewById(R.id.imgPicture))
                .execute(pictureurl);
    }


    private void setRepoListText(String str) {
        // This is used for setting the text of our repo list box to a specific string.
        // We will use this to write a "No repos found" message if the user doens't have any.
        this.tvRepoList.setText(str);
    }

    private void getRepoList(String username, String pincode, String uid) {
        // First, we insert the username into the repo url.
        // The repo url is defined in GitHubs API docs (https://developer.github.com/v3/repos/).
        //this.url = this.baseUrl + username + "/repos";
        if (uid != "") {
            this.url = this.baseUrl + uid;
        } else {
            this.url = this.baseUrl + username + "/" + pincode;
        };


        Log.e("Url", url);

        // Next, we create a new JsonArrayRequest. This will use Volley to make a HTTP request
        // that expects a JSON Array Response.
        // To fully understand this, I'd recommend readng the office docs: https://developer.android.com/training/volley/index.html
        JsonRequest arrReq = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Check the length of our response (to see if the user has any repos)
                        if (response.length() > 0) {
                            try {
                                JSONObject jsonObj = response;
                                String uid = jsonObj.get("Uid").toString();
                                String lastname = jsonObj.get("Displayname").toString();
                                String pictureurl = jsonObj.get("Picture").toString();
                                addToRepoList(uid, lastname, pictureurl);
                                openDoor(4);
                            } catch (JSONException e) {
                                // If there is an error then output this to the logs.
                                Log.e("Volley", "Invalid JSON Object.");
                            }
                        } else {
                            // The user didn't have any repos.
                            setRepoListText("No repos found.");
                        }

                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // If there a HTTP error then add a note to our repo list.
                        setRepoListText("Error while calling REST API");
                        Log.e("Volley", error.toString());
                    }
                }
        );
        // Add the request we just defined to our request queue.
        // The request queue will automatically handle the request as soon as it can.
        requestQueue.add(arrReq);
    }

    private void openDoor(Integer switcher) throws JSONException {
        putJsonRequest(switcher,"on");
    }

    private void closeDoor(Integer switcher) throws JSONException {
        putJsonRequest(switcher,"off");
    }

    public void putJsonRequest(Integer switcher, String switchto) throws JSONException {
        this.url = "http://192.168.250.159:82/WebRelay/api/relays/" + switcher;
        Log.e("Url", url);

        JSONObject data=new JSONObject();
        data.put("state",switchto );

        // Next, we create a new JsonArrayRequest. This will use Volley to make a HTTP request
        // that expects a JSON Array Response.
        // To fully understand this, I'd recommend readng the office docs: https://developer.android.com/training/volley/index.html
        JsonRequest arrReq = new JsonObjectRequest(Request.Method.PUT, url, data,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Check the length of our response (to see if the user has any repos)
                        if (response.length() > 0) {
                            try {
                                JSONObject jsonObj = response;
                                String uid = jsonObj.get("Uid").toString();
                                String lastname = jsonObj.get("Displayname").toString();
                                String pictureurl = jsonObj.get("Picture").toString();
                                addToRepoList(uid, lastname, pictureurl);

                            } catch (JSONException e) {
                                // If there is an error then output this to the logs.
                                Log.e("Volley", "Invalid JSON Object.");
                            }
                        } else {
                            // The user didn't have any repos.
                            setRepoListText("No repos found.");
                        }

                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // If there a HTTP error then add a note to our repo list.
                        setRepoListText("Error while calling REST API");
                        Log.e("Volley", error.toString());
                    }
                }
        );
        // Add the request we just defined to our request queue.
        // The request queue will automatically handle the request as soon as it can.
        requestQueue.add(arrReq);
    }

    public void getReposClicked() {
        // Clear the repo list (so we have a fresh screen to add to)
        clearRepoList();
        // Call our getRepoList() function that is defined above and pass in the
        // text which has been entered into the etGitHubUser text input field.
        getRepoList(etUsername.getText().toString(),etPincode.getText().toString(),"");
    }

    public void btnOpenRelaysClicked() {
        // Clear the repo list (so we have a fresh screen to add to)
        clearRepoList();
        // Call our getRepoList() function that is defined above and pass in the
        // text which has been entered into the etGitHubUser text input field.
        try {
            openDoor(4);

            //Handler handler = new Handler();
            //handler.postDelayed(new Runnable() {
                //public void run() {
                    // Actions to do after 10 seconds
                    //try {
                        //closeDoor(1);
                    //} catch (JSONException e) {
                        //e.printStackTrace();
                    //}
                //}
            //}, 5000);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private class DownloadImageFromInternet extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageFromInternet(ImageView imageView) {
            this.imageView = imageView;
            Toast.makeText(getApplicationContext(), "Please wait, it may take a few minute...", Toast.LENGTH_SHORT).show();
        }

        protected Bitmap doInBackground(String... urls) {
            String imageURL = urls[0];
            Bitmap bimage = null;
            try {
                InputStream in = new java.net.URL(imageURL).openStream();
                bimage = BitmapFactory.decodeStream(in);

            } catch (Exception e) {
                Log.e("Error Message", e.getMessage());
                e.printStackTrace();
            }
            return bimage;
        }

        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }

    private TagResultView.ViewState viewStateFromBundle(@Nullable Bundle b) {
        if(b == null) {
            return null;
        } else {
            byte[] tagCode = b.getByteArray(NfcAdapter.EXTRA_ID);
            byte tagType = b.getByte(EXTRA_TAG_TYPE, TagTypes.TAG_UNKNOWN);
            Parcelable[] messageParcels = b.getParcelableArray(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage message = null;
            if(messageParcels != null && messageParcels.length > 0) {
                message = (NdefMessage) messageParcels[0];
            }

            if(tagCode != null || message != null) {
                return new TagResultView.ViewState(tagCode,tagType,message);
            } else {
                return null;
            }
        }
    }

    private void writeToBundle(@NonNull Bundle bundle, @Nullable TagResultView.ViewState state) {
        if(state != null) {
            bundle.putByteArray(NfcAdapter.EXTRA_ID,state.getTagCode());
            bundle.putByte(EXTRA_TAG_TYPE, state.getTagType());
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG,"Received new intent");

//        byte[] tagCode = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
//        byte tagType = intent.getByteExtra(EXTRA_TAG_TYPE, TagTypes.TAG_UNKNOWN);
//        Parcelable[] messageParcels = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
//        NdefMessage message = null;
//        if(messageParcels.length > 0) {
//            message = (NdefMessage) messageParcels[0];
//        }
//
//        if(tagCode != null || message != null) {
//            currentViewState = new TagResultView.ViewState(tagCode,tagType,message);
//        } else {
//            currentViewState = null;
//        }

        currentViewState = viewStateFromBundle(intent.getExtras());
        String uid = getFormattedTagSerial(currentViewState.getTagCode());

        Log.i(TAG,"GetExtra: " + uid);

        this.tvTagResult.setText(getFormattedTagSerial(currentViewState.getTagCode()));

        getRepoList("","",uid.replace(":",""));
        //reset();
    }


    private String getFormattedTagSerial(@Nullable byte[] serial) {
        if(serial != null && serial.length > 0) {
            StringBuilder builder = new StringBuilder((serial.length * 3) - 1);

            for (int i = 0; i < serial.length; i++) {
                builder.append(String.format("%02X:", serial[i]));
            }
            builder.setLength(builder.length() - 1);
            return builder.toString();
        } else {
            return "";
        }
    }
}

