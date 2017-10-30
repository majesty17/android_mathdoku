package com.mathdoku;

import java.util.ArrayList;
import java.util.Arrays;

import android.util.Log;

public class GridCage {

    //定义几种运算
    public static final int ACTION_NONE = 0;
    public static final int ACTION_ADD = 1;
    public static final int ACTION_SUBTRACT = 2;
    public static final int ACTION_MULTIPLY = 3;
    public static final int ACTION_DIVIDE = 4;

    public static final int CAGE_UNDEF = -1;
    //只有一个数字
    public static final int CAGE_1 = 0;

    // O = Origin (0,0) - must be the upper leftmost cell
    // X = Other cells used in cage
    // 几种围笼的样式，0表示原点
    public static final int [][][] CAGE_COORDS = new int[][][] {
        // O
        {{0,0}},
        // O
        // X
        {{0,0},{0,1}},
        // OX
        {{0,0},{1,0}},
        // O
        // X
        // X
        {{0,0},{0,1},{0,2}},
        // OXX
        {{0,0},{1,0},{2,0}},
        // O
        // XX
        {{0,0},{0,1},{1,1}},
        // O
        //XX
        {{0,0},{0,1},{-1,1}},
        // OX
        //  X
        {{0,0},{1,0},{1,1}},
        // OX
        // X
        {{0,0},{1,0},{0,1}},
        // OX
        // XX
        {{0,0},{1,0},{0,1},{1,1}},
        // OX
        // X
        // X
        {{0,0},{1,0},{0,1},{0,2}},
        // OX
        //  X
        //  X
        {{0,0},{1,0},{1,1},{1,2}},
        // O
        // X
        // XX
        {{0,0},{0,1},{0,2},{1,2}},
        // O
        // X
        //XX
        {{0,0},{0,1},{0,2},{-1,2}}
        /*
        // OXX
        // X
        {{0,0},{1,0},{2,0},{0,1}},
        // OXX
        //   X
        {{0,0},{1,0},{2,0},{2,1}},
        // O
        // XXX
        {{0,0},{0,1},{1,1},{2,1}},
        //  O
        //XXX
        {{0,0},{-2,1},{-1,1},{0,1}},
        // O
        // XX
        // X
        {{0,0},{0,1},{0,2},{1,1}},
        // O
        //XX
        // X
        {{0,0},{0,1},{0,2},{-1,1}},
        // OXX
        //  X
        {{0,0},{1,0},{2,0},{1,1}},
        // O
        //XXX
        {{0,0},{-1,1},{0,1},{1,1}},
        // OXXX
        {{0,0},{1,0},{2,0},{3,0}},
        // O
        // X
        // X
        // X
        {{0,0},{0,1},{0,2},{0,3}},
        // O
        // XX
        //  X
        {{0,0},{0,1},{1,1},{1,2}},
        // O
        //XX
        //X
        {{0,0},{0,1},{-1,1},{-1,2}},
        // OX
        //  XX
        {{0,0},{1,0},{1,1},{2,1}},
        // OX
        //XX
        {{0,0},{1,0},{0,1},{-1,1}}
        */
    };

    // 围笼的运算类型
    public int mAction;
    // 运算结果
    public int mResult;
    // 围笼所包含的单元格
    public ArrayList<GridCell> mCells;
    // 围笼类型
    public int mType;
    // 围笼的id
    public int mId;
    // 所在的gridview？
    public GridView mGridView;
    // 用户的计算是否正确
    public boolean mUserMathCorrect;
    // 围笼(或者它其中的单元格是否被选中
    public boolean mSelected;
    // 运算符是否隐藏
    private boolean mOperatorHidden;
    // 满足围笼运算的所有数组列表缓存 Cached list of numbers which satisfy the cage's arithmetic
    private ArrayList<int[]> mPossibles;

