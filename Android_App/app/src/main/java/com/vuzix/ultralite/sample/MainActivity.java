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
import java.util.function.BiPredicate;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSION_REQUEST_INTERNET = 2;
    private EditText speechText;
    private EditText notificationEditText;
    private SpeechRecognizer speechRecognizer;


    int RC_SIGN_IN = 20;

    Boolean isListening = Boolean.FALSE;
    Boolean isPrompt = Boolean.FALSE;
    Boolean isProcessing = Boolean.FALSE;
    FirebaseAuth auth;
    GoogleSignInClient mGoogleSignInClient;

    String authCode = "default_code";
    String host_url = "https://ccghwd.pythonanywhere.com";
    String[] triggers = {"Jarvis", "Artist", "Largest", "Tardis"};
    private RecognitionListener triggerWordListener;
    private Handler handler = new Handler();
    private static final int PERMISSION_REQUEST_LOCATION = 3;
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        }


//        ImageView linkedImageView = findViewById(R.id.linked);

        notificationEditText = findViewById(R.id.notification_text_2);
        speechText = findViewById(R.id.speech_text_2);

        UltraliteSDK ultralite = UltraliteSDK.get(this);

        ultralite.getLinked().observe(this, linked -> {
//            linkedImageView.setImageResource(linked ? R.drawable.ic_check_24 : R.drawable.ic_close_24);
//            nameTextView.setText(ultralite.getName());
        });

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        initializeRecognitionListeners();
        speechRecognizer.setRecognitionListener(triggerWordListener);
        isListening = true;
        startListeningForTriggerWord();

//        Button speechInputButton = findViewById(R.id.speech_input_button_start);
//        speechInputButton.setOnClickListener(v -> {
//            isListening = true;
//            startListeningForTriggerWord();
//        });


//        Button stopSpeechInputButton = findViewById(R.id.speech_input_button_end);
//        stopSpeechInputButton.setOnClickListener(v -> {
//            isListening = false;
//            stopSpeechRecognition();
//        });

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Audio recording permission granted
            } else {
                Toast.makeText(this, "Audio recording permission is required.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_INTERNET) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Internet permission granted
            } else {
                Toast.makeText(this, "Internet permission is required.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted
            } else {
                Toast.makeText(this, "Location permission is required for navigation.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void initializeRecognitionListeners() {
        triggerWordListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                if (isListening) {
                    startListeningForTriggerWord();
                }
            }

            @Override
            public void onError(int error) {
                if (isListening) {
                    startListeningForTriggerWord();
                }
            }

            @Override
            public void onResults(Bundle results) {

                BiPredicate<String[], ArrayList<String>> containsTrigger = (array, list) -> {
                    if (list == null) return false;
                    for (String trigger : array) {
                        for (String spokenWord : list) {
                            if (spokenWord.toLowerCase().contains(trigger.toLowerCase())) {
                                return true;
                            }
                        }
                    }
                    return false;
                };

                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                StringBuilder sb = new StringBuilder();
                for (String str : matches) sb.append(str).append(" ");
                runOnUiThread(() -> speechText.setText(sb));
                if (isProcessing) {
                    mute();
                } else if (isPrompt) {
                    String promptText = sb.toString();
                    unmute();
                    runOnUiThread(() -> notificationEditText.setText("PROMPT: " + sb));
                    isPrompt = false;
                    isProcessing = true;
                    // todo:
                    if (promptText.toLowerCase().contains("define path to")) {
                        promptForDestination(promptText);
                    } else {
                       // handler.postDelayed(() -> {
                          //  sendToGlasses("Hello");
                            //isProcessing = false;
                            //startListeningForTriggerWord();
                       // }, 4000);
                        response = getResponse(authCode, promptText);
                        sendToGlasses(response);
                        isProcessing = false;
                        startListeningForTriggerWord();
                    }

                } else if (containsTrigger.test(triggers, matches)) {
                    // Trigger word detected
                    unmute();
                    runOnUiThread(() -> notificationEditText.setText("Listening..."));
                    isPrompt = true;
                }
                else {
                    mute();
                }
                startListeningForTriggerWord();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        };
    }

    private void mute() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
        }
    }

    private void unmute() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_RAISE, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_RAISE, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_RAISE, 0);
        }
    }
    private void startListeningForTriggerWord() {
        isListening = true;
        if (!isListening || isProcessing) {
            return;
        }
        speechRecognizer.setRecognitionListener(triggerWordListener);
        // Prepare the speech recognition intent
        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        // Start listening
        speechRecognizer.startListening(speechIntent);
    }

    private void stopListeningForTriggerWord() {
        speechRecognizer.stopListening();
        isListening = false;
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
//            Toast.makeText(MainActivity.this, "Sign in failed. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private String getResponse(String authCode, String voiceInput) {
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
                startListeningForTriggerWord();
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
    public void promptForDestination(String prompt) {
        String keyword = "define path to";
        String lowerPrompt = prompt.toLowerCase();
        int index = lowerPrompt.indexOf(keyword);
        String destination = "";
        if (index != -1) {
            // Use the original prompt for accurate casing
            destination = prompt.substring(index + keyword.length()).trim();
        }
        final String finalDestination = destination;
        runOnUiThread(() -> {
            Log.d("Destination", "Extracted destination: " + finalDestination);
            if (!finalDestination.isEmpty()) {
                startNavigation(finalDestination);
            } else {
                notificationEditText.setText("No destination recognized.");
                Toast.makeText(this, "Please specify a destination.", Toast.LENGTH_SHORT).show();
                isProcessing = false;
                startListeningForTriggerWord();
            }
        });
    }

    private void startNavigation(String destination) {
        if (destination == null || destination.isEmpty()) {
            Toast.makeText(this, "Destination is empty.", Toast.LENGTH_SHORT).show();
            isProcessing = false;
            startListeningForTriggerWord();
            return;
        }
        try {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(destination));
            Log.d("Navigation", "URI: " + gmmIntentUri.toString());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
                isProcessing = false;
                // Decide whether to start listening again or not
            } else {
                Toast.makeText(this, "Google Maps is not installed. Redirecting to Play Store.", Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps")));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps")));
                }
                isProcessing = false;
                startListeningForTriggerWord();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error starting navigation.", Toast.LENGTH_SHORT).show();
            isProcessing = false;
            startListeningForTriggerWord();
        }
    }
}
