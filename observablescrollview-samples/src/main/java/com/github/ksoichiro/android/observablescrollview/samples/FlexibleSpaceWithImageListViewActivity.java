/*
 * Copyright 2014 Soichiro Kashima
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ksoichiro.android.observablescrollview.samples;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

public class FlexibleSpaceWithImageListViewActivity extends BaseActivity implements ObservableScrollViewCallbacks {

    private static final float MAX_TEXT_SCALE_DELTA = 0.3f;
    private static final boolean TOOLBAR_IS_STICKY = true;

    private View mToolbar;
    private View mImageView;
    private View mOverlayView; //滑动的时候 在imageView最上方大小相同的View 用于显示缩放效果
    private View mListBackgroundView;
    private TextView mTitleView;
    private View mFab;
    private int mActionBarSize;
    private int mFlexibleSpaceShowFabOffset;
    private int mFlexibleSpaceImageHeight;
    private int mFabMargin;
    private int mToolbarColor;
    private boolean mFabIsShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flexiblespacewithimagelistview);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mFlexibleSpaceImageHeight = getResources().getDimensionPixelSize(R.dimen.flexible_space_image_height); //可扩展区域图片高度
        mFlexibleSpaceShowFabOffset = getResources().getDimensionPixelSize(R.dimen.flexible_space_show_fab_offset); //fab的位移
        mActionBarSize = getActionBarSize();//获取 ActionBar size
        mToolbarColor = getResources().getColor(R.color.primary); //toolBar 颜色

        mToolbar = findViewById(R.id.toolbar);
        if (!TOOLBAR_IS_STICKY) {  //是否toolBar 粘连
            mToolbar.setBackgroundColor(Color.TRANSPARENT);
        }
        mImageView = findViewById(R.id.image);
        mOverlayView = findViewById(R.id.overlay);
        ObservableListView listView = (ObservableListView) findViewById(R.id.list);
        listView.setScrollViewCallbacks(this);

        // Set padding view for ListView. This is the flexible space.
        View paddingView = new View(this);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                mFlexibleSpaceImageHeight);
        paddingView.setLayoutParams(lp);

        // This is required to disable header's list selector effect
        paddingView.setClickable(true);

        listView.addHeaderView(paddingView); //给ListView加上flexible space
        setDummyData(listView); //listView数据填充
        mTitleView = (TextView) findViewById(R.id.title);
        mTitleView.setText(getTitle());
        setTitle(null);
        mFab = findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(FlexibleSpaceWithImageListViewActivity.this, "FAB is clicked", Toast.LENGTH_SHORT).show();
            }
        });
        mFabMargin = getResources().getDimensionPixelSize(R.dimen.margin_standard); //fab边距
        //缩放fab,让其隐藏
        ViewHelper.setScaleX(mFab, 0);
        ViewHelper.setScaleY(mFab, 0);

        // mListBackgroundView makes ListView's background except header view.
        mListBackgroundView = findViewById(R.id.list_background);
        final View contentView = getWindow().getDecorView().findViewById(android.R.id.content);//相当于activity_flexiblespacewithimagelistview
        contentView.post(new Runnable() {
            @Override
            public void run() {
                // mListBackgroundView's should fill its parent vertically
                // but the height of the content view is 0 on 'onCreate'.
                // So we should get it with post().
                mListBackgroundView.getLayoutParams().height = contentView.getHeight();//获取到contentVIew的高度
            }
        });
    }

    /**
     * 重点在该回调函数中处理
     * @param scrollY     scroll position in Y axis
     * @param firstScroll true when this is called for the first time in the consecutive motion events
     * @param dragging    true when the view is dragged and false when the view is scrolled in the inertia
     */
    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        // Translate overlay and image
        //图片最大的缩放范围
        float flexibleRange = mFlexibleSpaceImageHeight - mActionBarSize; //240-56 ? overlayout的缩放最大区间
        //overLayout移动到actionBar上方最远的位置（为负）
        int minOverlayTransitionY = mActionBarSize - mOverlayView.getHeight(); //最小的缩放值  56-240?
        //一般view的移动只需要找到最大值和最小值，就OK了，位置以左上方为准
        ViewHelper.setTranslationY(mOverlayView, ScrollUtils.getFloat(-scrollY, minOverlayTransitionY, 0));
        ViewHelper.setTranslationY(mImageView, ScrollUtils.getFloat(-scrollY, minOverlayTransitionY, 0));

        //ScrollUtils.getFloat先求前两个最大，再求与最后一个的最小
        // Translate list background
        //移动ListVIew的background
        //移动是为了动态的改变背景的高度,可以注释以下代码对比效果,且移动的最大高度就是mFlexibleSpaceImageHeight，最小无
        ViewHelper.setTranslationY(mListBackgroundView, Math.max(0, -scrollY + mFlexibleSpaceImageHeight));

        // Change alpha of overlay
        //动态改变覆盖主布局的布局的透明度，最小0，最大1
        ViewHelper.setAlpha(mOverlayView, ScrollUtils.getFloat((float) scrollY / flexibleRange, 0, 1));

        // Scale title text
