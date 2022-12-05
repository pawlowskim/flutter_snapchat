import Flutter
import UIKit
import SCSDKLoginKit

public class SwiftSnapchatFlutterPlugin: NSObject, FlutterPlugin {
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "snapchat_flutter_plugin", binaryMessenger: registrar.messenger())
        let instance = SwiftSnapchatFlutterPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method.elementsEqual("getPlatformVersion") {
            result("iOS " + UIDevice.current.systemVersion)
        }else if call.method.elementsEqual("snap_chat_login"){
            self.openSnapChat(result: result)
        }else if call.method.elementsEqual("get_access_token"){
            self.getAccessToken(result: result)
        }else if call.method.elementsEqual("snap_chat_logout"){
            self.logout()
            result("logout")
        }else{
            result(FlutterMethodNotImplemented)
        }
    }
    
    func getAccessToken(result: @escaping FlutterResult) {
        var hashKey = [String:Any]()
        hashKey["token"] = SCSDKLoginClient.getAccessToken()
        result(hashKey);
    }
    
    func openSnapChat(result: @escaping FlutterResult){
        SCSDKLoginClient.login(from: (UIApplication.shared.keyWindow?.rootViewController.self!) ?? UIViewController(), completion: { success, error in
            if let error = error {
                print(error.localizedDescription)
                result(FlutterError.init(
                    code: "400",
                    message: error.localizedDescription,
                    details: nil))
                return
            }
            if success {
                DispatchQueue.main.async {
                    self.fetchSnapUserInfo(result: result)
                }
            }
        })
    }
    
    private func fetchSnapUserInfo(result:@escaping FlutterResult){
        let builder = SCSDKUserDataQueryBuilder().withDisplayName().withExternalId()
        let userDataQuery = builder.build()
        SCSDKLoginClient.fetchUserData(with:userDataQuery,
                                       success:{ (userData: SCSDKUserData?, partialError: Error?) in
            let displayName = userData?.displayName;
            let externalId = userData?.externalID;
            DispatchQueue.main.async {
                var hashKey = [String:Any]()
                if(displayName != nil && externalId != nil) {
                    hashKey["fullName"] = displayName
                    hashKey["_id"] = externalId
                    result(hashKey)
                } else {
                    result(FlutterError.init(
                        code: "400",
                        message :   "Display name or exernal Id not available",
                        details:nil))
                }
            }
        },
                                       failure:{ (error: Error?, isUserLoggedOut: Bool) in
            result(FlutterError.init(
                code: "400",
                message :error?.localizedDescription ?? "",
                details:nil))
        })
    }
    
    
    private func logout(){
        SCSDKLoginClient.clearToken()
    }
}