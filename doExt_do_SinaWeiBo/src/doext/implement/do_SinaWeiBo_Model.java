package doext.implement;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.MusicObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.VideoObject;
import com.sina.weibo.sdk.api.VoiceObject;
import com.sina.weibo.sdk.api.WebpageObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.utils.Utility;

import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import core.object.DoSingletonModule;
import doext.app.do_SinaWeiBo_App;
import doext.define.do_SinaWeiBo_IMethod;
import doext.sina.weibo.openapi.AccessTokenKeeper;
import doext.sina.weibo.openapi.Constants;
import doext.sina.weibo.openapi.LogoutAPI;
import doext.sina.weibo.openapi.UsersAPI;
import doext.sina.weibo.openapi.models.ErrorInfo;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_SinaWeiBo_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_SinaWeiBo_Model extends DoSingletonModule implements do_SinaWeiBo_IMethod {

	private AuthInfo mAuthInfo;
	/** SSO 授权认证实例 */
	private SsoHandler mSsoHandler;

	/** 微博分享的接口实例 */
	private IWeiboShareAPI mWeiboShareAPI;

	public do_SinaWeiBo_Model() throws Exception {
		super();
		do_SinaWeiBo_App.getInstance().setModuleTypeID(getTypeID());
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		// ...do something
		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("login".equals(_methodName)) {
			this.login(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("getUserInfo".equals(_methodName)) {
			this.getUserInfo(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("logout".equals(_methodName)) {
			this.logout(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("share".equals(_methodName)) {
			this.share(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	/**
	 * 获取用户信息；
	 * 
	 * @throws Exception
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void getUserInfo(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _uid = DoJsonHelper.getString(_dictParas,"uid", "");
		String _accessToken = DoJsonHelper.getString(_dictParas,"accessToken", "");
		String _refreshToken = DoJsonHelper.getString(_dictParas,"refreshToken", "");
		String _expires = DoJsonHelper.getString(_dictParas,"expires", "");
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		if (!TextUtils.isEmpty(_uid) && !TextUtils.isEmpty(_accessToken) && !TextUtils.isEmpty(_refreshToken) && !TextUtils.isEmpty(_expires)) {
			// 获取用户信息接口
			Oauth2AccessToken accessToken = new Oauth2AccessToken();
			accessToken.setUid(_uid);
			accessToken.setExpiresIn(_expires);
			accessToken.setRefreshToken(_refreshToken);
			accessToken.setToken(_accessToken);
			UsersAPI mUsersAPI = new UsersAPI(_activity, mAuthInfo.getAppKey(), accessToken);
			mUsersAPI.show(Long.parseLong(accessToken.getUid()), new MyRequestListener(_scriptEngine, _callbackFuncName));
		}

	}

	/**
	 * 微博 OpenAPI 回调接口。
	 */
	private class MyRequestListener implements RequestListener {
		private DoIScriptEngine scriptEngine;
		private String callbackFuncName;
		private DoInvokeResult invokeResult;

		public MyRequestListener(DoIScriptEngine _scriptEngine, String _callbackFuncName) {
			this.scriptEngine = _scriptEngine;
			this.callbackFuncName = _callbackFuncName;
			invokeResult = new DoInvokeResult(do_SinaWeiBo_Model.this.getUniqueKey());
		}

		@Override
		public void onComplete(String response) {
			if (!TextUtils.isEmpty(response)) {
				invokeResult.setResultText(response);
				scriptEngine.callback(callbackFuncName, invokeResult);
			}
		}

		@Override
		public void onWeiboException(WeiboException e) {
			DoServiceContainer.getLogEngine().writeError(" do_SinaWeiBo getUserInfo \n\t", e);
			ErrorInfo info = ErrorInfo.parse(e.getMessage());
			invokeResult.setError(info.toString());
			scriptEngine.callback(callbackFuncName, invokeResult);
		}
	};

	/**
	 * 使用sina微博登录；
	 * 
	 * @throws Exception
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void login(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _appId = DoJsonHelper.getString(_dictParas,"appId", "");
		if (TextUtils.isEmpty(_appId))
			throw new Exception("appId 不能为空");
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		// 创建授权认证信息
		if (mAuthInfo == null) {
			mAuthInfo = new AuthInfo(_activity, _appId, Constants.REDIRECT_URL, Constants.SCOPE);
		}

		if (null == mSsoHandler && mAuthInfo != null) {
			mSsoHandler = new SsoHandler(_activity, mAuthInfo);
		}

		if (mSsoHandler != null) {
			mSsoHandler.authorize(new AuthListener(_activity, _scriptEngine, _callbackFuncName));
		}
	}

	/**
	 * 登入按钮的监听器，接收授权结果。
	 */
	private class AuthListener implements WeiboAuthListener {
		private DoIScriptEngine scriptEngine;
		private String callbackFuncName;
		private DoInvokeResult invokeResult;
		private Activity activity;

		public AuthListener(Activity _activity, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
			this.scriptEngine = _scriptEngine;
			this.callbackFuncName = _callbackFuncName;
			invokeResult = new DoInvokeResult(do_SinaWeiBo_Model.this.getUniqueKey());
			this.activity = _activity;
		}

		@Override
		public void onComplete(Bundle values) {
			if (null == values || values.size() == 0) {
				Toast.makeText(activity, "登录失败", Toast.LENGTH_SHORT).show();
				return;
			}

			try {
				JSONObject _value = new JSONObject();
				_value.put("uid", values.getString("uid"));
				_value.put("access_token", values.getString("access_token"));
				_value.put("refresh_token", values.getString("refresh_token"));
				_value.put("expires_in", values.getString("expires_in"));
				invokeResult.setResultNode(_value);
			} catch (Exception e) {
				invokeResult.setException(e);
			} finally {
				scriptEngine.callback(callbackFuncName, invokeResult);
			}

			Oauth2AccessToken accessToken = Oauth2AccessToken.parseAccessToken(values);
			if (accessToken != null && accessToken.isSessionValid()) {
				AccessTokenKeeper.writeAccessToken(activity.getApplicationContext(), accessToken);
			}
		}

		@Override
		public void onWeiboException(WeiboException e) {
			DoServiceContainer.getLogEngine().writeError(" do_SinaWeiBo login \n\t", e);
			ErrorInfo info = ErrorInfo.parse(e.getMessage());
			invokeResult.setError(info.toString());
			scriptEngine.callback(callbackFuncName, invokeResult);
		}

		@Override
		public void onCancel() {
			Toast.makeText(activity, "取消授权", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 注销；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void logout(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		new LogoutAPI(_activity, mAuthInfo.getAppKey(), AccessTokenKeeper.readAccessToken(_activity)).logout(new LogOutRequestListener(_activity, _scriptEngine, _callbackFuncName));
	}

	/**
	 * 登出按钮的监听器，接收登出处理结果。（API 请求结果的监听器）
	 */
	private class LogOutRequestListener implements RequestListener {

		private Activity activity;
		private DoIScriptEngine scriptEngine;
		private String callbackFuncName;
		private DoInvokeResult invokeResult;

		public LogOutRequestListener(Activity _activity, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
			this.scriptEngine = _scriptEngine;
			this.callbackFuncName = _callbackFuncName;
			invokeResult = new DoInvokeResult(do_SinaWeiBo_Model.this.getUniqueKey());
			this.activity = _activity;
		}

		@Override
		public void onComplete(String response) {
			if (!TextUtils.isEmpty(response)) {
				try {
					JSONObject obj = new JSONObject(response);
					String value = obj.getString("result");
					if ("true".equalsIgnoreCase(value)) {
						invokeResult.setResultBoolean(true);
						AccessTokenKeeper.clear(activity);
					} else {
						invokeResult.setResultBoolean(false);
					}
				} catch (Exception ex) {
					invokeResult.setResultBoolean(false);
				} finally {
					scriptEngine.callback(callbackFuncName, invokeResult);
				}
			}
		}

		@Override
		public void onWeiboException(WeiboException e) {
		}
	}

	@Override
	public void share(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _appId = DoJsonHelper.getString(_dictParas,"appId", "");
		if (TextUtils.isEmpty(_appId))
			throw new Exception("appId 不能为空");
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		// 创建微博 SDK 接口实例
		mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(_activity, _appId);
		// 注册到新浪微博
		mWeiboShareAPI.registerApp();

		int _shareType = DoJsonHelper.getInt(_dictParas,"type", 0); // 分享的类型 0：默认，图文分享；1：网页分享；2：音乐分享；3：视频分享；4：音频分享；
		String _title = DoJsonHelper.getString(_dictParas,"title", ""); // 标题 分享的标题, 最长30个字符
		String _actionUrl = DoJsonHelper.getString(_dictParas,"url", ""); // 文件的远程链接, 以URL的形式传入
		String _imageUrl = DoJsonHelper.getString(_dictParas,"image", ""); // 图片地址 分享后显示的图片
		String _summary = DoJsonHelper.getString(_dictParas,"summary", ""); // 摘要分享的消息摘要，最长40个字

		Bitmap _image = getBitmap(_imageUrl, _scriptEngine);
		WeiboMultiMessage _weiboMessage = null;
		switch (_shareType) {
		case 1: // 网页分享
			_weiboMessage = new WeiboMultiMessage();
			WebpageObject _webpageObject = new WebpageObject();
			_webpageObject.identify = Utility.generateGUID();
			_webpageObject.title = _title;
			_webpageObject.description = _summary;
			_webpageObject.setThumbImage(_image);
			_webpageObject.actionUrl = _actionUrl;
			_webpageObject.defaultText = "Webpage 默认文本";
			_weiboMessage.mediaObject = _webpageObject;
			break;
		case 2: // 音乐分享
			_weiboMessage = new WeiboMultiMessage();
			MusicObject _musicObject = new MusicObject();
			_musicObject.identify = Utility.generateGUID();
			_musicObject.title = _title;
			_musicObject.description = _summary;
			_musicObject.setThumbImage(_image);
			_musicObject.actionUrl = _actionUrl;
			_musicObject.dataUrl = "www.weibo.com";
			_musicObject.dataHdUrl = "www.weibo.com";
			_musicObject.duration = 10;
			_musicObject.defaultText = "Music 默认文案";
			_weiboMessage.mediaObject = _musicObject;
			break;
		case 3: // 视频分享
			_weiboMessage = new WeiboMultiMessage();

			VideoObject _videoObject = new VideoObject();
			_videoObject.identify = Utility.generateGUID();
			_videoObject.title = _title;
			_videoObject.description = _summary;
			_videoObject.setThumbImage(_image);
			_videoObject.actionUrl = _actionUrl;
			_videoObject.dataUrl = "www.weibo.com";
			_videoObject.dataHdUrl = "www.weibo.com";
			_videoObject.duration = 10;
			_videoObject.defaultText = "Video 默认文案";
			_weiboMessage.mediaObject = _videoObject;

			break;
		case 4: // 音频分享
			_weiboMessage = new WeiboMultiMessage();

			VoiceObject _voiceObject = new VoiceObject();
			_voiceObject.identify = Utility.generateGUID();
			_voiceObject.title = _title;
			_voiceObject.description = _summary;
			_voiceObject.setThumbImage(_image);
			_voiceObject.actionUrl = _actionUrl;
			_voiceObject.dataUrl = "www.weibo.com";
			_voiceObject.dataHdUrl = "www.weibo.com";
			_voiceObject.duration = 10;
			_voiceObject.defaultText = "Voice 默认文案";
			_weiboMessage.mediaObject = _voiceObject;

			break;
		default:
			_weiboMessage = new WeiboMultiMessage();
			TextObject _textObject = new TextObject();
			_textObject.text = _title;
			_weiboMessage.textObject = _textObject;
			if (null == DoIOHelper.getHttpUrlPath(_imageUrl)) {
				_imageUrl = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), _imageUrl);
			} else {
				throw new Exception("纯图分享，只支持选择本地图片");
			}
			if (!TextUtils.isEmpty(_imageUrl)) {
				ImageObject _imageObject = new ImageObject(); // 只支持本地的图片
				_imageObject.imagePath = _imageUrl;
				_weiboMessage.imageObject = _imageObject;
			}
			break;
		}

		sendMessage(_scriptEngine, _callbackFuncName, _weiboMessage, _activity, _appId);
	}

	/**
	 * 第三方应用发送请求消息到微博，唤起微博分享界面。 注意：当
	 * {@link IWeiboShareAPI#getWeiboAppSupportAPI()} >= 10351 时，支持同时分享多条消息，
	 * 同时可以分享文本、图片以及其它媒体资源（网页、音乐、视频、声音中的一种）。
	 */
	private void sendMessage(final DoIScriptEngine _scriptEngine, final String _callbackFuncName, WeiboMultiMessage weiboMessage, final Activity activity, String appKey) {
		// 2. 初始化从第三方到微博的消息请求
		SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
		// 用transaction唯一标识一个请求
		request.transaction = String.valueOf(System.currentTimeMillis());
		request.multiMessage = weiboMessage;

		// 3. 发送请求消息到微博，唤起微博分享界面
//		if (mWeiboShareAPI.isWeiboAppInstalled()) {
//			mWeiboShareAPI.sendRequest(activity, request);
//		} else {
			AuthInfo authInfo = new AuthInfo(activity, appKey, Constants.REDIRECT_URL, Constants.SCOPE);
			Oauth2AccessToken accessToken = AccessTokenKeeper.readAccessToken(activity.getApplicationContext());
			String token = "";
			if (accessToken != null) {
				token = accessToken.getToken();
			}
	
			final DoInvokeResult _invokeResult = new DoInvokeResult(do_SinaWeiBo_Model.this.getUniqueKey());
			mWeiboShareAPI.sendRequest(activity, request, authInfo, token, new WeiboAuthListener() {
				@Override
				public void onWeiboException(WeiboException arg0) {
					_invokeResult.setResultBoolean(false);
					_scriptEngine.callback(_callbackFuncName, _invokeResult);
				}
	
				@Override
				public void onComplete(Bundle bundle) {
					Oauth2AccessToken newToken = Oauth2AccessToken.parseAccessToken(bundle);
					AccessTokenKeeper.writeAccessToken(activity.getApplicationContext(), newToken);
					_invokeResult.setResultBoolean(true);
					_scriptEngine.callback(_callbackFuncName, _invokeResult);
				}
	
				@Override
				public void onCancel() {
	
				}
			});
//		}
	}

	private Bitmap getBitmap(String _imageUrl, DoIScriptEngine _scriptEngine) throws Exception {
		if (null == DoIOHelper.getHttpUrlPath(_imageUrl)) {
			_imageUrl = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), _imageUrl);
		} else {
			throw new Exception("纯图分享，只支持选择本地图片");
		}
		if (TextUtils.isEmpty(_imageUrl)) {
			throw new Exception("分享图片不能为空");
		}
		return revitionImageSize(_imageUrl, 768, 1024);
	}

	private Bitmap revitionImageSize(String path, int maxWidth, int maxHeight) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(path)));
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(in, null, options);
		in.close();
		int i = 0;
		Bitmap bitmap = null;
		while (true) {
			if ((options.outWidth >> i <= maxWidth) && (options.outHeight >> i <= maxHeight)) {
				in = new BufferedInputStream(new FileInputStream(new File(path)));
				options.inSampleSize = (int) Math.pow(2.0D, i);
				options.inJustDecodeBounds = false;
				bitmap = BitmapFactory.decodeStream(in, null, options);
				break;
			}
			i += 1;
		}
		return bitmap;
	}
}