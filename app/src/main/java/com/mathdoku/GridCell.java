package com.mathdoku;

import java.util.ArrayList;
import java.util.Collections;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

public class GridCell {
    // 单元格的序号;从左到右,从上到下,从0开始
    public int mCellNumber;
    // 单元格的X坐标,从0开始
    public int mColumn;
    // 单元格的Y坐标,从0开始
    public int mRow;
    // 像素位置X
    public float mPosX;
    // 像素位置Y
    public float mPosY;
    // 单元格里的值
    public int mValue;
    // 用户输入的值
    private int mUserValue;
    // 围笼的id
    public int mCageId;
    // 围笼的字符串
    public String mCageText;
    // 单元格所在的Grid
    public GridView mGridView;
    // 用户候选数
    public ArrayList<Integer> mPossibles;
    // 是否显示背景警告(行列重复)
    public boolean mShowWarning;
    // 是否是被选中的单元格
    public boolean mSelected;
    // 用户作弊(提示了这个单元格)
    public boolean mCheated;
    // 用户输入不正确时是否高亮
    private boolean mInvalidHighlight;

    //四种边界类型:1,无边界;2,固体边界;3,警告边界;4,选中围笼的边界
    public static final int BORDER_NONE = 0;
    public static final int BORDER_SOLID = 1;
    public static final int BORDER_WARN = 3;
    public static final int BORDER_CAGE_SELECTED = 4;

    //当前单元格的四个边界类型
    public int[] mBorderTypes;

    //各种画笔
    private Paint mValuePaint;
    private Paint mBorderPaint;
    private Paint mCageSelectedPaint;

    private Paint mWrongBorderPaint;
    private Paint mCageTextPaint;
    private Paint mPossiblesPaint;
    private Paint mWarningPaint;
    private Paint mCheatedPaint;
    private Paint mSelectedPaint;

    public GridCell(GridView gridView, int cell) {
        //上文GridView对象
        mGridView = gridView;
        //Grid的边长(其实就是游戏难度)
        int gridSize = mGridView.mGridSize;
        //单元格编号
        mCellNumber = cell;
        //单元格坐标
        mColumn = cell % gridSize;
        mRow = (int)(cell / gridSize);
        mCageText = "";
        mCageId = -1;
        mValue = 0;
        mUserValue = 0;
        mShowWarning = false;
        mCheated = false;
        mInvalidHighlight = false;

        mPosX = 0;
        mPosY = 0;

        mValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mValuePaint.setColor(0xFF000000);
        // mValuePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        mBorderPaint = new Paint();
        mBorderPaint.setColor(0xFF000000);
        mBorderPaint.setStrokeWidth(2);


        mWrongBorderPaint = new Paint();
        mWrongBorderPaint.setColor(0xFFBB0000);
        mWrongBorderPaint.setStrokeWidth(2);

        mCageSelectedPaint = new Paint();
        mCageSelectedPaint.setColor(0xFF9BCF00);
        mCageSelectedPaint.setStrokeWidth(2);

        mWarningPaint = new Paint();
        mWarningPaint.setColor(0x50FF0000);
        mWarningPaint.setStyle(Paint.Style.FILL);

        mCheatedPaint = new Paint();
        mCheatedPaint.setColor(0x90ffcea0);
        mCheatedPaint.setStyle(Paint.Style.FILL);

        mSelectedPaint = new Paint();
        mSelectedPaint.setColor(0xD0F0D042);
        mSelectedPaint.setStyle(Paint.Style.FILL);

        mCageTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCageTextPaint.setColor(0xFF0000A0);
        mCageTextPaint.setTextSize(14);
        //mCageTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        mPossiblesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPossiblesPaint.setColor(0xFF000000);
        mPossiblesPaint.setTextSize(10);
        mPossiblesPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

        //初始化用户候选数list
        mPossibles = new ArrayList<Integer>();
        //mPossibles.add(1);
        //mPossibles.add(2);
        //mPossibles.add(3);
        //mPossibles.add(4);

        //mPossibles.add(5);

        //设置一个初始的边界
        setBorders(BORDER_NONE, BORDER_NONE, BORDER_NONE, BORDER_NONE);
    }