    public GridCage (GridView gridview, int type, boolean hiddenoperator) {
        mGridView = gridview;
        mType = type;
        mOperatorHidden = hiddenoperator;
        mPossibles = null;
        mUserMathCorrect = true;
        mSelected = false;
        mCells = new ArrayList<GridCell>();
    }

    public String toString() {
        String retStr = "";
        retStr += "Cage id: " + mId + ", Type: " + mType;
        retStr += ", Action: ";
        switch (mAction)
        {
        case ACTION_NONE:
            retStr += "None"; break;
        case ACTION_ADD:
            retStr += "Add"; break;
        case ACTION_SUBTRACT:
            retStr += "Subtract"; break;
        case ACTION_MULTIPLY:
            retStr += "Multiply"; break;
        case ACTION_DIVIDE:
            retStr += "Divide"; break;
        }
        retStr += ", Result: " + mResult;
        retStr += ", cells: ";
        for (GridCell cell : mCells)
            retStr += cell.mCellNumber + ", ";
        return retStr;
    }

    public boolean isOperatorHidden() {
        return mOperatorHidden;
    }

    public void setOperatorHidden(boolean operatorHidden) {
        mOperatorHidden = operatorHidden;
        mPossibles = null;	// Clear cached list of possible numbers
    }

    /*
     * 为围笼生成算法,半随机
     *
     * - 如果一个围笼的单元格数>=3,则只能是+和*
     * - 否则如果可以整除，则用除法，否则用减法
     *
     */
    public void setArithmetic() {
        mAction = -1;
        //如果只有一个单元格,则没有运算,结果等于唯一哪个单元格的值
        if (mType == CAGE_1) {
            mAction = ACTION_NONE;
            mResult = mCells.get(0).mValue;
            mCells.get(0).mCageText = "" + mResult;
            return;
        }

        //生成随机数0~1之间
        double rand = mGridView.mRandom.nextDouble();
        //默认的概率是+和*分别0.25;剩下的看俩数整除否
        double addChance = 0.25;
        double multChance = 0.5;
        //如果单元格数大于2,则+和*的概率对半
        if (mCells.size() > 2) {
            addChance = 0.5;
            multChance = 1.0;
        }

        if (rand <= addChance)
            mAction = ACTION_ADD;
        else if (rand <= multChance)
            mAction = ACTION_MULTIPLY;

        //处理*和+的计算结果
        if (mAction == ACTION_ADD) {
            int total = 0;
            for (GridCell cell : mCells) {
                total += cell.mValue;
            }
            mResult = total;
            if (mOperatorHidden)
                mCells.get(0).mCageText = mResult + "";
            else
                mCells.get(0).mCageText = mResult + "+";
        }
        if (mAction == ACTION_MULTIPLY) {
            int total = 1;
            for (GridCell cell : mCells) {
                total *= cell.mValue;
            }
            mResult = total;
            if (mOperatorHidden)
                mCells.get(0).mCageText = mResult + "";
            else
                mCells.get(0).mCageText = mResult + "x";
        }
        //如果有操作了，就返回
        if (mAction > -1) {
            return;
        }

        //处理2个单元格的情况
        if (mCells.size() < 2) {
            Log.d("KenKen", "Why only length 1? Type: " + this);
        }
        //拿到大的那个和小的那个
        int cell1Value = mCells.get(0).mValue;
        int cell2Value = mCells.get(1).mValue;
        int higher = cell1Value;
        int lower = cell2Value;
        boolean canDivide = false;
        if (cell1Value < cell2Value) {
            higher = cell2Value;
            lower = cell1Value;
        }
        //看看能不能整除,能的话为除法;不能的话为减法
        if (higher % lower == 0)
            canDivide = true;
        if (canDivide) {
            mResult = higher / lower;
            mAction = ACTION_DIVIDE;
            // mCells.get(0).mCageText = mResult + "\367";
            if (mOperatorHidden)
                mCells.get(0).mCageText = mResult + "";
            else
                mCells.get(0).mCageText = mResult + "/";
        } else {
            mResult = higher - lower;
            mAction = ACTION_SUBTRACT;
            if (mOperatorHidden)
                mCells.get(0).mCageText = mResult + "";
            else
                mCells.get(0).mCageText = mResult + "-";
        }
    }

