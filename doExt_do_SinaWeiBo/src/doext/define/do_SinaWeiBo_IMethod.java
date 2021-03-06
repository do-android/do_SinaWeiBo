package doext.define;

import org.json.JSONObject;
import core.interfaces.DoIScriptEngine;

/**
 * 声明自定义扩展组件方法
 */
public interface do_SinaWeiBo_IMethod {
	void getUserInfo(JSONObject _dictParas,DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception ;
	void login(JSONObject _dictParas,DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception ;
	void logout(JSONObject _dictParas,DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception ;
	void share(JSONObject _dictParas,DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception ;
}