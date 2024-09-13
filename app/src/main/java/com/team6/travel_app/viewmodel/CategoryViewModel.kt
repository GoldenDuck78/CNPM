package com.team6.travel_app.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.team6.travel_app.model.BaseClass
import com.team6.travel_app.model.Product
import com.team6.travel_app.service.ProductsAPI
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.observers.DisposableSingleObserver
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class CategoryViewModel(application: Application) : BaseViewModel(application) {

    private val disposable = CompositeDisposable()
    val categoryList = MutableLiveData<ArrayList<String>>()
    val categorizedProducts = MutableLiveData<ArrayList<Product>>()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://dummyjson.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

    private val api = retrofit.create(ProductsAPI::class.java)

    fun getCategoryFromAPI() {
        viewModelScope.launch {
            disposable.add(
                api.getCategoryFromAPI()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<List<String>>() {
                        override fun onSuccess(value: List<String>) {
                            categoryList.value = ArrayList(value)
                        }

                        override fun onError(e: Throwable) {
                            e.printStackTrace()
                            // Consider updating the UI to reflect the error
                        }
                    })
            )
        }
    }

    fun getCategorizedProductFromAPI(category: String) {
        val url = "products/category/$category"
        disposable.add(
            api.getCategorizedProduct(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<ResponseBody>() {
                    override fun onSuccess(response: ResponseBody) {
                        val baseClass = parseBaseClass(response.string())
                        categorizedProducts.value = baseClass?.products as ArrayList<Product>?
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        // Consider updating the UI to reflect the error
                    }
                })
        )
    }

    private fun parseBaseClass(json: String): BaseClass? {
        return try {
            val gson = Gson()
            gson.fromJson(json, BaseClass::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}
