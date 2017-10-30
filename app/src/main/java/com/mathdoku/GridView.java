package com.mathdoku;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

import com.mathdoku.DLX.SolveType;
import com.mathdoku.MathDokuDLX;
import com.mathdoku.GridCell;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.DiscretePathEffect;
import android.graphics.Paint.Style;
import android.graphics.Paint;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View.OnTouchListener;
import android.view.View;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GridView extends View implements OnTouchListener, Runnable  {

    public Button digits[];
    public LinearLayout controls;
    private Animation outAnimation;
    private ActionBar mActionBar;

    public int mGridSize;
    public Random mRandom;
    public Context mContext;
    public MathDoku mMathDoku;

    public ArrayList<GridCage> mCages;
    public ArrayList<GridCell> mCells;

    private boolean mActive = false;
    private boolean paused = true;
    public boolean mSelectorShown = false;
    public boolean markInvalidMaybes = false;
    public boolean maybe3x3 = false;
    public float mTrackPosX;
    public float mTrackPosY;
    public GridCell mSelectedCell;
    public int mCurrentWidth;
    public Paint mGridPaint;
    public Paint mBorderPaint;
    public int mBackgroundColor;

    public boolean mDupedigits;
    public boolean mBadMaths;
    public boolean hideselector = false;

    // Date of current game (used for saved games)
    public long mDate;
    private Handler mHandler;
    public long mDuration = 0;
    public long mStartTime = 0;

    private String mFilename;
    private BufferedWriter writer = null;

    public GridView(Context context) {
        super(context);
        mContext = context;
        initGridView();
    }

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initGridView();
    }
    public GridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initGridView();
    }

    private void initGridView() {
        mDupedigits = true;
        mBadMaths = true;
        mStartTime = System.currentTimeMillis();

        outAnimation = AnimationUtils.loadAnimation(mContext, R.anim.selectorzoomout);
        outAnimation.setAnimationListener(new AnimationListener() {
            public void onAnimationEnd(Animation animation) {
              controls.setVisibility(View.GONE);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
          });

        mGridPaint = new Paint();
        mGridPaint.setColor(0x80000000);
        mGridPaint.setStrokeWidth(0);

        mBorderPaint = new Paint();
        mBorderPaint.setColor(0xFF000000);
        mBorderPaint.setStrokeWidth(3);
        mBorderPaint.setStyle(Style.STROKE);

        mCurrentWidth = 0;
        mGridSize = 0;
        setActive(false);
        setOnTouchListener((OnTouchListener) this);
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    public boolean isActive() {
        return mActive;
    }

    public void onPause() {
        if (mActive && !isSolved()) {
            mDuration = System.currentTimeMillis() - mStartTime;
        }
        mHandler = null;
        paused = true;
    }

    public void onResume(ActionBar ab) {
        mActionBar = ab;
        mStartTime = System.currentTimeMillis() - mDuration;
        mHandler = new Handler();
        if (isActive()) {
            mHandler.postDelayed(this, 0);
        }
        paused = false;
    }

    @Override
    public void run() {
        if (!mActive) {
            return;
        }
        if (!isSolved()) {
            mDuration = System.currentTimeMillis() - mStartTime;
            if (!paused) {
                mHandler.postDelayed(this, 1000);
            }
        }
        mActionBar.setSubtitle(DateUtils.formatElapsedTime(mDuration/1000));
    }

    public boolean clearInvalidPossibles() {
        boolean isChanged = false;
        for (GridCell cell : mCells) {
            for (int i = 1; i <= mGridSize;i++ ) {
                if (cell.mPossibles.contains(i) == false) {
                    continue;
                }
                int possible = i;
                if (getNumValueInRow(cell, possible) >= 1 ||
                        getNumValueInCol(cell, possible) >= 1) {
                    cell.togglePossible(possible);
                    if (cell.mPossibles.size() == 1) {
                        cell.setUserValue(cell.mPossibles.get(0));
                    }
                    isChanged = true;
                }
            }
        }
        return isChanged;
    }

    //重新创建一个游戏(唯一解)
    public synchronized void reCreate(boolean hideOperators) {
        //解决方案数
        int num_solns;
        //尝试次数
        int num_attempts = 0;
        //设置游戏不活动
        setActive(false);
        //开始计时
        mStartTime = System.currentTimeMillis();
        mRandom = new Random();
        //size至少是4
        if (mGridSize < 4) return;
        //只要解决方案大于1，则一直重复
        do {
            //装满了单元格
            mCells = new ArrayList<GridCell>();
            int cellnum = 0;
            //初始化所有单元格
            for (int i = 0 ; i < mGridSize * mGridSize ; i++)
                mCells.add(new GridCell(this, cellnum++));
            //随机生成所有数字
            randomiseGrid();
            mTrackPosX = mTrackPosY = 0;
            //初始化围笼们
            mCages = new ArrayList<GridCage>();
            //创建围笼们
            CreateCages(hideOperators);
            num_attempts++;
            //创建dlx对象
            MathDokuDLX mdd = new MathDokuDLX(mGridSize, mCages);
            // Stop solving as soon as we find multiple solutions
            //寻找相应的解决方案个数
            num_solns = mdd.Solve(SolveType.MULTIPLE);
            Log.d ("MathDoku", "Num Solns = " + num_solns);
        } while (num_solns > 1);
        Log.d ("MathDoku", "Num Attempts = " + num_attempts);
        //开启游戏
        setActive(true);
        mHandler.postDelayed(this, 0);
        mSelectorShown = false;
    }

  // Returns cage id of cell at row, column
  // Returns -1 if not a valid cell or cage
    public int CageIdAt(int row, int column) {
        if (row < 0 || row >= mGridSize || column < 0 || column >= mGridSize)
            return -1;
        return mCells.get(column + row * mGridSize).mCageId;
    }


    //创建单数字围笼
    public int CreateSingleCages(boolean hideOperators) {
        //单数字围笼的个数:取size的一半(太多或太少不容易产生唯一解?)
        int singles = mGridSize / 2;
        boolean RowUsed[] = new boolean[mGridSize];
        boolean ColUsed[] = new boolean[mGridSize];
        boolean ValUsed[] = new boolean[mGridSize];
        //选择singles个位置和值不重复的单元格
        for (int i = 0 ; i < singles ; i++) {
            GridCell cell;
            while (true) {
                cell = mCells.get(mRandom.nextInt(mGridSize * mGridSize));
                if (!RowUsed[cell.mRow] && !ColUsed[cell.mColumn] && !ValUsed[cell.mValue-1])
                    break;
            }
            ColUsed[cell.mColumn] = true;
            RowUsed[cell.mRow] = true;
            ValUsed[cell.mValue-1] = true;
            //给他们生成围笼对象,设置围笼id
            GridCage cage = new GridCage(this, GridCage.CAGE_1, hideOperators);
            cage.mCells.add(cell);
            cage.setArithmetic();
            cage.setCageId(i);
            mCages.add(cage);
        }
        return singles;
    }

    /* 用填好的grid随机生成围笼 */
    public void CreateCages(boolean hideOperators) {
        boolean restart;
        do {
            //不重新开始
            restart = false;
            //先搞几个单数字围笼
            int cageId = CreateSingleCages(hideOperators);
            //遍历所有单元格
            for (int cellNum = 0 ; cellNum < mCells.size() ; cellNum++) {
                GridCell cell = mCells.get(cellNum);
                if (cell.CellInAnyCage())
                    continue; // 单元格已经在某个围笼里了,跳过

                //拿到基于这个单元格的所有合法围笼
                ArrayList<Integer> possible_cages = getvalidCages(cell);
                //如果只有一个，说明是单数字围笼;放弃，从新开始
                if (possible_cages.size() == 1) {	// Only possible cage is a single
                    ClearAllCages();
                    restart=true;
                    break;
                }

                // 从可能的围笼里面随机拿一种(不要单一数字的)
                int cage_type = possible_cages.get(mRandom.nextInt(possible_cages.size()-1)+1);
                // 创建围笼,初始化
                GridCage cage = new GridCage(this, cage_type, hideOperators);
                int [][]cage_coords = GridCage.CAGE_COORDS[cage_type];
                for (int coord_num = 0; coord_num < cage_coords.length; coord_num++) {
                    int col = cell.mColumn + cage_coords[coord_num][0];
                    int row = cell.mRow + cage_coords[coord_num][1];
                    cage.mCells.add(getCellAt(row, col));
                }

                cage.setArithmetic();  // 设置计算方法
                cage.setCageId(cageId++);  // 设置围笼id
                mCages.add(cage);  // 加到围笼list里
            }
        } while (restart);
        for (GridCage cage : mCages)
            cage.setBorders();
    }

    //拿到有效的围笼？
    public ArrayList<Integer> getvalidCages(GridCell origin)
    {
        //如果已经在围笼里了，返回空
        if (origin.CellInAnyCage())
            return null;

        //布尔map，保存不合法的围笼类型
        boolean [] InvalidCages = new boolean[GridCage.CAGE_COORDS.length];

        // 不用检查第0类围笼;从1类围笼开始
        for (int cage_num=1; cage_num < GridCage.CAGE_COORDS.length; cage_num++) {
            //拿到围笼的坐标组
            int [][]cage_coords = GridCage.CAGE_COORDS[cage_num];
            // 检查如果把这个单元格放在这类围笼里，在这类围笼下的其他的单元格是否放到了其他围笼里
            // 不用检查第一个坐标(0,0)
            for (int coord_num = 1; coord_num < cage_coords.length; coord_num++) {
                int col = origin.mColumn + cage_coords[coord_num][0];
                int row = origin.mRow + cage_coords[coord_num][1];
                GridCell c = getCellAt(row, col);
                //如果单元格不存在||已经在别的围笼里的话,把这类围笼标记为无效;开始下一类围笼的检查
                if (c == null || c.CellInAnyCage()) {
                    InvalidCages[cage_num] = true;
                    break;
                }
            }
        }

        //这里面放的是合法的围笼的序号
        ArrayList<Integer> valid =  new ArrayList<Integer>();
        for (int i=0; i<GridCage.CAGE_COORDS.length; i++)
            if (!InvalidCages[i])
                valid.add(i);
        //返回所有合法围笼的序号
        return valid;
    }

    public void ClearAllCages() {
        for (GridCell cell : mCells) {
            cell.mCageId = -1;
            cell.mCageText = "";
        }
        mCages = new ArrayList<GridCage>();
    }

    public void clearUserValues() {
        for (GridCell cell : mCells) {
            cell.clearUserValue();
        }
        invalidate();
    }

    /* Fetch the cell at the given row, column */
    public GridCell getCellAt(int row, int column) {
        if (row < 0 || row >= mGridSize)
            return null;
        if (column < 0 || column >= mGridSize)
            return null;

        return mCells.get(column + row * mGridSize);
    }

    /*
     * 对整个grid填充随机数;规则:
     *
     * - 1~N填充
     * - 行列不能重复
     */
    public void randomiseGrid() {
        int attempts;
        for (int value = 1 ; value < mGridSize+1 ; value++) {
            for (int row = 0 ; row < mGridSize ; row++) {
                attempts = 20;
                GridCell cell;
                int column;
                while (true) {
                    column = mRandom.nextInt(mGridSize);
                    cell = getCellAt(row, column);
                    if (--attempts == 0)
                        break;
                    if (cell.mValue != 0)
                        continue;
                    if (valueInColumn(column, value))
                        continue;
                    break;
                }
                if (attempts == 0) {
                    clearValue(value--);
                    break;
                }
                cell.mValue = value;
                //Log.d("KenKen", "New cell: " + cell);
            }
        }
    }

    /* Clear any cells containing the given number. */
    public void clearValue(int value) {
        for (GridCell cell : mCells)
            if (cell.mValue == value)
                cell.mValue = 0;
    }

    /* Determine if the given value is in the given row */
    public boolean valueInRow(int row, int value) {
        for (int column=0; column< mGridSize; column++)
            if (mCells.get(column+row*mGridSize).mValue == value)
                return true;
        return false;
    }

    /* Determine if the given value is in the given column */
    public boolean valueInColumn(int column, int value) {
        for (int row=0; row< mGridSize; row++)
            if (mCells.get(column+row*mGridSize).mValue == value)
                return true;
        return false;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Our target grid is a square, measuring 80% of the minimum dimension
        int measuredWidth = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);

        int dim = Math.min(measuredWidth, measuredHeight);

        setMeasuredDimension(dim, dim);
    }

    private int measure(int measureSpec) {

        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED)
            return 180;
        else
            return specSize;
    }

    private void setPressedStates() {
        for (int i=0;i<mGridSize;i++) {
            digits[i].setPressed(false);
        }
        if (mSelectedCell.isUserValueSet()) {
            digits[mSelectedCell.getUserValue()-1].setPressed(true);
        } else {
            for (Integer i : mSelectedCell.mPossibles) {
                digits[i-1].setPressed(true);
            }
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (mGridSize < 4 || mCages == null ) {
            mActive = false;
            return;
        }

        int width = getMeasuredWidth();

        if (width != mCurrentWidth)
            mCurrentWidth = width;

        // Fill canvas background
        canvas.drawColor(mBackgroundColor);

        // Check cage correctness
        for (GridCage cage : mCages)
            cage.userValuesCorrect();

        // Draw (dashed) grid
        for (int i = 1 ; i < mGridSize ; i++) {
            float pos = ((float)mCurrentWidth / (float)mGridSize) * i;
            canvas.drawLine(0, pos, mCurrentWidth, pos, mGridPaint);
            canvas.drawLine(pos, 0, pos, mCurrentWidth, mGridPaint);
        }
        if (mSelectedCell != null && digits != null) {
            setPressedStates();
        }

        // Draw cells
        for (GridCell cell : mCells) {
            if ((cell.isUserValueSet() && getNumValueInCol(cell) > 1) ||
                    (cell.isUserValueSet() && getNumValueInRow(cell) > 1))
                cell.mShowWarning = true;
            else
                cell.mShowWarning = false;
            cell.onDraw(canvas, false);
        }

        // Draw borders
        canvas.drawLine(0, 1, mCurrentWidth, 1, mBorderPaint);
        canvas.drawLine(1, 0, 1, mCurrentWidth, mBorderPaint);
        canvas.drawLine(0, mCurrentWidth-2, mCurrentWidth, mCurrentWidth-2, mBorderPaint);
        canvas.drawLine(mCurrentWidth-2, 0, mCurrentWidth-2, mCurrentWidth, mBorderPaint);

        // Draw cells
        for (GridCell cell : mCells) {
            cell.onDraw(canvas, true);
        }

        if (mActive && isSolved()) {
            mMathDoku.Solved();
            if (mSelectedCell != null)
                mSelectedCell.mSelected = false;
            setActive(false);
        }
    }

    // Given a cell number, returns origin x,y coordinates.
    private float[] CellToCoord(int cell) {
        float xOrd;
        float yOrd;
        int cellWidth = mCurrentWidth / mGridSize;
        xOrd = ((float)cell % mGridSize) * cellWidth;
        yOrd = ((int)(cell / mGridSize) * cellWidth);
        return new float[] {xOrd, yOrd};
    }

    // Opposite of above - given a coordinate, returns the cell number within.
    private GridCell CoordToCell(float x, float y) {
        int row = (int) ((y / (float)mCurrentWidth) * mGridSize);
        int col = (int) ((x / (float)mCurrentWidth) * mGridSize);
        // Log.d("KenKen", "Track x/y = " + col + " / " + row);
        return getCellAt(row, col);
    }

    public void setMarkInvalidMaybes(boolean value) {
        markInvalidMaybes = value;
    }

    public void setMaybe3x3(boolean value) {
        maybe3x3 = value;
    }

    public void gridTouched(GridCell cell) {
        if (controls.getVisibility() == View.VISIBLE) {
            // digitSelector.setVisibility(View.GONE);
            if (hideselector) {
                controls.startAnimation(outAnimation);
                //cell.mSelected = false;
                mSelectorShown = false;
            }
            requestFocus();
        } else {
            if (hideselector) {
                controls.setVisibility(View.VISIBLE);
                Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.selectorzoomin);
                controls.startAnimation(animation);
                mSelectorShown = true;
            }
            controls.requestFocus();
        }
    }

    public boolean onTouch(View arg0, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return false;
        if (!mActive)
            return false;

        // Find out where the grid was touched.
        float x = event.getX();
        float y = event.getY();
        int size = getMeasuredWidth();

        int row = (int)((size - (size-y))/(size/mGridSize));
        if (row > mGridSize-1) row = mGridSize-1;
        if (row < 0) row = 0;

        int col = (int)((size - (size-x))/(size/mGridSize));
        if (col > mGridSize-1) col = mGridSize-1;
        if (col < 0) col = 0;

        // We can now get the cell.
        GridCell cell = getCellAt(row, col);
        if (mSelectedCell != cell)
            playSoundEffect(SoundEffectConstants.CLICK);
        mSelectedCell = cell;

        float[] cellPos = CellToCoord(cell.mCellNumber);
        mTrackPosX = cellPos[0];
        mTrackPosY = cellPos[1];

        for (GridCell c : mCells) {
            c.mSelected = false;
            mCages.get(c.mCageId).mSelected = false;
        }
        mSelectedCell.mSelected = true;
        mCages.get(mSelectedCell.mCageId).mSelected = true;
        gridTouched(mSelectedCell);
        invalidate();
        return false;
    }

    // Handle trackball, both press down, and scrolling around to
    // select a cell.
    public boolean onTrackballEvent(MotionEvent event) {
        if (!mActive || mSelectorShown)
            return false;
        // On press event, take selected cell, call touched listener
        // which will popup the digit selector.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mSelectedCell.mSelected = true;
            gridTouched(mSelectedCell);
            return true;
        }
        // A multiplier amplifies the trackball event values
        int trackMult = 70;
        switch (mGridSize) {
            case 5:
                trackMult = 60;
                break;
            case 6:
                trackMult = 50;
                break;
            case 7:
                trackMult = 40;
                break;
            case 8:
                trackMult = 40;
        }
        // Fetch the trackball position, work out the cell it's at
        float x = event.getX();
        float y = event.getY();
        mTrackPosX += x*trackMult;
        mTrackPosY += y*trackMult;
        GridCell cell = CoordToCell(mTrackPosX, mTrackPosY);
        if (cell == null) {
            mTrackPosX -= x*trackMult;
            mTrackPosY -= y*trackMult;
            return true;
        }
        // Set the cell as selected
        if (mSelectedCell != null) {
            mSelectedCell.mSelected = false;
            if (mSelectedCell != cell)
                gridTouched(cell);
        }
        for (GridCell c : mCells) {
            c.mSelected = false;
            mCages.get(c.mCageId).mSelected = false;
        }
        mSelectedCell = cell;
        cell.mSelected = true;
        mCages.get(mSelectedCell.mCageId).mSelected = true;
        invalidate();
        return true;
    }


    // Return the number of times a given user value is in a row
    public int getNumValueInRow(GridCell ocell) {
        int count = 0;
        for (GridCell cell : mCells) {
            if (cell.mRow == ocell.mRow && cell.getUserValue() == ocell.getUserValue())
                count++;
        }
        return count;
    }
    public int getNumValueInRow(GridCell ocell, int value) {
        int count = 0;
        for (GridCell cell : mCells) {
            if (cell.mRow == ocell.mRow && cell.getUserValue() == value)
                count++;
        }
        return count;
    }

    // Return the number of times a given user value is in a column
    public int getNumValueInCol(GridCell ocell) {
        int count = 0;
        for (GridCell cell : mCells) {
            if (cell.mColumn == ocell.mColumn && cell.getUserValue() == ocell.getUserValue())
                count++;
        }
        return count;
    }

    public int getNumValueInCol(GridCell ocell, int value) {
        int count = 0;
        for (GridCell cell : mCells) {
            if (cell.mColumn == ocell.mColumn && cell.getUserValue() == value)
                count++;
        }
        return count;
    }

    // Solve the puzzle by setting the Uservalue to the actual value
    public void Solve() {
        for (GridCell cell : mCells)
            cell.setUserValue(cell.mValue);
        invalidate();
    }

    // Returns whether the puzzle is solved.
    public boolean isSolved() {
        if (mCells == null) {
            return false;
        }
        for (GridCell cell : mCells)
            if (!cell.isUserValueCorrect())
                return false;
        return true;
    }

    // Checks whether the user has made any mistakes
    public boolean isSolutionValidSoFar()
    {
        for (GridCell cell : mCells)
            if (cell.isUserValueSet())
                if (cell.getUserValue() != cell.mValue)
                    return false;

        return true;
    }

    // Highlight those cells where the user has made a mistake
    public void markInvalidChoices()
    {
        boolean isValid = true;
        for (GridCell cell : mCells)
            if (cell.isUserValueSet())
                if (cell.getUserValue() != cell.mValue) {
                    cell.setInvalidHighlight(true);
                    isValid = false;
                }

        if (!isValid)
            invalidate();

        return;
    }

    // Return the list of cells that are highlighted as invalid
    public ArrayList<GridCell> invalidsHighlighted()
    {
        ArrayList<GridCell> invalids = new ArrayList<GridCell>();
        for (GridCell cell : mCells)
            if (cell.getInvalidHighlight())
                invalids.add(cell);

        return invalids;
    }

    private void writeCell(GridCell cell) throws java.io.IOException {
        writer.write("CELL:");
        writer.write(cell.mCellNumber + ":");
        writer.write(cell.mRow + ":");
        writer.write(cell.mColumn + ":");
        writer.write(cell.mCageText + ":");
        writer.write(cell.mValue + ":");
        writer.write(cell.getUserValue() + ":");
        for (int possible : cell.mPossibles) {
            writer.write(possible + ",");
        }
        writer.write("\n");
    }

    private void writeCage(GridCage cage) throws java.io.IOException {
        writer.write("CAGE:");
        writer.write(cage.mId + ":");
        writer.write(cage.mAction + ":");
        writer.write(cage.mResult + ":");
        writer.write(cage.mType + ":");
        for (GridCell cell : cage.mCells)
            writer.write(cell.mCellNumber + ",");
        writer.write(":" + cage.isOperatorHidden());
        writer.write("\n");
    }

    public synchronized boolean Save(String filename) {// Avoid saving game at the same time as creating puzzle
        String mFilename = mContext.getFilesDir() + File.separator + filename;
        try {
            writer = new BufferedWriter(new FileWriter(mFilename));
            long now = System.currentTimeMillis();
            writer.write(now + "\n");
            writer.write(mGridSize + "\n");
            writer.write(isActive() + "\n");
            writer.write("Duration:");
            if (!isSolved()) {
                mDuration = now - mStartTime;
            }
            writer.write(Long.toString(mDuration/1000) + "\n");
            for (GridCell cell : mCells) {
                writeCell(cell);
            }
            if (mSelectedCell != null) {
                writer.write("SELECTED:" + mSelectedCell.mCellNumber + "\n");
            }
            ArrayList<GridCell> invalidchoices = invalidsHighlighted();
            if (invalidchoices.size() > 0) {
                writer.write("INVALID:");
                for (GridCell cell : invalidchoices) {
                    writer.write(cell.mCellNumber + ",");
                }
                writer.write("\n");
            }
            for (GridCage cage : mCages) {
                writeCage(cage);
            }
        } catch (IOException e) {
            Log.d("MathDoku", "Error saving game: "+e.getMessage());
            return false;
        } finally {
            try {
                if (writer != null)
                    writer.close();
                writer = null;
            } catch (IOException e) {
                //pass
                return false;
            }
        }
        Log.d("MathDoku", "Saved game.");
        return true;
    }


    public static long ReadDate(String filename) {
        String mFilename = filename;
        BufferedReader br = null;
        InputStream ins = null;
        try {
            ins = new FileInputStream(new File(mFilename));
            br = new BufferedReader(new InputStreamReader(ins), 8192);
            return Long.parseLong(br.readLine());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ins.close();
                br.close();
            } catch (Exception e) {
                // Nothing.
                return 0;
            }
        }
        return 0;
    }

    GridCell readCell(String[] cellParts) {
        int cellNum = Integer.parseInt(cellParts[1]);
        GridCell cell = new GridCell(this, cellNum);
        cell.mRow = Integer.parseInt(cellParts[2]);
        cell.mColumn = Integer.parseInt(cellParts[3]);
        cell.mCageText = cellParts[4];
        cell.mValue = Integer.parseInt(cellParts[5]);
        cell.setUserValue(Integer.parseInt(cellParts[6]));
        if (cellParts.length == 8) {
            for (String possible : cellParts[7].split(",")) {
                cell.mPossibles.add(Integer.parseInt(possible));
            }
        }
        return cell;
    }

    GridCage readCage(String[] cageParts) {
        GridCage cage;
        if (cageParts.length >= 7) {
            cage = new GridCage(this,
                    Integer.parseInt(cageParts[4]),
                    Boolean.parseBoolean(cageParts[6]));
        } else {
            cage = new GridCage(this,
                    Integer.parseInt(cageParts[4]),
                    false);
        }
        cage.mId = Integer.parseInt(cageParts[1]);
        cage.mAction = Integer.parseInt(cageParts[2]);
        cage.mResult = Integer.parseInt(cageParts[3]);
        for (String cellId : cageParts[5].split(",")) {
            int cellNum = Integer.parseInt(cellId);
            GridCell c = mCells.get(cellNum);
            c.mCageId = cage.mId;
            cage.mCells.add(c);
        }
        return cage;
    }


    public boolean Restore(String filename) {
        String line = null;
        String mFilename = mContext.getFilesDir() + File.separator + filename;
        BufferedReader br = null;
        InputStream ins = null;
        String[] cellParts;
        String[] cageParts;
        File file = new File(mFilename);
        boolean isAutoSave =  filename.equals(MathDoku.savegamename) ? true : false;
        try {
            ins = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(ins), 8192);
            mDate = Long.parseLong(br.readLine());
            mGridSize = Integer.parseInt(br.readLine());
            if (br.readLine().equals("true")) {
                setActive(true);
            } else {
                setActive(false);
            }
            mCells = new ArrayList<GridCell>();
            mCages = new ArrayList<GridCage>();
            mSelectedCell = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Duration")) {
                    String parts[] = line.split(":");
                    mDuration = Long.parseLong(parts[1]);
                    mDuration *= 1000;
                    mStartTime = System.currentTimeMillis() - mDuration;
                    continue;
                }
                if (line.startsWith("CELL:")) {
                    cellParts = line.split(":");
                    GridCell cell = readCell(cellParts);
                    mCells.add(cell);
                }
                if (line.startsWith("SELECTED:")) {
                    int selected = Integer.parseInt(line.split(":")[1]);
                    mSelectedCell = mCells.get(selected);
                    mSelectedCell.mSelected = true;
                }
                if (line.startsWith("INVALID:")) {
                    String invalidlist = line.split(":")[1];
                    for (String cellId : invalidlist.split(",")) {
                        int cellNum = Integer.parseInt(cellId);
                        GridCell c = mCells.get(cellNum);
                        c.setInvalidHighlight(true);
                    }
                }
                if (line.startsWith("CAGE:")) {
                    cageParts = line.split(":");
                    GridCage cage = readCage(cageParts);
                    mCages.add(cage);
                }
            }

        } catch (FileNotFoundException e) {
            Log.d("Mathdoku", "FNF Error restoring game: " + e.getMessage());
            return false;
        } catch (IOException e) {
            Log.d("Mathdoku", "IO Error restoring game: " + e.getMessage());
            return false;
        } finally {
            try {
                ins.close();
                br.close();
                if (isAutoSave) {
                    file.delete();
                }
            } catch (Exception e) {
                // Nothing.
                return false;
            }
        }
        invalidate();
        return true;
    }
}
