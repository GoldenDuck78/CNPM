package com.team6.travel_app.viewmodel

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.places.api.model.LocalDate
import com.team6.travel_app.adapter.CartAdapter
import com.team6.travel_app.data.Cart
import com.team6.travel_app.data.CartDatabase
import com.team6.travel_app.data.ImageDatabase
import com.team6.travel_app.data.ProductDatabase
import com.team6.travel_app.databinding.FragmentCartBinding
import com.team6.travel_app.model.Product
import com.team6.travel_app.model.TourBaseClass
import com.team6.travel_app.model.TourP
import com.team6.travel_app.service.ToursAPI
import com.team6.travel_app.view.MainActivity
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.team6.travel_app.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.round


class CartViewModel() : ViewModel() {
    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private lateinit var binding: FragmentCartBinding
    val cartList = MutableLiveData<ArrayList<Cart>>()
    val _cartList get() = cartList
    val isCartListLoading = MutableLiveData<Boolean>()
    val isErrorOccurred = MutableLiveData<Boolean>()
    val tempList = ArrayList<Cart>()
    var isCartListEmpty = MutableLiveData<Boolean>()
    private var _totalAmounth = MutableLiveData<Int>()
    val totalAmounth: LiveData<Int> get() = _totalAmounth
    private lateinit var cartAdapter: CartAdapter
    private var _product = MutableLiveData<Product?>()
    val product get() = _product

    /*init {
         cartDao = CartDatabase.invoke(context).cartDao()
    }*/

