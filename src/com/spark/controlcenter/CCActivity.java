package com.spark.controlcenter;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

public class CCActivity extends Activity {
	
	private ArrayList<String> arrayList;
    private EditText editText;
    private Button btnSend;
    private final static String TAG = "CCActivity";
    private TextView txtLog;
	private NetworkTask networktask;
	public boolean _bSocketStarted = false;
	private TextView txtStatus;
	private boolean mRequestToCancel = false;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control_center);
		
		arrayList = new ArrayList<String>();
		 
        editText = (EditText) findViewById(R.id.editText);        
        btnSend = (Button)findViewById(R.id.send_button);
        txtLog = (TextView)findViewById(R.id.txtLog);
        txtStatus = (TextView)findViewById(R.id.txtStatus);
        
        btnSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) { 
				if(!networktask.nsocket.isConnected())
					return;
                String message = editText.getText().toString();                
                //sends the message to the server
                networktask.sendData(message.getBytes());
            }
        });
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		mRequestToCancel = true;
		networktask.cancel(true);
		super.onPause();		
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		mRequestToCancel = false;
		networktask = new NetworkTask();
		networktask.execute();
		super.onResume();		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	            //newGame();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }

	}

	public class NetworkTask extends AsyncTask<Void, byte[], Boolean> {
		
		private Socket nsocket; //Network Socket
        private InputStream nis = null; //Network Input Stream
        private OutputStream nos = null; //Network Output Stream
        private byte[] writeData = null;

        @Override
        protected void onPreExecute() {
            //Log.i(TAG, "onPreExecute");
        }

        @Override
        protected Boolean doInBackground(Void... params) 
        { //This runs on a different thread
            boolean result = false;
            try {
                SocketAddress sockaddr = new InetSocketAddress("192.168.10.8", 1508);
                nsocket = new Socket();
                
                nsocket.connect(sockaddr, 5000); //5 second connection timeout
                
                if (nsocket.isConnected()) { 
                    nis = nsocket.getInputStream();
                    nos = nsocket.getOutputStream();
                    Log.i(TAG, "Connected: Waiting for data...");
                                        
                    final byte[] buffer = new byte[4096];
                    
                    Thread tRead = new Thread(new Runnable() {
						public void run() {
							int read;
							try {
								read = nis.read(buffer, 0, 4096);
								while(read != -1){
			                        byte[] tempdata = new byte[read];
			                        System.arraycopy(buffer, 0, tempdata, 0, read);
			                        publishProgress(tempdata);
			                        read = nis.read(buffer, 0, 4096); //This is blocking
			                        if(mRequestToCancel)
			                        {
			                        	sendData("BYE!".getBytes());
			                        }
			                    }	
							} catch (IOException e) {
								Log.e(TAG, e.getMessage());
							} //This is blocking
						} // run                    	
                    }); // Thread
                    tRead.run();                    
                } // isConnected
            } catch (Exception e) {
            	Log.e(TAG, "Exception " + e.getMessage());
                result = true;
            } 
            finally {
                try {
                	if(nis != null)
                		nis.close();
                	if(nos != null)
                		nos.close();
                	
                    nsocket.close();
                } catch (Exception e) {
                	Log.e(TAG, e.getMessage());
                	result = true;
                }
                Log.i(TAG, "Finished");
            }
            return result;
        }

        public boolean sendData(final byte[] cmd)
        { 
        	if(nsocket == null) return false;
        	if (!nsocket.isConnected()) return false;
        	if(cmd == null) return false;
        	
        	writeData = cmd;
        	Log.i(TAG, "Assigned Data");
        	
        	Thread tWrite = new Thread(new Runnable() {
				public void run() {
					if(writeData != null)
					{
						try {
							nos.write(writeData);
							Log.d(TAG, new String(writeData, "UTF-8"));
							writeData = null;
						} catch (IOException e) {
							Log.e(TAG, e.getMessage());
						}
					}
				} // run            	
            }); // thWrite
            tWrite.run();        	
        	return true;
        }
        
        @Override
        protected void onProgressUpdate(byte[]... values) {
            if (values.length > 0) {
                //Log.i(TAG, values[0].length + " bytes received.");
                txtLog.append(new String(values[0]));
            }
        }
        
        @Override
        protected void onCancelled() {
            Log.i(TAG, "Cancelled.");
            if (nsocket.isConnected()) 
            {
            	try {
            		if(nis != null)
                		nis.close();
                	if(nos != null)
                		nos.close();
	                nsocket.close();
				} catch (IOException e) {
					Log.e(TAG, e.getMessage());
				}                
            }
            btnSend.setVisibility(View.VISIBLE);
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                txtLog.setText("Not connected.");
            } else {
                Log.i(TAG, "Completed.");
            }
            btnSend.setVisibility(View.VISIBLE);
        }
    }
	

}
