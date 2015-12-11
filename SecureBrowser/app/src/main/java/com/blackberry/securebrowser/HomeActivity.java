/* Copyright (c) 2015 BlackBerry Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blackberry.securebrowser;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.snarula.securebrowser.R;

import java.util.Arrays;
import java.util.List;

/**
 * This is the main browser screen. This class
 * contains all the functionality of the secure browser.
 */
public class HomeActivity extends AppCompatActivity
                        implements PopupMenu.OnMenuItemClickListener {

    public static final String TAG = "SecureBrowser";
    public static final boolean DEBUG = true;
    public static final String PREFIX = "http://";
    public static final String SEC_PREFIX = "https://";
    public static final String WEB_PREFIX = "www.";

    private Spinner spinner;
    private EditText address;
    private WebView myWebView;
    private View.OnFocusChangeListener listener;
    private List<String> allowedSites;
    private ProgressBar progressBar;
    private ImageButton bookmark;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setUpFlags();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        if(DEBUG){
            Log.i(TAG,"OnCreate called");
        }

        allowedSites = optimizingSites(getResources()
                .getStringArray(R.array.site_array));

        setUpBrowserComponents();
        resisterForFocusChange();

    }

    /**
     * registers for focus change and changes the visibility
     * of keyboard on focus change.
     */
    private void resisterForFocusChange() {
        listener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (v.getId() == R.id.address && !hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        };
    }

    /**
     * This initializes the different browser components.
     */
    private void setUpBrowserComponents() {
        setUpActionBar();
        setUpWebView();
        setUpAddressBar();
    }

    /**
     * Sets the action bar attributes.
     */
    private void setUpActionBar() {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater inflator = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_layout, null);
        actionBar.setCustomView(v);
    }

    /**
     * Sets the webView attributes.
     */
    public void setUpWebView() {

        progressBar = (ProgressBar) findViewById(R.id.progress);
        myWebView = (WebView) findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings()
                .setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        myWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                hideKeyboard();
                if (allowedNav(url)) {
                    view.loadUrl(url);
                    return true;
                }
                Toast.makeText(HomeActivity.this, R.string.not_allowed, Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!address.getText().equals(url)) {
                    address.setText(url);
                }
                progressBar.setVisibility(View.VISIBLE);
                super.onPageStarted(view, url, favicon);
            }

        });
    }

    /**
     *  Sets the address bar attributes and
     *  registers its listener.
     */
    private void setUpAddressBar() {
        address = (EditText) findViewById(R.id.address);
        address.setOnFocusChangeListener(listener);
        address.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                loadAddress();
                return true;
            }
        });

        bookmark = (ImageButton) findViewById(R.id.bookmark);
        bookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpinner(v);
            }
        });
        setUpSpinner();
    }

    /**
     *  Registers for spinner events and defines
     *  their actions.
     */
    private void setUpSpinner() {
        spinner = (Spinner) findViewById(R.id.site_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.site_array, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                hideKeyboard();
                myWebView.loadUrl(spinner.getSelectedItem().toString());
                address.setText(spinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                //  not in use for now.
            }

        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_forward) {
            myWebView.goForward();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Adds the flag to the main browsing screen.
     */
    private void setUpFlags() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * checks and verifies if the address entered
     * in the address bar passes all the checks.
     */
    void loadAddress() {
        String check = String.valueOf(address.getText()).toLowerCase().trim();
        if (allowedNav(check)) {
            if (!(check.startsWith(PREFIX) || check.startsWith(SEC_PREFIX))) {
                check = PREFIX + check;
            }
            hideKeyboard();
            myWebView.loadUrl(check);
        } else {
            showToast("".equalsIgnoreCase(check) ? ResultCode.EMPTY : ResultCode.ERROR);
        }
    }

    /**
     * shows the toast message regarding the error code.
     * @param errorCode the code of the error.
     */
    private void showToast(ResultCode errorCode) {
        switch (errorCode){
            case ERROR:
                Toast.makeText(this, R.string.not_allowed, Toast.LENGTH_SHORT).show();
                break;

            case EMPTY:
                Toast.makeText(this, R.string.address_bar_empty, Toast.LENGTH_SHORT).show();
                break;

            default:
                break;
        }
    }

    /**
     * It hides the keyboard.
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(address.getWindowToken(), 0);
    }

    /**
     * checks if the navigation to the requested site is
     * allowed or not.
     * @param siteAddress the requested site.
     * @return the boolean result.
     */

    private boolean allowedNav(String siteAddress) {
        String refinedSiteAddr = getPreprocessedSite(siteAddress);
        for (int i = 0; i < allowedSites.size(); i++) {
            if (refinedSiteAddr.startsWith(allowedSites.get(i))) {
                return true;
            }
        }
        return false;
    }

    /** pre-processing the allowed sites and store
     * then in data structure.(This is optional
     * since we can save them in the required format in xml).
     *
     * @param sitesArray the array of allowed sites array.
     * @return the processed list of allowed sites.
     */
    private List<String> optimizingSites(String[] sitesArray) {
        for (int i = 0; i < sitesArray.length; i++) {
            sitesArray[i] = getPreprocessedSite(sitesArray[i]);
        }
        return Arrays.asList(sitesArray);
    }

    /**
     * this is a helper method to process the site input.
     * @param site the input site
     * @return the processed site.
     */
    private String getPreprocessedSite(String site) {
        if (site.startsWith(PREFIX) || site.startsWith(SEC_PREFIX)) {
            String temp = site.replace(PREFIX, "");
            site = temp.replace(SEC_PREFIX, "");
        }
        if (site.startsWith(WEB_PREFIX)) {
            String temp = site.replace(WEB_PREFIX, "");
            site = temp;
        }
        return site;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_forward) {
            myWebView.goForward();
            return true;
        }
        return true;
    }

    /**
     * It sends the click event to spinner.
     * @param v
     */
    private void showSpinner(View v) {
        spinner.performClick();
    }

    /**
     * The codes for different cases.
     */
    enum ResultCode {
        /**
         * In case the user try to access a site other than the allowed ones.
         */
        ERROR,
        /**
         * In case when the address bar is empty.
         */
        EMPTY;
    }
}
