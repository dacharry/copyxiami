package com.sheepm.activity;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.sheepm.Utils.Constants;
import com.sheepm.Utils.MediaUtil;
import com.sheepm.Utils.OtherUtil;
import com.sheepm.application.Myapp;
import com.sheepm.bean.Mp3Info;
import com.sheepm.copyxiami.R;
import com.sheepm.service.MusicService;

/**
 * 显示歌词的activity
 * 
 * @author sheepm
 * 
 */
public class MusicActivity extends Activity implements OnClickListener,
		OnSeekBarChangeListener {

	private static final String Tag = "MusicActivity";

	private ImageView mPlayState;
	private ImageView mMusicPrv;
	private ImageView mMusicPlay;
	private ImageView mMusicNext;

	private MusicReceiver receiver;

	private int[] playstyle = new int[] { R.drawable.state_random,
			R.drawable.state_list, R.drawable.state_single };

	private String[] playtext = new String[] { "随机播放", "列表循环", "单曲循环" };

//	private LyricFragment lyricFragment = new LyricFragment();
	List<Mp3Info> mp3Infos;
	private LinearLayout mLinearMusic;

	private Toast mToast;

	private boolean isFirst = true;
	private int position;

	private SeekBar mSeekBar;
	private long mDuration = 0;

	private TextView mTxtDuration;
	private TextView mTexting;
	boolean isTrue = true;
	private ImageView mImgAlbum;
	private TextView mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {  
            Window window = getWindow();  
            //透明状态栏  
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);  
            //透明导航栏  
//            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);  
        }    
		setContentView(R.layout.activity_music);
		initView();
		regFilter();
		registerListener();
		setMusicBg(position);
//		setDefaultFragment();
		new LooperThread().start(); // 异步线程更新ui
	}

	/**
	 * 设置广播监听拦截器
	 */
	private void regFilter() {
		IntentFilter filter = new IntentFilter();
		receiver = new MusicReceiver();
		filter.addAction(Constants.ACTION_LIST_SEARCH);
		filter.addAction(Constants.ACTION_NEXT);
		filter.addAction(Constants.ACTION_PAUSE);
		filter.addAction(Constants.ACTION_PLAY);
		filter.addAction(Constants.ACTION_PRV);
		filter.addAction(Constants.ACTION_SEEK);
		filter.setPriority(800);
		registerReceiver(receiver, filter);
	}

	/**
	 * 初始化控件及加载music数据
	 */
	private void initView() {
		mLinearMusic = (LinearLayout) findViewById(R.id.linear_music);
		position = this.getIntent().getIntExtra("position", 10000);
		Log.i("---"+Tag, "initview"+position);
		mp3Infos = MediaUtil.getMp3Infos(getApplicationContext());
		mSeekBar = (SeekBar) findViewById(R.id.seekBar1);
		mTxtDuration = (TextView) findViewById(R.id.text_duration);
		mTexting = (TextView) findViewById(R.id.texting);
		mPlayState = (ImageView) findViewById(R.id.play_state);
		mPlayState.setImageResource(playstyle[Myapp.state % 3]);
		mMusicPrv = (ImageView) findViewById(R.id.music_prv);
		mMusicPlay = (ImageView) findViewById(R.id.music_play);
		if (Myapp.isPlay) {
			mMusicPlay.setImageResource(R.drawable.lock_suspend);
		} else {
			mMusicPlay.setImageResource(R.drawable.lock_play);
		}
		mMusicNext = (ImageView) findViewById(R.id.music_next);
		mTitle =(TextView) findViewById(R.id.text_title);
		mImgAlbum = (ImageView) findViewById(R.id.album_img);
	}

	/**
	 * 设置事件监听
	 */
	private void registerListener() {
		mPlayState.setOnClickListener(this);
		mMusicPrv.setOnClickListener(this);
		mMusicPlay.setOnClickListener(this);
		mMusicNext.setOnClickListener(this);
		mSeekBar.setOnSeekBarChangeListener(this);
	}

	/**
	 * 设置该activity的背景
	 */
	private void setMusicBg(int position2) {
		Log.i("---"+Tag, ""+ position2);
		long album_id = mp3Infos.get(position2).getAlbumId();
		long song_id = mp3Infos.get(position2).getId();
		Bitmap artwork = MediaUtil.getArtwork(getApplicationContext(), song_id,
				album_id, true, false);
		Bitmap bitmap = OtherUtil.fastblur(artwork, 40);
		Drawable drawable = new BitmapDrawable(bitmap);
		mLinearMusic.setBackgroundDrawable(drawable);
		mDuration = mp3Infos.get(position2).getDuration();
		mTxtDuration.setText(MediaUtil.formatTime(mDuration));
		mTitle.setText(mp3Infos.get(position2).getTitle());
		mImgAlbum.setImageBitmap(artwork);
	}

