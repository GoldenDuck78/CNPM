package com.team6.travel_app.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.team6.travel_app.data.CustomerDatabase
import com.team6.travel_app.model.CusBaseClass
import com.team6.travel_app.model.Customer
import com.team6.travel_app.model.CustomerResponse
import com.team6.travel_app.service.CusAPI
import com.team6.travel_app.utils.CustomSharedPreferences
import com.team6.travel_app.view.LogInActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class CustomerViewModel : ViewModel() {
    private val _cus = MutableLiveData<ArrayList<Customer>?>()
    val cus get() = _cus
    private var customPreferences = CustomSharedPreferences()
    private var refreshTime = 0.01 * 60 * 1000 * 1000 * 1000L
    var rowCount = 0
    // Các phương thức
// getData
    fun getData(context: Context, acc: String, pass:String) {
        val updateTime = customPreferences.getTime()
        if (updateTime != null && updateTime != 0L && System.nanoTime() - updateTime < refreshTime) {
            print("ok")
        }else{
            getDataFromAPI(context, acc, pass)

        }
    }


    fun message(context: Context) {
        Toast.makeText(context,"tài khoản hoặc mật khẩu sai",Toast.LENGTH_SHORT).show()
    }

    private fun getDataFromAPI(context: Context, acc: String, pass: String) {
        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(LogInActivity.CUS_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create API service
        val service = retrofit.create(CusAPI::class.java)

        // Create API call
        val call = service.getData(acc, pass)

        // Execute API call
        call.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Print stack trace and show a Toast message
                t.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Failed to load data: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        // Convert response body to a string
                        val jsonResponse = responseBody.string()
                        print(jsonResponse)
                        // Parse the JSON response to CusBaseClass
                        val gson = GsonBuilder().create()
                        val baseClass = gson.fromJson(jsonResponse, CusBaseClass::class.java)

                        // Handle the parsed data
                        baseClass.customer?.let { customers ->
                            storeInSQLite(context, ArrayList(customers))
                            _cus.postValue(ArrayList(customers))
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Failed to load data: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Luu vào SQLite
    fun storeInSQLite(context: Context, cus: ArrayList<Customer>) {
        println("storeInSQLLite")
        viewModelScope.launch {
            val cusDb = CustomerDatabase.invoke(context).CustomerDao()
            cusDb.deleteAllRecords()
            rowCount =0
            cus.forEach {
                val aCus = com.team6.travel_app.data.Customer(it.id,it.password,it.cusName,it.email,it.phoneNumber,it.address)
                cusDb.insertEntity(aCus)
                rowCount+=1
                Toast.makeText(context,"Đăng nhập thành công",Toast.LENGTH_SHORT).show()
            }
//            Toast.makeText(context,"Đăng nhập thành công",Toast.LENGTH_SHORT).show()
        }

//        customPreferences.saveTime(System.nanoTime())
    }

    fun postRequest(context: Context, account: String, password: String, name: String, email: String, phone: String, address: String) {
        // Khởi tạo Retrofit
        val gson = GsonBuilder()
            .setLenient()
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl(LogInActivity.CUS_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val service = retrofit.create(CusAPI::class.java)

        // Tạo đối tượng Customer
        val cust = Customer("0", password, name, email, phone, address)

        // Gọi API
        val call = service.createPost(cust)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Đăng ký thất bại: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseBody = response.body()?.string()
                val errorBody = response.errorBody()?.string()
                println("Response body: $responseBody")
                println("Error body: $errorBody")

                if (response.isSuccessful) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Đăng ký thất bại: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


// Lấy dữ liệu từ SQLite
}