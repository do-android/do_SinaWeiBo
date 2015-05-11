package doext.implement;

import org.json.JSONObject;

import android.app.Activity;
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
import core.helper.jsonparse.DoJsonNode;
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
	public boolean invokeSyncMethod(String _methodName, DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
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
	public boolean invokeAsyncMethod(String _methodName, DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("login".equals(_methodName)) {
			this.login(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("getUserInfo".equals(_methodName)) {
			this.getUserInfo(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("logout".equals(_methodName)) {
			this.logout(_dictParas, _scriptEngine, _callbackFuncName);
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
	public void getUserInfo(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _uid = _dictParas.getOneText("uid", "");
		String _accessToken = _dictParas.getOneText("accessToken", "");
		String _refreshToken = _dictParas.getOneText("refreshToken", "");
		String _expires = _dictParas.getOneText("expires", "");
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
	public void login(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _appId = _dictParas.getOneText("appId", "");
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

			DoJsonNode _value = new DoJsonNode();
			_value.setOneText("uid", values.getString("uid"));
			_value.setOneText("access_token", values.getString("access_token"));
			_value.setOneText("refresh_token", values.getString("refresh_token"));
			_value.setOneText("expires_in", values.getString("expires_in"));

			try {
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
	public void logout(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
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
}