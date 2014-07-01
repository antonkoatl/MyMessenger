
package com.example.mymessenger.services;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.Locale;

import twitter4j.auth.AccessToken;

/**
 * Activity for request OAuth authorization in case of missing VK app.
 */

public class SimpleOpenAuthActivity extends Activity {
    public static final String URL_TO_LOAD = "extra-url-to-load";
    public static final String TOKEN_DATA_ACCESS = "extra-token-data-access";
    public static final String TOKEN_DATA_SECRET = "extra-token-data-secret";


    protected WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new VKOpenAuthView(this));

        findViewById(android.R.id.content).setBackgroundColor(Color.rgb(240, 242, 245));
        loadPage();
    }

    private void loadPage() {
        try {
	        String urlToLoad;
	        urlToLoad = getIntent().getStringExtra(URL_TO_LOAD);
	        if (urlToLoad == null)
	        {

	        }

            mWebView = (WebView) findViewById(android.R.id.copyUrl);
            mWebView.setWebViewClient(new OAuthWebViewClient());
            WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            mWebView.loadUrl(urlToLoad);
            mWebView.setVisibility(View.INVISIBLE);

        } catch (Exception e) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }



    private class OAuthWebViewClient extends WebViewClient {
        public boolean canShowPage = true;

        private boolean processUrl(String url) {
            Intent data = new Intent("VK_RESULT_INTENT_NAME");
            if (url.startsWith(msTwitter.CALLBACK_URI)) {
                //AccessToken at = mTwitter.getOAuthAccessToken(mRequestToken);
                Uri uri =  Uri.parse(url);
                data.putExtra(msTwitter.URL_TWITTER_OAUTH_VERIFIER, uri.getQueryParameter(msTwitter.URL_TWITTER_OAUTH_VERIFIER));
                setResult(RESULT_OK, data);
                finish();
                return true;
            } else if (url.startsWith(msTwitter.CANCEL_URI)) {
                setResult(RESULT_CANCELED, data);
                finish();
                return true;
            }
            return false;
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (processUrl(url))
                return true;
            canShowPage = true;
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            processUrl(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (canShowPage)
                view.setVisibility(View.VISIBLE);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            canShowPage = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(SimpleOpenAuthActivity.this)
                    .setMessage(description)
                    .setPositiveButton(com.vk.sdk.R.string.vk_retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            loadPage();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });
            try {
                builder.show();
            } catch (Exception e) {
                if (true)
                	e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent data = new Intent("VK_RESULT_INTENT_NAME");
        setResult(RESULT_CANCELED, data);
        super.onBackPressed();
    }

    private static class VKOpenAuthView extends RelativeLayout {
        public VKOpenAuthView(Context context) {
            super(context);
            ProgressBar progress = new ProgressBar(context);
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
            progress.setLayoutParams(lp);
            addView(progress);

            WebView webView = new WebView(context);
            lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            webView.setLayoutParams(lp);
            addView(webView);
            webView.setId(android.R.id.copyUrl);
            webView.setVisibility(View.INVISIBLE);
        }
    }
}