    /*
     * 对围笼里面的所有单元格置一个围笼id
     */
    public void setCageId(int id) {
        mId = id;
        for (GridCell cell : mCells)
            cell.mCageId = mId;
    }

    //四个判断用户输入是否满足计算结果的方法
    public boolean isAddMathsCorrect()
    {
        int total = 0;
        for (GridCell cell : mCells) {
            total += cell.getUserValue();
        }
        return (total == mResult);
    }

    public boolean isMultiplyMathsCorrect()
    {
        int total = 1;
        for (GridCell cell : mCells) {
            total *= cell.getUserValue();
        }
        return (total == mResult);
    }

    public boolean isDivideMathsCorrect()
    {
        if (mCells.size() != 2)
            return false;

        if (mCells.get(0).getUserValue() > mCells.get(1).getUserValue())
            return mCells.get(0).getUserValue() == (mCells.get(1).getUserValue() * mResult);
        else
            return mCells.get(1).getUserValue() == (mCells.get(0).getUserValue() * mResult);
    }

    public boolean isSubtractMathsCorrect()
    {
        if (mCells.size() != 2)
            return false;

        if (mCells.get(0).getUserValue() > mCells.get(1).getUserValue())
            return (mCells.get(0).getUserValue() - mCells.get(1).getUserValue()) == mResult;
        else
            return (mCells.get(1).getUserValue() - mCells.get(0).getUserValue()) == mResult;
    }

    // 返回用户的输入值的计算结果否满足围笼的值
    public boolean isMathsCorrect() {
        if (mCells.size() == 1)
            return mCells.get(0).isUserValueCorrect();

        if (mOperatorHidden) {
            if (isAddMathsCorrect() || isMultiplyMathsCorrect() ||
                    isDivideMathsCorrect() || isSubtractMathsCorrect())
                return true;
            else
                return false;
        }
        else {
            switch (mAction) {
                case ACTION_ADD :
                    return isAddMathsCorrect();
                case ACTION_MULTIPLY :
                    return isMultiplyMathsCorrect();
                case ACTION_DIVIDE :
                    return isDivideMathsCorrect();
                case ACTION_SUBTRACT :
                    return isSubtractMathsCorrect();
            }
        }
        throw new RuntimeException("isSolved() got to an unreachable point " + mAction + ": " + toString());
    }

    // Determine whether user entered values match the arithmetic.
    //
    // Only marks cells bad if all cells have a uservalue, and they dont
    // match the arithmetic hint.
    public void userValuesCorrect() {
        mUserMathCorrect = true;
        for (GridCell cell : mCells)
            if (!cell.isUserValueSet()) {
                setBorders();
                return;
            }

        mUserMathCorrect = isMathsCorrect();
        setBorders();
    }

    /*
     * Sets the borders of the cage's cells.
     */
    public void setBorders() {
        for (GridCell cell : mCells) {
            for(int x = 0 ; x < 4 ; x++) {
                cell.mBorderTypes[x] = 0;
            }
            if (mGridView.CageIdAt(cell.mRow-1, cell.mColumn) != mId)
                if (!mUserMathCorrect && mGridView.mBadMaths)
                    cell.mBorderTypes[0] = GridCell.BORDER_WARN;
                else if (mSelected)
                    cell.mBorderTypes[0] = GridCell.BORDER_CAGE_SELECTED;
                else
                    cell.mBorderTypes[0] = GridCell.BORDER_SOLID;

            if (mGridView.CageIdAt(cell.mRow, cell.mColumn+1) != mId)
                if(!mUserMathCorrect && mGridView.mBadMaths)
                    cell.mBorderTypes[1] = GridCell.BORDER_WARN;
                else if (mSelected)
                    cell.mBorderTypes[1] = GridCell.BORDER_CAGE_SELECTED;
                else
                    cell.mBorderTypes[1] = GridCell.BORDER_SOLID;

            if (mGridView.CageIdAt(cell.mRow+1, cell.mColumn) != mId)
                if(!mUserMathCorrect && mGridView.mBadMaths)
                    cell.mBorderTypes[2] = GridCell.BORDER_WARN;
                else if (mSelected)
                    cell.mBorderTypes[2] = GridCell.BORDER_CAGE_SELECTED;
                else
                    cell.mBorderTypes[2] = GridCell.BORDER_SOLID;

            if (mGridView.CageIdAt(cell.mRow, cell.mColumn-1) != mId)
                if(!mUserMathCorrect && mGridView.mBadMaths)
                    cell.mBorderTypes[3] = GridCell.BORDER_WARN;
                else if (mSelected)
                    cell.mBorderTypes[3] = GridCell.BORDER_CAGE_SELECTED;
                else
                    cell.mBorderTypes[3] = GridCell.BORDER_SOLID;
        }
    }

