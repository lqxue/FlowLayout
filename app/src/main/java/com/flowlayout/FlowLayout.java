package com.flowlayout;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 流式布局
 *
 * @author lqx Email:herolqx@126.com
 */
public class FlowLayout extends ViewGroup {

    /**
     * 一行中view之间的空隙
     */
    private int mHorizontalSpacing = dp2px(10);
    /**
     * 每行间隙
     */
    private int mVerticalSpacing = dp2px(10);

    /**
     * 记录所有的行,一行一行的存储,用于layout
     */
    private List<List<View>> allLines;

    /**
     * 记录每一行的行高,用于layout
     */
    List<Integer> lineHeights;

    /**
     * 在Java代码new FlowLayout
     *
     * @param context
     */
    public FlowLayout(Context context) {
        this(context, null);
    }

    /**
     * 通过xml加载FlowLayout
     *
     * @param context
     * @param attrs
     */
    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * 设置不同的主题
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        initMeasureParams();
        //测量子view

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        //获取流式布局父亲给流式布局的参考宽高大小
        int selfWidth = MeasureSpec.getSize(widthMeasureSpec);
        int selfHeight = MeasureSpec.getSize(heightMeasureSpec);

        //保存一行中所有view
        List<View> lineViews = new ArrayList<>();
        //记录这行已经使用的宽的size
        int lineWidthUsed = 0;
        //一行的行高
        int lineHeight = 0;

        //Measure过程中,子View要求父viewGroup的宽高
        int parentNeedWidth = 0;
        int parentNeedHeight = 0;

        //获取子View的个数
        int childCount = getChildCount();
        //遍历子View进行测量
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            LayoutParams childAtLayoutParams = childView.getLayoutParams();

            //获取子View的MeasureSpec
            int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight, childAtLayoutParams.width);
            int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, paddingTop + paddingBottom, childAtLayoutParams.height);

            //叫子View测量
            childView.measure(childWidthMeasureSpec, childHeightMeasureSpec);

            //获取子View的宽高
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            //通过宽来判断是否要换行,通过换行后的每行的行高来获取整个viewGroup的行高
            //需要换行
            if ((childMeasuredWidth + mHorizontalSpacing + lineWidthUsed) > selfWidth - paddingLeft - paddingRight) {
                //换行的时候记录每一行的view  用于layout
                allLines.add(lineViews);
                lineHeights.add(lineHeight);

                //一旦换行,我们就可以判断当前行和之前行的高度,加起来为流式布局ViewGroup需要的宽高
                parentNeedWidth = Math.max(parentNeedWidth, lineWidthUsed + childMeasuredWidth + mHorizontalSpacing);
                parentNeedHeight = parentNeedHeight + lineHeight + mVerticalSpacing;

                lineViews = new ArrayList<>();
                lineWidthUsed = 0;
                lineHeight = 0;

            }
            //view 是分行layout的 所以要记录每一行有哪些View,这样可以方便进行layout布局
            lineViews.add(childView);
            //每行都会有自己的宽高
            lineWidthUsed = lineWidthUsed + childMeasuredWidth + mHorizontalSpacing;
            lineHeight = Math.max(lineHeight, childMeasuredHeight);
        }

        //添加最后一行
        allLines.add(lineViews);
        lineHeights.add(lineHeight);

        //根据子Vew的度量结果,来重新度量自己 ViewGroup 作为一个 inGroup,它自已也是一个iew,它的大小也需要根据它的父亲给它提供的宽高来度量
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int realWidth = (widthMode == MeasureSpec.EXACTLY ? selfWidth : parentNeedWidth);
        int realHeight = (heightMode == MeasureSpec.EXACTLY ? selfHeight : parentNeedHeight);

        //测量自己
        setMeasuredDimension(realWidth, realHeight);
    }

    /**
     * 初始化Measure需要的参数
     */
    private void initMeasureParams() {
        if (allLines == null) {
            allLines = new ArrayList<>();
        } else {
            allLines.clear();
        }
        if (lineHeights == null) {
            lineHeights = new ArrayList<>();
        } else {
            lineHeights.clear();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int lineCount = allLines.size();

        int curL = getPaddingLeft();
        int curT = getPaddingTop();
        for (int i = 0; i < lineCount; i++) {
            List<View> lineViews = allLines.get(i);
            int lineHeight = lineHeights.get(i);
            for (int j = 0; j < lineViews.size(); j++) {
                View view = lineViews.get(j);
                int left = curL;
                int top = curT;

                //只有在layout()之后才能获取值
                // int right = left + view.getWidth();
                // int bottom = top + view.getHeight();

                //在setMeasuredDimension()之后能获取值
                int right = left + view.getMeasuredWidth();
                int bottom = top + view.getMeasuredHeight();
                view.layout(left, top, right, bottom);

                curL = right + mHorizontalSpacing;
            }
            curL = getPaddingLeft();
            curT = curT + lineHeight + mVerticalSpacing;
        }
    }

    public static int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }
}
