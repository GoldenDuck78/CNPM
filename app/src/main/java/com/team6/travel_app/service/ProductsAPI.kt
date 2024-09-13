package com.team6.travel_app.service

import com.team6.travel_app.model.BaseClass
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface ProductsAPI {
    @GET("products?limit=100")
    fun getData(): Call<ResponseBody>

    @GET
    fun getCategorizedProduct(@Url text: String): Single<ResponseBody>

    @GET("category")
    fun getCategoryFromAPI(): Single<List<String>>

    @GET("smartphones")
    fun getSmartPhonesFromAPI(): Single<BaseClass>
}
