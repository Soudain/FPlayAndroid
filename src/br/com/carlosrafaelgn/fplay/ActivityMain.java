//
// FPlayAndroid is distributed under the FreeBSD License
//
// Copyright (c) 2013-2014, Carlos Rafael Gimenes das Neves
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
// The views and conclusions contained in the software and documentation are those
// of the authors and should not be interpreted as representing official policies,
// either expressed or implied, of the FreeBSD Project.
//
// https://github.com/carlosrafaelgn/FPlayAndroid
//
package br.com.carlosrafaelgn.fplay;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils.TruncateAt;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import br.com.carlosrafaelgn.fplay.activity.ActivityVisualizer;
import br.com.carlosrafaelgn.fplay.list.Song;
import br.com.carlosrafaelgn.fplay.list.SongList;
import br.com.carlosrafaelgn.fplay.playback.Player;
import br.com.carlosrafaelgn.fplay.ui.BackgroundActivityMonitor;
import br.com.carlosrafaelgn.fplay.ui.BgButton;
import br.com.carlosrafaelgn.fplay.ui.BgListView;
import br.com.carlosrafaelgn.fplay.ui.BgSeekBar;
import br.com.carlosrafaelgn.fplay.ui.CustomContextMenu;
import br.com.carlosrafaelgn.fplay.ui.UI;
import br.com.carlosrafaelgn.fplay.ui.drawable.BorderDrawable;
import br.com.carlosrafaelgn.fplay.ui.drawable.ColorDrawable;
import br.com.carlosrafaelgn.fplay.ui.drawable.TextIconDrawable;
import br.com.carlosrafaelgn.fplay.util.Timer;
import br.com.carlosrafaelgn.fplay.visualizer.AlbumArtVisualizer;
import br.com.carlosrafaelgn.fplay.visualizer.OpenGLVisualizerJni;
import br.com.carlosrafaelgn.fplay.visualizer.SimpleVisualizerJni;
import br.com.carlosrafaelgn.fplay.visualizer.Visualizer;

//
//How to create a ListView using ArrayAdapter in Android
//http://anujarosha.wordpress.com/2011/11/17/how-to-create-a-listview-using-arrayadapter-in-android/
//
//Customizing Android ListView Items with Custom ArrayAdapter
//http://www.ezzylearning.com/tutorial.aspx?tid=1763429
//
//Communicating with the UI Thread
//https://developer.android.com/training/multiple-threads/communicate-ui.html
//
//Difference of px, dp, dip and sp in Android?
//http://stackoverflow.com/questions/2025282/difference-of-px-dp-dip-and-sp-in-android
//
//Supporting Keyboard Navigation
//http://developer.android.com/training/keyboard-input/navigation.html
//
//Why are nested weights bad for performance? Alternatives?
//http://stackoverflow.com/questions/9430764/why-are-nested-weights-bad-for-performance-alternatives
//
//Maintain/Save/Restore scroll position when returning to a ListView
//http://stackoverflow.com/questions/3014089/maintain-save-restore-scroll-position-when-returning-to-a-listview
//
public final class ActivityMain extends ActivityItemView implements Timer.TimerHandler, Player.PlayerObserver, View.OnClickListener, BgSeekBar.OnBgSeekBarChangeListener, BgListView.OnAttachedObserver, BgListView.OnBgListViewKeyDownObserver, ActivityFileSelection.OnFileSelectionListener, BgButton.OnPressingChangeListener {
	private static final int MAX_SEEK = 10000, MNU_ADDSONGS = 100, MNU_CLEARLIST = 101, MNU_LOADLIST = 102, MNU_SAVELIST = 103, MNU_TOGGLECONTROLMODE = 104, MNU_RANDOMMODE = 105, MNU_EFFECTS = 106, MNU_VISUALIZER = 107, MNU_SETTINGS = 108, MNU_EXIT = 109, MNU_SORT_BY_TITLE = 110, MNU_SORT_BY_ARTIST = 111, MNU_SORT_BY_ALBUM = 112, MNU_VISUALIZER_SPECTRUM = 113, MNU_REPEAT = 114, MNU_REPEATONE = 115, MNU_VISUALIZER_BLUETOOTH = 116, MNU_VISUALIZER_LIQUID = 117, MNU_VISUALIZER_SPIN = 118, MNU_VISUALIZER_PARTICLE = 119, MNU_VISUALIZER_IMMERSIVE_PARTICLE = 120, MNU_VISUALIZER_ALBUMART = 121;
	private View vwVolume;
	private TextView lblTitle, lblArtist, lblTrack, lblAlbum, lblLength, lblMsgSelMove;
	private TextIconDrawable lblTitleIcon;
	private BgSeekBar barSeek, barVolume;
	private ViewGroup panelControls, panelSecondary, panelSelection;
	private BgButton btnAdd, btnPrev, btnPlay, btnNext, btnMenu, btnMoveSel, btnRemoveSel, btnCancelSel, btnDecreaseVolume, btnIncreaseVolume, btnVolume;
	private BgListView list;
	private Timer tmrSong, tmrUpdateVolumeDisplay, tmrVolume;
	private int firstSel, lastSel, lastTime, volumeButtonPressed, tmrVolumeInitialDelay, vwVolumeId;
	private boolean playingBeforeSeek, selectCurrentWhenAttached, skipToDestruction, forceFadeOut;//, ignoreAnnouncement;
	private StringBuilder timeBuilder, volumeBuilder;
	public static boolean localeHasBeenChanged;

	@Override
	public CharSequence getTitle() {
		final Song currentSong = Player.currentSong;
		return "FPlay: " + ((currentSong == null) ? getText(R.string.nothing_playing) : currentSong.title);
	}

	private void saveListViewPosition() {
		if (list != null && list.getAdapter() != null) {
			final int i = list.getFirstVisiblePosition();
			if (i < 0)
				return;
			final View v = list.getChildAt(0);
			Player.listFirst = i;
			Player.listTop = ((v == null) ? 0 : v.getTop());
		}
	}
	
	private void restoreListViewPosition(boolean selectCurrent) {
		if (list != null) {
			if (Player.positionToSelect >= 0) {
				list.centerItem(Player.positionToSelect);
				Player.positionToSelect = -1;
				return;
			}
			final int c = Player.songs.getCurrentPosition();
			if (Player.listFirst >= 0 && (!selectCurrent || Player.songs.selecting || Player.songs.moving)) {
				list.setSelectionFromTop(Player.listFirst, Player.listTop);
			} else {
				if (selectCurrent && Player.lastCurrent != c && c >= 0) {
					if (Player.songs.getSelection() != c)
						Player.songs.setSelection(c, true);
					list.centerItem(c);
				} else {
					if (Player.listFirst >= 0)
						list.setSelectionFromTop(Player.listFirst, Player.listTop);
					else
						list.centerItem(Player.songs.getSelection());
				}
			}
			Player.lastCurrent = -1;
		}
	}
	
	private String volumeToString() {
		if (Player.volumeControlType == Player.VOLUME_CONTROL_STREAM) {
			return Integer.toString(Player.getStreamVolume());
		} else {
			int volumeDB = Player.volumeDB;
			if (Player.volumeControlType == Player.VOLUME_CONTROL_DB) {
				if (volumeDB <= Player.MIN_VOLUME_DB)
					return "-\u221E dB";
				if (volumeDB >= 0)
					return "-0.00 dB";
				volumeDB = -volumeDB;
				volumeBuilder.delete(0, volumeBuilder.length());
				volumeBuilder.append('-');
				volumeBuilder.append(volumeDB / 100);
				volumeBuilder.append('.');
				volumeDB %= 100;
				if (volumeDB < 10)
					volumeBuilder.append('0');
				volumeBuilder.append(volumeDB);
				volumeBuilder.append(" dB");
				return volumeBuilder.toString();
			}
			volumeBuilder.delete(0, volumeBuilder.length());
			volumeBuilder.append(Player.getGenericVolumePercent());
			volumeBuilder.append('%');
			return volumeBuilder.toString();
		}
	}
	
	private void setVolumeIcon() {
		if (btnVolume != null) {
			final int max;
			final int v;
			switch (Player.volumeControlType) {
			case Player.VOLUME_CONTROL_STREAM:
				max = Player.getStreamMaxVolume();
				v = Player.getStreamVolume();
				break;
			case Player.VOLUME_CONTROL_DB:
			case Player.VOLUME_CONTROL_PERCENT:
				max = -Player.MIN_VOLUME_DB;
				v = max + Player.volumeDB;
				break;
			default:
				btnVolume.setText(UI.ICON_VOLUME4);
				return;
			}
			if (v == max)
				btnVolume.setText(UI.ICON_VOLUME4);
			else if (v == 0)
				btnVolume.setText(UI.ICON_VOLUME0);
			else if (v > ((max + 1) >> 1))
				btnVolume.setText(UI.ICON_VOLUME3);
			else if (v > (max >> 2))
				btnVolume.setText(UI.ICON_VOLUME2);
			else
				btnVolume.setText(UI.ICON_VOLUME1);
		}
	}
	
	private void updateVolumeDisplay() {
		if (barVolume != null) {
			barVolume.setValue((Player.volumeControlType == Player.VOLUME_CONTROL_STREAM) ? Player.getStreamVolume() : ((Player.volumeDB - Player.MIN_VOLUME_DB) / 5));
			barVolume.setText(volumeToString());
		} else {
			setVolumeIcon();
		}
	}
	
	private void startSelecting() {
		if (firstSel >= 0) {
			if (UI.isLargeScreen) {
				final ViewGroup.LayoutParams p = lblMsgSelMove.getLayoutParams();
				if (p.height != UI.defaultControlSize) {
					p.height = UI.defaultControlSize;
					lblMsgSelMove.setLayoutParams(p);
				}
			} else if (!UI.isLandscape) {
				final int h = (UI.defaultControlSize << 1) + panelControls.getPaddingBottom() + panelSecondary.getPaddingBottom();
				final ViewGroup.LayoutParams p = panelSelection.getLayoutParams();
				if (p != null && p.height != h) {
					p.height = h;
					panelSelection.setLayoutParams(p);
				}
			}
			btnMoveSel.setVisibility(View.VISIBLE);
			btnRemoveSel.setVisibility(View.VISIBLE);
			btnCancelSel.setText(R.string.cancel);
			panelControls.setVisibility(View.GONE);
			panelSecondary.setVisibility(View.GONE);
			panelSelection.setVisibility(View.VISIBLE);
			lblMsgSelMove.setText(R.string.msg_sel);
			if (!UI.isLargeScreen)
				lblTitle.setVisibility(View.GONE);
			lblMsgSelMove.setVisibility(View.VISIBLE);
			lblMsgSelMove.setSelected(true);
			if (UI.isLargeScreen) {
				btnCancelSel.setNextFocusLeftId(R.id.btnRemoveSel);
				UI.setNextFocusForwardId(list, R.id.btnMoveSel);
			} else if (UI.isLandscape) {
				btnCancelSel.setNextFocusUpId(R.id.btnRemoveSel);
				UI.setNextFocusForwardId(list, R.id.btnMoveSel);
			} else {
				btnCancelSel.setNextFocusRightId(R.id.btnMoveSel);
				UI.setNextFocusForwardId(btnCancelSel, R.id.btnMoveSel);
				UI.setNextFocusForwardId(list, R.id.btnCancelSel);
			}
			Player.songs.selecting = true;
			Player.songs.moving = false;
			list.skipUpDownTranslation = true;
		}
	}
	
	private void startMovingSelection() {
		if (Player.songs.getFirstSelectedPosition() >= 0) {
			btnMoveSel.setVisibility(View.INVISIBLE);
			btnRemoveSel.setVisibility(View.INVISIBLE);
			btnCancelSel.setText(R.string.done);
			lblMsgSelMove.setText(R.string.msg_move);
			lblMsgSelMove.setSelected(true);
			if (UI.isLargeScreen) {
				btnCancelSel.setNextFocusLeftId(R.id.list);
			} else if (UI.isLandscape) {
				btnCancelSel.setNextFocusUpId(R.id.list);
			} else {
				btnCancelSel.setNextFocusRightId(R.id.list);
				UI.setNextFocusForwardId(btnCancelSel, R.id.list);
			}
			UI.setNextFocusForwardId(list, R.id.btnCancelSel);
			Player.songs.selecting = false;
			Player.songs.moving = true;
			list.skipUpDownTranslation = true;
		}
	}
	
	private void removeSelection() {
		if (Player.songs.getFirstSelectedPosition() >= 0) {
			Player.songs.removeSelection();
			cancelSelection(true);
		} else {
			list.skipUpDownTranslation = false;
		}
	}
	
	private void cancelSelection(boolean removed) {
		if (list.isInTouchMode()) {
			final int p = Player.songs.getCurrentPosition();
			if (p >= 0 && p < Player.songs.getCount())
				Player.songs.setSelection(p, true);
			else
				Player.songs.setSelection(-1, true);
		} else if (!removed && firstSel >= 0 && lastSel >= 0) {
			int p = Player.songs.getSelection();
			if (p < 0)
				p = ((firstSel < lastSel) ? Player.songs.getFirstSelectedPosition() : Player.songs.getLastSelectedPosition());
			Player.songs.setSelection(p, false);
		}
		Player.songs.selecting = false;
		Player.songs.moving = false;
		list.skipUpDownTranslation = false;
		firstSel = -1;
		lastSel = -1;
		lblMsgSelMove.setVisibility(View.GONE);
		if (!UI.isLargeScreen)
			lblTitle.setVisibility(View.VISIBLE);
		lblTitle.setSelected(true);
		panelSelection.setVisibility(View.GONE);
		UI.setNextFocusForwardId(list, UI.isLargeScreen ? vwVolumeId : R.id.btnPrev);
		panelControls.setVisibility(View.VISIBLE);
		panelSecondary.setVisibility(View.VISIBLE);
	}
	
	private void bringCurrentIntoView() {
		if (!Player.songs.moving && !Player.songs.selecting) {
			final int p = Player.songs.getCurrentPosition();
			if (p <= list.getFirstVisiblePosition() || p >= list.getLastVisiblePosition())
				list.centerItemSmoothly(p);
		}
	}
	
	private void addSongs(View sourceView) {
		if (Player.state == Player.STATE_INITIALIZED) {
			Player.alreadySelected = false;
			//startActivity(UI.oldBrowserBehavior ? new ActivityBrowser() : new ActivityBrowser2());
			startActivity(new ActivityBrowser2(), 0, sourceView, sourceView != null);
		}
	}
	
	private boolean decreaseVolume() {
		final boolean ret = Player.decreaseVolume();
		setVolumeIcon();
		return ret;
	}
	
	private boolean increaseVolume() {
		final boolean ret = Player.increaseVolume();
		setVolumeIcon();
		return ret;
	}
	
	@Override
	public void onPlayerChanged(Song currentSong, boolean songHasChanged, boolean preparingHasChanged, Throwable ex) {
		final String icon = (Player.playing ? UI.ICON_PAUSE : UI.ICON_PLAY);
		if (btnPlay != null) {
			btnPlay.setText(icon);
			btnPlay.setContentDescription(getText(Player.playing ? R.string.pause : R.string.play));
		}
		if (lblTitleIcon != null)
			lblTitleIcon.setIcon(icon);
		if (songHasChanged) {
			if (lblTitle != null) {
				lblTitle.setText((currentSong == null) ? getText(R.string.nothing_playing) : ((barSeek == null && Player.isCurrentSongPreparing()) ? (getText(R.string.loading) + " " + currentSong.title) : currentSong.title));
				lblTitle.setSelected(true);
				//if (ignoreAnnouncement)
				//	ignoreAnnouncement = false;
				//else if (UI.accessibilityManager != null && UI.accessibilityManager.isEnabled())
				//	UI.announceAccessibilityText(title);
			}
			if (lblArtist != null)
				lblArtist.setText((currentSong == null) ? "-" : currentSong.artist);
			if (lblTrack != null)
				lblTrack.setText((currentSong == null || currentSong.track <= 0) ? "-" : Integer.toString(currentSong.track));
			if (lblAlbum != null)
				lblAlbum.setText((currentSong == null) ? "-" : currentSong.album);
			if (lblLength != null)
				lblLength.setText((currentSong == null) ? "-" : currentSong.length);
		} else if (preparingHasChanged) {
			if (barSeek != null) {
				if (Player.isCurrentSongPreparing() && !barSeek.isTracking()) {
					barSeek.setText(R.string.loading);
					barSeek.setValue(0);
				}
			} else if (lblTitle != null) {
				lblTitle.setText((currentSong == null) ? getText(R.string.nothing_playing) : (Player.isCurrentSongPreparing() ? (getText(R.string.loading) + " " + currentSong.title) : currentSong.title));
				lblTitle.setSelected(true);
			}
		}
		if (Player.playing && !Player.controlMode) {
			if (!tmrSong.isAlive())
				tmrSong.start(250);
		} else {
			tmrSong.stop();
		}
		lastTime = -2;
		handleTimer(tmrSong, null);
	}
	
	@Override
	public void onPlayerControlModeChanged(boolean controlMode) {
		if (Player.songs.selecting || Player.songs.moving)
			cancelSelection(false);
		if (controlMode)
			Player.lastCurrent = Player.songs.getCurrentPosition();
		onCleanupLayout();
		forceFadeOut = !controlMode;
		onCreateLayout(false);
		forceFadeOut = false;
		resume(true);
		System.gc();
	}
	
	@Override
	public void onPlayerGlobalVolumeChanged() {
		updateVolumeDisplay();
	}
	
	@Override
	public void onPlayerAudioSinkChanged(int audioSink) {
		//when changing the output, the global volume usually changes
		if (Player.volumeControlType == Player.VOLUME_CONTROL_STREAM) {
			updateVolumeDisplay();
			if (barVolume != null)
				barVolume.setMax(Player.getStreamMaxVolume());
			tmrUpdateVolumeDisplay.start(750);
		}
	}
	
	@Override
	public void onPlayerMediaButtonPrevious() {
		if (!Player.controlMode)
			bringCurrentIntoView();
	}
	
	@Override
	public void onPlayerMediaButtonNext() {
		if (!Player.controlMode)
			bringCurrentIntoView();
	}
	
