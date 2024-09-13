package com.team6.travel_app.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team6.travel_app.data.*
import com.team6.travel_app.utils.CustomSharedPreferences
import kotlinx.coroutines.*
import com.team6.travel_app.data.TourDatabase
import com.team6.travel_app.model.Tour
import com.team6.travel_app.model.TourBaseClass
import com.team6.travel_app.service.ToursAPI
import com.team6.travel_app.view.MainActivity
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TourViewModel : ViewModel() {
    private var _isListEmpty = MutableLiveData<Boolean>()
    val isListEmpty: LiveData<Boolean> get() = _isListEmpty
    private var _isLoading = MutableLiveData<Boolean>()
    val isLoading get() = _isLoading
    //đã có s
    private var _tours2 = MutableLiveData<List<com.team6.travel_app.data.Tour>>()
    val tours2 get() = _tours2
    // chưa xác định
    private var _tour = MutableLiveData<Tour>()
    val product get() =  _tour

    private val _tours = MutableLiveData<ArrayList<Tour>?>()
    val tours get() = _tours

    private lateinit var customerDatabase: CustomerDatabase

    private var customPreferences = CustomSharedPreferences()
    private var refreshTime = 0.01 * 60 * 1000 * 1000 * 1000L




    fun removeItemFromRoom(position: Int, context: Context) =
        viewModelScope.launch(Dispatchers.IO) {
            val tourDatabase = TourDatabase.invoke(context)
            tourDatabase.tourDao().delete(position)
            if (tourDatabase.tourDao().rowCount() == 0) {
                _isListEmpty.postValue(true)
            }
        }
    fun getDataFromRoom(context : Context) =
        viewModelScope.launch(Dispatchers.IO) {
            val db = TourDatabase.invoke(context = context)
            _tours2.postValue(db.tourDao().getAllEntities())
    }


    //done
    fun getData(context: Context) {
        val updateTime = customPreferences.getTime()
        //Log.i(TAG, "$refreshTime  getData: "+(System.nanoTime() - updateTime!!))
        if (updateTime != null && updateTime != 0L && System.nanoTime() - updateTime < refreshTime) {
            //getDataFromSQLite(context)
            //Toast.makeText(context,"làm mới thành công...",Toast.LENGTH_SHORT).show()
        }else{
            getDataFromAPI(context)
            //Toast.makeText(context,"làm mới thành công",Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDataFromAPI(context: Context){
        val customerDatabase = CustomerDatabase.invoke(context).CustomerDao()
        CoroutineScope(Dispatchers.IO).launch {
            val cus_id = customerDatabase.getId()
            val retrofit = Retrofit
                .Builder()
                .baseUrl(MainActivity.TOUR_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service = retrofit.create(ToursAPI::class.java)
            val call = service.getData(cus_id)
            call.enqueue(object : Callback<TourBaseClass> {
                override fun onFailure(call: Call<TourBaseClass>, t: Throwable) {
                    t.printStackTrace()
                }

                override fun onResponse(
                    call: Call<TourBaseClass>,
                    response: Response<TourBaseClass>
                ) {
                    response.body()?.let {
                        println("onResponse is worked")
                        it.tours?.let { it1 -> storeInSQLite(context, it1 as ArrayList<Tour>) }
                        _tours.postValue(it.tours as ArrayList<Tour>?)
                    }
                }
            })
        }
        //Toast.makeText(context,"làm mới thành công....",Toast.LENGTH_SHORT).show()
    }

    // Luu vào SQLite
    fun storeInSQLite(context: Context, tours: ArrayList<Tour>) {
        println("store In DB")
        viewModelScope.launch {
            val tourDb = TourDatabase.invoke(context).tourDao()
            tourDb.deleteAllRecords()
            tours.forEach {
                val aTour = com.team6.travel_app.data.Tour(it.id,it.title,it.description,it.price,it.quantity,it.stock,it.discountPercentage,it.brand,it.category,
                    it.thumbnail, it.registrationDate,it.images, it.isChecked)
                tourDb.insertEntity(aTour)
            }
        }
        customPreferences.saveTime(System.nanoTime())

    }


}