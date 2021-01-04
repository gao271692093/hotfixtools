package com.glg.hotfixtools.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.glg.hotfixtools.R;
import aidl.FixStatusCallback;
import aidl.MyAIDLService;

public class MainActivity extends AppCompatActivity {

    public static String PACKAGE_NAME = "package_name";
    public static String PATCH_PATH = "patch_path";
    private String packagePath = "";
    private String patchPath = "";
    private EditText path;
    private Button connect;
    private Button fix;
    private EditText patch_path;
    private Button select;
    private Button close;
    private TextView app_info;
    private ImageView app_icon;
    private TextView status;
    private MyAIDLService aidlService;
    private boolean connected = false;
    private StringBuffer appInfo = new StringBuffer();
    private StringBuffer fixInfo = new StringBuffer();
    private int selectedId = 0;
    private String items[] = {"翼鲲应用市场","鲲翼Launcher","翼课教师","翼课堂教师","翼课学生","翼课学生HD","翼课堂学生","双语优榜"};
    private String packages[] = {"com.ekwing.appstore","com.android.launcher3","com.ekwing.intelligence.teachers","com.ekwing.wisdom.teacher",
            "com.ekwing.students","com.ekwing.studentshd","com.ekwing.wisdomclassstu","com.ekwing.scansheet"};
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connected = true;
            aidlService = MyAIDLService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected = false;
            aidlService = null;
            Toast.makeText(MainActivity.this, "已断开连接", Toast.LENGTH_SHORT).show();
        }
    };
    private FixStatusCallback fixStatusCallback = new FixStatusCallback.Stub() {
        @Override
        public void onLoad(int mode, final int code, final String info, final int handlePatchVersion) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fixInfo.append("Code:" + code + "  Info:" + info + "  PatchVersion:" + handlePatchVersion + "\n");
                    status.setText(fixInfo.toString());
                    switch (code) {
                        case 12:
                            fixInfo.replace(7, 11, "成功");
                            fixInfo.append("修复成功，请重启APP验证修复结果\n");
                            status.setText(fixInfo.toString());
                            Toast.makeText(MainActivity.this, "修复成功", Toast.LENGTH_SHORT).show();
                            break;
                        case 11://zip文件解压失败(本地加载方案不会出现该状态)
                        case 13://补丁加载失败（也就是修复失败）
                        case 17://请求不可用（本地加载方案不会出现该状态）
                        case 20://补丁无效或者补丁文件不存在
                            fixInfo.replace(7, 11, "失败");
                            if(code == 20) {
                                fixInfo.append("补丁无效或者补丁文件不存在\n");
                            }
                            status.setText(fixInfo.toString());
                            Toast.makeText(MainActivity.this, "修复失败", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            });
        }
    };
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.select:
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setIcon(R.drawable.hotfix);
                    builder.setTitle("请选择你想修复的APP");
                    builder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectedId = which;
                        }
                    });
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            path.setText(packages[selectedId]);
                            path.setSelection(path.getText().length());
                        }
                    });
                    builder.create().show();
                    break;
                case R.id.connect:
                    if(!TextUtils.isEmpty(path.getText().toString())) {
                        packagePath = path.getText().toString();
                        final PackageManager manager = getApplicationContext().getPackageManager();
                        Intent i = manager.getLaunchIntentForPackage(packagePath);
                        if(i != null) {
                            SharedPreferences.Editor editor = getPreferences(0).edit();
                            editor.putString(PACKAGE_NAME, packagePath);
                            editor.apply();
                            Intent intent = new Intent();
                            intent.setAction(packagePath + ".service.HotFixService");
                            //从 Android 5.0开始 隐式Intent绑定服务的方式已不能使用,所以这里需要设置Service所在服务端的包名
                            intent.setPackage(packagePath);//注意这里设置的包名是与服务端设置的package名是相同的
                            bindService(intent, connection, BIND_AUTO_CREATE);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if(!MainActivity.this.connected) {
                                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                                        if(appInfo.toString().endsWith("成功") || appInfo.toString().endsWith("失败")) {
                                            appInfo.replace(appInfo.length() - 2, appInfo.length(), "失败");
                                        } else {
                                            appInfo.append("\n连接失败");
                                        }
                                    } else {
                                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                                        if(appInfo.toString().endsWith("成功") || appInfo.toString().endsWith("失败")) {
                                            appInfo.replace(appInfo.length() - 2, appInfo.length(), "成功");
                                        } else {
                                            appInfo.append("\n连接成功");
                                        }
                                    }
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            app_info.setText(appInfo.toString());
                                        }
                                    });
                                }
                            }, 500);
                            appInfo.delete(0, appInfo.length());
                            try {
                                appInfo.append("test app information:\nname:"
                                        + manager.getApplicationInfo(packagePath, 0).loadLabel(manager)
                                        + "\npackageName:" + packagePath
                                        + "\nversion name:" + manager.getPackageInfo(packagePath, 0).versionName
                                );
                                app_info.setText(appInfo.toString());
                                app_icon.setImageDrawable(manager.getApplicationIcon(packagePath));
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "请填写正确的包名并确保应用正在运行", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case R.id.fix:
                    if(connected) {
                        if(TextUtils.isEmpty(patch_path.getText().toString())) {
                            Toast.makeText(MainActivity.this, "请输入补丁的绝对路径", Toast.LENGTH_SHORT).show();
                        } else {
                            patchPath = patch_path.getText().toString();
                            if(!patchPath.equals(getPreferences(0).getString(PATCH_PATH, ""))) {
                                SharedPreferences.Editor editor = getPreferences(0).edit();
                                editor.putString(PATCH_PATH, patch_path.getText().toString());
                                editor.apply();
                            }
                            fix();
                            fixInfo.delete(0, fixInfo.length());
                            fixInfo.append("修复状态：修复中...\n");
                            status.setText(fixInfo.toString());
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "请先连接需要修复的应用", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.close:
                    if(connected) {
                        try {
                            unbindService(connection);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(MainActivity.this, "已断开", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "当前未连接任何应用", Toast.LENGTH_SHORT).show();
                    }
                    if(!TextUtils.isEmpty(packagePath)) {
                        packagePath = "";
                        patchPath = "";
                        connected = false;
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
        setUpData();
    }

    private void initView() {
        path = findViewById(R.id.path);
        select = findViewById(R.id.select);
        connect = findViewById(R.id.connect);
        patch_path = findViewById(R.id.patch_path);
        fix = findViewById(R.id.fix);
        close = findViewById(R.id.close);
        app_info = findViewById(R.id.app_info);
        app_icon = findViewById(R.id.app_icon);
        status = findViewById(R.id.status);
    }

    private void initListener() {
        select.setOnClickListener(onClickListener);
        connect.setOnClickListener(onClickListener);
        fix.setOnClickListener(onClickListener);
        close.setOnClickListener(onClickListener);
    }

    private void setUpData() {
        path.setText(getPreferences(0).getString(PACKAGE_NAME, ""));
        patch_path.setText(getPreferences(0).getString(PATCH_PATH, ""));
    }

    private void fix() {
        try {
            aidlService.fix(patchPath, fixStatusCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
            fixInfo.replace(7, 10, "失败");
            status.setText(fixInfo.toString());
            Toast.makeText(MainActivity.this, "修复失败", Toast.LENGTH_SHORT).show();
        }
    }
}