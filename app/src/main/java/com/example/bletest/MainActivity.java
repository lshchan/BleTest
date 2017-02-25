package com.example.bletest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.UUID;
/***
 *

 *
 */
public class MainActivity extends Activity {

    TextView t1,t2,t3;
    EditText et;
    Button SettingName,ON,ON_MT,Display,Avaliable_Time,Start_search,Start_Seve,Link_to_Sever,Send;
    ListView listView;
    private IntentFilter intentFilter1,intentFilter2,intentFilter3,intentFilter4;
    private MyReceiver mMyReceiver;
    private SearchReceiver mSearchStart;
    private SearchReceiver mSearchStop;
    private SearchDeviceReceiver mSearchDeviceRecevier;

    ArrayList<BluetoothDevice>deList=new ArrayList<BluetoothDevice>();
    int linkNo;



    //连接后获取输出流
    OutputStream outputStream;
    BluetoothAdapter bluetooth;
    String uuid="a60f35f0-b93a-11de-8a39-08002009c666";//服务端蓝牙设备的UUID
    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==-1){
                Toast.makeText(getApplicationContext(), "等待用户连接。。。。", Toast.LENGTH_SHORT).show();
            }
            if(msg.what==-2){
                Toast.makeText(getApplicationContext(), "已与用户连接。。。。", Toast.LENGTH_SHORT).show();
            }
            if(msg.what==-3){
                Toast.makeText(getApplicationContext(), "等待服务端接受。。。。", Toast.LENGTH_SHORT).show();
            }
            if(msg.what==-4){
                Toast.makeText(getApplicationContext(), "服务端已接受。。。。", Toast.LENGTH_SHORT).show();
            }
            if(msg.what==1){

                Toast.makeText(getApplicationContext(), "接收"+(String)msg.obj, Toast.LENGTH_SHORT).show();
            }
            if(msg.what==2){

                Toast.makeText(getApplicationContext(), "发送 "+(String)msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        t1=(TextView) findViewById(R.id.t1);
        t2=(TextView) findViewById(R.id.t2);
        t3=(TextView) findViewById(R.id.t3);
        et=(EditText) findViewById(R.id.et);
        SettingName=(Button) findViewById(R.id.SettingName);
        ON=(Button) findViewById(R.id.ON);
        ON_MT=(Button) findViewById(R.id.ON_MT);
        Display=(Button) findViewById(R.id.Display);
        Avaliable_Time=(Button) findViewById(R.id.Avaliable_Time);
        Start_search=(Button) findViewById(R.id.Start_search);
        Start_Seve=(Button) findViewById(R.id.Start_Seve);
        Link_to_Sever=(Button) findViewById(R.id.Link_to_Sever);
        Send=(Button) findViewById(R.id.Send);
        listView=(ListView)findViewById(R.id.listview);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                linkNo=position;
                String xsn=deList.get(position).getName();//2
                AlertDialog.Builder dialog=new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("连接到服务器");
                dialog.setMessage(xsn);
                dialog.setCancelable(false);
                dialog.setPositiveButton("连接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        linktoServer();
                        Log.d("linktoSever",String.valueOf(position));
                    }
                });
                dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialog.show();
            }
        });

        //安卓手机可能有多个蓝牙适配器，目前只能使用默认蓝牙适配器，通过该方法可以获取到默认适配器
        bluetooth=BluetoothAdapter.getDefaultAdapter();



        //启用和禁用BluetoothAdapter是耗时的异步操作，需注册Receiver监听状态变化
        intentFilter1=new IntentFilter();
        intentFilter1.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mMyReceiver=new MyReceiver();
        registerReceiver(mMyReceiver,intentFilter1);

        //调用startDiscovery()方法可以开启设备扫描，注册该Receiver可以收到扫描状态变化（开始扫描还是结束扫描）
        intentFilter2=new IntentFilter();
        intentFilter2.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        mSearchStart=new SearchReceiver();
        registerReceiver(mSearchStart,intentFilter2);

        //调用startDiscovery()方法可以开启设备扫描，注册该Receiver可以收到扫描到的设备
        intentFilter3=new IntentFilter();
        intentFilter3.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mSearchStop=new SearchReceiver();
        registerReceiver(mSearchStop,intentFilter3);

        //通过(BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        //可以获得扫描到的设备
        intentFilter4=new IntentFilter();
        intentFilter4.addAction(BluetoothDevice.ACTION_FOUND);
        mSearchDeviceRecevier=new SearchDeviceReceiver();
        registerReceiver(mSearchDeviceRecevier,intentFilter4);

        SettingName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(bluetooth.setName(et.getText().toString())){
                    Toast.makeText(getApplicationContext(), "设置成功", Toast.LENGTH_SHORT).show();
                }else
                    Toast.makeText(getApplicationContext(), "设置失败", Toast.LENGTH_SHORT).show();
            }
        });
        //加入    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>权限
        //后可以直接使用enable()方法启动本机蓝牙适配器，并且可以修改蓝牙友好名称
        ON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                bluetooth.enable();
            }
        });
        //使用下面语句可以弹出对话框提示是否开启，onActivityResult()方法可获取用户选项
        ON_MT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
            }
        });
        Display.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(bluetooth.isEnabled()){
                    t1.setText("地址"+bluetooth.getAddress());
                    t2.setText("名称"+bluetooth.getName());
                    t3.setText("可见性"+bluetooth.getScanMode());
                }else
                    Toast.makeText(getApplicationContext(), "蓝牙不可用", Toast.LENGTH_SHORT).show();
            }
        });
        //修改本机蓝牙可见时间
        Avaliable_Time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1200);
                startActivityForResult(intent, 2);
            }
        });
        Start_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(bluetooth.isEnabled()){
                    deList.clear();
                    bluetooth.startDiscovery();
                }
            }
        });
        Start_Seve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                createServer();

            }

        });

        Link_to_Sever.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                linktoServer();
            }

        });

        Send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                send();
            //    Toast.makeText(MainActivity.this,"Send,,,,",Toast.LENGTH_SHORT).show();
            }

        });



    }

    private void linktoServer() {
        try {
            BluetoothDevice bluetoothDevice=deList.get(linkNo);//1
            String xsn=deList.get(linkNo).getName();//2
            Log.d("dList",xsn);//2
            bluetoothDevice.getAddress();
            final BluetoothSocket bluetoothSocket=bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
            new Thread(){
                @Override
                public void run() {
                    try {
                        handler.sendEmptyMessage(-3);
                        bluetoothSocket.connect();
                        handler.sendEmptyMessage(-4);
                        outputStream=bluetoothSocket.getOutputStream();
                        getInfo(bluetoothSocket);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("TAG","NO LINK");
                    }
                };
            }.start();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
    //创建服务端，类似于Socket编程
    private void createServer()  {
        Log.e("TAG","createSever....");
        try {
            final BluetoothServerSocket btserver = bluetooth.listenUsingRfcommWithServiceRecord("server", UUID.fromString(uuid));
            new Thread(){
                @Override
                public void run() {
                    Log.e("TAG","createSever1....");
                    try {
                        while(true){
                            Log.e("TAG","createSever2....");
                            handler.sendEmptyMessage(-1);
                            final BluetoothSocket serverSocket = btserver.accept();//阻塞式方法，需开线程
                            Log.e("TAG","createSever21....");
                            handler.sendEmptyMessage(-2);
                            Log.e("TAG","createSever3....");
                            outputStream=serverSocket.getOutputStream();
                            new Thread(){//当有新连接时，交给新线程完成
                                public void run() {
                                    getInfo(serverSocket);
                                }
                            }.start();
                            Log.d("TAG","createSever sucess");
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        Log.d("TAG","createSever fail");
                    }

                };
            }.start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    private void send() {
        String ss=et.getText().toString().trim();
        Log.d("send",ss);
        try {
            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(outputStream));
            bw.write(ss);
            bw.newLine();//注意换行
            bw.flush();//刷新缓存
            Message msg=new Message();
            msg.obj=ss;
            msg.what=2;
            handler.sendMessage(msg);
            Toast.makeText(MainActivity.this,"sending"+ss,Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(MainActivity.this,"error sending: "+ss,Toast.LENGTH_SHORT).show();
        }
    }
    private void getInfo(BluetoothSocket serverSocket) {

        try {
            InputStream inputStream=serverSocket.getInputStream();
            BufferedReader br=new BufferedReader(new InputStreamReader(inputStream));
            while(true){
                String msg=br.readLine();
                Message msgs=new Message();
                msgs.obj=msg;
                msgs.what=1;
                handler.sendMessage(msgs);

            }
        } catch (IOException e) {
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1&&resultCode==RESULT_OK){
            Toast.makeText(getApplicationContext(), "蓝牙已启用（onActivityResult）", Toast.LENGTH_SHORT).show();
        }
        if(requestCode==2&&resultCode!=RESULT_CANCELED){
            Toast.makeText(getApplicationContext(), "蓝牙可见时间已修改"+requestCode+"（onActivityResult）", Toast.LENGTH_SHORT).show();
        }

    }

    private class MyReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context arg0, Intent intent) {
            int state=intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if(state==BluetoothAdapter.STATE_TURNING_ON)
                Toast.makeText(getApplicationContext(), "蓝牙正在开启", Toast.LENGTH_SHORT).show();
            if(state==BluetoothAdapter.STATE_ON)
                Toast.makeText(getApplicationContext(), "蓝牙已经开启", Toast.LENGTH_SHORT).show();
            if(state==BluetoothAdapter.STATE_TURNING_OFF)
                Toast.makeText(getApplicationContext(), "蓝牙正在关闭", Toast.LENGTH_SHORT).show();
            if(state==BluetoothAdapter.STATE_OFF)
                Toast.makeText(getApplicationContext(), "蓝牙已关闭", Toast.LENGTH_SHORT).show();
        }

    }
    private class SearchReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context arg0, Intent intent) {
            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())){
                Toast.makeText(getApplicationContext(), "启动发现。。。", Toast.LENGTH_SHORT).show();
            }
            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())){
                Toast.makeText(getApplicationContext(), "发现结束。。。", Toast.LENGTH_SHORT).show();
            }
        }

    }
    private class SearchDeviceReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context arg0, Intent intent) {
            et.append(intent.getStringExtra(BluetoothDevice.EXTRA_NAME)+": "+"\n");
            Toast.makeText(getApplicationContext(), intent.getStringExtra(BluetoothDevice.EXTRA_NAME), Toast.LENGTH_SHORT).show();

            try{
                deList.add((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                Toast.makeText(getApplicationContext(), "发现设备", Toast.LENGTH_SHORT).show();
            }catch(Exception e){}
//bListView
            int xim=deList.size();
            String [] data=new String[deList.size()];
            for (int i = 0; i <xim ; i++) {
                data[i]=deList.get(i).getName();
            }
            ArrayAdapter<String> adapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,data);
            listView.setAdapter(adapter);
//endListView
        }




    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mMyReceiver);
        unregisterReceiver(mSearchStart);
        unregisterReceiver(mSearchStop);
        unregisterReceiver(mSearchDeviceRecevier);
        }
}