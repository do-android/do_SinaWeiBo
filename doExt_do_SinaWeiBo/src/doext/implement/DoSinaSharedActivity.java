package doext.implement;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;

import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.MusicObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.VideoObject;
import com.sina.weibo.sdk.api.VoiceObject;
import com.sina.weibo.sdk.api.WebpageObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.constant.WBConstants;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.utils.Utility;

import core.DoServiceContainer;
import doext.sina.weibo.openapi.AccessTokenKeeper;
import doext.sina.weibo.openapi.Constants;

public class DoSinaSharedActivity extends Activity implements IWeiboHandler.Response {
	/** 微博微博分享接口实例 */
	private IWeiboShareAPI mWeiboShareAPI = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String appId = getIntent().getStringExtra("appId");
		// 创建微博分享接口实例
		mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(this, appId);
		mWeiboShareAPI.registerApp();
		int _shareType = getIntent().getIntExtra("type", 0);
		String _title = getIntent().getStringExtra("title");
		String _actionUrl = getIntent().getStringExtra("url");
		String _imageUrl = getIntent().getStringExtra("image");
		String _summary = getIntent().getStringExtra("summary");
		try {
			Bitmap _image = getBitmap(_imageUrl);
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
				if (!TextUtils.isEmpty(_imageUrl)) {
					ImageObject _imageObject = new ImageObject(); // 只支持本地的图片
					_imageObject.imagePath = _imageUrl;
					_weiboMessage.imageObject = _imageObject;
				}
				break;
			}
			sendMessage(_weiboMessage, appId);
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("01DoSinaSharedActivity", e);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		mWeiboShareAPI.handleWeiboResponse(intent, this);
	}

	@Override
	public void onResponse(BaseResponse baseResp) {
		if (baseResp != null) {
			Intent intent = new Intent();
			switch (baseResp.errCode) {
			case WBConstants.ErrorCode.ERR_OK:
				intent.putExtra("result", "ERR_OK");
				break;
			case WBConstants.ErrorCode.ERR_CANCEL:
				intent.putExtra("result", "ERR_CANCEL");
				break;
			case WBConstants.ErrorCode.ERR_FAIL:
				intent.putExtra("result", "ERR_FAIL");
				break;
			}
			DoSinaSharedActivity.this.setResult(200, intent);
			DoSinaSharedActivity.this.finish();
		}
	}

	/**
	 * 第三方应用发送请求消息到微博，唤起微博分享界面。 注意：当
	 * {@link IWeiboShareAPI#getWeiboAppSupportAPI()} >= 10351 时，支持同时分享多条消息，
	 * 同时可以分享文本、图片以及其它媒体资源（网页、音乐、视频、声音中的一种）。
	 */
	private void sendMessage(WeiboMultiMessage weiboMessage, String appKey) {
		SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
		// 用transaction唯一标识一个请求
		request.transaction = String.valueOf(System.currentTimeMillis());
		request.multiMessage = weiboMessage;

		AuthInfo authInfo = new AuthInfo(DoSinaSharedActivity.this, appKey, Constants.REDIRECT_URL, Constants.SCOPE);
		Oauth2AccessToken accessToken = AccessTokenKeeper.readAccessToken(DoSinaSharedActivity.this);
		String token = "";
		if (accessToken != null) {
			token = accessToken.getToken();
		}

		mWeiboShareAPI.sendRequest(DoSinaSharedActivity.this, request, authInfo, token, new WeiboAuthListener() {
			@Override
			public void onWeiboException(WeiboException arg0) {

			}

			@Override
			public void onComplete(Bundle bundle) {
				Oauth2AccessToken newToken = Oauth2AccessToken.parseAccessToken(bundle);
				AccessTokenKeeper.writeAccessToken(DoSinaSharedActivity.this, newToken);
			}

			@Override
			public void onCancel() {

			}
		});
	}

	private Bitmap getBitmap(String _imageUrl) throws Exception {
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
