package com.team6.travel_app.service

import com.team6.travel_app.model.BaseClass
import com.team6.travel_app.model.CusBaseClass
import com.team6.travel_app.model.Customer
import com.team6.travel_app.model.CustomerResponse
import com.team6.travel_app.model.Tour
import com.team6.travel_app.model.TourBaseClass
import com.team6.travel_app.model.TourP
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Query
import retrofit2.http.Url

interface CusAPI {
    @GET("customer") //extension
    fun getData(
        @Query("email") email: String,
        @Query("pass") password: String
    ) : Call<ResponseBody>

    @POST("customer")
    fun createPost(
        @Body customer: Customer
    ):Call<ResponseBody>
}