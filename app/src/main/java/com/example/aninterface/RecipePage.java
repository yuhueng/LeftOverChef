package com.example.aninterface;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class RecipePage extends AppCompatActivity {

    private ImageView imageView;
    private String generatedString;

    // FUNCTION TO SEARCH IMAGE FROM INTERNET USING GOOGLE CUSTOM SEARCH API //
    private void searchImage(String query, String apiKey) {
        String cx = "d6406233621ac4b28"; // Replace with your Custom Search Engine ID
        String url = "https://www.googleapis.com/customsearch/v1?q=" + query + "&searchType=image&key=" + apiKey + "&cx=" + cx;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray items = response.getJSONArray("items");
                            if (items.length() > 0) {
                                JSONObject item = items.getJSONObject(0);
                                String imageUrl = item.getString("link");
                                Picasso.get().load(imageUrl).into(imageView);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

        // Add the request to the RequestQueue
        Volley.newRequestQueue(this).add(request);
    }

    private static final String API_KEY = "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"; // Replace with your OpenAI API key


    // FUNCTION TO GENERATE TEXT USING OPENAI //
    public static String generateResponse(String prompt) throws IOException {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");

        String requestBody = "{\"model\":\"gpt-3.5-turbo-instruct\", \"prompt\":\"" + prompt + "\", \"max_tokens\":300}";
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .post(RequestBody.create(mediaType, requestBody))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        okhttp3.Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static String extractGeneratedText(String jsonResponse) throws IOException {
        // Use Jackson ObjectMapper to parse JSON
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);

        // Extract text from the "choices" array
        JsonNode choicesArray = jsonNode.path("choices");
        String generatedText = choicesArray.isArray() && choicesArray.size() > 0
                ? choicesArray.get(0).path("text").asText()
                : "No text found in the response";

        return generatedText;
    }

    // HTTP Requests should be done asynchronously, which means it should be done on a separate thread, not the main thread.
    private class NetworkTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String prompt = params[0];
            try {
                return generateResponse(prompt);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        protected void onPostExecute(String response) {
            try {
                generatedString = extractGeneratedText(response);
                TextView textView = findViewById(R.id.text_recipePage_generatedText);
                textView.setText(generatedString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String getFirstWords(String input, int numberOfWords) {
        if (input == null || numberOfWords <= 0) {
            return ""; // Return an empty string if input is null or numberOfWords is non-positive
        }

        StringBuilder sb = new StringBuilder();
        int wordCount = 0;

        for (char c : input.toCharArray()) {
            if (c != ' ') {
                sb.append(c); // Append characters until a space is encountered
            } else {
                wordCount++;
                if (wordCount == numberOfWords) {
                    break; // Exit loop when the specified number of words is reached
                }
                sb.append(c); // Append the space character
            }
        }

        return sb.toString().trim(); // Trim any leading/trailing spaces and return the result
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipe_page);

        imageView = findViewById(R.id.image_recipePage_searchedImage);

        // Make an image search request
        String query = getFirstWords(generatedString, 2); // Replace with your desired search query

        System.out.println(query);
        String apiKey = "AIzaSyDbrOusjueLtlTNgSHOJcachiTW606mXsg"; // Replace with your Google API key
        searchImage(query, apiKey);

        //
        String prompt = "I have leftover ingredients. The ingredients consist of 2 apples, 2 onions and garlics, and the basic cooking ingredients.Limit to 50 words" +
                "First few words must be the name of the dish ";

        new NetworkTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, prompt);
    }


}

