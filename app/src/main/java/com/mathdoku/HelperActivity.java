package com.mathdoku;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class HelperActivity extends Activity implements View.OnClickListener {

    EditText et_result = null;
    EditText et_has = null;
    EditText et_hasno = null;
    EditText et_size = null;
    EditText et_gamesize = null;
    CheckBox checkBox_ifhassame = null;
    Spinner spinner_action = null;
    TextView tv_all_result = null;
    Button btn_cal = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper);

        et_result = (EditText) findViewById(R.id.et_result);
        et_size = (EditText) findViewById(R.id.et_size);
        et_gamesize = (EditText) findViewById(R.id.et_gamesize);
        et_has = (EditText) findViewById(R.id.et_has);
        et_hasno = (EditText) findViewById(R.id.et_hasno);
        checkBox_ifhassame = (CheckBox) findViewById(R.id.checkBox_ifhassame);
        spinner_action = (Spinner) findViewById(R.id.spinner_action);
        tv_all_result = (TextView) findViewById(R.id.tv_all_result);
        btn_cal = (Button) findViewById(R.id.btn_cal);

        //从intent里拿到数据
        Intent intent = this.getIntent();
        int result = intent.getIntExtra("mResult", 0);
        int action = intent.getIntExtra("mAction", 0);
        int gridtype = intent.getIntExtra("mType", 0);
        int gamesize = intent.getIntExtra("mGamesize", 0);
        int size = 0;

        //设置操作符号
        List<String> list = new ArrayList<String>();
        list.add("加法 +");
        list.add("减法 -");
        list.add("乘法 x");
        list.add("除法 /");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner_action.setAdapter(adapter);

        //填写各项东西
        et_result.setText(result + "");
        et_gamesize.setText(gamesize + "");
        spinner_action.setSelection(action - 1);
        if (gridtype >= 0 && gridtype < GridCage.CAGE_COORDS.length) {
            size = GridCage.CAGE_COORDS[gridtype].length;
            et_size.setText("" + size);

        } else {
            Toast.makeText(this, "数据有误!", Toast.LENGTH_SHORT).show();
            return;
        }
        checkBox_ifhassame.setChecked(gridtype >= 5 && gridtype != 22 && gridtype != 23);

        tv_all_result.setText(getResult(result, size, gamesize, action, checkBox_ifhassame.isChecked(), et_has.getText().toString(), et_hasno.getText().toString()));

        btn_cal.setOnClickListener(this);
    }


    //核心计算功能
    private String getResult(int result, int size, int gamesize, int action, boolean hassame, String has, String hasno) {
        //
        Log.d("MathDoku", "in getResult()");
        Log.d("MathDoku", "result:" + result);
        Log.d("MathDoku", "size:" + size);
        Log.d("MathDoku", "gamesize:" + gamesize);
        Log.d("MathDoku", "action:" + action);
        Log.d("MathDoku", "hassame:" + hassame);
        Log.d("MathDoku", "has:" + has + "; hasno" + hasno);

        if (size > 4) {
            return "个数超过4，太卡了。。";
        }

        List<String> list = new ArrayList<>();
        //1，用前三个数来枚举
        switch (action) {/*           + - x /             */
            case 1:
                for (int i = (int) Math.pow(10.0, size) / 9; i < Math.pow(10.0, size); i++) {
                    Log.d("MathDoku", "i is " + i);
                    int sum = 0, num = i, last = Integer.MAX_VALUE, j;
                    for (j = 0; j < size; j++) {
                        int mowei = num % 10;
                        if (mowei > last || mowei > gamesize)
                            break;
                        sum += mowei;
                        num = num / 10;
                        last = mowei;
                    }
                    if (sum == result && j == size)
                        list.add(i + "");
                }

                break;
            case 2:
                for (int i = 1; i + result <= gamesize; i++) {
                    list.add(i + "" + (i + result));
                }
                break;
            case 3:
                for (int i = (int) Math.pow(10.0, size) / 9; i < Math.pow(10.0, size); i++) {
                    Log.d("MathDoku", "i is " + i);
                    int sum = 1, num = i, last = Integer.MAX_VALUE, j;
                    for (j = 0; j < size; j++) {
                        int mowei = num % 10;
                        if (mowei > last || mowei > gamesize)
                            break;
                        sum *= mowei;
                        num = num / 10;
                        last = mowei;
                    }
                    if (sum == result && j == size)
                        list.add(i + "");
                }
                break;
            case 4:
                for (int i = 1; i <= gamesize && i * result <= gamesize; i++) {
                    list.add(i + "" + (i * result));
                }
                break;
            default:
                return "操作符不合法";
        }


        //2，用后三者来过滤
        //2.1 干掉超过2个连续一样的，根据是否可以一样确定是否删除一个一样的
        for (int i = list.size() - 1; i >= 0; i--) {
            String str = list.get(i);
            int same_count = 0;
            for (int j = 0; j < str.length() - 1; j++) {
                if (str.charAt(j) == str.charAt(j + 1)) {
                    same_count++;
                }
            }
            if (same_count >= 2 || (same_count==1 && hassame==false))
                list.remove(i);
        }

        //2.3 处理has&hasno
        for (int i = list.size() - 1; i >= 0; i--) {
            String str = list.get(i);
            for (int j = 0; j < has.length(); j++) {
                if (str.indexOf(has.charAt(j)) < 0) {
                    list.remove(i);
                    break;
                }
            }
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            String str = list.get(i);
            for (int j = 0; j < hasno.length(); j++) {
                if (str.indexOf(hasno.charAt(j)) >= 0) {
                    list.remove(i);
                    break;
                }
            }
        }

        //3拼sring
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i) + ", ");
        }

        return sb.toString();
    }

    @Override
    public void onClick(View v) {
        int result = Integer.parseInt(et_result.getText().toString());
        int size = Integer.parseInt(et_size.getText().toString());
        int gamesize = Integer.parseInt(et_gamesize.getText().toString());
        int action = spinner_action.getSelectedItemPosition() + 1;

        String ret = getResult(result, size, gamesize, action, checkBox_ifhassame.isChecked(), et_has.getText().toString(), et_hasno.getText().toString());
        tv_all_result.setText(ret);
    }
}
