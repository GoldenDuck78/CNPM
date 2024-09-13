package com.team6.travel_app.view

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import com.team6.travel_app.R
import com.team6.travel_app.data.CartDatabase
import com.team6.travel_app.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import vn.momo.momo_partner.AppMoMoLib

class PaymentActivity : AppCompatActivity() {
    private var amount = "10000" // Default amount
    private val fee = "0"
    private val merchantName = "TL Travel"
    private val merchantCode = "MOMOC2IC20220510"
    private val merchantNameLabel = "TL Travel"
    private val description = "Thanh toán dịch vụ ABC"
    private lateinit var tvMessage: TextView
    private lateinit var btnBack : Button
    private lateinit var viewModel: CartViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        tvMessage = findViewById(R.id.tvMessage)
        btnBack = findViewById(R.id.btnBack)
        amount = intent.getStringExtra("amount")!!
        AppMoMoLib.getInstance().setEnvironment(AppMoMoLib.ENVIRONMENT.DEVELOPMENT) // Or PRODUCTION
        requestPayment()
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun requestPayment() {
        AppMoMoLib.getInstance().setAction(AppMoMoLib.ACTION.PAYMENT)
        AppMoMoLib.getInstance().setActionType(AppMoMoLib.ACTION_TYPE.GET_TOKEN)

        val eventValue: MutableMap<String, Any> = HashMap()
        // Client Required
        eventValue["merchantname"] = merchantName
        eventValue["merchantcode"] = merchantCode
        eventValue["amount"] = amount
        eventValue["orderId"] = "orderId123456789"
        eventValue["orderLabel"] = "Mã đơn hàng"
        // Client Optional - bill info
        eventValue["merchantnamelabel"] = merchantNameLabel
        eventValue["fee"] = fee
        eventValue["description"] = description

        // Client extra data
        eventValue["requestId"] = "$${System.currentTimeMillis()}"
        eventValue["partnerCode"] = merchantCode

        // Example extra data
        val objExtraData = JSONObject()
        try {
            objExtraData.put("site_code", "008")
            objExtraData.put("site_name", "CGV Cresent Mall")
            objExtraData.put("screen_code", 0)
            objExtraData.put("screen_name", "Special")
            objExtraData.put("movie_name", "Kẻ Trộm Mặt Trăng 3")
            objExtraData.put("movie_format", "2D")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        eventValue["extraData"] = objExtraData.toString()

        AppMoMoLib.getInstance().requestMoMoCallBack(this, eventValue)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppMoMoLib.getInstance().REQUEST_CODE_MOMO && resultCode == RESULT_OK) {
            data?.let {
                when (it.getIntExtra("status", -1)) {
                    0 -> {
                        // TOKEN IS AVAILABLE
                        val token = it.getStringExtra("data")
                        val phoneNumber = it.getStringExtra("phonenumber")
                        tvMessage.text = "Thanh toán thành công !"
                        // TODO: send phoneNumber & token to your server side to process payment with MoMo server

                    }
                    1, 2 -> {
                        // TOKEN FAIL
                        val message = it.getStringExtra("message") ?: "Thất bại"
                        tvMessage.text = "Thanh toán thất  bại !"
                    }
                    else -> {
                        // TOKEN FAIL
//                        tvMessage.text = getString(R.string.not_receive_info)
                    }
                }
            } ?: run {
//                tvMessage.text = getString(R.string.not_receive_info)
            }
        } else {
//            tvMessage.text = getString(R.string.not_receive_info_err)
        }
    }
}