    public String toString() {
        String str = "<cell:" + mCellNumber + " col:" + mColumn +
            " row:" + mRow + " posX:" + mPosX + " posY:" +
            mPosY + " val:" + mValue + ", userval: " + mUserValue + ">";
        return str;
    }

    /* 对四个边界的类型进行set
     *
     * 以下几个类型:BORDER_NONE, BORDER_SOLID, BORDER_WARN or BORDER_CAGE_SELECTED.
     */
    public void setBorders(int north, int east, int south, int west) {
        int[] borders = new int[4];
        borders[0] = north;
        borders[1] = east;
        borders[2] = south;
        borders[3] = west;
        mBorderTypes = borders;
    }

    /* 对单元格不同的边界类型，返回响应的画笔对象 */
    private Paint getBorderPaint(int border) {
        switch (mBorderTypes[border]) {
            case BORDER_NONE:
                return null;
            case BORDER_SOLID :
                return mBorderPaint;
            case BORDER_WARN :
                return mWrongBorderPaint;
            case BORDER_CAGE_SELECTED :
                return mCageSelectedPaint;
        }
        return null;
    }

    //可能值切换:如果没找到,则加进去;如果找到了,则删除
    public void togglePossible(int digit) {
        if (mPossibles.indexOf(new Integer(digit)) == -1)
            mPossibles.add(digit);
        else
            mPossibles.remove(new Integer(digit));
        Collections.sort(mPossibles);
    }

    //拿到用户输入值
    public int getUserValue() {
        return mUserValue;
    }

    //用户的输入是一组值吗？
    public boolean isUserValueSet() {
        return mUserValue != 0;
    }

    //设置一个用户值
    public void setUserValue(int digit) {
        mUserValue = digit;
        mInvalidHighlight = false;
    }

    //清除用户输入值
    public void clearUserValue() {
        setUserValue(0);
    }

    //用户的输入值正确吗？
    public boolean isUserValueCorrect()
    {
        return mUserValue == mValue;
    }

    /* Returns whether the cell is a member of any cage */
    // 这个单元格是否在围笼里？
    public boolean CellInAnyCage()
    {
        return mCageId != -1;
    }

    //设置是否进行不正确高亮
    public void setInvalidHighlight(boolean value) {
        mInvalidHighlight = value;
    }
    //获取是否进行不正确高亮
    public boolean getInvalidHighlight() {
        return mInvalidHighlight;
    }

