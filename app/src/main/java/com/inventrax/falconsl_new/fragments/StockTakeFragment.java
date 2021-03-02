package com.inventrax.falconsl_new.fragments;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cipherlab.barcode.GeneralString;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.ScannerUnavailableException;
import com.honeywell.aidc.TriggerStateChangeEvent;
import com.honeywell.aidc.UnsupportedPropertyException;
import com.inventrax.falconsl_new.R;
import com.inventrax.falconsl_new.activities.MainActivity;
import com.inventrax.falconsl_new.common.Common;
import com.inventrax.falconsl_new.common.constants.EndpointConstants;
import com.inventrax.falconsl_new.common.constants.ErrorMessages;
import com.inventrax.falconsl_new.interfaces.ApiInterface;
import com.inventrax.falconsl_new.pojos.StockTake;
import com.inventrax.falconsl_new.pojos.StockTakeDetails;
import com.inventrax.falconsl_new.pojos.WMSCoreMessage;
import com.inventrax.falconsl_new.pojos.WMSExceptionMessage;
import com.inventrax.falconsl_new.room.AppDatabase;
import com.inventrax.falconsl_new.room.RoomAppDatabase;
import com.inventrax.falconsl_new.room.StockTakeDAO;
import com.inventrax.falconsl_new.room.StockTakeTable;
import com.inventrax.falconsl_new.services.RetrofitBuilderHttpsEx;
import com.inventrax.falconsl_new.util.DialogUtils;
import com.inventrax.falconsl_new.util.ExceptionLoggerUtils;
import com.inventrax.falconsl_new.util.FragmentUtils;
import com.inventrax.falconsl_new.util.ProgressDialogUtils;
import com.inventrax.falconsl_new.util.ScanValidator;
import com.inventrax.falconsl_new.util.SoundUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Padmaja Rani.B on 19/12/2018
 */

public class StockTakeFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener {

    private static final String classCode = "API_FRAG_StockTake";
    private View rootView;
    private CardView cvScanPallet, cvScanSku, cvScanDock;
    private ImageView ivScanPallet, ivScanSku, ivScanDock;
    private EditText etBin, etCarton;
    private TextView lblScannedSku;

    private Button btnClear, btnSubmit;
    DialogUtils dialogUtils;
    FragmentUtils fragmentUtils;
    private Common common = null;
    String scanner = null;
    String getScanner = null;
    private IntentFilter filter;
    private ScanValidator scanValidator;
    private Gson gson;
    private WMSCoreMessage core;
    private String Materialcode = null;
    private String userId = null, scanType = null, accountId = null;
    int warehouseID = 0, tenantID = 0;

    SoundUtils sound = null;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    private boolean isLocation = false, isContanierScanned = false, isRsnScanned = false;
    SoundUtils soundUtils;

    StockTakeDAO db;
    AppDatabase dataBase;

    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;

