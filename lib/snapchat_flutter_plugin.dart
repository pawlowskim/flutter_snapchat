import 'dart:async';

import 'package:flutter/services.dart';

class SnapchatFlutterPlugin {
  static const MethodChannel _channel =
      const MethodChannel('snapchat_flutter_plugin');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<Map<dynamic, dynamic>?> get snapchatLogin {
    return _channel.invokeMethod<Map<dynamic, dynamic>>('snap_chat_login');
  }

  static Future<String?> get snapchatLogout {
    return _channel.invokeMethod('snap_chat_logout');
  }

  static Future<Map<dynamic, dynamic>?> get getAccessToken {
    return _channel.invokeMethod('get_access_token');
  }
}