    /* Draw the cell. Border and text is drawn. */
    //画单元格，边界和文字已经被画过了
    public void onDraw(Canvas canvas, boolean onlyBorders) {

        //单元格的像素长度
        float cellSize = (float)mGridView.getMeasuredWidth() / (float)mGridView.mGridSize;
        // 计算单元格的像素坐标(上左)
        mPosX = cellSize * mColumn;
        mPosY = cellSize * mRow;

        //四边的像素位置
        float north = mPosY;
        float south = mPosY + cellSize;
        float east = mPosX + cellSize;
        float west = mPosX;
        //拿到她上下左右的单元格
        GridCell cellAbove = mGridView.getCellAt(mRow-1, mColumn);
        GridCell cellLeft = mGridView.getCellAt(mRow, mColumn-1);
        GridCell cellRight = mGridView.getCellAt(mRow, mColumn+1);
        GridCell cellBelow = mGridView.getCellAt(mRow+1, mColumn);

        //如果不是只画边界
        if (!onlyBorders) {
            if ((mShowWarning && mGridView.mDupedigits) || mInvalidHighlight)
                canvas.drawRect(west + 1, north+1, east-1, south-1, mWarningPaint);
            if (mSelected)
                canvas.drawRect(west+1, north+1, east-1, south-1, mSelectedPaint);
            if (mCheated)
                canvas.drawRect(west+1, north+1, east-1, south-1, mCheatedPaint);
        } else {
            if (mBorderTypes[0] > 2)
                if (cellAbove == null)
                    north += 2;
                else
                    north += 1;
            if (mBorderTypes[3] > 2)
                if (cellLeft == null)
                    west += 2;
                else
                    west += 1;
            if (mBorderTypes[1] > 2)
                if (cellRight == null)
                    east -= 3;
                else
                    east -= 2;
            if (mBorderTypes[2] > 2)
                if (cellBelow == null)
                    south -= 3;
                else
                    south -= 2;
        }
        // North
        Paint borderPaint = getBorderPaint(0);
        if (!onlyBorders && mBorderTypes[0] > 2)
            borderPaint = mBorderPaint;
        if (borderPaint != null) {
            canvas.drawLine(west, north, east, north, borderPaint);
        }

        // East
        borderPaint = getBorderPaint(1);
        if (!onlyBorders && mBorderTypes[1] > 2)
            borderPaint = mBorderPaint;
        if (borderPaint != null)
            canvas.drawLine(east, north, east, south, borderPaint);

        // South
        borderPaint = getBorderPaint(2);
        if (!onlyBorders && mBorderTypes[2] > 2)
            borderPaint = mBorderPaint;
        if (borderPaint != null)
            canvas.drawLine(west, south, east, south, borderPaint);

        // West
        borderPaint = getBorderPaint(3);
        if (!onlyBorders && mBorderTypes[3] > 2)
            borderPaint = mBorderPaint;
        if (borderPaint != null) {
            canvas.drawLine(west, north, west, south, borderPaint);
        }

        if (onlyBorders)
            return;

        // 单元格的值
        if (isUserValueSet()) {
            int textSize = (int)(cellSize*3/4);
            mValuePaint.setTextSize(textSize);
            float leftOffset = cellSize/2 - textSize/4;
            float topOffset;
            topOffset = cellSize/2 + textSize/3;
            canvas.drawText("" + mUserValue, mPosX + leftOffset, mPosY + topOffset, mValuePaint);
        }

        int cageTextSize = (int)(cellSize/3);
        mCageTextPaint.setTextSize(cageTextSize);
        // 围笼的文字
        if (!mCageText.equals("")) {
            canvas.drawText(mCageText, mPosX + 2, mPosY + cageTextSize, mCageTextPaint);

            // canvas.drawText(mCageText, mPosX + 2, mPosY + 13, mCageTextPaint);
        }

        //如果有候选
        if (mPossibles.size()>1) {
            if (mGridView.maybe3x3) {
                mPossiblesPaint.setFakeBoldText(true);
                mPossiblesPaint.setTextSize((int)(cellSize/4.5));
                int xOffset = (int) (cellSize/3);
                int yOffset = (int) (cellSize/2) + 1;
                float xScale = (float) 0.21 * cellSize;
                float yScale = (float) 0.21 * cellSize;
                for (int i = 0 ; i < mPossibles.size() ; i++) {
                    int possible = mPossibles.get(i);
                    mPossiblesPaint.setColor(0xFF000000);
                    if (mGridView.markInvalidMaybes && (mGridView.getNumValueInRow(this, possible) >= 1 ||
                                mGridView.getNumValueInCol(this, possible) >= 1)) {
                        mPossiblesPaint.setColor(0x50FF0000);
                    }
                    float xPos = mPosX + xOffset + ((possible-1)%3)*xScale;
                    float yPos = mPosY + yOffset + ((int)(possible-1)/3)*yScale;
                    canvas.drawText(Integer.toString(possible), xPos, yPos, mPossiblesPaint);
                }
            }
            else {
                mPossiblesPaint.setFakeBoldText(false);
                mPossiblesPaint.setTextSize((int)((cellSize*1.5)/mPossibles.size()));
                int offset = 0;
                for (int i = 0 ; i < mPossibles.size() ; i++) {
                    int possible = mPossibles.get(i);
                    if (mGridView.markInvalidMaybes && (mGridView.getNumValueInRow(this, possible) >= 1 ||
                                mGridView.getNumValueInCol(this, possible) >= 1)) {
                        mPossiblesPaint.setColor(0x50FF0000);
                    } else {
                        mPossiblesPaint.setColor(0xFF000000);
                    }
                    canvas.drawText(Integer.toString(possible), mPosX+3+offset, mPosY + cellSize-5, mPossiblesPaint);
                    offset += mPossiblesPaint.measureText(Integer.toString(possible));
                }
            }
        }
    }

}