//        Log.d("flexibleRange:",flexibleRange+"");
//        Log.d("scrollY:",scrollY+"");
//        Log.d("flexibleRange - scrollY:",flexibleRange - scrollY+"");
        //文字的最大放大比例 1.3 倍，最小比例 0
        float scale = 1 + ScrollUtils.getFloat((flexibleRange - scrollY) / flexibleRange, 0, MAX_TEXT_SCALE_DELTA);
        setPivotXToTitle();
        ViewHelper.setPivotY(mTitleView, 0);
        ViewHelper.setScaleX(mTitleView, scale);//缩放x轴和y轴
        ViewHelper.setScaleY(mTitleView, scale);

        // Translate title text
        //文字最大的Y轴移动距离，需根据其高度的放大倍数来计算
        int maxTitleTranslationY = (int) (mFlexibleSpaceImageHeight - mTitleView.getHeight() * scale);
        //文字滚动时移动的距离
        int titleTranslationY = maxTitleTranslationY - scrollY;
        //如果需要显示toolBar,则移动的最大距离不能为负，意思就是说文字的起点不能低于（0,0）坐标
        //否则 就能移动到Y坐标为负的位置
        if (TOOLBAR_IS_STICKY) {
            titleTranslationY = Math.max(0, titleTranslationY); //是否粘连toolBar只需在滚动距离为负的时候将其设置为O 就会在顶部显示他
        }
        ViewHelper.setTranslationY(mTitleView, titleTranslationY);

        // Translate FAB
        //fab可移动的最大距离
        int maxFabTranslationY = mFlexibleSpaceImageHeight - mFab.getHeight() / 2;
        float fabTranslationY = ScrollUtils.getFloat(
                -scrollY + mFlexibleSpaceImageHeight - mFab.getHeight() / 2,
                mActionBarSize - mFab.getHeight() / 2,
                maxFabTranslationY);
        //兼容API 11 前后
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // On pre-honeycomb, ViewHelper.setTranslationX/Y does not set margin,
            // which causes FAB's OnClickListener not working.
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mFab.getLayoutParams();
            lp.leftMargin = mOverlayView.getWidth() - mFabMargin - mFab.getWidth();
            lp.topMargin = (int) fabTranslationY;
            mFab.requestLayout();
        } else {
            ViewHelper.setTranslationX(mFab, mOverlayView.getWidth() - mFabMargin - mFab.getWidth());
           // Log.d("setTranslationX:", mOverlayView.getWidth() - mFabMargin - mFab.getWidth() + "");
            ViewHelper.setTranslationY(mFab, fabTranslationY);
        }

        // Show/hide FAB
        //移动的距离大于了 120dp 就隐藏fab,否则显示
        if (fabTranslationY < mFlexibleSpaceShowFabOffset) {
            hideFab();
        } else {
            showFab();
        }


        if (TOOLBAR_IS_STICKY) {//需要显示toolBar时
            // Change alpha of toolbar background
            //当需要显示toolBar时,则在移动到最后的距离小于等于ActionBarSize时将其透明度设置为 100%
            if (-scrollY + mFlexibleSpaceImageHeight <= mActionBarSize) {
                mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(1, mToolbarColor));
            } else {
                mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, mToolbarColor));
            }
        } else {//不需要显示toolBar时，移动toolBar
            // Translate Toolbar
            if (scrollY < mFlexibleSpaceImageHeight) {
                ViewHelper.setTranslationY(mToolbar, 0);
            } else {
                ViewHelper.setTranslationY(mToolbar, -scrollY);
            }
        }
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void setPivotXToTitle() {
        Configuration config = getResources().getConfiguration();
        if (Build.VERSION_CODES.JELLY_BEAN_MR1 <= Build.VERSION.SDK_INT
                && config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            ViewHelper.setPivotX(mTitleView, findViewById(android.R.id.content).getWidth());
        } else {
            ViewHelper.setPivotX(mTitleView, 0);
        }
    }

    private void showFab() {
        if (!mFabIsShown) {
            ViewPropertyAnimator.animate(mFab).cancel();
            ViewPropertyAnimator.animate(mFab).scaleX(1).scaleY(1).setDuration(200).start();
            mFabIsShown = true;
        }
    }

    private void hideFab() {
        if (mFabIsShown) {
            ViewPropertyAnimator.animate(mFab).cancel();
            ViewPropertyAnimator.animate(mFab).scaleX(0).scaleY(0).setDuration(200).start();
            mFabIsShown = false;
        }
    }
}
