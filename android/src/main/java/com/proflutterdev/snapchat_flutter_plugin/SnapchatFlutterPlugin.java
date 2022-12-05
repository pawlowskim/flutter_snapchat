package com.proflutterdev.snapchat_flutter_plugin;

import androidx.annotation.NonNull;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.snap.loginkit.AccessTokenResultCallback;
import com.snap.loginkit.LoginStateCallback;
import com.snap.loginkit.LoginResultCallback;
import com.snap.loginkit.SnapLogin;
import com.snap.loginkit.SnapLoginProvider;
import com.snap.loginkit.UserDataQuery;
import com.snap.loginkit.UserDataResultCallback;
import com.snap.loginkit.exceptions.AccessTokenException;
import com.snap.loginkit.exceptions.LoginException;
import com.snap.loginkit.exceptions.UserDataException;
import com.snap.loginkit.models.MeData;
import com.snap.loginkit.models.UserDataResult;

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
		LoginStateCallback {
	private static final String CHANNEL = "snapchat_flutter_plugin";
	private Context _context;
	private MethodChannel.Result _result;
	private MethodChannel channel;

	private SnapLogin _snapLogin;

	@Override
	public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
		channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL);
		channel.setMethodCallHandler(this);
		_context = flutterPluginBinding.getApplicationContext();
		_snapLogin = SnapLoginProvider.get(_context);
		_snapLogin.addLoginStateCallback(this);
	}

	@Override
	public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
		channel.setMethodCallHandler(null);
		_snapLogin = null;
	}

	@Override
	public void onMethodCall(MethodCall call, Result result) {
		Log.d(CHANNEL, call.method);
		if (call.method.equals("getPlatformVersion")) {
			result.success("Android " + android.os.Build.VERSION.RELEASE);
		}
		else if (call.method.equals("snap_chat_login")) {
			this._result = result;
			_snapLogin.startTokenGrant(this);
		}
		else if (call.method.equals("get_access_token")) {
			this._result = result;
			getAccessToken();
		}
		else if (call.method.equals("snap_chat_logout")) {
			_snapLogin.clearToken();
			result.success("logout");
		}
		else {
			result.notImplemented();
		}
	}

	private void getAccessToken() {
		_snapLogin.fetchAccessToken(new AccessTokenResultCallback() {
			public void onSuccess(@NonNull String accessToken) {
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("token", accessToken);
				if (_result != null) {
					_result.success(data);
					_result = null;
				}
			}

			public void onFailure(@NonNull AccessTokenException exception) {
				if (_result != null) {
					_result.error("400", exception.toString(), null);
					_result = null;
				}
			}
		});
	}

	private void fetchUserData() {
		final UserDataQuery userDataQuery = UserDataQuery.newBuilder()
				.withDisplayName()
				.withExternalId()
				.build();

		// Call the fetch API
		_snapLogin.fetchUserData(userDataQuery, new UserDataResultCallback() {
			@Override
			public void onSuccess(@NonNull UserDataResult userDataResult) {
				// Handle Success
				if (userDataResult.getData() == null) {
					if (_result != null) {
						_result.error("400", "Error in login", null);
						_result = null;
					}
					return;
				}

				MeData meData = userDataResult.getData().getMeData();
				if (meData == null) {
					if (_result != null) {
						_result.error("400", "Error in login", null);
						_result = null;
					}
					return;
				}
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("fullName", meData.getDisplayName());
				data.put("_id", meData.getExternalId());
				if (_result != null) {
					_result.success(data);
					_result = null;
				}
			}

			@Override
			public void onFailure(@NonNull UserDataException exception) {
				if (_result != null) {
					_result.error("400", exception.toString(), null);
					_result = null;
				}
			}
		});
	}

	@Override
	public void onStart() {
		Log.d(CHANNEL, "Snapchat login started");
	}

	@Override
	public void onSuccess(@NonNull String accessToken) {
		Log.d(CHANNEL, "Snapchat login success");
		fetchUserData();
	}

	@Override
	public void onFailure(@NonNull LoginException exception) {
		Log.d(CHANNEL, "Snapchat login failure");
		if (_result != null) {
			_result.error("400", exception.toString(), null);
			_result = null;
		}
	}

	@Override
	public void onLogout() {
		Log.d(CHANNEL, "Snapchat logout");
	}

}
