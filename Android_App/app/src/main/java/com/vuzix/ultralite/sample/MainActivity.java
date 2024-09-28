package com.vuzix.ultralite.sample;
import android.os.Handler;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.services.calendar.CalendarScopes;
import com.google.firebase.auth.FirebaseAuth;
import com.vuzix.ultralite.LVGLImage;
import com.vuzix.ultralite.UltraliteSDK;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSION_REQUEST_INTERNET = 1;
    private EditText speechText;
    private EditText notificationEditText;
    

    int RC_SIGN_IN = 20;

    Boolean isListening = Boolean.FALSE;
    FirebaseAuth auth;
    GoogleSignInClient mGoogleSignInClient;

    String authCode = "default_code";
    String host_url = "https://ccghwd.pythonanywhere.com";
    String trigger = "Jarvis";
    private RecognitionListener triggerWordListener;
    private RecognitionListener captureListener;
    private SpeechRecognizer triggerSpeechRecognizer;
    private SpeechRecognizer captureSpeechRecognizer;
    private Handler handler = new Handler();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(CalendarScopes.CALENDAR_EVENTS))
                .requestServerAuthCode("137591440076-a250qqhgi5t2ggtme5mvi2ir7cs3e2ct.apps.googleusercontent.com", false) // Replace YOUR_SERVER_CLIENT_ID
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignIn();
//        gAuth.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                googleSignIn();
//            }
//        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, PERMISSION_REQUEST_INTERNET);
        }


        ImageView linkedImageView = findViewById(R.id.linked);

        notificationEditText = findViewById(R.id.notification_text_2);
        speechText = findViewById(R.id.speech_text_2);

        UltraliteSDK ultralite = UltraliteSDK.get(this);

        ultralite.getLinked().observe(this, linked -> {
            linkedImageView.setImageResource(linked ? R.drawable.ic_check_24 : R.drawable.ic_close_24);
//            nameTextView.setText(ultralite.getName());
        });

        // Initialize SpeechRecognizer
        triggerSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        captureSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        
        initializeRecognitionListeners();
        isListening = true;
        startListeningForTriggerWord();

        Button speechInputButton = findViewById(R.id.speech_input_button_start);
        speechInputButton.setOnClickListener(v -> {
            isListening = true;
            startListeningForTriggerWord();
        });


        Button stopSpeechInputButton = findViewById(R.id.speech_input_button_end);
        stopSpeechInputButton.setOnClickListener(v -> {
            isListening = false;
            stopSpeechRecognition();
        });

    }
    private void initializeRecognitionListeners() {
        triggerWordListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                if (isListening) {
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        // Do nothing, we already stopped listening after 5 seconds
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Speech recognition error: " + error, Toast.LENGTH_SHORT).show());
                    }
                    // Schedule the next listening session after 5 seconds
                    handler.postDelayed(() -> startListeningForTriggerWord(), 5000);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    runOnUiThread(() -> speechText.setText(recognizedText));
                    if (recognizedText.toLowerCase().contains(trigger.toLowerCase())) {
                        // Stop trigger recognizer
                        triggerSpeechRecognizer.stopListening();
                        // Start listening for user request
                        startListeningForCapture();
                        return;
                    }
                }
    // Schedule the next listening session after 5 seconds
                handler.postDelayed(() -> startListeningForTriggerWord(), 5000);
            }
            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        };

        captureListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                // After capture, go back to listening for the trigger word
                startListeningForTriggerWord();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String capturedSpeech = matches.get(0);
                    runOnUiThread(() -> speechText.setText(capturedSpeech));
                    // Send captured speech to AI and display response
                    new Thread(() -> {
                        String response = getResponse(authCode, capturedSpeech);
                        // Add a slight delay before displaying the AI response
                        try {
                            Thread.sleep(500); // 500 milliseconds delay
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(() -> sendToGlasses(response));
                    }).start();
                }
                // After processing, go back to listening for the trigger word
                startListeningForTriggerWord();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        };
        triggerSpeechRecognizer.setRecognitionListener(triggerWordListener);
        captureSpeechRecognizer.setRecognitionListener(captureListener);
    }
    private void startListeningForTriggerWord() {
        if (!isListening) {
            return;
        }    
        // Prepare the speech recognition intent
        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        // Start listening
        triggerSpeechRecognizer.startListening(speechIntent);
        // Schedule stopListening after 5 seconds
        handler.postDelayed(() -> {
            triggerSpeechRecognizer.stopListening();
        }, 5000);
    }

    private void startListeningForCapture() {
        // Prepare the speech recognition intent
        Intent captureIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        captureIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        captureIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        // Send "Listening..." message to the glasses
        runOnUiThread(() -> sendToGlasses("Listening..."));
        // Add a short delay before starting to listen
        new Handler().postDelayed(() -> {
            // Start listening
            captureSpeechRecognizer.startListening(captureIntent);
            // Stop listening after 5 seconds
            new Handler().postDelayed(() -> captureSpeechRecognizer.stopListening(), 5000);
        }, 500); // 500 milliseconds delay
    }

    private void stopSpeechRecognition() {
        if (triggerSpeechRecognizer != null) {
            triggerSpeechRecognizer.stopListening();
        }
        if (captureSpeechRecognizer != null) {
            captureSpeechRecognizer.stopListening();
        }
    }

    private void googleSignIn() {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            authCode = account.getServerAuthCode();
            Toast.makeText(MainActivity.this, "Signed into Google", Toast.LENGTH_SHORT).show();
            Log.d("AUTH_CODE", "Auth code is: " + authCode);
        } catch (ApiException e) {
            Log.e("SIGN_IN_ERROR", "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(MainActivity.this, "Sign in failed. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private String getResponse(String authCode, String voiceInput) {
        if (authCode == null || authCode.equals("default_code")) {
            runOnUiThread(() -> googleSignIn());
            return "Please sign in to continue.";
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            try {
                String encodedVoiceInput = URLEncoder.encode(voiceInput, StandardCharsets.UTF_8.toString());
                Log.d("AUTH_CODE", "Auth code before endpoint is: " + authCode);
                String urlString = String.format("%s/everyday/wear/rest/api/speech/output/%s/%s", host_url, authCode, encodedVoiceInput);
                Log.d("URL", "URL is: " + urlString);
                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) { // success
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder(); // Use StringBuilder instead of StringBuffer
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    System.out.println(response.toString());
                    return response.toString();
                } else {
                    System.out.println("GET request not worked");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
        try {
            String result = future.get();
            Log.d("HTTP_GET_RESULT", "Result from server: " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "Something went wrong";
        } finally {
            executor.shutdown();
        }
    }


    private void sendToGlasses(String content) {
        UltraliteSDK ultralite = UltraliteSDK.get(this);
        notificationEditText.setText(content);
        String notificationText = notificationEditText.getText().toString();
        ultralite.sendNotification("Jarvis", notificationText,
                loadLVGLImage(this, R.drawable.rocket));
    }

    private static LVGLImage loadLVGLImage(Context context, int resource) {
        return LVGLImage.fromBitmap(loadBitmap(context, resource), LVGLImage.CF_INDEXED_1_BIT);
    }

    @SuppressWarnings("ConstantConditions")
    private static Bitmap loadBitmap(Context context, int resource) {
        BitmapDrawable drawable = (BitmapDrawable) ResourcesCompat.getDrawable(
                context.getResources(), resource, context.getTheme());
        return drawable.getBitmap();
    }

}



