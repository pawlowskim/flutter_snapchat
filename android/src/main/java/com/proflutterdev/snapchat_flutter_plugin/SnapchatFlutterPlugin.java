package com.proflutterdev.snapchat_flutter_plugin;

import androidx.annotation.NonNull;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.snapchat.kit.sdk.core.controller.LoginStateController;
import com.snapchat.kit.sdk.SnapLogin;
import com.snapchat.kit.sdk.login.models.MeData;
import com.snapchat.kit.sdk.login.models.UserDataResponse;
import com.snapchat.kit.sdk.login.networking.FetchUserDataCallback;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * SnapchatFlutterPlugin
 */
public class SnapchatFlutterPlugin implements FlutterPlugin, MethodCallHandler,
		LoginStateController.OnLoginStateChangedListener {
	private static final String CHANNEL = "snapchat_flutter_plugin";
	private Context _context;
	private MethodChannel.Result _result;
	private MethodChannel channel;

	@Override
	public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
		channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL);
		channel.setMethodCallHandler(this);
		_context = flutterPluginBinding.getApplicationContext();
	}

	@Override
	public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
		channel.setMethodCallHandler(null);
	}

	@Override
	public void onMethodCall(MethodCall call, Result result) {
		Log.d(CHANNEL, call.method);
		if (call.method.equals("getPlatformVersion")) {
			result.success("Android " + android.os.Build.VERSION.RELEASE);
		}
		else if (call.method.equals("snap_chat_login")) {
			this._result = result;
			SnapLogin.getLoginStateController(_context).addOnLoginStateChangedListener(this);
			SnapLogin.getAuthTokenManager(_context).startTokenGrant();
		}
		else if (call.method.equals("get_access_token")) {
			getAccessToken(result);
		}
		else if (call.method.equals("snap_chat_logout")) {
			SnapLogin.getLoginStateController(_context).removeOnLoginStateChangedListener(this);
			SnapLogin.getAuthTokenManager(_context).clearToken();
			result.success(null);
		}
		else {
			result.notImplemented();
		}
	}

	private void getAccessToken(MethodChannel.Result result) {
		String accToken = SnapLogin.getAuthTokenManager(_context).getAccessToken();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("token", accToken);
		result.success(data);
	}

	private void fetchUserData() {
		String query = "{me{displayName,externalId}}";
		SnapLogin.fetchUserData(_context, query, null, new FetchUserDataCallback() {
			@Override
			public void onSuccess(UserDataResponse userDataResponse) {
				if (userDataResponse == null || userDataResponse.getData() == null) {
					return;
				}

				MeData meData = userDataResponse.getData().getMe();
				if (meData == null) {
					_result.error("400", "Error in login", null);
					return;
				}
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("fullName", meData.getDisplayName());
				data.put("_id", meData.getExternalId());
				if (meData.getBitmojiData() != null) {
					if (!TextUtils.isEmpty(meData.getBitmojiData().getAvatar())) {
						data.put("avatar", meData.getBitmojiData().getAvatar());
					}
				}
				_result.success(data);

			}

			@Override
			public void onFailure(boolean isNetworkError, int statusCode) {
				_result.error("400", "Error in login", null);
			}
		});
	}

	@Override
	public void onLoginSucceeded() {
		fetchUserData();
	}

	@Override
	public void onLoginFailed() {
		_result.error("400", "Error in login", null);
	}

	@Override
	public void onLogout() {
		_result.success("logout");
	}

}
