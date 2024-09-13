package com.team6.travel_app.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.team6.travel_app.data.Image
import com.team6.travel_app.data.ImageDatabase
import com.team6.travel_app.data.ProductDatabase
import com.team6.travel_app.model.BaseClass
import com.team6.travel_app.model.CusBaseClass
import com.team6.travel_app.model.Product
import com.team6.travel_app.service.ProductsAPI
import com.team6.travel_app.utils.CustomSharedPreferences
import com.team6.travel_app.view.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeViewModel : ViewModel() {
    private val _products = MutableLiveData<ArrayList<Product>?>()
    val products get() = _products
    private var customPreferences = CustomSharedPreferences()
    private var refreshTime = 0.01 * 60 * 1000 * 1000 * 1000L


    // Các phương thức
    // getData
    fun getData(context: Context) {
        val updateTime = customPreferences.getTime()
        //Log.i(TAG, "$refreshTime  getData: "+(System.nanoTime() - updateTime!!))
        if (updateTime != null && updateTime != 0L && System.nanoTime() - updateTime < refreshTime) {
            getDataFromSQLite(context)

        }else{
            getDataFromAPI(context)
            //Toast.makeText(context,"tải tour thành công",Toast.LENGTH_LONG).show()
        }
    }

    // getDataFromApi
    private fun getDataFromAPI(context: Context) {
        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(MainActivity.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create API service
        val service = retrofit.create(ProductsAPI::class.java)

        // Create API call
        val call = service.getData()

        // Execute API call
        call.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Failed to load data: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                println(response.message())
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        println("Data received successfully")

                        val jsonResponse = responseBody.string()

                        // Parse the JSON response to a list of Product
                        val gson = GsonBuilder().create()
                        val productType = object : TypeToken<List<Product>>() {}.type
                        val products: List<Product> = gson.fromJson(jsonResponse, productType)
                        // Store products in SQLite and update LiveData
                        storeInSQLite(context, ArrayList(products))
                        _products.postValue(ArrayList(products))
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
    fun storeInSQLite(context: Context, products: ArrayList<Product>) {
        println("storeInSQLLite")
        viewModelScope.launch {
            val productDb = ProductDatabase.invoke(context).productDao()
            val imageDb = ImageDatabase.invoke(context).imageDao()
            productDb.deleteAllRecords()
            imageDb.deleteAllRecords()
            products.forEach {
                val aProduct = com.team6.travel_app.data.Product(it.id,it.title,it.description,it.price,it.discountPercentage,it.rating,it.stock,it.brand,it.category,
                it.thumbnail)
                productDb.insertEntity(aProduct)
            }
            products.forEach {
                it.images?.forEach { imageUrl ->
                    val anImage = Image(it.id, imageUrl)
                    imageDb.insert(anImage)
                }
            }
        }
        customPreferences.saveTime(System.nanoTime())
    }

    // Lấy dữ liệu từ SQLite
    private fun getDataFromSQLite(context: Context) {
        viewModelScope.launch {
            val productDb = ProductDatabase(context).productDao().getAllRecords()
            val imageDb = ImageDatabase(context).imageDao().getAllRecords()
            val products1 = arrayListOf<Product>()
            productDb.forEach{
                val imageList = it.id?.let { it1 ->
                    ImageDatabase.invoke(context).imageDao().getRecord(it1)
                }
                val list = Product(it.id,it.title,it.description,it.price,it.discountPercentage,it.rating,it.stock,it.brand,it.category,
                    it.thumbnail,imageList)
                products1.add(list)
            }
            Toast.makeText(context,"Products From SQLite",Toast.LENGTH_LONG).show()
            _products.postValue(products1)
        }
    }
}