    public ArrayList<int[]> getPossibleNums()
    {
        if (mPossibles == null) {
            if (mOperatorHidden)
                mPossibles = setPossibleNumsNoOperator();
            else
                mPossibles = setPossibleNums();
        }
        return mPossibles;
    }

    private ArrayList<int[]> setPossibleNumsNoOperator()
    {
        ArrayList<int[]> AllResults = new ArrayList<int[]>();

        if (mAction == ACTION_NONE) {
            assert (mCells.size() == 1);
            int number[] = {mResult};
            AllResults.add(number);
            return AllResults;
        }

        if (mCells.size() == 2) {
            for (int i1=1; i1<=mGridView.mGridSize; i1++)
                for (int i2=i1+1; i2<=mGridView.mGridSize; i2++)
                    if (i2 - i1 == mResult || i1 - i2 == mResult || mResult*i1 == i2 || mResult*i2 == i1 || i1+i2 == mResult || i1*i2 == mResult) {
                        int numbers[] = {i1, i2};
                        AllResults.add(numbers);
                        numbers = new int[] {i2, i1};
                        AllResults.add(numbers);
                    }
            return AllResults;
        }

        // ACTION_ADD:
        AllResults = getalladdcombos(mGridView.mGridSize,mResult,mCells.size());

        // ACTION_MULTIPLY:
        ArrayList<int[]> multResults = getallmultcombos(mGridView.mGridSize,mResult,mCells.size());

        // Combine Add & Multiply result sets
        for (int[] possibleset: multResults)
        {
            boolean foundset = false;
            for (int[] currentset: AllResults) {
                if (Arrays.equals(possibleset, currentset)) {
                    foundset = true;
                    break;
                }
            }
            if (!foundset)
                AllResults.add(possibleset);
        }

        return AllResults;
    }

    /*
     * Generates all combinations of numbers which satisfy the cage's arithmetic
     * and MathDoku constraints i.e. a digit can only appear once in a column/row
     */
    private ArrayList<int[]> setPossibleNums()
    {
        ArrayList<int[]> AllResults = new ArrayList<int[]>();

        switch (mAction) {
            case ACTION_NONE:
                assert (mCells.size() == 1);
                int number[] = {mResult};
                AllResults.add(number);
                break;
            case ACTION_SUBTRACT:
                assert(mCells.size() == 2);
                for (int i1=1; i1<=mGridView.mGridSize; i1++)
                    for (int i2=i1+1; i2<=mGridView.mGridSize; i2++)
                        if (i2 - i1 == mResult || i1 - i2 == mResult) {
                            int numbers[] = {i1, i2};
                            AllResults.add(numbers);
                            numbers = new int[] {i2, i1};
                            AllResults.add(numbers);
                        }
                break;
            case ACTION_DIVIDE:
                assert(mCells.size() == 2);
                for (int i1=1; i1<=mGridView.mGridSize; i1++)
                    for (int i2=i1+1; i2<=mGridView.mGridSize; i2++)
                        if (mResult*i1 == i2 || mResult*i2 == i1) {
                            int numbers[] = {i1, i2};
                            AllResults.add(numbers);
                            numbers = new int[] {i2, i1};
                            AllResults.add(numbers);
                        }
                break;
            case ACTION_ADD:
                AllResults = getalladdcombos(mGridView.mGridSize,mResult,mCells.size());
                break;
            case ACTION_MULTIPLY:
                AllResults = getallmultcombos(mGridView.mGridSize,mResult,mCells.size());
                break;
        }
        return AllResults;
    }

