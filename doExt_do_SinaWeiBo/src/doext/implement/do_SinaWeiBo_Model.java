package doext.implement;

import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;

import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.interfaces.DoActivityResultListener;
import core.interfaces.DoIPageView;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import core.object.DoSingletonModule;
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
public class do_SinaWeiBo_Model extends DoSingletonModule implements do_SinaWeiBo_IMethod, DoActivityResultListener {

	private AuthInfo mAuthInfo;
	/** SSO 授权认证实例 */
	private SsoHandler mSsoHandler;

	private DoIPageView doActivity;

	public do_SinaWeiBo_Model() throws Exception {
		super();
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
		String _uid = DoJsonHelper.getString(_dictParas, "uid", "");
		String _accessToken = DoJsonHelper.getString(_dictParas, "accessToken", "");
		String _refreshToken = DoJsonHelper.getString(_dictParas, "refreshToken", "");
		String _expires = DoJsonHelper.getString(_dictParas, "expires", "");
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
		String _appId = DoJsonHelper.getString(_dictParas, "appId", "");
		if (TextUtils.isEmpty(_appId))
			throw new Exception("appId 不能为空");
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();

		doActivity = _scriptEngine.getCurrentPage().getPageView();
		doActivity.registActivityResultListener(this);
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
				doActivity.unregistActivityResultListener(do_SinaWeiBo_Model.this);
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

	private DoIScriptEngine scriptEngineShared;
	private String callbackFuncName;

	@Override
	public void share(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _appId = DoJsonHelper.getString(_dictParas, "appId", "");
		if (TextUtils.isEmpty(_appId))
			throw new Exception("appId 不能为空");
		this.scriptEngineShared = _scriptEngine;
		this.callbackFuncName = _callbackFuncName;
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		doActivity = _scriptEngine.getCurrentPage().getPageView();
		doActivity.registActivityResultListener(this);
		int _shareType = DoJsonHelper.getInt(_dictParas, "type", 0); // 分享的类型
																		// 0：默认，图文分享；1：网页分享；2：音乐分享；3：视频分享；4：音频分享；
		String _title = DoJsonHelper.getString(_dictParas, "title", ""); // 标题
																			// 分享的标题,
																			// 最长30个字符
		String _actionUrl = DoJsonHelper.getString(_dictParas, "url", ""); // 文件的远程链接,
																			// 以URL的形式传入
		String _imageUrl = DoJsonHelper.getString(_dictParas, "image", ""); // 图片地址
																			// 分享后显示的图片
		String _summary = DoJsonHelper.getString(_dictParas, "summary", ""); // 摘要分享的消息摘要，最长40个字
		if (!_imageUrl.equals("") && null == DoIOHelper.getHttpUrlPath(_imageUrl)) {
			_imageUrl = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), _imageUrl);
		}

		String _packageName = _activity.getPackageName();
		ComponentName _componetName = new ComponentName(_packageName, "doext.implement.DoSinaSharedActivity");
		Intent i = new Intent();
		i.putExtra("appId", _appId);
		i.putExtra("type", _shareType);
		i.putExtra("url", _actionUrl);
		i.putExtra("title", _title);
		i.putExtra("image", _imageUrl);
		i.putExtra("summary", _summary);
		i.setComponent(_componetName);
		_activity.startActivityForResult(i, 200);

	}

	private void sharedResult(boolean sharedTag) {
		DoInvokeResult _invokeResult = new DoInvokeResult(do_SinaWeiBo_Model.this.getUniqueKey());
		_invokeResult.setResultBoolean(sharedTag);
		scriptEngineShared.callback(callbackFuncName, _invokeResult);
		doActivity.unregistActivityResultListener(this);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// SSO 授权回调
		// 重要：发起 SSO 登陆的 Activity 必须重写 onActivityResults
		if (mSsoHandler != null) {
			mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
		}
		if (resultCode == 200) {
			String _result = data.getExtras().getString("result");
			if (_result.equals("ERR_OK")) {
				sharedResult(true);
			} else {
				sharedResult(false);
			}
		}
	}

}