//	private void setDefaultFragment() {
//		FragmentManager manager = getFragmentManager();
//		FragmentTransaction transaction = manager.beginTransaction();
//		transaction.replace(R.id.display, lyricFragment);
//		transaction.commit();
//	}

	public class MusicReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.ACTION_NEXT)) {
				Myapp.isPlay = true;
				isFirst = false;
				if ((Myapp.state % 3) == 1 || (Myapp.state % 3) == 2) {
					if (position < mp3Infos.size() - 1) {
						++position;
					} else {
						position = 0;
					}
				} else if ((Myapp.state % 3) == 0) {
					position = Myapp.position;
				}
				Log.i("---position", "" + position);
				setMusicBg(position);
				Message message = Message.obtain();
				message.obj = mp3Infos.get(position);
				handler.sendMessage(message);
			} else if (intent.getAction().equals(Constants.ACTION_PAUSE)) {
				Message message = Message.obtain();
				message.obj = mp3Infos.get(position);
				handler.sendMessage(message);
			} else if (intent.getAction().equals(Constants.ACTION_PLAY)) {
				if (isFirst) {
					isFirst = false;
					Myapp.isPlay = true;
					Message message = Message.obtain();
					message.obj = mp3Infos.get(position);
					handler.sendMessage(message);

				}
			} else if (intent.getAction().equals(Constants.ACTION_PRV)) {
				Myapp.isPlay = true;
				isFirst = false;

				if ((Myapp.state % 3) == 1 || (Myapp.state % 3) == 2) {
					if (position == 0) {
						position = mp3Infos.size() - 1;
					} else {
						--position;
					}
				} else if ((Myapp.state % 3) == 0) {
					position = Myapp.position;
				}
				setMusicBg(position);
				Message message = Message.obtain();
				message.obj = mp3Infos.get(position);
				handler.sendMessage(message);
			} else if (intent.getAction().equals(Constants.ACTION_SEEK)) {
				int progress = intent.getIntExtra("progress", 0);
				Log.i("---" + Tag, "" + progress);
				long duration = MusicService.getDuration();
				Log.i("---" + Tag, "" + duration);
				int to = progress * (int) duration / 100;
				Log.i("---" + Tag, "" + to);
				MusicService.player.seekTo(to);
			}
		}

	}

	/**
	 * 设置播放按钮的状态
	 */
	private Handler handler = new Handler() {

		public void handleMessage(android.os.Message msg) {

			Mp3Info info = (Mp3Info) msg.obj;
			if (Myapp.isPlay) {
				mMusicPlay.setImageResource(R.drawable.lock_suspend);
			} else {
				mMusicPlay.setImageResource(R.drawable.lock_play);
			}
			long duration = info.getDuration();
			String text = MediaUtil.formatTime(duration);
			mTxtDuration.setText(text);
			mTitle.setText(info.getTitle());
		};
	};

	private Handler handler2 = new Handler() {

		public void handleMessage(Message msg) {

			if (MusicService.isPlaying) {
				long current = MusicService.getCurrent();
				long duration = MusicService.getDuration();
				String text_current = MediaUtil.formatTime(current);
				mTexting.setText(text_current);
				long a = 100L * current / duration;
				int progress = new Long(a).intValue();
				mSeekBar.setProgress(progress);
			}
		};

	};

	class LooperThread extends Thread {

		@Override
		public void run() {

			while (isTrue) {
				try {
					handler2.sendMessage(new Message());
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.play_state:
			++Myapp.state;
			switch (Myapp.state % 3) {
			case 0:
				mPlayState.setImageResource(playstyle[0]);
				ShowToast(playtext[0]);
				break;
			case 1:
				mPlayState.setImageResource(playstyle[1]);
				ShowToast(playtext[1]);
				break;
			case 2:
				mPlayState.setImageResource(playstyle[2]);
				ShowToast(playtext[2]);
				break;

			}
			break;

		case R.id.music_prv:
			Intent intent_prv = new Intent();
			intent_prv.setAction(Constants.ACTION_PRV);
			sendBroadcast(intent_prv);
			break;
		case R.id.music_play:
			if (mMusicPlay
					.getDrawable()
					.getConstantState()
					.equals(getResources().getDrawable(R.drawable.lock_suspend)
							.getConstantState())) {
				Intent broadcast = new Intent();
				broadcast.setAction(Constants.ACTION_PAUSE);
				sendBroadcast(broadcast);
				mMusicPlay.setImageResource(R.drawable.lock_play);
			} else if (mMusicPlay
					.getDrawable()
					.getConstantState()
					.equals(getResources().getDrawable(R.drawable.lock_play)
							.getConstantState())) {
				Intent broadcast = new Intent();
				broadcast.setAction(Constants.ACTION_PLAY);
				sendBroadcast(broadcast);
				mMusicPlay.setImageResource(R.drawable.lock_suspend);
			}
			break;

		case R.id.music_next:
			Intent intent_next = new Intent();
			intent_next.setAction(Constants.ACTION_NEXT);
			sendBroadcast(intent_next);
			break;

		default:
			break;
		}
	}

	public void ShowToast(String text) {
		if (mToast == null) {
			mToast = Toast.makeText(MusicActivity.this, text,
					Toast.LENGTH_SHORT);
			mToast.show();
		} else {
			mToast.setText(text);
			mToast.show();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// Log.i("---" + Tag, "" + MusicService.getCurrent());
		// Log.i("---" + Tag, "" + MusicService.getDuration());
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		int progress = seekBar.getProgress();
		Log.i("---" + Tag, "" + progress);
		Intent intent = new Intent();
		intent.setAction(Constants.ACTION_SEEK);
		intent.putExtra("progress", progress);
		sendBroadcast(intent);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		// 按返回键时将isTrue设为false，让线程不再继续
		isTrue = false;
		finish();
	}

}