    fun getDataFromRoom(
        context: Context,
        binding: FragmentCartBinding,
        cartDatabase: CartDatabase
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            cartDatabase.cartDao().getAllEntities().forEach {
                tempList.add(it)
            }
        }
        cartList.value = tempList
        isCartListLoading.value = false
        isErrorOccurred.value = false
    }

    fun removeItemFromRoom(position: Int, cartDatabase: CartDatabase) =
        viewModelScope.launch(Dispatchers.IO) {
            isCartListEmpty.postValue(false)
            val request = cartDatabase.cartDao()
            println("before deletion the size is : ${request.rowCount()}")
            request.delete(position)
            calculateTotalAmount(cartDatabase)
            println("after deletion the size is : ${request.rowCount()}")

            if (request.rowCount() == 0) {
                isCartListEmpty.postValue(true)
            } else {
                isCartListEmpty.postValue(false)
            }
        }

    fun getIsCartListEmpty(): Boolean? {
        return isCartListEmpty.value
    }

    fun calculateTotalAmount(cartDatabase: CartDatabase) = viewModelScope.launch(Dispatchers.IO) {
        var total = 0
        _totalAmounth.postValue(0)
        val cartList = cartDatabase.cartDao().getAllEntities()
        cartList.forEach { cart ->
            println("the value of cartlist : $cartList")
            val discountedPrice = cart.price!! * (100 - cart.discountPercentage!!) / 100
            var price = round(discountedPrice).toInt()
            if (cart.isDeposited == 1){
                price = round(price * 0.9).toInt()
            }
            val quantity = cart.quantity
            total += quantity * price
        }
        println("the value of temp total : $total")
        _totalAmounth.postValue(total)
        println("the value of total Amounth : ${_totalAmounth.value}")

    }


    fun updateQuantity(increaseQuantity : Boolean, id: Int, database: CartDatabase, holder: CartAdapter.PlaceHolder, bd: FragmentCartBinding) = viewModelScope.launch(Dispatchers.IO) {
        var quantity =  database.cartDao().getQuantity(id)
        if (increaseQuantity) {
            database.cartDao().updateQuantity(holder.binding.product?.id!!, ++quantity)
        } else if (!increaseQuantity && quantity > 1) {
            database.cartDao().updateQuantity(holder.binding.product?.id!!, --quantity)
        }

        CoroutineScope(Dispatchers.IO).launch {
            var total = 0
            val cartList = database.cartDao().getAllEntities()
            cartList.forEach { cart ->
                val quantity = cart.quantity
                val price = cart.price
                total += quantity * price!!
            }

            withContext(Dispatchers.Main) {
                bd.totalAmount.text = total.toString() + " đ"
            }
        }

        val price = database.cartDao().getPrice(holder.binding.product?.id!!)
        CoroutineScope(Dispatchers.Main).launch {
            holder.binding.productQuantityEditText.setText(quantity.toString())
            println("the edit text set to $quantity")
            //holder.binding.textViewProductPrice.text = ""
            holder.binding.textViewOriginalPrice.text = (quantity * price).toString() + " đ"
        }
    }
    fun onViewClicked(context : Context, id : Int) {
        viewModelScope.launch() {
            Log.i(TAG, "getClickedEntity: $id")
            val images = ImageDatabase(context = context).imageDao().getRecord(id)
            val item = ProductDatabase(context).productDao().getRecord(id)
            val product1 = Product(item.id,item.title,item.description,item.price,item.discountPercentage,item.rating,item.stock,item.brand,item.category,item.thumbnail,images)
            _product.value = product1
            Log.i(TAG, "onViewClicked: the value of product is : $product1")
            Log.i(TAG, "onViewClicked: ${_product.value}")
            Log.i(TAG, "onViewClicked: ${product.value}")

        }


    }


    fun getTotalAmount(): LiveData<Int>{
        return _totalAmounth
    }

    fun postRequest(context: Context, database: CartDatabase) {
        val cusDb = CustomerDatabase.invoke(context).CustomerDao()
        val cusid = cusDb.getId()

        val retrofit = Retrofit.Builder()
            .baseUrl(MainActivity.TOUR_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ToursAPI::class.java)
        val cartList = database.cartDao().getAllEntities()

         CoroutineScope(Dispatchers.IO).launch {
            val completedRequests = mutableListOf<Deferred<Unit>>()
            cartList.forEach { cart ->
                val currentDate = Date()
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedDate = formatter.format(currentDate)

                val tourp = if (cart.isDeposited == 1) {
                    TourP(cart.quantity, formattedDate, cusid, cart.id, 1, cart.price, 0)
                } else {
                    TourP(cart.quantity, formattedDate, cusid, cart.id, 1)
                }

                val call = if (cart.isDeposited == 1) {
                    service.putData(tourp)
                } else {
                    service.createPost(tourp)
                }

                val request = async {
                    try {
                        val response = call.execute()
                        if (response.isSuccessful) {
                            println("Request successful for cart ID: ${cart.id}")
                        } else {
                            println("Request failed for cart ID: ${cart.id} with response code: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        println("Request error for cart ID: ${cart.id} - ${e.message}")
                    }
                }
                completedRequests.add(request)
            }
            completedRequests.awaitAll()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Payment processing complete", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
    fun postRequestDeposit(context: Context, database: CartDatabase,id:Int){
        val cusDb = CustomerDatabase.invoke(context).CustomerDao()
        val cusid = cusDb.getId()
        val retrofit = Retrofit
            .Builder()
            .baseUrl(MainActivity.TOUR_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(ToursAPI::class.java)
        val cart = database.cartDao().getById(id)
        val currentDate = Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = formatter.format(currentDate)
        val tourp = TourP(cart.quantity, formattedDate, cusid, cart.id, 1, cart.price,1)
        val call = service.createPost(tourp)
        call.enqueue(object : Callback<TourBaseClass> {
            override fun onFailure(call: Call<TourBaseClass>, t: Throwable) {
                t.printStackTrace()
            }
            override fun onResponse(
                call: Call<TourBaseClass>,
                response: Response<TourBaseClass>
            ) {

                print(response.message())
            }
        })
        CoroutineScope(Dispatchers.Main).launch {
//            Toast.makeText(context, "thanh toán thành công", Toast.LENGTH_SHORT).show()
        }

    }

}