    public Button btn_ok;
    public EditText et_qty;


    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanner = intent.getStringExtra(GeneralString.BcReaderData);  // Scanned Barcode info
            ProcessScannedinfo(scanner.trim().toString());
        }
    };


    public void myScannedData(Context context, String barcode) {
        try {
            ProcessScannedinfo(barcode.trim());
        } catch (Exception e) {
            //  Toast.makeText(context, ""+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public StockTakeFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_stocktake, container, false);
        barcodeReader = MainActivity.getBarcodeObject();
        loadFormControls();
        return rootView;

    }

    // Form controls
    private void loadFormControls() {

        cvScanPallet = (CardView) rootView.findViewById(R.id.cvScanPallet);
        cvScanSku = (CardView) rootView.findViewById(R.id.cvScanSku);
        cvScanDock = (CardView) rootView.findViewById(R.id.cvScanDock);

        ivScanPallet = (ImageView) rootView.findViewById(R.id.ivScanPallet);
        ivScanSku = (ImageView) rootView.findViewById(R.id.ivScanSku);
        ivScanDock = (ImageView) rootView.findViewById(R.id.ivScanBin);

        btnSubmit = (Button) rootView.findViewById(R.id.btnSubmit);

        etBin = (EditText) rootView.findViewById(R.id.etBin);
        etCarton = (EditText) rootView.findViewById(R.id.etCarton);

        lblScannedSku = (TextView) rootView.findViewById(R.id.lblScannedSku);


        /*etQty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    MainActivity mainActivity=(MainActivity)getActivity();
                    mainActivity.barcode="";
                    return  true;
                }
                return false;
            }
        });*/

        dataBase = new RoomAppDatabase(getActivity()).getAppDatabase();

        db = dataBase.getStockTakeDAO();

        //db.deleteAll();

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        scanType = sp.getString("scanType", "");
        accountId = sp.getString("AccountId", "");
        warehouseID = sp.getInt("WarehouseID", 0);
        tenantID = sp.getInt("TenantID", 0);

        //btnClear.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);


        exceptionLoggerUtils = new ExceptionLoggerUtils();
        sound = new SoundUtils();
        common = new Common();
        errorMessages = new ErrorMessages();
        gson = new GsonBuilder().create();
        core = new WMSCoreMessage();
        soundUtils = new SoundUtils();
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

        // For Cipher Barcode reader
        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", true);
        getActivity().sendBroadcast(RTintent);
        this.filter = new IntentFilter();
        this.filter.addAction("sw.reader.decode.complete");
        getActivity().registerReceiver(this.myDataReceiver, this.filter);

        //For Honey well
        AidcManager.create(getActivity(), new AidcManager.CreatedCallback() {

            @Override
            public void onCreated(AidcManager aidcManager) {

                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();
                try {
                    barcodeReader.claim();
                    HoneyWellBarcodeListeners();

                } catch (ScannerUnavailableException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    //button Clicks
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.cvScanPallet:
                if (isContanierScanned) {

                    //ValidatePalletCode();
                    isContanierScanned = false;
                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
                    ivScanPallet.setImageResource(R.drawable.fullscreen_img);

                    Materialcode = "";
                    isRsnScanned = false;
                    cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                    ivScanSku.setImageResource(R.drawable.fullscreen_img);
                }
                break;

            case R.id.btnSubmit:

                List<StockTakeTable> insertedRecords = db.getAll();
                if (insertedRecords != null && insertedRecords.size() > 0) {
                    StockTakeDetails stockTakeDetails = null;
                    List<StockTakeDetails> stockTakeDetailsList = new ArrayList<>();
                    for (StockTakeTable record : insertedRecords) {
                        stockTakeDetails = new StockTakeDetails();
                        stockTakeDetails.setCartonCode(record.carton);
                        stockTakeDetails.setLocationCode(record.bin);
                        stockTakeDetails.setQuantity(record.qty);
                        stockTakeDetails.setMaterialCode(record.sku);
                        stockTakeDetailsList.add(stockTakeDetails);
                    }


                    SubmitData(stockTakeDetailsList);

                }

                break;

            default:
                break;
        }
    }

    public void SubmitData(List<StockTakeDetails> list) {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.StockTakeDTO, getContext());

            StockTake stockTake = new StockTake();
            stockTake.setStockTakeDetails(list);
            message.setEntityObject(stockTake);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.UpsertStockTake(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        ProgressDialogUtils.closeProgressDialog();
                        if (response.body() != null) {
                            if (response.body().equals("1")) {
                                clearFields();
                                common.showUserDefinedAlertType("Successfully posted ", getActivity(), getContext(), "Success");
                            } else {
                                common.showUserDefinedAlertType("Something went wrong please submit again", getActivity(), getContext(), "Error");
                            }
                        } else {
                            common.showUserDefinedAlertType("Something went wrong please submit again", getActivity(), getContext(), "Error");
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_02", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
        }
    }

    public void clearFields() {

        db.deleteAll();

        cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
        ivScanSku.setImageResource(R.drawable.fullscreen_img);

        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
        ivScanPallet.setImageResource(R.drawable.fullscreen_img);

        cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.locationColor));
        ivScanDock.setImageResource(R.drawable.fullscreen_img);

        etBin.setText("");
        etCarton.setText("");
        lblScannedSku.setText("");


    }


    @Override
    public void onBarcodeEvent(final BarcodeReadEvent barcodeReadEvent) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //update UI to reflect the data
                //List<String> list = new ArrayList<String>();
                //list.add("Barcode data: " + barcodeReadEvent.getBarcodeData());

                getScanner = barcodeReadEvent.getBarcodeData();
                ProcessScannedinfo(getScanner);


            }

        });
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {

    }

    @Override
    public void onTriggerEvent(TriggerStateChangeEvent triggerStateChangeEvent) {

    }

    public void HoneyWellBarcodeListeners() {

        barcodeReader.addTriggerListener(this);

        if (barcodeReader != null) {
            // set the trigger mode to client control
            barcodeReader.addBarcodeListener(this);
            try {
                barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE, BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
            } catch (UnsupportedPropertyException e) {
                // Toast.makeText(this, "Failed to apply properties", Toast.LENGTH_SHORT).show();
            }

            Map<String, Object> properties = new HashMap<String, Object>();
            // Set Symbologies On/Off
            properties.put(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_GS1_128_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_UPC_A_ENABLE, true);
            properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, true);
            // Set Max Code 39 barcode length
            properties.put(BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10);
            // Turn on center decoding
            properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
            // Enable bad read response
            properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, true);
            // Apply the settings
            barcodeReader.setProperties(properties);
        }

    }


    //Assigning scanned value to the respective fields
    public void ProcessScannedinfo(String scannedData) {

        if (ProgressDialogUtils.isProgressActive() || Common.isPopupActive()) {
            common.showUserDefinedAlertType(errorMessages.EMC_082, getActivity(), getContext(), "Warning");
            return;
        }

        if (scannedData != null && !Common.isPopupActive()) {

            if (!ProgressDialogUtils.isProgressActive()) {

               /* if (!isLocation) {
                    //ValidateLocation(scannedData);
                } else {
                    if (!isContanierScanned) {
                        //ValidatePallet(scannedData);
                    } else {
                        //ValiDateMaterial(scannedData);
                    }
                }*/


                if (ScanValidator.isBinScanned(scannedData)) {

                    etBin.setText(scannedData);
                    cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.white));
                    ivScanDock.setImageResource(R.drawable.check);

                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
                    ivScanPallet.setImageResource(R.drawable.fullscreen_img);
                    etCarton.setText("");

                    cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                    ivScanSku.setImageResource(R.drawable.fullscreen_img);
                    lblScannedSku.setText("");

                    if (db.getLocations() != null) {
                        List<String> binList = db.getLocations();
                        for (String bin : binList) {
                            String localBin = bin.split("-")[1] + bin.split("-")[2];
                            String scannedBin = scannedData.split("-")[1] + scannedData.split("-")[2];
                            if (!localBin.equals(scannedBin)) {
                                common.showUserDefinedAlertType("Please post previous records before proceeding", getActivity(), getContext(), "Warning");
                                cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.locationColor));
                                ivScanDock.setImageResource(R.drawable.fullscreen_img);
                                etBin.setText("");

                                return;
                            }
                        }
                    }

                } else if (ScanValidator.isContainerScanned(scannedData)) {
                    if (etBin.getText().toString().isEmpty()) {
                        common.showUserDefinedAlertType("Please scan bin", getActivity(), getContext(), "Error");
                        return;
                    }
                    etCarton.setText(scannedData);
                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                    ivScanPallet.setImageResource(R.drawable.check);
                } else {
                    if (etBin.getText().toString().isEmpty()) {
                        common.showUserDefinedAlertType(errorMessages.EMC_084, getActivity(), getContext(), "Error");
                        return;
                    }
                    if (etCarton.getText().toString().isEmpty()) {
                        common.showUserDefinedAlertType(errorMessages.EMC_085, getActivity(), getContext(), "Error");
                        return;
                    }

                    lblScannedSku.setText(scannedData);
                    cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.white));
                    ivScanSku.setImageResource(R.drawable.check);

                    db.insert(new StockTakeTable(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString(),"1"));

                    /*StockTakeTable previousRecord = db.getPreviousRecord(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString());
                    if (previousRecord != null) {
                        showDialog(getActivity(), previousRecord.qty, true);
                    } else {
                        showDialog(getActivity(), "", false);
                    }*/


                }

            } else {
                if (!Common.isPopupActive()) {
                    common.showUserDefinedAlertType(errorMessages.EMC_080, getActivity(), getContext(), "Error");
                }
                sound.alertWarning(getActivity(), getContext());
            }

        }
    }

    public void showDialog(Context mContext, String qty, final boolean isPreviousRecord) {
        final Dialog dialog = new Dialog(mContext);
        dialog.setContentView(R.layout.alert_dialog);
        btn_ok = (Button) dialog.findViewById(R.id.btn_ok);
        et_qty = (EditText) dialog.findViewById(R.id.et_qty);

        et_qty.setText(qty);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!et_qty.getText().toString().isEmpty()) {
                    if (!et_qty.getText().toString().equals("0")) {

                        if (isPreviousRecord) {
                            db.update(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString(), et_qty.getText().toString());
                        } else {
                            db.insert(new StockTakeTable(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString(), et_qty.getText().toString()));
                        }
                        dialog.dismiss();

                        /*if (db.getAll() != null) {
                            List<StockTakeTable> tables = db.getAll();
                            if (tables.size() > 0) {
                                //Toast.makeText(getActivity(), gson.toJson(tables).toString(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getActivity(), "No records found", Toast.LENGTH_SHORT).show();
                        }*/
                    } else {
                        common.showUserDefinedAlertType("Quantity should not 0 ", getActivity(), getContext(), "Error");
                    }
                } else {
                    common.showUserDefinedAlertType("Please enter quantity before proceeding", getActivity(), getContext(), "Error");
                }


            }
        });
        dialog.show();
    }


    // sending exception to the database
    public void logException() {
        try {

            String textFromFile = exceptionLoggerUtils.readFromFile(getActivity());

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Exception, getActivity());
            WMSExceptionMessage wmsExceptionMessage = new WMSExceptionMessage();
            wmsExceptionMessage.setWMSMessage(textFromFile);
            message.setEntityObject(wmsExceptionMessage);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.LogException(message);
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                ProgressDialogUtils.closeProgressDialog();
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                        try {

                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);


                        } catch (Exception ex) {

                            /*try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(),classCode,"002",getContext());

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            logException();*/


                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        ProgressDialogUtils.closeProgressDialog();
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
                    }
                });
            } catch (Exception ex) {
                ProgressDialogUtils.closeProgressDialog();
                // Toast.makeText(LoginActivity.this, ex.toString(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0003);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeReader != null) {
            // release the scanner claim so we don't get any scanner
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
            }
            // notifications while paused.
            barcodeReader.release();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                // Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show();
            }
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.stock_take_Title));
    }

    //Barcode scanner API
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (barcodeReader != null) {
            // unregister barcode event listener honeywell
            barcodeReader.removeBarcodeListener((BarcodeReader.BarcodeListener) this);

            // unregister trigger state change listener
            barcodeReader.removeTriggerListener((BarcodeReader.TriggerListener) this);
        }

        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", false);
        getActivity().sendBroadcast(RTintent);
        getActivity().unregisterReceiver(this.myDataReceiver);
        super.onDestroyView();
    }


}