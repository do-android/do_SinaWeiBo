package doext.define;

import core.helper.jsonparse.DoJsonNode;
import core.interfaces.DoIScriptEngine;

/**
 * 声明自定义扩展组件方法
 */
public interface do_SinaWeiBo_IMethod {
	void getUserInfo(DoJsonNode _dictParas,DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception ;
	void login(DoJsonNode _dictParas,DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception ;
	void logout(DoJsonNode _dictParas,DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception ;
}