    // The following two variables are required by the recursive methods below.
    // They could be passed as parameters of the recursive methods, but this
    // reduces performance.
    private int[] numbers;
    private ArrayList<int[]> result_set;

    private ArrayList<int[]> getalladdcombos (int max_val, int target_sum, int n_cells)
    {
        numbers = new int[n_cells];
        result_set = new ArrayList<int[]> ();
        getaddcombos(max_val, target_sum, n_cells);
        return result_set;
    }

    /*
     * Recursive method to calculate all combinations of digits which add up to target
     *
     * @param max_val		maximum permitted value of digit (= dimension of grid)
     * @param target_sum	the value which all the digits should add up to
     * @param n_cells		number of digits still to select
     */
    private void getaddcombos(int max_val, int target_sum, int n_cells)
    {
        for (int n=1; n<= max_val; n++)
        {
            if (n_cells == 1)
            {
                if (n == target_sum) {
                    numbers[0] = n;
                    if (satisfiesConstraints(numbers))
                        result_set.add(numbers.clone());
                }
            }
            else {
                numbers[n_cells-1] = n;
                getaddcombos(max_val, target_sum-n, n_cells-1);
            }
        }
        return;
    }

    private ArrayList<int[]> getallmultcombos (int max_val, int target_sum, int n_cells)
    {
        numbers = new int[n_cells];
        result_set = new ArrayList<int[]> ();
        getmultcombos(max_val, target_sum, n_cells);

        return result_set;
    }

    /*
     * Recursive method to calculate all combinations of digits which multiply up to target
     *
     * @param max_val		maximum permitted value of digit (= dimension of grid)
     * @param target_sum	the value which all the digits should multiply up to
     * @param n_cells		number of digits still to select
     */
    private void getmultcombos(int max_val, int target_sum, int n_cells)
    {
        for (int n=1; n<= max_val; n++)
        {
            if (target_sum % n != 0)
                continue;

            if (n_cells == 1)
            {
                if (n == target_sum) {
                    numbers[0] = n;
                    if (satisfiesConstraints(numbers))
                        result_set.add(numbers.clone());
                }
            }
            else {
                numbers[n_cells-1] = n;
                getmultcombos(max_val, target_sum/n, n_cells-1);
            }
        }
        return;
    }

    /*
     * Check whether the set of numbers satisfies all constraints
     * Looking for cases where a digit appears more than once in a column/row
     * Constraints:
     * 0 -> (mGridSize * mGridSize)-1 = column constraints
     * (each column must contain each digit)
     * mGridSize * mGridSize -> 2*(mGridSize * mGridSize)-1 = row constraints
     * (each row must contain each digit)
     */
    private boolean satisfiesConstraints(int[] test_nums) {

        boolean constraints[] = new boolean[mGridView.mGridSize*mGridView.mGridSize*2];
        int constraint_num;
        for (int i = 0; i<mCells.size(); i++) {
            constraint_num = mGridView.mGridSize*(test_nums[i]-1) + mCells.get(i).mColumn;
            if (constraints[constraint_num])
                return false;
            else
                constraints[constraint_num]= true;
            constraint_num = mGridView.mGridSize*mGridView.mGridSize + mGridView.mGridSize*(test_nums[i]-1) + mCells.get(i).mRow;
            if (constraints[constraint_num])
                return false;
            else
                constraints[constraint_num]= true;
        }
        return true;
    }

}
