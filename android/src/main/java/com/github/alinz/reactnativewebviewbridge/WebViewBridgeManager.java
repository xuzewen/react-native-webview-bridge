package com.github.alinz.reactnativewebviewbridge;

import android.content.Intent;
import android.net.Uri;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.webview.ReactWebViewManager;
import java.util.Map;
import javax.annotation.Nullable;

public class WebViewBridgeManager extends ReactWebViewManager {
    private static final String REACT_CLASS = "RCTWebViewBridge";
  
    public static final int COMMAND_SEND_TO_BRIDGE = 101;
  
    @Override public String getName() {
      return REACT_CLASS;
    }
  
    @Override public @Nullable Map<String, Integer> getCommandsMap() {
      Map<String, Integer> commandsMap = super.getCommandsMap();
  
      commandsMap.put("sendToBridge", COMMAND_SEND_TO_BRIDGE);
  
      return commandsMap;
    }
  
    @Override protected WebView createViewInstance(ThemedReactContext reactContext) {
      WebView root = super.createViewInstance(reactContext);
      root.addJavascriptInterface(new JavascriptBridge(root), "WebViewBridge");
      root.setDownloadListener(new WebviewDownload(reactContext));
      return root;
    }
  
    @Override public void receiveCommand(WebView root, int commandId, @Nullable ReadableArray args) {
      super.receiveCommand(root, commandId, args);
  
      switch (commandId) {
        case COMMAND_SEND_TO_BRIDGE:
          sendToBridge(root, args.getString(0));
          break;
        default:
          //do nothing!!!!
      }
    }
  
    private void sendToBridge(WebView root, String message) {
      String script = "WebViewBridge.onMessage('" + message + "');";
      WebViewBridgeManager.evaluateJavascript(root, script);
    }
  
    static private void evaluateJavascript(WebView root, String javascript) {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
        root.evaluateJavascript(javascript, null);
      } else {
        root.loadUrl("javascript:" + javascript);
      }
    }
  
    @ReactProp(name = "allowFileAccessFromFileURLs")
    public void setAllowFileAccessFromFileURLs(WebView root, boolean allows) {
      root.getSettings().setAllowFileAccessFromFileURLs(allows);
    }
  
    @ReactProp(name = "allowUniversalAccessFromFileURLs")
    public void setAllowUniversalAccessFromFileURLs(WebView root, boolean allows) {
      root.getSettings().setAllowUniversalAccessFromFileURLs(allows);
    }
  
    /**
     * 下载
     */
    class WebviewDownload implements DownloadListener {
      ReactContext reactContext;
  
      public WebviewDownload(ReactContext reactContext) {
        this.reactContext = reactContext;
      }
  
      @Override public void onDownloadStart(String url, String userAgent, String contentDisposition,
          String mimetype, long contentLength) {
        try {
          Uri uri = Uri.parse(url);
          Intent intent = new Intent(Intent.ACTION_VIEW, uri);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          reactContext.startActivity(intent);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }