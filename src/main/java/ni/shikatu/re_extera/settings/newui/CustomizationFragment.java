package ni.shikatu.re_extera.settings.newui;

import android.content.Context;
import ni.shikatu.re_extera.utils.Res;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import ni.shikatu.re_extera.hooks.chatmessagecell.MeasureTime;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.UItemUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.EditTextSettingsCell;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.ColorPicker;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.SelectAnimatedEmojiDialog;
import ni.shikatu.re_extera.settings.components.AltSeekbar;

public class CustomizationFragment extends BasePreferencesActivityExtended {

    private enum CustomizationIds {
        TRANSPARENT_DELETED_MESSAGES_PREVIEW_ID,
        CUSTOM_DELETED_MARK_ID,
        DELETED_MARK_COLOR_HEADER_ID,
        DELETED_MARK_COLOR_ID,
        DISABLE_COLORED_REPLIES_ID,
        TRANSPARENT_DELETED_MESSAGES_ID,
        TRANSPARENT_DELETED_MESSAGES_SLIDER_ID;

        public int getId() {
            return ordinal() + 1;
        }
    }

    @Override
    public String getTitle() {
        return Localization.CUSTOMIZATION;
    }

    private ChatMessageCell previewCell;
    private LinearLayout colorsLayout;
    private ImageView deletedMarkEmojiButton;
    private AnimatedEmojiDrawable deletedMarkEmojiButtonDrawable;
    private static final int EMOJI_PICKER_TYPE_STATUS = 0;

    private void refreshPreview() {
        if (previewCell != null && previewCell.getMessageObject() != null) {
            previewCell.getMessageObject().forceUpdate = true;
            previewCell.setMessageObject(previewCell.getMessageObject(), null, false, false, false);
        }
    }

    private void updateColorsSelection() {
        if (colorsLayout == null) return;
        int currentColor = Settings.getDeletedMarkColor();
        int customColor = Settings.getDeletedMarkCustomColor();
        for (int i = 0; i < colorsLayout.getChildCount(); i++) {
            View child = colorsLayout.getChildAt(i);
            if (child instanceof ColorCircle) {
                ColorCircle circle = (ColorCircle) child;
                boolean selected = false;
                if (circle.isDefault) {
                    selected = (currentColor == 0);
                } else if (circle.isCustom) {
                    selected = (currentColor != 0 && currentColor == customColor && !isStandardColor(currentColor));
                } else {
                    selected = (currentColor == circle.color);
                }
                circle.setSelectedColor(selected);
            }
        }
    }

    private boolean isStandardColor(int color) {
        int[] standard = {0xFFE53935, 0xFF1E88E5, 0xFFFFB300, 0xFF43A047, 0xFF8E24AA};
        for (int c : standard) {
            if (c == color) return true;
        }
        return false;
    }

    private class ColorCircle extends View {
        public int color;
        public boolean isCustom;
        public boolean isDefault;
        private boolean isSelectedColor;
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Drawable icon;

        public ColorCircle(Context context, int color, boolean isCustom, boolean isDefault) {
            super(context);
            this.color = color;
            this.isCustom = isCustom;
            this.isDefault = isDefault;
            setWillNotDraw(false);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(AndroidUtilities.dp(2));
            
            setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            
            if (isCustom) {
                icon = ContextCompat.getDrawable(context, Res.drawable("msg_edit", org.telegram.messenger.R.drawable.msg_edit));
                if (icon != null) {
                    icon = icon.mutate();
                }
            }
        }
        
        public void setSelectedColor(boolean selected) {
            this.isSelectedColor = selected;
            invalidate();
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int radius = AndroidUtilities.dp(16);
            
            if (isDefault) {
                paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                canvas.drawCircle(cx, cy, radius, paint);
            } else if (isCustom && color == 0) {
                android.graphics.Shader shader = new android.graphics.SweepGradient(cx, cy, 
                    new int[]{0xFFE53935, 0xFFFFB300, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA, 0xFFE53935}, null);
                paint.setShader(shader);
                canvas.drawCircle(cx, cy, radius, paint);
                paint.setShader(null);
            } else {
                paint.setColor(color);
                canvas.drawCircle(cx, cy, radius, paint);
            }
            
            if (isSelectedColor) {
                strokePaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                canvas.drawCircle(cx, cy, radius + AndroidUtilities.dp(3), strokePaint);
            }
            
            if (isCustom && icon != null) {
                int iconSize = AndroidUtilities.dp(16);
                icon.setBounds(cx - iconSize/2, cy - iconSize/2, cx + iconSize/2, cy + iconSize/2);
                int iconColor = 0xFFFFFFFF;
                if (color != 0) {
                    double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
                    iconColor = luminance > 0.5 ? 0xFF000000 : 0xFFFFFFFF;
                }
                icon.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN));
                icon.draw(canvas);
            }
        }
    }

    private void showColorPickerDialog(int initialColor, final Runnable onColorSelected) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose Color");
        
        final int[] selectedColor = new int[]{initialColor == 0 ? 0xFFFFFFFF : initialColor};
        
        ColorPicker colorPicker = new ColorPicker(getContext(), false, new ColorPicker.ColorPickerDelegate() {
            @Override
            public void setColor(int color, int state, boolean notify) {
                selectedColor[0] = color;
                Settings.setDeletedMarkColor(color);
                if (previewCell != null && previewCell.getMessageObject() != null) {
                    previewCell.getMessageObject().forceUpdate = true;
                    previewCell.setMessageObject(previewCell.getMessageObject(), null, false, false, false);
                }
            }
        });
        
        LinearLayout wrapper = new LinearLayout(getContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(colorPicker, LayoutHelper.createLinear(-1, 300));
        builder.setView(wrapper);
        
        builder.setPositiveButton(Localization.SAVE, (dialog, which) -> {
            Settings.setDeletedMarkCustomColor(selectedColor[0]);
            Settings.setDeletedMarkColor(selectedColor[0]);
            onColorSelected.run();
        });
        builder.setNegativeButton(Localization.CANCEL, (dialog, which) -> {
            Settings.setDeletedMarkColor(initialColor);
            if (previewCell != null && previewCell.getMessageObject() != null) {
                previewCell.getMessageObject().forceUpdate = true;
                previewCell.setMessageObject(previewCell.getMessageObject(), null, false, false, false);
            }
            onColorSelected.run();
        });
        
        showDialog(builder.create());
    }

    private View colorSelectorView() {
        HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
        scrollView.setClipToPadding(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setHorizontalScrollBarEnabled(false);
        
        colorsLayout = new LinearLayout(getContext());
        colorsLayout.setOrientation(LinearLayout.HORIZONTAL);
        colorsLayout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        
        int[] colors = {0, 0xFFE53935, 0xFF1E88E5, 0xFFFFB300, 0xFF43A047, 0xFF8E24AA};
        
        ColorCircle customCircle = new ColorCircle(getContext(), Settings.getDeletedMarkCustomColor(), true, false);
        colorsLayout.addView(customCircle, LayoutHelper.createLinear(48, 48, 0, 0, 4, 0));
        customCircle.setOnClickListener(v -> {
            int currentCustom = Settings.getDeletedMarkCustomColor();
            if (currentCustom == 0 || Settings.getDeletedMarkColor() == currentCustom) {
                showColorPickerDialog(Settings.getDeletedMarkColor(), () -> {
                    customCircle.color = Settings.getDeletedMarkCustomColor();
                    updateColorsSelection();
                });
            } else {
                Settings.setDeletedMarkColor(currentCustom);
                if (previewCell != null && previewCell.getMessageObject() != null) {
                    previewCell.getMessageObject().forceUpdate = true;
                    previewCell.setMessageObject(previewCell.getMessageObject(), null, false, false, false);
                }
                updateColorsSelection();
            }
        });
        
        for (int c : colors) {
            boolean isDefault = (c == 0);
            ColorCircle circle = new ColorCircle(getContext(), c, false, isDefault);
            colorsLayout.addView(circle, LayoutHelper.createLinear(48, 48, 0, 0, 4, 0));
            circle.setOnClickListener(v -> {
                Settings.setDeletedMarkColor(c);
                if (previewCell != null && previewCell.getMessageObject() != null) {
                    previewCell.getMessageObject().forceUpdate = true;
                    previewCell.setMessageObject(previewCell.getMessageObject(), null, false, false, false);
                }
                updateColorsSelection();
            });
        }
        
        scrollView.addView(colorsLayout, LayoutHelper.createScroll(-2, -2, 17));
        updateColorsSelection();
        return scrollView;
    }

    private FrameLayout customMarkView() {
        FrameLayout frameLayout = new FrameLayout(getContext());
        EditTextSettingsCell customPrefix = new EditTextSettingsCell(getContext());
        customPrefix.setTextAndHint(Settings.getCustomPrefix(), Localization.LEAVE_BLANK_FOR_RECYCLE, false);
        customPrefix.getTextView().setPadding(
                customPrefix.getTextView().getPaddingLeft(),
                customPrefix.getTextView().getPaddingTop(),
                AndroidUtilities.dp(56),
                customPrefix.getTextView().getPaddingBottom()
        );
        customPrefix.getTextView().addTextChangedListener(new TextWatcher() { 
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    Settings.setDeletedMarkEmojiId(0L);
                }
                Settings.setCustomPrefix(s.toString());
                MeasureTime.notifyMarkChanged(Settings.getCustomPrefix());
                refreshPreview();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        frameLayout.addView(customPrefix);

        ImageView emojiButton = new ImageView(getContext());
        emojiButton.setContentDescription(Localization.DELETED_MARK_EMOJI);
        emojiButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        emojiButton.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        emojiButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        emojiButton.setOnClickListener(this::showDeletedMarkEmojiPicker);
        bindDeletedMarkEmojiButton(emojiButton);
        frameLayout.addView(emojiButton, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        return frameLayout;
    }

    private void bindDeletedMarkEmojiButton(ImageView button) {
        if (deletedMarkEmojiButtonDrawable != null && deletedMarkEmojiButton != null) {
            deletedMarkEmojiButtonDrawable.removeView(deletedMarkEmojiButton);
        }
        deletedMarkEmojiButton = button;
        deletedMarkEmojiButtonDrawable = null;

        long emojiId = Settings.getDeletedMarkEmojiId();
        if (emojiId == 0L) {
            button.setImageResource(Res.drawable("input_smile", org.telegram.messenger.R.drawable.input_smile));
            button.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.SRC_IN));
            return;
        }
        try {
            AnimatedEmojiDrawable drawable = AnimatedEmojiDrawable.make(UserConfig.selectedAccount, 0, emojiId);
            button.clearColorFilter();
            button.setImageDrawable(drawable);
            drawable.addView(button);
            deletedMarkEmojiButtonDrawable = drawable;
        } catch (Throwable ignored) {
            button.setImageResource(Res.drawable("input_smile", org.telegram.messenger.R.drawable.input_smile));
            button.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.SRC_IN));
        }
    }

    private void showDeletedMarkEmojiPicker(View anchor) {
        final SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow[] popup = new SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow[1];
        SelectAnimatedEmojiDialog picker = new SelectAnimatedEmojiDialog(this, getContext(), true, null, EMOJI_PICKER_TYPE_STATUS, getResourceProvider()) {
            @Override
            public void onEmojiSelected(View view, Long documentId, TLRPC.Document document, TL_stars.TL_starGiftUnique gift, Integer until) {
                if (gift != null) {
                    return;
                }
                Settings.setDeletedMarkEmojiId(documentId == null ? 0L : documentId.longValue());
                Settings.setCustomPrefix("");
                MeasureTime.notifyMarkChanged("");
                refreshPreview();
                listView.adapter.update(true);
                if (popup[0] != null) {
                    popup[0].dismiss();
                }
            }
        };
        long emojiId = Settings.getDeletedMarkEmojiId();
        picker.setSelected(emojiId == 0L ? null : Long.valueOf(emojiId));
        picker.setLongPressEnabled(false);
        popup[0] = new SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow(picker, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {
        };
        popup[0].showAsDropDown(anchor);
        popup[0].dimBehind();
    }

    private FrameLayout previewView() {
        FrameLayout frameLayout = new FrameLayout(getContext());
        
        ChatMessageCell chatMessageCell = new ChatMessageCell(getContext(), UserConfig.selectedAccount);
        
        TLRPC.TL_message replyMsg = new TLRPC.TL_message();
        replyMsg.message = "are you using re:extera?";
        replyMsg.out = true;
        replyMsg.id = 2;
        TLRPC.TL_peerUser replyFromUser = new TLRPC.TL_peerUser();
        replyFromUser.user_id = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        replyMsg.from_id = replyFromUser;
        MessageObject replyMessageObject = new MessageObject(UserConfig.selectedAccount, replyMsg, true, false);
        
        TLRPC.TL_message msg = new TLRPC.TL_message();
        msg.message = "sure, best plugin ever trust";
        msg.date = (int) (System.currentTimeMillis() / 1000);
        msg.dialog_id = -999999999L;
        msg.flags = 259 | 8;
        
        TLRPC.TL_peerUser fromUser = new TLRPC.TL_peerUser();
        fromUser.user_id = 123456789L;
        msg.from_id = fromUser;
        
        msg.id = 1;
        msg.media = new TLRPC.TL_messageMediaEmpty();
        msg.out = false;
        
        TLRPC.TL_messageReplyHeader replyHeader = new TLRPC.TL_messageReplyHeader();
        replyHeader.reply_to_msg_id = 2;
        msg.reply_to = replyHeader;
        
        TLRPC.TL_peerUser peerUser = new TLRPC.TL_peerUser();
        peerUser.user_id = -999999999L;
        msg.peer_id = peerUser;
        
        MessageObject messageObject = new MessageObject(UserConfig.selectedAccount, msg, true, false);
        messageObject.deleted = true;
        messageObject.eventId = 1;
        messageObject.customName = "lostya";
        messageObject.customReplyName = "aartzz";
        messageObject.replyMessageObject = replyMessageObject;
        messageObject.resetLayout();
        
        chatMessageCell.isChat = true;
        chatMessageCell.setFullyDraw(true);
        chatMessageCell.setMessageObject(messageObject, null, false, false, false);
        
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frameLayout.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        frameLayout.addView(chatMessageCell, LayoutHelper.createFrame(-1, -2));
        
        Drawable wallpaper = Theme.getCachedWallpaperNonBlocking();
        if (wallpaper != null) {
            frameLayout.setBackground(wallpaper);
        } else {
            frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        }
        
        previewCell = chatMessageCell;
        return frameLayout;
    }

    private View alphaSliderView() {
        AltSeekbar slider = new AltSeekbar(getContext(), new AltSeekbar.OnDrag() {
            @Override
            public void run(float value) {
                float alpha = value / 100f;
                Settings.setTransparentDeletedMessagesAlpha(alpha);
                if (previewCell != null) {
                    if (Settings.getTransparentDeletedMessages()) {
                        previewCell.setAlpha(alpha);
                    }
                }
            }
        }, 0, 100, Localization.ENABLE_ALPHA, "0%", "100%");
        slider.setProgress(Settings.getTransparentDeletedMessagesAlpha() * 100f);
        return slider;
    }

    @Override
    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        UItem previewItem = UItem.asCustom(CustomizationIds.TRANSPARENT_DELETED_MESSAGES_PREVIEW_ID.getId(), previewView());
        previewItem.intValue = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
        previewItem.transparent = true;
        items.add(previewItem);
        
        items.add(UItem.asHeader(Localization.CUSTOM_PREFIX));
        items.add(UItemUtils.setLinkAlias(UItem.asCustom(CustomizationIds.CUSTOM_DELETED_MARK_ID.getId(), customMarkView()), "reExteraCustomDeletedMark", this));
        items.add(UItemUtils.asShadow());
        
        items.add(UItem.asHeader("Deleted Mark Color"));
        items.add(UItem.asCustom(CustomizationIds.DELETED_MARK_COLOR_ID.getId(), colorSelectorView()));
        items.add(UItemUtils.asShadow());

        items.add(UItemUtils.setLinkAlias(UItem.asCheck(CustomizationIds.DISABLE_COLORED_REPLIES_ID.getId(), Localization.DISABLE_COLORED_REPLIES).setChecked(Settings.getDisableColoredReplies()), "reExteraDisableColoredReplies", this));

        items.add(UItemUtils.setLinkAlias(UItem.asCheck(CustomizationIds.TRANSPARENT_DELETED_MESSAGES_ID.getId(), Localization.ENABLE_ALPHA).setChecked(Settings.getTransparentDeletedMessages()), "reExteraTransparentDeletedMessages", this));
        
        if (Settings.getTransparentDeletedMessages()) {
            items.add(UItem.asCustom(CustomizationIds.TRANSPARENT_DELETED_MESSAGES_SLIDER_ID.getId(), alphaSliderView()));
        }
        
        items.add(UItemUtils.asShadow());
        
        if (previewCell != null) {
            if (Settings.getTransparentDeletedMessages()) {
                previewCell.setAlpha(Settings.getTransparentDeletedMessagesAlpha());
            } else {
                previewCell.setAlpha(1.0f);
            }
        }
    }

    @Override
    public void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > CustomizationIds.values().length) {
            return;
        }
        CustomizationIds clicked = CustomizationIds.values()[item.id - 1];
        switch (clicked) {
            case DISABLE_COLORED_REPLIES_ID:
                Settings.setDisableColoredReplies(!Settings.getDisableColoredReplies());
                refreshCheckBox(item, position, Settings.getDisableColoredReplies(), true);
                if (previewCell != null) {
                    previewCell.invalidate();
                }
                break;
            case TRANSPARENT_DELETED_MESSAGES_ID:
                Settings.setTransparentDeletedMessages(!Settings.getTransparentDeletedMessages());
                refreshCheckBox(item, position, Settings.getTransparentDeletedMessages(), true);
                if (previewCell != null) {
                    if (Settings.getTransparentDeletedMessages()) {
                        previewCell.setAlpha(Settings.getTransparentDeletedMessagesAlpha());
                    } else {
                        previewCell.setAlpha(1.0f);
                    }
                }
                break;
        }
    }

    @Override
    public void onFragmentDestroy() {
        if (deletedMarkEmojiButtonDrawable != null && deletedMarkEmojiButton != null) {
            deletedMarkEmojiButtonDrawable.removeView(deletedMarkEmojiButton);
        }
        deletedMarkEmojiButton = null;
        deletedMarkEmojiButtonDrawable = null;
        if (fragmentView != null) {
            fragmentView.clearFocus();
            AndroidUtilities.hideKeyboard(fragmentView);
        }
        super.onFragmentDestroy();
    }
}