	@Override
 	public View getNullContextMenuView() {
		return ((!Player.songs.selecting && !Player.songs.moving && Player.state == Player.STATE_INITIALIZED) ? btnMenu : null);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		if (UI.forcedLocale != UI.LOCALE_NONE && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN && !localeHasBeenChanged) {
			localeHasBeenChanged = true;
			UI.reapplyForcedLocale(getApplication(), getHostActivity());
		}
		UI.prepare(menu);
		menu.add(0, MNU_ADDSONGS, 0, R.string.add_songs)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_FPLAY));
		UI.separator(menu, 0, 1);
		Menu s2, s = menu.addSubMenu(1, 0, 0, R.string.list)
				.setIcon(new TextIconDrawable(UI.ICON_LIST));
		UI.prepare(s);
		s.add(0, MNU_CLEARLIST, 0, R.string.clear_list)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_REMOVE));
		s.add(0, MNU_LOADLIST, 1, R.string.load_list)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_LOAD));
		s.add(0, MNU_SAVELIST, 2, R.string.save_list)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_SAVE));
		UI.separator(s, 0, 3);
		s.add(0, MNU_SORT_BY_TITLE, 4, R.string.sort_by_title)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_MOVE));
		s.add(0, MNU_SORT_BY_ARTIST, 5, R.string.sort_by_artist)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_MOVE));
		s.add(0, MNU_SORT_BY_ALBUM, 6, R.string.sort_by_album)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_MOVE));
		UI.separator(menu, 1, 1);
		menu.add(2, MNU_TOGGLECONTROLMODE, 0, R.string.control_mode)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(Player.controlMode ? UI.ICON_OPTCHK : UI.ICON_OPTUNCHK));
		if (UI.isLandscape && !UI.isLargeScreen) {
			s = menu.addSubMenu(2, 0, 1, R.string.more)
					.setIcon(new TextIconDrawable(UI.ICON_MENU));
			UI.prepare(s);
		} else {
			s = menu;
		}
		if (Player.songs.isRepeatingOne()) {
			s2 = s.addSubMenu(2, 0, 0, getText(R.string.repeat_one) + "\u2026")
				.setIcon(new TextIconDrawable(UI.ICON_REPEATONE));
		} else if (Player.songs.isInRandomMode()) {
			s2 = s.addSubMenu(2, 0, 0, getText(R.string.random_mode) + "\u2026")
				.setIcon(new TextIconDrawable(UI.ICON_SHUFFLE));
		} else {
			s2 = s.addSubMenu(2, 0, 0, getText(R.string.repeat_all) + "\u2026")
				.setIcon(new TextIconDrawable(UI.ICON_REPEAT));
		}
		UI.prepare(s2);
		s2.add(2, MNU_REPEAT, 0, getText(R.string.repeat_all))
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable((Player.songs.isRepeatingOne() || Player.songs.isInRandomMode()) ? UI.ICON_RADIOUNCHK : UI.ICON_RADIOCHK));
		s2.add(2, MNU_REPEATONE, 0, getText(R.string.repeat_one))
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(Player.songs.isRepeatingOne() ? UI.ICON_RADIOCHK : UI.ICON_RADIOUNCHK));
		s2.add(2, MNU_RANDOMMODE, 0, getText(R.string.random_mode))
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(Player.songs.isInRandomMode() ? UI.ICON_RADIOCHK : UI.ICON_RADIOUNCHK));
		UI.separator(s, 2, 1);
		s.add(2, MNU_EFFECTS, 2, R.string.audio_effects)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_EQUALIZER));
		s2 = s.addSubMenu(2, 0, 3, getText(R.string.visualizer) + "\u2026")
			.setIcon(new TextIconDrawable(UI.ICON_VISUALIZER));
		UI.prepare(s2);
		s2.add(2, MNU_VISUALIZER_SPECTRUM, 0, "Spectrum")
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_VISUALIZER));
		s2.add(2, MNU_VISUALIZER_SPIN, 1, "Spinning Rainbow")
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_VISUALIZER));
		s2.add(2, MNU_VISUALIZER_PARTICLE, 2, "Sound Particles")
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_VISUALIZER));
		s2.add(2, MNU_VISUALIZER_IMMERSIVE_PARTICLE, 3, "Into the Particles")
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_3DPAN));
		UI.separator(s2, 2, 4);
		s2.add(2, MNU_VISUALIZER_ALBUMART, 5, R.string.album_art)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_ALBUMART));
		s2.add(2, MNU_VISUALIZER_BLUETOOTH, 6, "Bluetooth + Arduino")
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_BLUETOOTH));
		UI.separator(s2, 2, 7);
		s2.add(2, MNU_VISUALIZER_LIQUID, 8, "Liquid Spectrum")
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_VISUALIZER));
		s2.add(2, MNU_VISUALIZER, 9, "Classic")
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_VISUALIZER));
		s.add(2, MNU_SETTINGS, 4, R.string.settings)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_SETTINGS));
		UI.separator(menu, 2, 5);
		menu.add(3, MNU_EXIT, 0, R.string.exit)
			.setOnMenuItemClickListener(this)
			.setIcon(new TextIconDrawable(UI.ICON_EXIT));
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		final int id = item.getItemId();
		if (id == MNU_EXIT) {
			getHostActivity().setExitOnDestroy(true);
			if (Player.playing)
				Player.playPause();
			finish(0, null, false);
			return true;
		}
		if (Player.state != Player.STATE_INITIALIZED)
			return true;
		switch (item.getItemId()) {
		case MNU_ADDSONGS:
			addSongs(null);
			break;
		case MNU_CLEARLIST:
			Player.songs.clear();
			break;
		case MNU_LOADLIST:
			Player.alreadySelected = false;
			startActivity(new ActivityFileSelection(getText(R.string.load_list), MNU_LOADLIST, false, true, getText(R.string.item_list).toString(), "#lst", this), 0, null, false);
			break;
		case MNU_SAVELIST:
			startActivity(new ActivityFileSelection(getText(R.string.save_list), MNU_SAVELIST, true, false, getText(R.string.item_list).toString(), "#lst", this), 0, null, false);
			break;
		case MNU_SORT_BY_TITLE:
			Player.songs.sort(SongList.SORT_BY_TITLE);
			break;
		case MNU_SORT_BY_ARTIST:
			Player.songs.sort(SongList.SORT_BY_ARTIST);
			break;
		case MNU_SORT_BY_ALBUM:
			Player.songs.sort(SongList.SORT_BY_ALBUM);
			break;
		case MNU_TOGGLECONTROLMODE:
			Player.setControlMode(!Player.controlMode);
			break;
		case MNU_REPEAT:
			Player.songs.setRepeatingOne(false);
			Player.songs.setRandomMode(false);
			break;
		case MNU_REPEATONE:
			Player.songs.setRepeatingOne(true);
			break;
		case MNU_RANDOMMODE:
			Player.songs.setRandomMode(!Player.songs.isInRandomMode());
			break;
		case MNU_EFFECTS:
			startActivity(new ActivityEffects(), 0, null, false);
			break;
		case MNU_VISUALIZER:
			getHostActivity().startActivity((new Intent(getApplication(), ActivityVisualizer.class)).putExtra(Visualizer.EXTRA_VISUALIZER_CLASS_NAME, SimpleVisualizerJni.class.getName()));
			break;
		case MNU_VISUALIZER_SPECTRUM:
			getHostActivity().startActivity((new Intent(getApplication(), ActivityVisualizer.class)).putExtra(Visualizer.EXTRA_VISUALIZER_CLASS_NAME, OpenGLVisualizerJni.class.getName()));
			break;
		case MNU_VISUALIZER_LIQUID:
			getHostActivity().startActivity((new Intent(getApplication(), ActivityVisualizer.class)).putExtra(Visualizer.EXTRA_VISUALIZER_CLASS_NAME, OpenGLVisualizerJni.class.getName()).putExtra(OpenGLVisualizerJni.EXTRA_VISUALIZER_TYPE, OpenGLVisualizerJni.TYPE_LIQUID));
			break;
		case MNU_VISUALIZER_SPIN:
			getHostActivity().startActivity((new Intent(getApplication(), ActivityVisualizer.class)).putExtra(Visualizer.EXTRA_VISUALIZER_CLASS_NAME, OpenGLVisualizerJni.class.getName()).putExtra(OpenGLVisualizerJni.EXTRA_VISUALIZER_TYPE, OpenGLVisualizerJni.TYPE_SPIN));
			break;
		case MNU_VISUALIZER_PARTICLE:
			getHostActivity().startActivity((new Intent(getApplication(), ActivityVisualizer.class)).putExtra(Visualizer.EXTRA_VISUALIZER_CLASS_NAME, OpenGLVisualizerJni.class.getName()).putExtra(OpenGLVisualizerJni.EXTRA_VISUALIZER_TYPE, OpenGLVisualizerJni.TYPE_PARTICLE));
			break;
		case MNU_VISUALIZER_IMMERSIVE_PARTICLE:
			getHostActivity().startActivity((new Intent(getApplication(), ActivityVisualizer.class)).putExtra(Visualizer.EXTRA_VISUALIZER_CLASS_NAME, OpenGLVisualizerJni.class.getName()).putExtra(OpenGLVisualizerJni.EXTRA_VISUALIZER_TYPE, OpenGLVisualizerJni.TYPE_IMMERSIVE_PARTICLE));
			break;
		case MNU_VISUALIZER_BLUETOOTH:
			startActivity(new ActivitySettings(false, true), 0, null, false);
			break;
		case MNU_VISUALIZER_ALBUMART:
			getHostActivity().startActivity((new Intent(getApplication(), ActivityVisualizer.class)).putExtra(Visualizer.EXTRA_VISUALIZER_CLASS_NAME, AlbumArtVisualizer.class.getName()));
			break;
		case MNU_SETTINGS:
			startActivity(new ActivitySettings(false, false), 0, null, false);
			break;
		}
		return true;
	}
	
	@Override
	public void onClick(View view) {
		if (view == btnAdd) {
			addSongs(view);
		} else if (view == btnPrev) {
			if (Player.previous() && !Player.controlMode)
				bringCurrentIntoView();
		} else if (view == btnPlay) {
			Player.playPause();
		} else if (view == btnNext) {
			if (Player.next() && !Player.controlMode)
				bringCurrentIntoView();
		} else if (view == btnMenu) {
			CustomContextMenu.openContextMenu(btnMenu, this);
		} else if (view == btnMoveSel) {
			startMovingSelection();
		} else if (view == btnRemoveSel) {
			removeSelection();
		} else if (view == btnCancelSel) {
			cancelSelection(false);
		} else if (view == btnDecreaseVolume) {
			//this click will only actually perform an action when triggered by keys
			if (volumeButtonPressed == 0)
				decreaseVolume();
		} else if (view == btnIncreaseVolume) {
			//this click will only actually perform an action when triggered by keys
			if (volumeButtonPressed == 0)
				increaseVolume();
		} else if (view == btnVolume) {
			Player.showStreamVolumeUI();
		} else if (view == lblTitle) {
			if (Player.controlMode)
				Player.playPause();
		} else if (view == list) {
			if (Player.songs.getCount() == 0)
				addSongs(view);
		}
	}
	
	@Override
	public void processItemClick(int position) {
		if (Player.songs.selecting) {
			lastSel = position;
			Player.songs.setSelection(firstSel, position, position, true, true);
		} else if (Player.songs.moving) {
			Player.songs.moveSelection(position);
		} else {
			if (UI.doubleClickMode) {
				if (Player.songs.getFirstSelectedPosition() == position) {
					if (Player.songs.getItemT(position) == Player.currentSong && !Player.playing)
						Player.playPause();
					else
						Player.play(position);
				} else {
					Player.songs.setSelection(position, position, true, true);
				}
			} else {
				Player.songs.setSelection(position, position, true, true);
				if (Player.songs.getItemT(position) == Player.currentSong && !Player.playing)
					Player.playPause();
				else
					Player.play(position);
			}
		}
	}
	
	@Override
	public void processItemLongClick(int position) {
		if (!Player.songs.selecting && !Player.songs.moving) {
			//select the clicked item before opening the menu
			if (!Player.songs.isSelected(position))
				Player.songs.setSelection(position, position, position, true, true);
			firstSel = Player.songs.getFirstSelectedPosition();
			lastSel = firstSel;
			startSelecting();
		}
	}
	
	@Override
	public boolean onBackPressed() {
		/*if (Player.controlMode) {
			Player.setControlMode(false);
			return true;
		} else*/ if (Player.songs.selecting || Player.songs.moving) {
			cancelSelection(false);
			return true;
		}
		return UI.blockBackKey;
	}
	
	@Override
	protected void onCreate() {
		if (Player.state >= Player.STATE_TERMINATING) {
			skipToDestruction = true;
			return;
		} else {
			skipToDestruction = false;
		}
		localeHasBeenChanged = false;
		addWindowFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		if (UI.keepScreenOn)
			addWindowFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
			clearWindowFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (UI.notFullscreen)
			clearWindowFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getHostActivity().setRequestedOrientation((UI.forcedOrientation == 0) ? ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED : ((UI.forcedOrientation < 0) ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
		//whenever the activity is being displayed, the volume keys must control
		//the music volume and nothing else!
		getHostActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
		Player.songs.selecting = false;
		Player.songs.moving = false;
		firstSel = -1;
		lastSel = -1;
		lastTime = -2;
		timeBuilder = new StringBuilder(16);
		volumeBuilder = new StringBuilder(16);
		tmrSong = new Timer(this, "Song Timer", false, true, true);
		tmrUpdateVolumeDisplay = new Timer(this, "Update Volume Display Timer", true, true, false);
		tmrVolume = new Timer(this, "Volume Timer", false, true, true);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreateLayout(boolean firstCreation) {
		if (Player.state >= Player.STATE_TERMINATING || skipToDestruction) {
			skipToDestruction = true;
			finish(0, null, false);
			return;
		}
		setContentView(Player.controlMode ? (UI.isLandscape ? R.layout.activity_main_control_l : R.layout.activity_main_control) : (UI.isLandscape ? ((UI.isLargeScreen && UI.controlsToTheLeft) ? R.layout.activity_main_l_left : R.layout.activity_main_l) : R.layout.activity_main), true, forceFadeOut);
		lblTitle = (TextView)findViewById(R.id.lblTitle);
		btnPrev = (BgButton)findViewById(R.id.btnPrev);
		btnPrev.setOnClickListener(this);
		btnNext = (BgButton)findViewById(R.id.btnNext);
		btnNext.setOnClickListener(this);
		btnMenu = (BgButton)findViewById(R.id.btnMenu);
		btnMenu.setOnClickListener(this);
		if (Player.controlMode) {
			findViewById(R.id.panelControls).setBackgroundDrawable(new ColorDrawable(UI.color_control_mode));
			UI.largeText(lblTitle);
			btnPrev.setIconNoChanges(UI.ICON_PREV);
			btnNext.setIconNoChanges(UI.ICON_NEXT);
			btnMenu.setIconNoChanges(UI.ICON_MENU);
			btnPrev.setIconStretchable(true);
			btnNext.setIconStretchable(true);
			btnMenu.setIconStretchable(true);
			
			volumeButtonPressed = 0;
			btnDecreaseVolume = (BgButton)findViewById(R.id.btnDecreaseVolume);
			btnDecreaseVolume.setOnClickListener(this);
			btnDecreaseVolume.setOnPressingChangeListener(this);
			btnDecreaseVolume.setIconNoChanges(UI.ICON_DECREASE_VOLUME);
			btnDecreaseVolume.setIconStretchable(true);
			btnIncreaseVolume = (BgButton)findViewById(R.id.btnIncreaseVolume);
			btnIncreaseVolume.setOnClickListener(this);
			btnIncreaseVolume.setOnPressingChangeListener(this);
			btnIncreaseVolume.setIconNoChanges(UI.ICON_INCREASE_VOLUME);
			btnIncreaseVolume.setIconStretchable(true);
			btnVolume = (BgButton)findViewById(R.id.btnVolume);
			btnVolume.setIconNoChanges(UI.ICON_VOLUME0);
			btnVolume.setIconStretchable(true);
			btnVolume.setEnabled(false);
			
			Player.songs.selecting = false;
			Player.songs.moving = false;
			
			lblTitle.setOnClickListener(this);
			final int w = getDecorViewWidth(), h = getDecorViewHeight();
			final int min, max;
			if (w < h) {
				min = w;
				max = h;
			} else {
				min = h;
				max = w;
			}
			int panelH = (UI.isLandscape ? ((min * 25) / 100) : ((max * 14) / 100));
			if (!UI.isLandscape && panelH > (min >> 2))
				panelH = (min >> 2);
			findViewById(R.id.panelTop).setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, panelH));
			
			RelativeLayout.LayoutParams rp = new RelativeLayout.LayoutParams(panelH, panelH);
			rp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			btnDecreaseVolume.setLayoutParams(rp);
			
			rp = new RelativeLayout.LayoutParams(panelH, panelH);
			rp.addRule(RelativeLayout.RIGHT_OF, R.id.btnDecreaseVolume);
			btnVolume.setLayoutParams(rp);
			
			rp = new RelativeLayout.LayoutParams(panelH, panelH);
			rp.addRule(RelativeLayout.RIGHT_OF, R.id.btnVolume);
			btnIncreaseVolume.setLayoutParams(rp);
			
			rp = new RelativeLayout.LayoutParams(panelH, panelH);
			rp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			btnMenu.setLayoutParams(rp);
			
			lblTitleIcon = new TextIconDrawable(UI.ICON_PLAY, UI.colorState_text_control_mode_reactive.getDefaultColor(), panelH >> 1);
			lblTitle.setCompoundDrawables(null, null, lblTitleIcon, null);
			
			if (UI.isLowDpiScreen && !UI.isLargeScreen) {
				final int lds = (UI.isLandscape ? UI.dpToPxI(12) : UI._8dp);
				btnDecreaseVolume.setPadding(lds, lds, lds, lds);
				btnIncreaseVolume.setPadding(lds, lds, lds, lds);
				btnVolume.setPadding(lds, lds, lds, lds);
				btnMenu.setPadding(lds, lds, lds, lds);
			} else {
				btnDecreaseVolume.setPadding(UI._16dp, UI._16dp, UI._16dp, UI._16dp);
				btnIncreaseVolume.setPadding(UI._16dp, UI._16dp, UI._16dp, UI._16dp);
				btnVolume.setPadding(UI._16dp, UI._16dp, UI._16dp, UI._16dp);
				btnMenu.setPadding(UI._16dp, UI._16dp, UI._16dp, UI._16dp);
			}
			
			if (UI.isLandscape) {
				lblTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, (min * 9) / 100);
				final int pa = (min * 7) / 100;
				btnPrev.setPadding(pa, pa, pa, pa);
				btnNext.setPadding(pa, pa, pa, pa);
			} else {
				LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (max * 40) / 100);
				p2.topMargin = UI._16dp;
				p2.bottomMargin = UI._16dp;
				lblTitle.setLayoutParams(p2);
				lblTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, (max * 5) / 100);
				final int ph = (min * 12) / 100, pv = (max * 12) / 100;
				btnPrev.setPadding(ph, pv, ph, pv);
				btnNext.setPadding(ph, pv, ph, pv);
			}
			btnDecreaseVolume.setTextColor(UI.colorState_text_control_mode_reactive);
			btnVolume.setTextColor(UI.colorState_text_control_mode_reactive);
			btnIncreaseVolume.setTextColor(UI.colorState_text_control_mode_reactive);
			btnMenu.setTextColor(UI.colorState_text_control_mode_reactive);
			lblTitle.setTextColor(UI.colorState_text_control_mode_reactive);
			btnPrev.setTextColor(UI.colorState_text_control_mode_reactive);
			btnNext.setTextColor(UI.colorState_text_control_mode_reactive);
		} else {
			UI.largeText(lblTitle);
			btnPrev.setIcon(UI.ICON_PREV);
			btnNext.setIcon(UI.ICON_NEXT);
			btnMenu.setIcon(UI.ICON_MENU);
			
			btnAdd = (BgButton)findViewById(R.id.btnAdd);
			btnAdd.setOnClickListener(this);
			btnAdd.setIcon(UI.ICON_FPLAY);
			
			if (!UI.marqueeTitle) {
				lblTitle.setEllipsize(TruncateAt.END);
				lblTitle.setHorizontallyScrolling(false);
			} else {
				lblTitle.setHorizontalFadingEdgeEnabled(false);
				lblTitle.setVerticalFadingEdgeEnabled(false);
				lblTitle.setFadingEdgeLength(0);
			}
			lblTitle.setTextColor(UI.colorState_text_title_static);
			
			lblArtist = (TextView)findViewById(R.id.lblArtist);
			if (UI.isLargeScreen != (lblArtist != null))
				UI.isLargeScreen = (lblArtist != null);
			
			lblMsgSelMove = (TextView)findViewById(R.id.lblMsgSelMove);
			UI.largeText(lblMsgSelMove);
			lblMsgSelMove.setTextColor(UI.colorState_text_title_static);
			lblMsgSelMove.setHorizontalFadingEdgeEnabled(false);
			lblMsgSelMove.setVerticalFadingEdgeEnabled(false);
			lblMsgSelMove.setFadingEdgeLength(0);
			barSeek = (BgSeekBar)findViewById(R.id.barSeek);
			barSeek.setAdditionalContentDescription(getText(R.string.go_to).toString());
			barSeek.setOnBgSeekBarChangeListener(this);
			barSeek.setMax(MAX_SEEK);
			barSeek.setVertical(UI.isLandscape && !UI.isLargeScreen);
			barSeek.setFocusable(false);
			btnPlay = (BgButton)findViewById(R.id.btnPlay);
			btnPlay.setOnClickListener(this);
			btnPlay.setIcon(UI.ICON_PLAY);
			list = (BgListView)findViewById(R.id.list);
			list.setScrollBarType(UI.songListScrollBarType);
			list.setOnKeyDownObserver(this);
			list.setEmptyListOnClickListener(this);
			//Apparently, not all devices can properly draw the character &#9835; :(
			final String originalText = getText(R.string.touch_to_add_songs).toString();
			final int iconIdx = originalText.indexOf('\u266B');
			if (iconIdx > 0) {
				final SpannableStringBuilder txt = new SpannableStringBuilder(originalText);
				txt.setSpan(new ImageSpan(new TextIconDrawable(UI.ICON_FPLAY, UI.color_text_disabled, UI._22sp, 0), DynamicDrawableSpan.ALIGN_BASELINE), iconIdx, iconIdx + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				list.setCustomEmptyText(txt);
			} else {
				list.setCustomEmptyText(originalText);
			}
			panelControls = (ViewGroup)findViewById(R.id.panelControls);
			panelSecondary = (ViewGroup)findViewById(R.id.panelSecondary);
			panelSelection = (ViewGroup)findViewById(R.id.panelSelection);
			btnMoveSel = (BgButton)findViewById(R.id.btnMoveSel);
			btnMoveSel.setOnClickListener(this);
			btnMoveSel.setIcon(UI.ICON_MOVE, UI.isLargeScreen || !UI.isLandscape, true);
			btnRemoveSel = (BgButton)findViewById(R.id.btnRemoveSel);
			btnRemoveSel.setOnClickListener(this);
			btnRemoveSel.setIcon(UI.ICON_DELETE, UI.isLargeScreen || !UI.isLandscape, true);
			btnCancelSel = (BgButton)findViewById(R.id.btnCancelSel);
			btnCancelSel.setOnClickListener(this);
			
			barVolume = (BgSeekBar)findViewById(R.id.barVolume);
			btnVolume = (BgButton)findViewById(R.id.btnVolume);
			
			if (UI.isLowDpiScreen && !UI.isLargeScreen) {
				barVolume.setTextSizeIndex(1);
				barSeek.setTextSizeIndex(1);
			} else {
				btnCancelSel.setTextSize(TypedValue.COMPLEX_UNIT_PX, UI._22sp);
			}
			
			if (UI.isLargeScreen) {
				UI.mediumTextAndColor((TextView)findViewById(R.id.lblTitleStatic));
				UI.mediumTextAndColor((TextView)findViewById(R.id.lblArtistStatic));
				UI.mediumTextAndColor((TextView)findViewById(R.id.lblTrackStatic));
				UI.mediumTextAndColor((TextView)findViewById(R.id.lblAlbumStatic));
				UI.mediumTextAndColor((TextView)findViewById(R.id.lblLengthStatic));
				lblArtist.setTextColor(UI.colorState_text_title_static);
				UI.largeText(lblArtist);
				lblTrack = (TextView)findViewById(R.id.lblTrack);
				lblTrack.setTextColor(UI.colorState_text_title_static);
				UI.largeText(lblTrack);
				lblAlbum = (TextView)findViewById(R.id.lblAlbum);
				lblAlbum.setTextColor(UI.colorState_text_title_static);
				UI.largeText(lblAlbum);
				lblLength = (TextView)findViewById(R.id.lblLength);
				lblLength.setTextColor(UI.colorState_text_title_static);
				UI.largeText(lblLength);
			} else {
				lblTrack = null;
				lblAlbum = null;
				lblLength = null;
			}
			
			if (Player.volumeControlType == Player.VOLUME_CONTROL_NONE) {
				panelSecondary.removeView(barVolume);
				barVolume = null;
				btnVolume.setVisibility(View.VISIBLE);
				btnVolume.setOnClickListener(this);
				btnVolume.setIcon(UI.ICON_VOLUME4);
				vwVolume = btnVolume;
				vwVolumeId = R.id.btnVolume;
				if (UI.isLargeScreen) {
					UI.setNextFocusForwardId(list, R.id.btnVolume);
					UI.setNextFocusForwardId(barSeek, R.id.btnVolume);
					barSeek.setNextFocusRightId(R.id.btnVolume);
					if (!UI.isLandscape)
						btnAdd.setNextFocusLeftId(R.id.btnVolume);
					btnAdd.setNextFocusUpId(R.id.btnVolume);
					btnPrev.setNextFocusUpId(R.id.btnVolume);
					btnPlay.setNextFocusUpId(R.id.btnVolume);
					btnNext.setNextFocusUpId(R.id.btnVolume);
					btnMenu.setNextFocusUpId(R.id.btnVolume);
				} else {
					if (UI.isLandscape) {
						btnPrev.setNextFocusRightId(R.id.btnVolume);
						btnPlay.setNextFocusRightId(R.id.btnVolume);
						btnNext.setNextFocusRightId(R.id.btnVolume);
						btnAdd.setNextFocusRightId(R.id.btnVolume);
					} else {
						btnPrev.setNextFocusDownId(R.id.btnVolume);
						btnPlay.setNextFocusDownId(R.id.btnVolume);
						btnNext.setNextFocusDownId(R.id.btnVolume);
						btnAdd.setNextFocusDownId(R.id.btnVolume);
					}
					UI.setNextFocusForwardId(btnMenu, R.id.btnVolume);
					btnMenu.setNextFocusRightId(R.id.btnVolume);
					btnMenu.setNextFocusDownId(R.id.btnVolume);
				}
			} else {
				panelSecondary.removeView(btnVolume);
				btnVolume = null;
				barVolume.setAdditionalContentDescription(getText(R.string.volume).toString());
				barVolume.setOnBgSeekBarChangeListener(this);
				barVolume.setMax((Player.volumeControlType == Player.VOLUME_CONTROL_STREAM) ? Player.globalMaxVolume : (-Player.MIN_VOLUME_DB / 5));
				barVolume.setVertical(UI.isLandscape && !UI.isLargeScreen);
				barVolume.setKeyIncrement((Player.volumeControlType == Player.VOLUME_CONTROL_STREAM) ? 1 : 20);
				vwVolume = barVolume;
				vwVolumeId = R.id.barVolume;
			}

			if (UI.isLargeScreen) {
				findViewById(R.id.panelInfo).setBackgroundDrawable(new BorderDrawable(UI.color_highlight, UI.color_window, ((UI.isLandscape && !UI.controlsToTheLeft) ? UI.thickDividerSize : 0), (!UI.isLandscape ? UI.thickDividerSize : 0), ((UI.isLandscape && UI.controlsToTheLeft) ? UI.thickDividerSize : 0), 0, true));
			} else {
				if (UI.isLandscape && UI.extraSpacing) {
					findViewById(R.id.panelInfo).setBackgroundDrawable(new ColorDrawable(UI.color_window));
				} else {
					lblTitle.setBackgroundDrawable(new ColorDrawable(UI.color_window));
					lblMsgSelMove.setBackgroundDrawable(new ColorDrawable(UI.color_window));
					panelControls.setBackgroundDrawable(new ColorDrawable(UI.color_window));
				}
				if (UI.isLandscape) {
					list.setTopLeftBorders();
					if (!UI.extraSpacing) {
						panelSecondary.setBackgroundDrawable(new ColorDrawable(UI.color_window));
						panelSelection.setBackgroundDrawable(new ColorDrawable(UI.color_window));
					}
				} else {
					panelSecondary.setBackgroundDrawable(new BorderDrawable(UI.color_highlight, UI.color_window, 0, 0, 0, UI.thickDividerSize, true));
					panelSelection.setBackgroundDrawable(new BorderDrawable(UI.color_highlight, UI.color_window, 0, 0, 0, UI.thickDividerSize, true));
					panelSecondary.setPadding(0, 0, 0, UI._8dp + UI.thickDividerSize);
				}
				if (UI.extraSpacing) {
					if (UI.isLandscape) {
						panelControls.setPadding(UI._8dp, 0, UI._8dp, 0);
						final ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams)panelControls.getLayoutParams();
						p.bottomMargin = UI._8dp;
						panelControls.setLayoutParams(p);
					} else {
						panelControls.setPadding(UI._8dp, 0, UI._8dp, UI._8dp);
					}
					panelSelection.setPadding(UI._8dp, 0, UI._8dp, UI._8dp + (UI.isLandscape ? 0 : UI.thickDividerSize));
				} else {
					if (!UI.isLandscape)
						panelControls.setPadding(0, 0, 0, UI.isLowDpiScreen ? 0 : UI._8dp);
					if (btnVolume != null) {
						final ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams)btnVolume.getLayoutParams();
						p.rightMargin = 0;
						btnVolume.setLayoutParams(p);
					}
				}
			}
			btnCancelSel.setDefaultHeight();
			UI.prepareEdgeEffectColor(getApplication());
			final boolean m = Player.songs.moving;
			if (m || Player.songs.selecting)
				startSelecting();
			if (m)
				startMovingSelection();
		}
	}
	
	@Override
	public void onBgListViewAttached(BgListView list) {
		restoreListViewPosition(selectCurrentWhenAttached);
		selectCurrentWhenAttached = false;
	}
	
	@Override
	public boolean onBgListViewKeyDown(BgListView list, int keyCode) {
		switch (keyCode) {
		case UI.KEY_LEFT:
			if (Player.songs.selecting)
				(UI.isLargeScreen ? btnCancelSel : (UI.isLandscape ? btnMoveSel : btnRemoveSel)).requestFocus();
			else if (Player.songs.moving)
				btnCancelSel.requestFocus();
			else if (UI.isLargeScreen)
				btnMenu.requestFocus();
			else
				vwVolume.requestFocus();
			return true;
		case UI.KEY_RIGHT:
			if (Player.songs.selecting)
				((UI.isLargeScreen || UI.isLandscape) ? btnMoveSel : btnCancelSel).requestFocus();
			else if (Player.songs.moving)
				btnCancelSel.requestFocus();
			else if (UI.isLargeScreen)
				(UI.isLandscape ? btnAdd : vwVolume).requestFocus();
			else if (UI.isLandscape)
				btnPrev.requestFocus();
			else
				btnAdd.requestFocus();
			return true;
		}
		final int s = Player.songs.getSelection();
		if (Player.songs.moving || Player.songs.selecting) {
			switch (keyCode) {
			case UI.KEY_DEL:
				if (s >= 0)
					removeSelection();
				return true;
			case UI.KEY_EXTRA:
			case UI.KEY_ENTER:
				if (Player.songs.selecting)
					startMovingSelection();
				else
					cancelSelection(false);
				return true;
			case UI.KEY_UP:
			case UI.KEY_DOWN:
			case UI.KEY_PAGE_UP:
			case UI.KEY_PAGE_DOWN:
			case UI.KEY_HOME:
			case UI.KEY_END:
				int n = list.getNewPosition(s, keyCode, false);
				if (n < 0)
					return true;
				final boolean center = (n <= (list.getFirstVisiblePosition() + 1) || n >= (list.getLastVisiblePosition() - 1));
				if (Player.songs.moving) {
					Player.songs.moveSelection(n);
				} else {
					lastSel = n;
					Player.songs.setSelection(firstSel, n, n, true, true);
				}
				if (center)
					list.centerItem(n);
				return true;
			}
		} else {
			switch (keyCode) {
			case UI.KEY_DEL:
				if (s >= 0)
					Player.songs.removeSelection();
				return true;
			case UI.KEY_EXTRA:
				if (s >= 0)
					processItemLongClick(s);
				return true;
			case UI.KEY_ENTER:
				if (s >= 0) {
					if (Player.songs.getItemT(s) == Player.currentSong && !Player.playing)
						Player.playPause();
					else
						Player.play(s);
				}
				return true;
			}
		}
		return false;
	}
	
	private void resume(boolean selectCurrent) {
		UI.songActivity = this;
		Player.songs.setObserver(list);
		updateVolumeDisplay();
		if (list != null) {
			selectCurrentWhenAttached = selectCurrent;
			list.notifyMeWhenFirstAttached(this);
		}
		//ignoreAnnouncement = true;
		onPlayerChanged(Player.currentSong, true, true, null);
	}
	
	@Override
	protected void onResume() {
		Player.isMainActiveOnTop = true;
		Player.observer = this;
		Player.registerMediaButtonEventReceiver();
		resume(true);
		UI.showNextStartupMsg(getHostActivity());
	}
	
	@Override
	protected void onOrientationChanged() {
		onCleanupLayout();
		onCreateLayout(false);
		resume(false);
	}
	
	@Override
	protected void onPause() {
		if (skipToDestruction)
			return;
		Player.isMainActiveOnTop = false;
		saveListViewPosition();
		if (tmrSong != null)
			tmrSong.stop();
		if (tmrUpdateVolumeDisplay != null)
			tmrUpdateVolumeDisplay.stop();
		if (tmrVolume != null)
			tmrVolume.stop();
		volumeButtonPressed = 0;
		if (Player.songs.selecting || Player.songs.moving)
			cancelSelection(false);
		UI.songActivity = null;
		Player.songs.setObserver(null);
		Player.observer = null;
		lastTime = -2;
		if (!Player.controlMode)
			Player.lastCurrent = Player.songs.getCurrentPosition();
		if (UI.forcedLocale != UI.LOCALE_NONE && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN && !localeHasBeenChanged) {
			localeHasBeenChanged = true;
			UI.reapplyForcedLocale(getApplication(), getHostActivity());
		}
	}
	
	@Override
	protected void onCleanupLayout() {
		saveListViewPosition();
		lblTitle = null;
		lblArtist = null;
		lblTrack = null;
		lblAlbum = null;
		lblLength = null;
		lblTitleIcon = null;
		lblMsgSelMove = null;
		barSeek = null;
		barVolume = null;
		vwVolume = null;
		btnAdd = null;
		btnPrev = null;
		btnPlay = null;
		btnNext = null;
		btnMenu = null;
		panelControls = null;
		panelSecondary = null;
		panelSelection = null;
		btnMoveSel = null;
		btnRemoveSel = null;
		btnCancelSel = null;
		btnDecreaseVolume = null;
		btnIncreaseVolume = null;
		btnVolume = null;
		list = null;
		if (tmrSong != null)
			tmrSong.stop();
		if (tmrUpdateVolumeDisplay != null)
			tmrUpdateVolumeDisplay.stop();
		if (tmrVolume != null)
			tmrVolume.stop();
	}
	
	@Override
	protected void onDestroy() {
		skipToDestruction = false;
		if (tmrSong != null) {
			tmrSong.release();
			tmrSong = null;
		}
		if (tmrUpdateVolumeDisplay != null) {
			tmrUpdateVolumeDisplay.release();
			tmrUpdateVolumeDisplay = null;
		}
		if (tmrVolume != null) {
			tmrVolume.release();
			tmrVolume = null;
		}
		timeBuilder = null;
		volumeBuilder = null;
	}
	
	@Override
	public void handleTimer(Timer timer, Object param) {
		if (timer == tmrVolume) {
			if (tmrVolumeInitialDelay > 0) {
				tmrVolumeInitialDelay--;
			} else {
				switch (volumeButtonPressed) {
				case 1:
					if (!decreaseVolume())
						tmrVolume.stop();
					break;
				case 2:
					if (!increaseVolume())
						tmrVolume.stop();
					break;
				default:
					tmrVolume.stop();
					break;
				}
			}
			return;
		} else if (timer == tmrUpdateVolumeDisplay) {
			updateVolumeDisplay();
			return;
		}
		if (Player.isCurrentSongPreparing()) {
			if (barSeek != null && !barSeek.isTracking()) {
				barSeek.setText(R.string.loading);
				barSeek.setValue(0);
			}
			return;
		}
		final int m = Player.getCurrentPosition(),
				t = ((m < 0) ? -1 : (m / 1000));
		if (t == lastTime) return;
		lastTime = t;
		if (t < 0) {
			if (barSeek != null && !barSeek.isTracking()) {
				barSeek.setText(R.string.no_info);
				barSeek.setValue(0);
			}
		} else {
			Song.formatTimeSec(t, timeBuilder);
			if (barSeek != null && !barSeek.isTracking()) {
				final Song s = Player.currentSong;
				int v = 0;
				if (s != null && s.lengthMS > 0) {
					if (m >= 214740) //avoid overflow! ;)
						v = (int)(((long)m * (long)MAX_SEEK) / (long)s.lengthMS);
					else
						v = (m * MAX_SEEK) / s.lengthMS;
				}
				barSeek.setText(timeBuilder.toString());
				barSeek.setValue(v);
			}
		}
	}
	
	private int getMSFromBarValue(int value) {
		final Song s = Player.currentSong;
		if (s == null || s.lengthMS <= 0 || value < 0)
			return -1;
		return (int)(((long)value * (long)s.lengthMS) / (long)MAX_SEEK);
	}
	
	@Override
	public void onValueChanged(BgSeekBar seekBar, int value, boolean fromUser, boolean usingKeys) {
		if (fromUser) {
			if (seekBar == barVolume) {
				if (Player.volumeControlType == Player.VOLUME_CONTROL_STREAM)
					Player.setStreamVolume(value);
				else
					Player.setVolumeDB((value * 5) + Player.MIN_VOLUME_DB);
				seekBar.setText(volumeToString());
			} else if (seekBar == barSeek) {
				value = getMSFromBarValue(value);
				if (value < 0) {
					seekBar.setText(R.string.no_info);
					seekBar.setValue(0);
				} else {
					Song.formatTime(value, timeBuilder);
					seekBar.setText(timeBuilder.toString());
				}
			}
		}
	}
	
	@Override
	public boolean onStartTrackingTouch(BgSeekBar seekBar) {
		if (seekBar == barSeek) {
			if (Player.currentSong != null && Player.currentSong.lengthMS > 0) {
				playingBeforeSeek = Player.playing;
				if (playingBeforeSeek)
					Player.playPause();
				if (UI.expandSeekBar && !UI.isLargeScreen)
					vwVolume.setVisibility(View.GONE);
				return true;
			}
			return false;
		}
		return true;
	}
	
	@Override
	public void onStopTrackingTouch(BgSeekBar seekBar, boolean cancelled) {
		if (seekBar == barVolume && Player.volumeControlType == Player.VOLUME_CONTROL_STREAM) {
			updateVolumeDisplay();
		} else if (seekBar == barSeek) {
			if (Player.currentSong != null) {
				final int ms = getMSFromBarValue(seekBar.getValue());
				if (cancelled || ms < 0) {
					if (playingBeforeSeek)
						Player.playPause();
					else
						handleTimer(tmrSong, null);
				} else {
					Player.seekTo(ms, playingBeforeSeek);
				}
			}
			if (UI.expandSeekBar && !UI.isLargeScreen)
				vwVolume.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onFileSelected(int id, String path, String name) {
		if (id == MNU_LOADLIST) {
			Player.songs.clear();
			Player.songs.startDeserializing(getApplication(), path, true, false, false);
			BackgroundActivityMonitor.start(getHostActivity());
		} else {
			Player.songs.serialize(getApplication(), path);
		}
	}
	
	@Override
	public void onAddClicked(int id, String path, String name) {
		if (id == MNU_LOADLIST) {
			Player.songs.startDeserializing(getApplication(), path, false, true, false);
			BackgroundActivityMonitor.start(getHostActivity());
		}
	}
	
	@Override
	public void onPlayClicked(int id, String path, String name) {
		if (id == MNU_LOADLIST) {
			Player.songs.startDeserializing(getApplication(), path, false, !Player.clearListWhenPlayingFolders, true);
			BackgroundActivityMonitor.start(getHostActivity());
		}
	}
	
	@Override
	public void onPressingChanged(BgButton button, boolean pressed) {
		if (button == btnDecreaseVolume) {
			if (pressed) {
				if (Player.volumeControlType == Player.VOLUME_CONTROL_NONE) {
					Player.showStreamVolumeUI();
					tmrVolume.stop();
				} else {
					volumeButtonPressed = 1;
					tmrVolumeInitialDelay = 3;
					if (decreaseVolume())
						tmrVolume.start(175);
					else
						tmrVolume.stop();
				}
			} else if (volumeButtonPressed == 1) {
				volumeButtonPressed = 0;
				tmrVolume.stop();
			}
		} else if (button == btnIncreaseVolume) {
			if (pressed) {
				if (Player.volumeControlType == Player.VOLUME_CONTROL_NONE) {
					Player.showStreamVolumeUI();
					tmrVolume.stop();
				} else {
					volumeButtonPressed = 2;
					tmrVolumeInitialDelay = 3;
					if (increaseVolume())
						tmrVolume.start(175);
					else
						tmrVolume.stop();
				}
			} else if (volumeButtonPressed == 2) {
				volumeButtonPressed = 0;
				tmrVolume.stop();
			}
		}
	}
}
